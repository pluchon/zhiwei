import request from "@/utils/request";

// 加载疑似重复工单 AI 详情
export const getDuplicateDetail = (orderId) =>
  request({
    url: `/admin/repair-orders/ai/${orderId}/duplicate-detail`,
    method: "get",
  });

// 确认工单 AI 关联
export const confirmOrderAiLink = (data) =>
  request({
    url: "/admin/repair-orders/ai/links/confirm",
    method: "post",
    data,
  });

// 解除工单 AI 关联
export const removeOrderAiLink = (linkId) =>
  request({
    url: `/admin/repair-orders/ai/links/${linkId}/remove`,
    method: "post",
  });

// 派单辅助 AI 分析
export const analyzeDispatch = (orderId) =>
  request({
    url: `/admin/repair-orders/ai/${orderId}/dispatch-analysis`,
    method: "post",
  });
