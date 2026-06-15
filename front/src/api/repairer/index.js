import request from "@/utils/request";

// 维修师傅查询本人接单状态
export const getRepairerAvailability = () =>
  request({
    url: "/repair/repairer/availability",
    method: "get",
  });

// 维修师傅更新本人接单状态
export const updateRepairerAvailability = (data) =>
  request({
    url: "/repair/repairer/availability",
    method: "put",
    data,
  });

// 管理员只读查询维修师傅接单状态列表
export const listRepairerAvailability = (params) =>
  request({
    url: "/admin/repairers/availability",
    method: "get",
    params,
  });

// 维修师傅查询个人工作统计
export const getRepairerStatistics = (params) =>
  request({
    url: "/repair/repairer/statistics",
    method: "get",
    params,
  });

// 维修师傅首页看板
export const getRepairerDashboard = (rangeDays) =>
  request({
    url: "/repair/repairer-dashboard",
    method: "get",
    params: { rangeDays },
  });
