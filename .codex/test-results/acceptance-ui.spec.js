const { test, expect } = require("@playwright/test");

const BASE = process.env.E2E_BASE_URL || "http://127.0.0.1:5173";
const PASSWORD = "husa123456";

async function login(page, userNo) {
  await page.goto(`${BASE}/login`);
  await page.getByPlaceholder("请输入学号或工号").fill(userNo);
  await page.getByRole("button", { name: "验证", exact: true }).click();
  await expect(page.getByRole("button", { name: "已验证", exact: true })).toBeVisible();
  await page.getByPlaceholder("请输入密码").fill(PASSWORD);
  await page.getByRole("button", { name: "进入系统" }).click();
  await page.waitForURL((url) => !url.pathname.includes("/login"), { timeout: 20000 });
  await expect(page.getByText("首页", { exact: true }).first()).toBeVisible();
}

async function expectMenu(page, text, visible) {
  const item = page.getByText(text, { exact: true }).first();
  if (visible) {
    await expect(item).toBeVisible();
  } else {
    await expect(item).toHaveCount(0);
  }
}

test("管理员仅显示管理类菜单，不继承报修人或维修人员菜单", async ({ page }) => {
  await login(page, "admin");
  await expectMenu(page, "工单管理", true);
  await expectMenu(page, "用户管理", true);
  await expectMenu(page, "提交报修", false);
  await expectMenu(page, "报修记录", false);
  await expectMenu(page, "待接工单", false);
  await expectMenu(page, "我的工单", false);
  await page.screenshot({ path: "evidence-admin-menu.png", fullPage: true });
});

test("学生仅显示报修人菜单，直接访问管理页面被拦截", async ({ page }) => {
  await login(page, "student");
  await expectMenu(page, "提交报修", true);
  await expectMenu(page, "报修记录", true);
  await expectMenu(page, "工单管理", false);
  await expectMenu(page, "用户管理", false);
  await expectMenu(page, "待接工单", false);

  await page.goto(`${BASE}/admin/users`);
  await expect(page).toHaveURL(/\/admin\/users$/);
  await expect(page.getByText("404", { exact: false }).first()).toBeVisible();
  await page.screenshot({ path: "evidence-student-forbidden.png", fullPage: true });
});

test("维修人员仅显示维修工作菜单，不显示管理员菜单", async ({ page }) => {
  await login(page, "repairer");
  await expectMenu(page, "待接工单", true);
  await expectMenu(page, "我的工单", true);
  await expectMenu(page, "工单管理", false);
  await expectMenu(page, "用户管理", false);
  await expectMenu(page, "提交报修", false);
  await page.screenshot({ path: "evidence-repairer-menu.png", fullPage: true });
});

test("未登录访问业务页面会跳转登录页并保留 redirect", async ({ page }) => {
  await page.goto(`${BASE}/admin/orders`);
  await expect(page).toHaveURL(/\/login\?redirect=\/admin\/orders$/);
  await expect(page.getByRole("button", { name: "进入系统" })).toBeVisible();
});
