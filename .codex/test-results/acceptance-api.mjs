import fs from "node:fs";

const BASE = "http://127.0.0.1:8080";
const PASSWORD = "husa123456";
const results = [];

function record(id, ok, detail = "") {
  results.push({ id, ok, detail });
  console.log(`${ok ? "PASS" : "FAIL"} ${id} ${detail}`);
}

async function request(path, { method = "GET", token, data } = {}) {
  const headers = {};
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (data !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  const response = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: data === undefined ? undefined : JSON.stringify(data),
  });
  const contentType = response.headers.get("content-type") || "";
  let body;
  if (contentType.includes("application/json")) {
    body = await response.json();
  } else {
    body = Buffer.from(await response.arrayBuffer());
  }
  return { status: response.status, body, contentType };
}

async function login(userNo) {
  const challenge = await request("/auth/captcha/challenge", {
    method: "POST",
    data: { scene: "LOGIN_PASSWORD", target: userNo },
  });
  const ticket = await request("/auth/captcha/ticket", {
    method: "POST",
    data: {
      challengeId: challenge.body.data.challengeId,
      scene: "LOGIN_PASSWORD",
      target: userNo,
    },
  });
  const response = await request("/auth/login/password", {
    method: "POST",
    data: {
      userNo,
      password: PASSWORD,
      captchaTicket: ticket.body.data.captchaTicket,
    },
  });
  if (response.body.code !== 200) {
    throw new Error(`${userNo} 登录失败: ${JSON.stringify(response.body)}`);
  }
  return response.body.data.token;
}

function uuid() {
  return `acceptance-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

async function orderDetail(token, orderId) {
  const response = await request(`/repair/orders/${orderId}`, { token });
  return response.body.data.order;
}

async function uploadOrderImage(token, orderId) {
  const form = new FormData();
  const image = fs.readFileSync(new URL("../test/image.png", import.meta.url));
  form.append("file", new Blob([image], { type: "image/png" }), "image.png");
  const response = await fetch(`${BASE}/repair/orders/${orderId}/attachments`, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}` },
    body: form,
  });
  return response.json();
}

async function createAndSubmit(token, location, suffix, repairType = "NORMAL", assetId = null) {
  const created = await request("/repair/orders", {
    method: "POST",
    token,
    data: {
      requestId: uuid(),
      repairType,
      assetId,
      title: `完整验收-${suffix}`,
      description: `完整功能验收测试 ${suffix}`,
      categoryId: 1,
      campusId: location.campusId,
      buildingId: location.buildingId,
      floor: "3",
      room: "301",
      locationDetail: suffix,
      contactPhone: "13800000002",
    },
  });
  if (created.body.code !== 200) {
    throw new Error(`创建工单失败: ${JSON.stringify(created.body)}`);
  }
  const order = created.body.data;
  const attachment = await uploadOrderImage(token, order.orderId);
  if (attachment.code !== 200) {
    throw new Error(`上传现场图片失败: ${JSON.stringify(attachment)}`);
  }
  const submitted = await request(`/repair/orders/${order.orderId}/submit`, {
    method: "POST",
    token,
    data: { version: order.version },
  });
  if (submitted.body.code !== 200) {
    throw new Error(`提交工单失败: ${JSON.stringify(submitted.body)}`);
  }
  return { created: order, submitted };
}

