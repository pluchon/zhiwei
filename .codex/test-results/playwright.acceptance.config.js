module.exports = {
  testDir: ".",
  testMatch: "acceptance-ui.spec.js",
  outputDir: "playwright-output",
  timeout: 60000,
  use: {
    baseURL: process.env.E2E_BASE_URL || "http://127.0.0.1:5173",
    trace: "retain-on-failure",
  },
};
