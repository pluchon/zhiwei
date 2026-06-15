import request from "@/utils/request";

// 上传 Excel 并创建导入批次
export const uploadAssetImport = (file) => {
  const data = new FormData();
  data.append("file", file);
  return request({
    url: "/admin/asset-import/upload",
    method: "post",
    data,
    headers: { "Content-Type": "multipart/form-data", repeatSubmit: false },
  });
};

// 上传图片并创建图片导入批次
export const uploadAssetImportImages = (files) => {
  const data = new FormData();
  files.forEach((file) => data.append("files", file));
  return request({
    url: "/admin/asset-import/upload-images",
    method: "post",
    data,
    headers: { "Content-Type": "multipart/form-data", repeatSubmit: false },
  });
};

// 分页查询导入批次列表
export const listAssetImportBatches = (params) =>
  request({
    url: "/admin/asset-import/batches",
    method: "get",
    params,
  });

// 查询导入批次详情
export const getAssetImportBatch = (batchId) =>
  request({
    url: `/admin/asset-import/batches/${batchId}`,
    method: "get",
  });

// 分页查询批次内资产卡片
export const listAssetImportItems = (batchId, params) =>
  request({
    url: `/admin/asset-import/batches/${batchId}/items`,
    method: "get",
    params,
  });

// 编辑待审核资产卡片
export const updateAssetImportItem = (itemId, data) =>
  request({
    url: `/admin/asset-import/items/${itemId}`,
    method: "put",
    data,
  });

// 忽略待审核资产卡片
export const ignoreAssetImportItem = (itemId) =>
  request({
    url: `/admin/asset-import/items/${itemId}/ignore`,
    method: "post",
  });

// 确认单条资产卡片入库
export const confirmAssetImportItem = (itemId) =>
  request({
    url: `/admin/asset-import/items/${itemId}/confirm`,
    method: "post",
  });

// 批量确认资产卡片入库
export const confirmAssetImportBatch = (data) =>
  request({
    url: "/admin/asset-import/items/confirm",
    method: "post",
    data,
  });

// 删除导入批次及未入库卡片
export const deleteAssetImportBatch = (batchId) =>
  request({
    url: `/admin/asset-import/batches/${batchId}/delete`,
    method: "post",
  });
