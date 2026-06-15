import request from "@/utils/request";

// 分页查询资产列表
export const listAssets = (params) =>
  request({
    url: "/assets",
    method: "get",
    params,
  });

// 查询资产详情
export const getAsset = (id) =>
  request({
    url: `/assets/${id}`,
    method: "get",
  });

// 查询资产维修历史
export const getAssetRepairHistory = (id, params) =>
  request({
    url: `/assets/${id}/repair-history`,
    method: "get",
    params,
  });

// 管理员新增资产
export const createAsset = (data) =>
  request({
    url: "/assets",
    method: "post",
    data,
  });

// 管理员修改资产
export const updateAsset = (id, data) =>
  request({
    url: `/assets/${id}`,
    method: "put",
    data,
  });

// 管理员变更资产状态
export const changeAssetStatus = (id, data) =>
  request({
    url: `/assets/${id}/status`,
    method: "post",
    data,
  });

// 管理员逻辑删除资产
export const deleteAsset = (id, version) =>
  request({
    url: `/assets/${id}/delete`,
    method: "post",
    params: { version },
  });

// 管理员恢复已删除资产
export const restoreAsset = (id) =>
  request({
    url: `/assets/${id}/restore`,
    method: "post",
  });

// 管理员上传资产图片
export const uploadAssetImage = (file) => {
  const data = new FormData();
  data.append("file", file);
  return request({
    url: "/assets/image",
    method: "post",
    data,
    headers: { "Content-Type": "multipart/form-data", repeatSubmit: false },
  });
};
