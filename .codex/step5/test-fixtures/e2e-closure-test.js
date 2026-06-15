/**
 * Step5 完整闭环 E2E 测试脚本（Playwright MCP browser_run_code_unsafe 可粘贴运行）
 * 依赖：前端 5173、后端 8080、Docker MySQL/Redis、密码 husa123456
 */
async (page) => {
  const BASE = 'http://127.0.0.1:5173';
  const API = `${BASE}/dev-api`;
  const PASSWORD = 'husa123456';
  const FIXTURE = 'c:/JavaCode/items/Campus-System/.codex/step5/test-fixtures';
  const results = [];

  async function apiLogin(userNo) {
    const ch = await page.request.post(`${API}/auth/captcha/challenge`, {
      data: { scene: 'LOGIN_PASSWORD', target: userNo },
    });
    const chJson = await ch.json();
    const challengeId = chJson.data?.challengeId;
    const tk = await page.request.post(`${API}/auth/captcha/ticket`, {
      data: { challengeId, scene: 'LOGIN_PASSWORD', target: userNo },
    });
    const ticket = (await tk.json()).data?.ticket;
    const login = await page.request.post(`${API}/auth/login/password`, {
      data: { userNo, password: PASSWORD, captchaTicket: ticket },
    });
    const loginJson = await login.json();
    if (loginJson.code !== 200) throw new Error(`${userNo} login failed: ${JSON.stringify(loginJson)}`);
    return loginJson.data.token;
  }

  function authHeaders(token) {
    return { Authorization: `Bearer ${token}` };
  }

  async function uiLogin(userNo) {
    await page.context().clearCookies();
    await page.goto(`${BASE}/login`);
    await page.waitForTimeout(1200);
    const inputs = page.locator('.el-tab-pane input');
    await inputs.nth(0).fill(userNo);
    await page.getByRole('button', { name: '验证', exact: true }).click();
    await page.getByRole('button', { name: '已验证', exact: true }).waitFor();
    await inputs.nth(1).fill(PASSWORD);
    await page.getByRole('button', { name: '进入系统' }).click();
    await page.waitForURL((u) => !u.pathname.includes('/login'), { timeout: 15000 });
  }

  // --- 1. 资产 Excel 导入 ---
  try {
    const adminToken = await apiLogin('admin');
    const excelBuf = await page.request.get(`file:///${FIXTURE}/step5-test-assets.xlsx`).catch(() => null);
    const fs = require('fs');
    const excelPath = FIXTURE.replace(/\//g, '\\') + '\\step5-test-assets.xlsx';
    const formData = new (require('form-data'))();
    // Use Playwright multipart upload via API
    const uploadResp = await page.request.post(`${API}/admin/asset-import/upload`, {
      headers: authHeaders(adminToken),
      multipart: {
        file: {
          name: 'step5-test-assets.xlsx',
          mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          buffer: require('fs').readFileSync(excelPath),
        },
      },
    });
    const uploadJson = await uploadResp.json();
    results.push({
      step: 'excel_upload',
      ok: uploadJson.code === 200,
      batchId: uploadJson.data?.batchId,
      total: uploadJson.data?.totalCount,
    });
    const excelBatchId = uploadJson.data?.batchId;

    // 图片导入
    const pngPath = FIXTURE.replace(/\//g, '\\') + '\\step5-test-asset.png';
    const imgResp = await page.request.post(`${API}/admin/asset-import/upload-images`, {
      headers: authHeaders(adminToken),
      multipart: {
        files: {
          name: 'step5-test-asset.png',
          mimeType: 'image/png',
          buffer: require('fs').readFileSync(pngPath),
        },
      },
    });
    const imgJson = await imgResp.json();
    results.push({
      step: 'image_upload',
      ok: imgJson.code === 200,
      batchId: imgJson.data?.batchId,
      total: imgJson.data?.totalCount,
    });

    // 批次列表含来源
    const batches = await page.request.get(`${API}/admin/asset-import/batches?pageNum=1&pageSize=10`, {
      headers: authHeaders(adminToken),
    });
    const batchList = (await batches.json()).data?.records || [];
    results.push({
      step: 'batch_list',
      ok: batchList.some((b) => b.sourceType === 'EXCEL') && batchList.some((b) => b.sourceType === 'IMAGE'),
      count: batchList.length,
    });

    // Excel 批次详情
    if (excelBatchId) {
      const itemsResp = await page.request.get(
        `${API}/admin/asset-import/batches/${excelBatchId}/items?pageNum=1&pageSize=10`,
        { headers: authHeaders(adminToken) },
      );
      const items = (await itemsResp.json()).data?.records || [];
      results.push({
        step: 'excel_items',
        ok: items.length >= 2,
        names: items.map((i) => i.assetName),
        aiStatus: items[0]?.aiRecognizeStatus,
      });
    }
  } catch (e) {
    results.push({ step: 'asset_import', error: e.message });
  }

  // --- 2. 报修闭环：创建两单相似工单 ---
  let orderId1, orderId2;
  try {
    const studentToken = await apiLogin('student');
    const locResp = await page.request.get(`${API}/repair/locations/options`, {
      headers: authHeaders(studentToken),
    });
    const locations = (await locResp.json()).data || [];
    const campus = locations[0];
    const building = campus?.buildings?.[0];
    if (!campus) throw new Error('no campus');

    const mkOrder = async (suffix) => {
      const create = await page.request.post(`${API}/repair/orders`, {
        headers: authHeaders(studentToken),
        data: {
          requestId: crypto.randomUUID(),
          title: 'Step5闭环测试-教室灯不亮',
          description: '同一教室照明故障，日光灯闪烁无法正常使用，请尽快维修。',
          categoryId: 1,
          repairType: 'NORMAL',
          campusId: campus.campusId,
          buildingId: building?.buildingId || null,
          floor: '2',
          room: '201',
          locationDetail: suffix,
          contactPhone: '13800000002',
        },
      });
      const cj = await create.json();
      if (cj.code !== 200) throw new Error('create failed: ' + JSON.stringify(cj));
      const order = cj.data;
      const dup = await page.request.post(`${API}/repair/orders/${order.orderId}/duplicate-check`, {
        headers: authHeaders(studentToken),
      });
      const dupJson = await dup.json();
      const submit = await page.request.post(`${API}/repair/orders/${order.orderId}/submit`, {
        headers: authHeaders(studentToken),
        data: { version: order.version },
      });
      const submitJson = await submit.json();
      return {
        orderId: order.orderId,
        duplicateCheck: dupJson.data,
        submit: submitJson.data,
        submitCode: submitJson.code,
      };
    };

    const first = await mkOrder('E2E-A');
    orderId1 = first.orderId;
    results.push({ step: 'repair_order_1', ok: first.submitCode === 200, orderId: orderId1 });

    const second = await mkOrder('E2E-B');
    orderId2 = second.orderId;
    results.push({
      step: 'repair_order_2_duplicate',
      ok: second.submitCode === 200,
      orderId: orderId2,
      dupSuspected: second.duplicateCheck?.suspected,
      dupReminder: !!second.duplicateCheck?.reporterReminder,
      submitReminder: !!second.submit?.duplicateReminder,
    });

    const adminToken = await apiLogin('admin');
    const orderDetail = await page.request.get(`${API}/admin/orders/${orderId2}`, {
      headers: authHeaders(adminToken),
    }).catch(() => null);
    if (orderDetail) {
      const od = (await orderDetail.json()).data?.order;
      results.push({
        step: 'admin_order_flag',
        suspectedDuplicate: od?.suspectedDuplicate,
        duplicateReason: od?.duplicateReason?.slice?.(0, 80),
      });
    }

    // AI 分析 & 重复详情
    const aiAnalysis = await page.request.post(
      `${API}/admin/repair-orders/ai/${orderId2}/dispatch-analysis`,
      { headers: authHeaders(adminToken) },
    );
    const aiJson = await aiAnalysis.json();
    results.push({
      step: 'ai_dispatch_analysis',
      ok: aiJson.code === 200,
      hasText: !!(aiJson.data?.analysisText),
      degraded: aiJson.data?.degraded,
    });

    const dupDetail = await page.request.get(
      `${API}/admin/repair-orders/ai/${orderId2}/duplicate-detail`,
      { headers: authHeaders(adminToken) },
    );
    const dupDetailJson = await dupDetail.json();
    results.push({
      step: 'duplicate_detail',
      ok: dupDetailJson.code === 200,
      suspected: dupDetailJson.data?.suspectedDuplicate,
      links: dupDetailJson.data?.links?.length || 0,
    });
  } catch (e) {
    results.push({ step: 'repair_flow', error: e.message });
  }

  // --- 3. AI 助手：统计 + 导出预览 ---
  try {
    const adminToken = await apiLogin('admin');
    const statsChat = await page.request.post(`${API}/ai/assistant/chat`, {
      headers: authHeaders(adminToken),
      data: { sceneType: 'STATISTICS', message: '查看最近7天工单统计' },
    });
    const statsJson = await statsChat.json();
    results.push({
      step: 'ai_stats',
      ok: statsJson.code === 200,
      hasOverview: !!statsJson.data?.statisticsResult?.overview,
      reply: statsJson.data?.replyText?.slice(0, 60),
    });

    const exportChat = await page.request.post(`${API}/ai/assistant/chat`, {
      headers: authHeaders(adminToken),
      data: { sceneType: 'EXPORT', message: '导出最近30天工单' },
    });
    const exportJson = await exportChat.json();
    const previewToken = exportJson.data?.exportPreview?.previewToken;
    results.push({
      step: 'ai_export_preview',
      ok: exportJson.code === 200,
      hasPreview: !!previewToken,
    });

    if (previewToken) {
      const exportResp = await page.request.post(`${API}/ai/assistant/export/confirm`, {
        headers: authHeaders(adminToken),
        data: { previewToken },
      });
      results.push({
        step: 'ai_export_confirm',
        ok: exportResp.ok(),
        contentType: exportResp.headers()['content-type'],
      });
    }

    const repairerToken = await apiLogin('repairer');
    const repairerExport = await page.request.post(`${API}/ai/assistant/chat`, {
      headers: authHeaders(repairerToken),
      data: { sceneType: 'EXPORT', message: '导出工单' },
    });
    results.push({
      step: 'repairer_export_forbidden',
      forbidden: (await repairerExport.json()).code !== 200,
    });
  } catch (e) {
    results.push({ step: 'ai_assistant', error: e.message });
  }

  // --- 4. UI 验证关键页面 ---
  try {
    await uiLogin('admin');
    await page.goto(`${BASE}/admin/orders`);
    await page.waitForTimeout(2000);
    results.push({
      step: 'ui_admin_orders',
      suspectedFilter: await page.getByText('疑似重复').first().isVisible().catch(() => false),
    });

    if (orderId2) {
      await page.goto(`${BASE}/repair/detail/${orderId2}`);
      await page.waitForTimeout(2500);
      const aiBtn = await page.getByRole('button', { name: 'AI 分析' }).isVisible().catch(() => false);
      if (aiBtn) {
        await page.getByRole('button', { name: 'AI 分析' }).click();
        await page.waitForTimeout(3000);
      }
      results.push({
        step: 'ui_repair_detail',
        aiBtn,
        hasAnalysis: await page.locator('.ai-analysis-text').isVisible().catch(() => false),
        url: page.url(),
      });
    }

    await page.goto(`${BASE}/admin/asset-import`);
    await page.waitForTimeout(1500);
    results.push({
      step: 'ui_asset_import',
      excel: await page.locator('button').filter({ hasText: '上传 Excel' }).first().isVisible(),
      image: await page.locator('button').filter({ hasText: '上传图片' }).first().isVisible(),
    });
  } catch (e) {
    results.push({ step: 'ui_verify', error: e.message });
  }

  return results;
}
