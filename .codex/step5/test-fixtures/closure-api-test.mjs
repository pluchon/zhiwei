import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const BASE = "http://127.0.0.1:8080";
const PASSWORD = "husa123456";
const PNG = path.join(__dirname, "step5-test-asset.png");

async function login(userNo) {
  const ch = await fetch(`${BASE}/auth/captcha/challenge`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ scene: "LOGIN_PASSWORD", target: userNo }),
  });
  const cj = await ch.json();
  const tk = await fetch(`${BASE}/auth/captcha/ticket`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      challengeId: cj.data.challengeId,
      scene: "LOGIN_PASSWORD",
      target: userNo,
    }),
  });
  const tj = await tk.json();
  const lg = await fetch(`${BASE}/auth/login/password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      userNo,
      password: PASSWORD,
      captchaTicket: tj.data.captchaTicket,
    }),
  });
  const lj = await lg.json();
  if (lj.code !== 200) throw new Error(`${userNo}: ${JSON.stringify(lj)}`);
  return lj.data.token;
}

function rid() {
  return `e2e-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

const auth = (token) => ({ Authorization: `Bearer ${token}` });

async function uploadAttachment(token, orderId) {
  const form = new FormData();
  const blob = new Blob([fs.readFileSync(PNG)], { type: "image/png" });
  form.append("file", blob, "scene.png");
  const resp = await fetch(`${BASE}/repair/orders/${orderId}/attachments`, {
    method: "POST",
    headers: auth(token),
    body: form,
  });
  const json = await resp.json();
  if (json.code !== 200) throw new Error(`attachment: ${JSON.stringify(json)}`);
}

async function main() {
  const results = {};

  const studentToken = await login("student");
  const loc = await (
    await fetch(`${BASE}/repair/locations/options`, { headers: auth(studentToken) })
  ).json();
  const campus = loc.data[0];
  const building = campus.buildings[0];

  async function createSubmit(suffix) {
    const c = await fetch(`${BASE}/repair/orders`, {
      method: "POST",
      headers: { ...auth(studentToken), "Content-Type": "application/json" },
      body: JSON.stringify({
        requestId: rid(),
        title: "Step5闭环测试-教室灯不亮",
        description: "同一教室照明故障，日光灯闪烁无法正常使用，请尽快维修。",
        categoryId: 1,
        repairType: "NORMAL",
        campusId: campus.campusId,
        buildingId: building.buildingId,
        floor: "2",
        room: "201",
        locationDetail: suffix,
        contactPhone: "13800000002",
      }),
    });
    const cj = await c.json();
    const order = cj.data;
    await uploadAttachment(studentToken, order.orderId);
    const dup = await (
      await fetch(`${BASE}/repair/orders/${order.orderId}/duplicate-check`, {
        method: "POST",
        headers: auth(studentToken),
      })
    ).json();
    const sub = await (
      await fetch(`${BASE}/repair/orders/${order.orderId}/submit`, {
        method: "POST",
        headers: { ...auth(studentToken), "Content-Type": "application/json" },
        body: JSON.stringify({ version: order.version }),
      })
    ).json();
    return { orderId: order.orderId, dup: dup.data, sub: sub.data, code: sub.code };
  }

  results.repair1 = await createSubmit("E2E-A");
  results.repair2 = await createSubmit("E2E-B");

  const adminToken = await login("admin");
  const orderId2 = results.repair2.orderId;

  results.aiAnalysis = await (
    await fetch(`${BASE}/admin/repair-orders/ai/${orderId2}/dispatch-analysis`, {
      method: "POST",
      headers: auth(adminToken),
    })
  ).json();

  results.duplicateDetail = await (
    await fetch(`${BASE}/admin/repair-orders/ai/${orderId2}/duplicate-detail`, {
      headers: auth(adminToken),
    })
  ).json();

  results.dupFilter = await (
    await fetch(`${BASE}/repair/orders?pageNum=1&pageSize=20&suspectedDuplicate=1`, {
      headers: auth(adminToken),
    })
  ).json();

  const exChat = await (
    await fetch(`${BASE}/ai/assistant/chat`, {
      method: "POST",
      headers: { ...auth(adminToken), "Content-Type": "application/json" },
      body: JSON.stringify({ sceneType: "EXPORT", message: "导出最近30天工单" }),
    })
  ).json();
  const previewToken = exChat.data?.exportPreview?.previewToken;
  if (previewToken) {
    const exp = await fetch(`${BASE}/ai/assistant/export/confirm`, {
      method: "POST",
      headers: { ...auth(adminToken), "Content-Type": "application/json" },
      body: JSON.stringify({ previewToken }),
    });
    const buf = await exp.arrayBuffer();
    results.exportConfirm = {
      status: exp.status,
      type: exp.headers.get("content-type"),
      size: buf.byteLength,
    };
  }

  await new Promise((r) => setTimeout(r, 15000));
  const batches = await (
    await fetch(`${BASE}/admin/asset-import/batches?pageNum=1&pageSize=10`, {
      headers: auth(adminToken),
    })
  ).json();
  const imageBatch = batches.data.records.find((b) => b.sourceType === "IMAGE");
  if (imageBatch) {
    const items = await (
      await fetch(
        `${BASE}/admin/asset-import/batches/${imageBatch.batchId}/items?pageNum=1&pageSize=10`,
        { headers: auth(adminToken) },
      )
    ).json();
    results.imageBatchItem = items.data.records[0];
  }

  console.log(JSON.stringify(results, null, 2));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
