import request from "@/utils/request";

// 管理员分页查询人工恢复申请
export const listManualRecoveries = (params) =>
  request({
    url: "/admin/manual-account-recovery",
    method: "get",
    params,
  });

// 管理员查询人工恢复申请详情
export const getManualRecovery = (recoveryId) =>
  request({
    url: `/admin/manual-account-recovery/${recoveryId}`,
    method: "get",
  });

// 管理员创建人工恢复申请
export const createManualRecovery = (data) =>
  request({
    url: "/admin/manual-account-recovery",
    method: "post",
    data,
  });

// 发起管理员撤销待复核申请
export const cancelManualRecovery = (recoveryId) =>
  request({
    url: `/admin/manual-account-recovery/${recoveryId}/cancel`,
    method: "post",
  });

// 复核管理员审批人工恢复申请
export const reviewManualRecovery = (recoveryId, data) =>
  request({
    url: `/admin/manual-account-recovery/${recoveryId}/review`,
    method: "post",
    data,
  });

// 用户查询可验证的人工恢复申请信息
export const getManualRecoveryVerifyInfo = (recoveryId) =>
  request({
    url: `/manual-account-recovery/${recoveryId}/verify-info`,
    method: "get",
    headers: { isToken: false },
  });

// 用户完成新手机号验证并换绑
export const verifyManualRecoveryPhone = (recoveryId, data) =>
  request({
    url: `/manual-account-recovery/${recoveryId}/verify-phone`,
    method: "post",
    headers: { isToken: false },
    data,
  });
