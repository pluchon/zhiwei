import request from "@/utils/request";

// 查询启用中的资产分类列表
export const listEnabledAssetCategories = () =>
  request({
    url: "/asset/categories/enabled",
    method: "get",
  });

// 管理员分页查询资产分类
export const adminListAssetCategories = (params) =>
  request({
    url: "/asset/categories",
    method: "get",
    params,
  });

// 管理员新增资产分类
export const createAssetCategory = (data) =>
  request({
    url: "/asset/categories",
    method: "post",
    data,
  });

// 管理员修改资产分类
export const updateAssetCategory = (id, data) =>
  request({
    url: `/asset/categories/${id}`,
    method: "put",
    data,
  });

// 管理员停用资产分类
export const disableAssetCategory = (id) =>
  request({
    url: `/asset/categories/${id}/disable`,
    method: "post",
  });

// 导出当前筛选条件下或已选中的资产分类
export const exportAssetCategories = (params) =>
  request({
    url: "/asset/categories/export",
    method: "get",
    params,
    responseType: "blob",
  });
