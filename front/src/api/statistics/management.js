import request from "@/utils/request";

// 管理员查询管理统计汇总
export const getManagementStatistics = (params) =>
  request({
    url: "/admin/statistics/management",
    method: "get",
    params,
  });

// 管理员导出管理统计 Excel
export const exportManagementStatistics = (params) =>
  request({
    url: "/admin/statistics/management/export",
    method: "get",
    params,
    responseType: "blob",
  });
