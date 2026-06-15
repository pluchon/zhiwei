module.exports = {
  testDir: ".",
  testMatch: "gap-closure.spec.js",
  timeout: 90000,
  use: {
    baseURL: process.env.E2E_BASE_URL || "http://127.0.0.1:5173",
    trace: "on-first-retry",
  },
};
