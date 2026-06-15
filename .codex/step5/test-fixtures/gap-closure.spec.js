/**
 * Step5 Gap 收尾 E2E — 仅覆盖本次新增/补齐功能
 * 账号：admin / student / teacher / repairer，密码：husa123456
 * 运行：npx playwright test gap-closure.spec.js --config=playwright.gap.config.js
 */
const { test, expect } = require("@playwright/test");

const BASE = process.env.E2E_BASE_URL || "http://127.0.0.1:5173";
const API = process.env.E2E_API_URL || "http://127.0.0.1:8080";
const PASSWORD = "husa123456";

async function apiLogin(request, userNo) {
  const ch = await request.post(`${API}/auth/captcha/challenge`, {
    data: { scene: "LOGIN_PASSWORD", target: userNo },
  });
  expect(ch.ok()).toBeTruthy();
  const challengeId = (await ch.json()).data?.challengeId;
  const tk = await request.post(`${API}/auth/captcha/ticket`, {
    data: { challengeId, scene: "LOGIN_PASSWORD", target: userNo },
  });
  const ticket = (await tk.json()).data?.captchaTicket;
  const login = await request.post(`${API}/auth/login/password`, {
    data: { userNo, password: PASSWORD, captchaTicket: ticket },
  });
  const loginJson = await login.json();
  expect(loginJson.code).toBe(200);
  return loginJson.data.token;
}

async function uiLogin(page, userNo) {
  await page.context().clearCookies();
  await page.goto(`${BASE}/login`);
  await page.waitForTimeout(1200);
  await page.getByPlaceholder("请输入学号或工号").fill(userNo);
  await page.getByRole("button", { name: "验证", exact: true }).click();
  await expect(page.getByRole("button", { name: "已验证", exact: true })).toBeVisible();
  await page.getByPlaceholder("请输入密码").fill(PASSWORD);
  await page.getByRole("button", { name: "进入系统" }).click();
  await page.waitForURL((url) => !url.pathname.includes("/login"), { timeout: 20000 });
}

async function findDispatchableOrderId(request, token) {
  const headers = { Authorization: `Bearer ${token}` };
  for (const status of [1, 2]) {
    const resp = await request.get(`${API}/repair/orders?pageNum=1&pageSize=10&status=${status}`, { headers });
    const records = (await resp.json()).data?.records || [];
    if (records.length) {
      return records[0].orderId;
    }
  }
  const resp = await request.get(`${API}/repair/orders?pageNum=1&pageSize=20`, { headers });
  const records = (await resp.json()).data?.records || [];
  const match = records.find((row) => [1, 2].includes(row.status));
  return match?.orderId;
}

test.describe("Step5 gap — 派单分析", () => {
  test("管理员可调用派单 AI 分析 API", async ({ request }) => {
    const token = await apiLogin(request, "admin");
    const orderId = await findDispatchableOrderId(request, token);
    test.skip(!orderId, "暂无待匹配/待接单工单");
    const headers = { Authorization: `Bearer ${token}` };
    const resp = await request.post(`${API}/admin/repair-orders/ai/${orderId}/dispatch-analysis`, { headers });
    const json = await resp.json();
    expect(json.code).toBe(200);
    expect(json.data?.analysisText).toBeTruthy();
  });

  test("管理员工单列表显示 AI 辅助列并可打开分析弹窗", async ({ page, request }) => {
    await apiLogin(request, "admin");
    await uiLogin(page, "admin");
    await page.goto(`${BASE}/admin/orders?status=2`);
    await page.waitForTimeout(2000);
    await expect(page.getByText("AI 辅助").first()).toBeVisible();
    const aiBtn = page.getByRole("button", { name: "AI 分析" }).first();
    await expect(aiBtn).toBeVisible();
    await aiBtn.click();
    await expect(page.getByText("AI 派单分析").first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText("不推荐或排序具体维修师傅").first()).toBeVisible();
  });

  test("工单详情派单弹窗内可主动触发 AI 分析", async ({ page, request }) => {
    const token = await apiLogin(request, "admin");
    const orderId = await findDispatchableOrderId(request, token);
    test.skip(!orderId, "暂无待匹配/待接单工单");

    await uiLogin(page, "admin");
    await page.goto(`${BASE}/repair/detail/${orderId}`);
    await page.waitForTimeout(2000);
    await page.getByRole("button", { name: "手动派单" }).click();
    await expect(page.getByText("AI 派单辅助").first()).toBeVisible();
    await page.locator(".dispatch-ai-block").getByRole("button", { name: "AI 分析" }).click();
    await page.waitForTimeout(4000);
    await expect(page.locator(".dispatch-ai-block .ai-analysis-text, .dispatch-ai-block .sub-text").first()).toBeVisible();
  });
});