async function run() {
  const tokens = {
    admin: await login("admin"),
    student: await login("student"),
    teacher: await login("teacher"),
    repairer: await login("repairer"),
  };
  record("AUTH-DEFAULT-ROLES", Object.values(tokens).every(Boolean), "四类默认账号登录成功");

  const unauthorized = await request("/admin/users");
  record("ROLE-UNAUTH-401", unauthorized.status === 401, `status=${unauthorized.status}`);

  const studentAdmin = await request("/admin/users", { token: tokens.student });
  record("ROLE-STUDENT-ADMIN-403", studentAdmin.status === 403, `status=${studentAdmin.status}`);

  const adminRepairer = await request("/repair/repairer/availability", { token: tokens.admin });
  record("ROLE-ADMIN-NOT-REPAIRER", adminRepairer.status === 403, `status=${adminRepairer.status}`);

  const users = await request("/admin/users?pageNum=1&pageSize=20", { token: tokens.admin });
  const adminUser = users.body.data.records.find((item) => item.userNo === "admin");
  record("ADMIN-LIST-USERS", users.body.code === 200 && !!adminUser, `total=${users.body.data.total}`);

  const createAdmin = await request("/admin/users", {
    method: "POST",
    token: tokens.admin,
    data: {
      userNo: `admin-${Date.now()}`,
      realName: "越权管理员",
      nickName: "越权管理员",
      roleCode: "ADMIN",
      phoneNumber: `139${String(Date.now()).slice(-8)}`,
      initialPassword: "Test123456!",
    },
  });
  record("ADMIN-CANNOT-CREATE-ADMIN", createAdmin.status === 403, `status=${createAdmin.status}`);

  const updateAdmin = await request(`/admin/users/${adminUser.userId}`, {
    method: "PUT",
    token: tokens.admin,
    data: { nickName: "不应修改" },
  });
  record("ADMIN-CANNOT-UPDATE-ADMIN", updateAdmin.status === 403, `status=${updateAdmin.status}`);

  const pauseWithoutReason = await request("/repair/repairer/availability", {
    method: "PUT",
    token: tokens.repairer,
    data: { acceptingState: "PAUSED" },
  });
  record("REPAIRER-PAUSE-REQUIRES-REASON", pauseWithoutReason.body.code !== 200, `code=${pauseWithoutReason.body.code}`);

  const pause = await request("/repair/repairer/availability", {
    method: "PUT",
    token: tokens.repairer,
    data: { acceptingState: "PAUSED", pauseReason: "完整验收暂停测试" },
  });
  const resume = await request("/repair/repairer/availability", {
    method: "PUT",
    token: tokens.repairer,
    data: { acceptingState: "AVAILABLE" },
  });
  record("REPAIRER-PAUSE-RESUME", pause.body.code === 200 && resume.body.code === 200);

  const locations = await request("/repair/locations/options", { token: tokens.student });
  const campus = locations.body.data[0];
  const location = { campusId: campus.campusId, buildingId: campus.buildings[0].buildingId };
  record("LOCATION-OPTIONS", !!location.campusId && !!location.buildingId);

  const concurrentOrder = await createAndSubmit(tokens.student, location, "并发接单");
  const submittedOrder = concurrentOrder.submitted.body.data.order;
  const accepts = await Promise.all([
    request(`/repair/orders/${submittedOrder.orderId}/accept`, {
      method: "POST",
      token: tokens.repairer,
      data: { version: submittedOrder.version },
    }),
    request(`/repair/orders/${submittedOrder.orderId}/accept`, {
      method: "POST",
      token: tokens.repairer,
      data: { version: submittedOrder.version },
    }),
  ]);
  const acceptSuccessCount = accepts.filter((item) => item.body.code === 200).length;
  record("ORDER-CONCURRENT-ACCEPT", acceptSuccessCount === 1, `success=${acceptSuccessCount}`);

  let detail = await orderDetail(tokens.repairer, submittedOrder.orderId);
  record("ORDER-AFTER-ACCEPT", detail.status === 3, `status=${detail.status}`);

  const illegalConfirm = await request(`/repair/orders/${submittedOrder.orderId}/confirm`, {
    method: "POST",
    token: tokens.teacher,
    data: { version: detail.version },
  });
  record("ORDER-OTHER-REPORTER-CANNOT-CONFIRM", illegalConfirm.body.code !== 200, `code=${illegalConfirm.body.code}`);

  const reporterComment = await request(`/repair/orders/${submittedOrder.orderId}/comments`, {
    method: "POST",
    token: tokens.student,
    data: { content: "学生验收评论" },
  });
  const repairerComment = await request(`/repair/orders/${submittedOrder.orderId}/comments`, {
    method: "POST",
    token: tokens.repairer,
    data: { content: "维修师傅验收评论" },
  });
  record("ORDER-COMMENTS", reporterComment.body.code === 200 && repairerComment.body.code === 200);

  const started = await request(`/repair/orders/${submittedOrder.orderId}/start`, {
    method: "POST",
    token: tokens.repairer,
    data: { version: detail.version },
  });
  detail = await orderDetail(tokens.repairer, submittedOrder.orderId);
  record("ORDER-START", started.body.code === 200 && detail.status === 4, `status=${detail.status}`);

  const result = await request(`/repair/orders/${submittedOrder.orderId}/result`, {
    method: "POST",
    token: tokens.repairer,
    data: { version: detail.version, description: "完整验收维修完成" },
  });
  detail = await orderDetail(tokens.student, submittedOrder.orderId);
  record("ORDER-RESULT", result.body.code === 200 && detail.status === 5, `status=${detail.status}`);

  const confirmed = await request(`/repair/orders/${submittedOrder.orderId}/confirm`, {
    method: "POST",
    token: tokens.student,
    data: { version: detail.version },
  });
  detail = await orderDetail(tokens.student, submittedOrder.orderId);
  record("ORDER-CONFIRM", confirmed.body.code === 200 && detail.status === 7, `status=${detail.status}`);

  const evaluation = await request(`/repair/orders/${submittedOrder.orderId}/evaluation`, {
    method: "POST",
    token: tokens.student,
    data: { star: 5, content: "完整验收评价" },
  });
  const repeatedEvaluation = await request(`/repair/orders/${submittedOrder.orderId}/evaluation`, {
    method: "POST",
    token: tokens.student,
    data: { star: 4, content: "重复评价" },
  });
  record("ORDER-EVALUATION-ONCE", evaluation.body.code === 200 && repeatedEvaluation.body.code !== 200);

  const dispatchOrder = await createAndSubmit(tokens.teacher, location, "管理员派单");
  const dispatchSubmitted = dispatchOrder.submitted.body.data.order;
  const candidates = await request(`/admin/orders/${dispatchSubmitted.orderId}/dispatch-candidates`, { token: tokens.admin });
  const candidate = candidates.body.data[0];
  const dispatch = await request(`/admin/orders/${dispatchSubmitted.orderId}/dispatch`, {
    method: "POST",
    token: tokens.admin,
    data: {
      repairerId: candidate.userId,
      dispatchNote: "完整验收管理员派单",
      version: dispatchSubmitted.version,
    },
  });
  const dispatchedDetail = await orderDetail(tokens.admin, dispatchSubmitted.orderId);
  record(
    "ADMIN-DISPATCH",
    dispatch.body.code === 200 && dispatchedDetail.status === 3,
    `dispatch=${JSON.stringify(dispatch.body)} status=${dispatchedDetail.status}`,
  );

  const categoryName = `验收资产分类${Date.now()}`;
  const assetCategory = await request("/asset/categories", {
    method: "POST",
    token: tokens.admin,
    data: { categoryName, status: 0 },
  });
  const duplicateCategory = await request("/asset/categories", {
    method: "POST",
    token: tokens.admin,
    data: { categoryName, status: 0 },
  });
  record("ASSET-CATEGORY-UNIQUE", assetCategory.body.code === 200 && duplicateCategory.body.code !== 200);

  const asset = await request("/assets", {
    method: "POST",
    token: tokens.admin,
    data: {
      assetName: "完整验收投影仪",
      assetCategoryId: assetCategory.body.data.assetCategoryId,
      campusId: location.campusId,
      buildingId: location.buildingId,
      floor: "3",
      room: "301",
      locationDetail: "讲台上方",
      description: "完整验收资产",
      purchaseDate: "2026-01-01",
    },
  });
  record("ASSET-CREATE", asset.body.code === 200, `assetId=${asset.body.data?.assetId}`);

  const studentCreateAsset = await request("/assets", {
    method: "POST",
    token: tokens.student,
    data: { assetName: "越权资产" },
  });
  record("ASSET-STUDENT-CANNOT-CREATE", studentCreateAsset.status === 403, `status=${studentCreateAsset.status}`);

  const assetOrder = await createAndSubmit(tokens.student, location, "资产报修", "ASSET", asset.body.data.assetId);
  const assetOrder2 = await createAndSubmit(tokens.teacher, location, "重复资产报修", "ASSET", asset.body.data.assetId).catch((error) => ({ error }));
  record("ASSET-ONLY-ONE-ACTIVE-ORDER", !!assetOrder.submitted && !!assetOrder2.error, assetOrder2.error?.message || "");

  const suggestion = await request("/repair/suggestions", {
    method: "POST",
    token: tokens.repairer,
    data: {
      category: "REPAIR_PROCESS",
      title: "完整验收建议",
      content: "建议优化完整验收流程中的筛选展示。",
    },
  });
  const studentSuggestion = await request("/repair/suggestions", {
    method: "POST",
    token: tokens.student,
    data: { category: "OTHER", title: "越权建议", content: "不应成功" },
  });
  const handled = await request(`/repair/suggestions/${suggestion.body.data.suggestionId}/handle`, {
    method: "POST",
    token: tokens.admin,
    data: { status: "ACCEPTED", adminReply: "完整验收已采纳" },
  });
  record("SUGGESTION-ROLE-AND-HANDLE", suggestion.body.code === 200 && studentSuggestion.status === 403 && handled.body.code === 200);

  const studentAi = await request("/ai/assistant/chat", {
    method: "POST",
    token: tokens.student,
    data: { message: "查看统计" },
  });
  const repairerAi = await request("/ai/assistant/chat", {
    method: "POST",
    token: tokens.repairer,
    data: { message: "查看我的工作概览" },
  });
  record(
    "AI-ROLE-BOUNDARY",
    studentAi.body.code !== 200 && repairerAi.body.code === 200,
    `student=${JSON.stringify(studentAi.body)} repairer=${JSON.stringify(repairerAi.body)}`,
  );

  const notifications = await request("/notifications?pageNum=1&pageSize=100", { token: tokens.student });
  const readAll = await request("/notifications/read-all", { method: "POST", token: tokens.student, data: {} });
  const unread = await request("/notifications/unread-state", { token: tokens.student });
  record("NOTIFICATION-READ-ALL", notifications.body.code === 200 && readAll.body.code === 200 && unread.body.data.hasUnread === false);

  const dashboard = await request("/admin/repair-dashboard", { token: tokens.admin });
  const managementStatistics = await request("/admin/statistics/management?rangeType=LAST_30_DAYS", { token: tokens.admin });
  record("ADMIN-DASHBOARD-STATISTICS", dashboard.body.code === 200 && managementStatistics.body.code === 200);

  const exportOrders = await request("/admin/orders/export", { token: tokens.admin });
  record("ADMIN-ORDER-EXPORT", exportOrders.status === 200 && exportOrders.contentType.includes("spreadsheetml"), `bytes=${exportOrders.body.length}`);

  const failed = results.filter((item) => !item.ok);
  fs.writeFileSync(
    new URL("./acceptance-api-result.json", import.meta.url),
    JSON.stringify({ total: results.length, passed: results.length - failed.length, failed: failed.length, results }, null, 2),
    "utf8",
  );
  console.log(`SUMMARY total=${results.length} passed=${results.length - failed.length} failed=${failed.length}`);
  if (failed.length > 0) {
    process.exitCode = 1;
  }
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});
