/**
 * Step5 Gap 收尾 — Playwright MCP 测试脚本
 * 账号：admin / student / teacher / repairer，密码：husa123456
 */
async (page) => {
  const BASE = "http://127.0.0.1:5173";
  const API = "http://127.0.0.1:8080";
  const PASSWORD = "husa123456";
  const results = [];

  async function apiLogin(userNo) {
    const ch = await page.request.post(`${API}/auth/captcha/challenge`, {
      data: { scene: "LOGIN_PASSWORD", target: userNo },
    });
    const challengeId = (await ch.json()).data?.challengeId;
    const tk = await page.request.post(`${API}/auth/captcha/ticket`, {
      data: { challengeId, scene: "LOGIN_PASSWORD", target: userNo },
    });
    const ticket = (await tk.json()).data?.captchaTicket;
    const login = await page.request.post(`${API}/auth/login/password`, {
      data: { userNo, password: PASSWORD, captchaTicket: ticket },
    });
    const loginJson = await login.json();
    if (loginJson.code !== 200) {
      throw new Error(`${userNo} login failed: ${JSON.stringify(loginJson)}`);
    }
    return loginJson.data.token;
  }

  async function uiLogin(userNo) {
    await page.context().clearCookies();
    await page.goto(`${BASE}/login`);
    await page.waitForTimeout(1200);
    await page.getByPlaceholder("请输入学号或工号").fill(userNo);
    await page.getByRole("button", { name: "验证", exact: true }).click();
    await page.getByRole("button", { name: "已验证", exact: true }).waitFor();
    await page.getByPlaceholder("请输入密码").fill(PASSWORD);
    await page.getByRole("button", { name: "进入系统" }).click();
    await page.waitForURL((url) => !url.pathname.includes("/login"), { timeout: 20000 });
  }

  async function findDispatchableOrderId(token) {
    const headers = { Authorization: `Bearer ${token}` };
    for (const status of [1, 2]) {
      const resp = await page.request.get(`${API}/repair/orders?pageNum=1&pageSize=10&status=${status}`, { headers });
      const records = (await resp.json()).data?.records || [];
      if (records.length) {
        return records[0].orderId;
      }
    }
    const resp = await page.request.get(`${API}/repair/orders?pageNum=1&pageSize=20`, { headers });
    const records = (await resp.json()).data?.records || [];
    const match = records.find((row) => [1, 2].includes(row.status));
    return match?.orderId;
  }

  // 1. 派单 AI 分析 API
  try {
    const adminToken = await apiLogin("admin");
    const headers = { Authorization: `Bearer ${adminToken}` };
    const orderId = await findDispatchableOrderId(adminToken);
    if (!orderId) {
      results.push({ step: "dispatch_api", skipped: true, reason: "暂无待匹配/待接单工单" });
    } else {
      const resp = await page.request.post(`${API}/admin/repair-orders/ai/${orderId}/dispatch-analysis`, { headers });
      const json = await resp.json();
      results.push({
        step: "dispatch_api",
        ok: json.code === 200 && !!json.data?.analysisText,
        orderId,
      });
    }
  } catch (e) {
    results.push({ step: "dispatch_api", error: e.message });
  }

  // 2. 工单列表 AI 辅助列 + 弹窗
  try {
    await uiLogin("admin");
    await page.goto(`${BASE}/admin/orders?status=2`);
    await page.waitForTimeout(2000);
    const hasColumn = await page.getByText("AI 辅助").first().isVisible();
    const aiBtn = page.getByRole("button", { name: "AI 分析" }).first();
    await aiBtn.click();
    await page.waitForTimeout(5000);
    results.push({
      step: "list_ai_dialog",
      ok: hasColumn && (await page.getByText("AI 派单分析").first().isVisible()),
      hasDisclaimer: await page.getByText("不推荐或排序具体维修师傅").first().isVisible().catch(() => false),
    });
  } catch (e) {
    results.push({ step: "list_ai_dialog", error: e.message });
  }

  // 3. 派单弹窗内 AI 分析
  try {
    const adminToken = await apiLogin("admin");
    const orderId = await findDispatchableOrderId(adminToken);
    if (!orderId) {
      results.push({ step: "dispatch_dialog_ai", skipped: true, reason: "暂无待匹配/待接单工单" });
    } else {
      await uiLogin("admin");
      await page.goto(`${BASE}/repair/detail/${orderId}`);
      await page.waitForTimeout(2000);
      await page.getByRole("button", { name: "手动派单" }).click();
      await page.locator(".dispatch-ai-block").getByRole("button", { name: "AI 分析" }).click();
      await page.waitForTimeout(8000);
      const hasResult = await page.locator(".dispatch-ai-block .ai-analysis-text, .dispatch-ai-block .sub-text").first().isVisible().catch(() => false);
      results.push({ step: "dispatch_dialog_ai", ok: hasResult, orderId });
    }
  } catch (e) {
    results.push({ step: "dispatch_dialog_ai", error: e.message });
  }

  // 4. 疑似重复详情 API
  try {
    const adminToken = await apiLogin("admin");
    const headers = { Authorization: `Bearer ${adminToken}` };
    const orders = await page.request.get(`${API}/repair/orders?pageNum=1&pageSize=20&suspectedDuplicate=1`, { headers });
    const records = (await orders.json()).data?.records || [];
    if (!records.length) {
      results.push({ step: "duplicate_detail", skipped: true, reason: "暂无疑似重复工单" });
    } else {
      const orderId = records[0].orderId;
      const resp = await page.request.get(`${API}/admin/repair-orders/ai/${orderId}/duplicate-detail`, { headers });
      const json = await resp.json();
      results.push({
        step: "duplicate_detail",
        ok: json.code === 200 && json.data?.suspectedDuplicate === true && Array.isArray(json.data?.links),
        orderId,
      });
    }
  } catch (e) {
    results.push({ step: "duplicate_detail", error: e.message });
  }

  // 5. NL 导出预览
  try {
    const adminToken = await apiLogin("admin");
    const headers = { Authorization: `Bearer ${adminToken}` };
    const resp = await page.request.post(`${API}/ai/assistant/chat`, {
      headers,
      data: { message: "导出最近30天状态为待匹配的工单" },
    });
    const json = await resp.json();
    const preview = json.data?.exportPreview;
    results.push({
      step: "export_preview",
      ok: json.code === 200 && preview?.exportType && preview?.estimatedCount != null,
    });
  } catch (e) {
    results.push({ step: "export_preview", error: e.message });
  }

  // 6. 语义搜索工单
  try {
    const adminToken = await apiLogin("admin");
    const headers = { Authorization: `Bearer ${adminToken}` };
    const resp = await page.request.post(`${API}/ai/assistant/chat`, {
      headers,
      data: { message: "搜索历史工单：教室灯不亮" },
    });
    const json = await resp.json();
    results.push({
      step: "order_search",
      ok: json.code === 200 && !!(json.data?.orderSearchResult || json.data?.replyText),
    });
  } catch (e) {
    results.push({ step: "order_search", error: e.message });
  }

  // 7. 维修师傅搜资产 + 学生不可用
  try {
    const repairerToken = await apiLogin("repairer");
    const assetResp = await page.request.post(`${API}/ai/assistant/chat`, {
      headers: { Authorization: `Bearer ${repairerToken}` },
      data: { message: "搜索资产：投影仪" },
    });
    const assetOk = (await assetResp.json()).code === 200;
    const studentToken = await apiLogin("student");
    const forbidden = await page.request.post(`${API}/ai/assistant/chat`, {
      headers: { Authorization: `Bearer ${studentToken}` },
      data: { message: "查看统计" },
    });
    results.push({
      step: "role_access",
      ok: assetOk && (await forbidden.json()).code !== 200,
    });
  } catch (e) {
    results.push({ step: "role_access", error: e.message });
  }

  // 8. 建议相似检测
  try {
    const token = await apiLogin("repairer");
    const headers = { Authorization: `Bearer ${token}` };
    const resp = await page.request.post(`${API}/repair/suggestions/similarity-check`, {
      headers,
      data: {
        category: "PROCESS",
        title: "优化接单流程建议",
        content: "建议在待接单列表增加筛选条件，方便快速定位本校区工单。",
      },
    });
    const json = await resp.json();
    results.push({
      step: "suggestion_similarity",
      ok: json.code === 200 && typeof json.data?.hasSimilar === "boolean",
    });
  } catch (e) {
    results.push({ step: "suggestion_similarity", error: e.message });
  }

  const passed = results.filter((r) => r.ok).length;
  const failed = results.filter((r) => r.error || r.ok === false).length;
  const skipped = results.filter((r) => r.skipped).length;
  return { summary: { total: results.length, passed, failed, skipped }, results };
};