test.describe("Step5 gap — 疑似重复详情", () => {
  test("管理员可加载疑似重复详情（含判定理由字段）", async ({ request }) => {
    const token = await apiLogin(request, "admin");
    const headers = { Authorization: `Bearer ${token}` };
    const orders = await request.get(`${API}/repair/orders?pageNum=1&pageSize=20&suspectedDuplicate=1`, { headers });
    const records = (await orders.json()).data?.records || [];
    test.skip(!records.length, "暂无疑似重复工单");
    const orderId = records[0].orderId;
    const resp = await request.get(`${API}/admin/repair-orders/ai/${orderId}/duplicate-detail`, { headers });
    const json = await resp.json();
    expect(json.code).toBe(200);
    expect(json.data?.suspectedDuplicate).toBe(true);
    expect(Array.isArray(json.data?.links)).toBe(true);
  });
});

test.describe("Step5 gap — AI 助手导出与语义搜索", () => {
  test("管理员自然语言导出预览返回结构化字段", async ({ request }) => {
    const token = await apiLogin(request, "admin");
    const headers = { Authorization: `Bearer ${token}` };
    const resp = await request.post(`${API}/ai/assistant/chat`, {
      headers,
      data: { message: "导出最近30天状态为待匹配的工单" },
    });
    const json = await resp.json();
    expect(json.code).toBe(200);
    const preview = json.data?.exportPreview;
    expect(preview).toBeTruthy();
    expect(preview.exportType).toBeTruthy();
    expect(preview.estimatedCount).not.toBeNull();
  });

  test("管理员可语义搜索历史工单", async ({ request }) => {
    const token = await apiLogin(request, "admin");
    const headers = { Authorization: `Bearer ${token}` };
    const resp = await request.post(`${API}/ai/assistant/chat`, {
      headers,
      data: { message: "搜索历史工单：教室灯不亮" },
    });
    const json = await resp.json();
    expect(json.code).toBe(200);
    expect(json.data?.orderSearchResult || json.data?.replyText).toBeTruthy();
  });

  test("维修师傅可语义搜索资产，学生不可使用 AI 助手", async ({ request }) => {
    const repairerToken = await apiLogin(request, "repairer");
    const assetResp = await request.post(`${API}/ai/assistant/chat`, {
      headers: { Authorization: `Bearer ${repairerToken}` },
      data: { message: "搜索资产：投影仪" },
    });
    expect((await assetResp.json()).code).toBe(200);

    const studentToken = await apiLogin(request, "student");
    const forbidden = await request.post(`${API}/ai/assistant/chat`, {
      headers: { Authorization: `Bearer ${studentToken}` },
      data: { message: "查看统计" },
    });
    expect((await forbidden.json()).code).not.toBe(200);
  });
});

test.describe("Step5 gap — 建议相似提醒", () => {
  test("维修师傅提交前可检测相似建议且不阻断接口", async ({ request }) => {
    const token = await apiLogin(request, "repairer");
    const headers = { Authorization: `Bearer ${token}` };
    const resp = await request.post(`${API}/repair/suggestions/similarity-check`, {
      headers,
      data: {
        category: "PROCESS",
        title: "优化接单流程建议",
        content: "建议在待接单列表增加筛选条件，方便快速定位本校区工单。",
      },
    });
    const json = await resp.json();
    expect(json.code).toBe(200);
    expect(typeof json.data?.hasSimilar).toBe("boolean");
    if (json.data?.othersSimilar) {
      expect(json.data.title).toBeUndefined();
    }
  });
});
