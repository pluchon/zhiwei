import request from "@/utils/request";

// 获取后台管理列表数据
export const adminList = (resource, params) =>
  request({
    url: `/admin/${resource}`,
    method: "get",
    params,
  });

// 新增后台管理数据
export const adminCreate = (resource, data) =>
  request({
    url: `/admin/${resource}`,
    method: "post",
    data,
  });

// 更新后台管理数据
export const adminUpdate = (resource, id, data) =>
  request({
    url: `/admin/${resource}/${id}`,
    method: "put",
    data,
  });

// 删除/移除后台管理数据
export const adminDelete = (resource, id) =>
  request({
    url: `/admin/${resource}/${id}`,
    method: "delete",
  });

// 获取报修后台统计看板数据
export const getRepairDashboard = (rangeDays = 30) =>
  request({
    url: "/admin/repair-dashboard",
    method: "get",
    params: { rangeDays },
  });

// 导出当前筛选条件下的工单
export const exportOrders = (params) =>
  request({
    url: "/admin/orders/export",
    method: "get",
    params,
    responseType: "blob",
  });

// 导出当前筛选条件下或已选中的用户
export const exportUsers = (params) =>
  request({
    url: "/admin/users/export",
    method: "get",
    params,
    responseType: "blob",
  });

// 获取手动派单的候选维修师傅列表
export const getDispatchCandidates = (id) =>
  request({
    url: `/admin/orders/${id}/dispatch-candidates`,
    method: "get",
  });

// 后台管理员手动指派维修工单
export const adminDispatchOrder = (id, data) =>
  request({
    url: `/admin/orders/${id}/dispatch`,
    method: "post",
    data,
  });
