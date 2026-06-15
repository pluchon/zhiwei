import request from "@/utils/request";

// 登录页公开概览
export const fetchPortalSummary = () =>
  request({
    url: "/auth/portal/summary",
    method: "get",
    headers: { isToken: false },
  });

// 获取滑块验证码挑战数据
export const captchaChallenge = (data) =>
  request({
    url: "/auth/captcha/challenge",
    method: "post",
    headers: { isToken: false, repeatSubmit: false },
    data,
  });

// 校验滑块验证码并生成验证票据
export const captchaTicket = (data) =>
  request({
    url: "/auth/captcha/ticket",
    method: "post",
    headers: { isToken: false, repeatSubmit: false },
    data,
  });

// 发送短信验证码
export const sendVerificationCode = (data) =>
  request({
    url: "/auth/verification-codes",
    method: "post",
    headers: { isToken: false, repeatSubmit: false },
    data,
  });

// 开始激活流程，验证账号与初始密码
export const activationStart = (data) =>
  request({
    url: "/auth/activation/start",
    method: "post",
    headers: { isToken: false },
    data,
  });

// 完成激活流程，绑定手机并设置新密码
export const activationComplete = (data) =>
  request({
    url: "/auth/activation/complete",
    method: "post",
    headers: { isToken: false },
    data,
  });

// 验证密保手机及验证码以获取重置票据
export const recoveryVerify = (data) =>
  request({
    url: "/auth/recovery/verify",
    method: "post",
    headers: { isToken: false },
    data,
  });

// 完成密码重置流程
export const recoveryComplete = (data) =>
  request({
    url: "/auth/recovery/complete",
    method: "post",
    headers: { isToken: false },
    data,
  });

// 修改当前登录用户的密码
export const changePassword = (data) =>
  request({
    url: "/auth/password",
    method: "put",
    data
  });

// 上传个人头像（自动审核）
export const uploadProfileAvatar = (file) => {
  const data = new FormData();
  data.append("file", file);
  return request({
    url: "/auth/profile/avatar",
    method: "post",
    data,
    headers: { "Content-Type": "multipart/form-data", repeatSubmit: false },
  });
};