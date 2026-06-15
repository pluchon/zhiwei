import request from "@/utils/request";

// 获取前台位置级联选项数据（校区-楼栋级联列表）
export const listLocationOptions = () =>
  request({
    url: "/repair/locations/options",
    method: "get",
  });

// 后台管理员获取所有校区列表
export const adminListCampuses = () =>
  request({
    url: "/admin/locations/campuses",
    method: "get",
  });

// 后台管理员根据校区ID获取对应的楼栋列表
export const adminListBuildings = (campusId) =>
  request({
    url: `/admin/locations/campuses/${campusId}/buildings`,
    method: "get",
  });

// 后台管理员创建校区
export const adminCreateCampus = (data) =>
  request({
    url: "/admin/locations/campuses",
    method: "post",
    data,
  });

// 后台管理员更新校区信息
export const adminUpdateCampus = (id, data) =>
  request({
    url: `/admin/locations/campuses/${id}`,
    method: "put",
    data,
  });

// 启用指定校区
export const enableCampus = (id) =>
  request({
    url: `/admin/locations/campuses/${id}/enable`,
    method: "post",
  });

// 停用指定校区
export const disableCampus = (id) =>
  request({
    url: `/admin/locations/campuses/${id}/disable`,
    method: "post",
  });

// 删除指定校区
export const deleteCampus = (id) =>
  request({
    url: `/admin/locations/campuses/${id}/delete`,
    method: "post",
  });

// 恢复已删除校区
export const restoreCampus = (id) =>
  request({
    url: `/admin/locations/campuses/${id}/restore`,
    method: "post",
  });

// 后台管理员创建楼栋
export const adminCreateBuilding = (data) =>
  request({
    url: "/admin/locations/buildings",
    method: "post",
    data,
  });

// 后台管理员更新楼栋信息
export const adminUpdateBuilding = (id, data) =>
  request({
    url: `/admin/locations/buildings/${id}`,
    method: "put",
    data,
  });

// 启用指定楼栋
export const enableBuilding = (id) =>
  request({
    url: `/admin/locations/buildings/${id}/enable`,
    method: "post",
  });

// 停用指定楼栋
export const disableBuilding = (id) =>
  request({
    url: `/admin/locations/buildings/${id}/disable`,
    method: "post",
  });

// 删除指定楼栋
export const deleteBuilding = (id) =>
  request({
    url: `/admin/locations/buildings/${id}/delete`,
    method: "post",
  });

// 恢复已删除楼栋
export const restoreBuilding = (id) =>
  request({
    url: `/admin/locations/buildings/${id}/restore`,
    method: "post",
  });
