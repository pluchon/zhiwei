import request from "@/utils/request";

// 维修师傅查询本人建议列表
export const listMySuggestions = (params) =>
  request({
    url: "/repair/suggestions/mine",
    method: "get",
    params,
  });

// 管理员查询全部建议列表
export const listAdminSuggestions = (params) =>
  request({
    url: "/repair/suggestions",
    method: "get",
    params,
  });

// 查询建议详情
export const getSuggestion = (id) =>
  request({
    url: `/repair/suggestions/${id}`,
    method: "get",
  });

// 维修师傅提交建议
export const submitSuggestion = (data) =>
  request({
    url: "/repair/suggestions",
    method: "post",
    data,
  });

// 提交前检测相似建议
export const checkSuggestionSimilarity = (data, excludeSuggestionId) =>
  request({
    url: "/repair/suggestions/similarity-check",
    method: "post",
    params: excludeSuggestionId ? { excludeSuggestionId } : {},
    data,
  });

// 维修师傅编辑并重新提交建议
export const updateSuggestion = (id, data) =>
  request({
    url: `/repair/suggestions/${id}`,
    method: "put",
    data,
  });

// 维修师傅撤回待处理建议
export const withdrawSuggestion = (id) =>
  request({
    url: `/repair/suggestions/${id}/withdraw`,
    method: "post",
  });

// 管理员处理建议
export const handleSuggestion = (id, data) =>
  request({
    url: `/repair/suggestions/${id}/handle`,
    method: "post",
    data,
  });
