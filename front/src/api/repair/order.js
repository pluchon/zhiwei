import request from "@/utils/request";

// 分页查询用户的报修工单列表（支持学生/教师/维修师傅/管理员的权限边界查询）
export const getReporterDashboard = (rangeDays) =>
  request({
    url: "/repair/reporter-dashboard",
    method: "get",
    params: { rangeDays },
  });

export const listOrders = (params) =>
  request({
      url: "/repair/orders",
      method: "get",
      params
  });

// 维修师傅分页查询所有自己能接取的待接单工单
export const listAvailableOrders = (params) =>
  request({
      url: "/repair/orders/available",
      method: "get",
      params
  });

// 获取指定工单的详细信息（包含工单主体、故障类型、附件、评论和日志）
export const getOrder = (id) =>
  request({
      url: `/repair/orders/${id}`,
      method: "get"
  });

// 报修人创建新的报修工单（默认状态为草稿）
export const createOrder = (data) =>
  request({
      url: "/repair/orders",
      method: "post",
      data
  });

// 报修人更新草稿状态下的工单信息（支持版本号乐观锁控制）
export const updateOrder = (id, data) =>
  request({
      url: `/repair/orders/${id}`,
      method: "put",
      data
  });

// 提交前检测是否疑似重复报修
export const checkDuplicate = (id) =>
  request({
    url: `/repair/orders/${id}/duplicate-check`,
    method: "post",
  });

// 执行工单流转动作（提交、接单、开始、完成、退回、确认、无法修复）
export const orderAction = (id, action, data = {}) =>
  request({
      url: `/repair/orders/${id}/${action}`,
      method: "post",
      data
  });

// 在工单详情页添加流转过程中的评论或留言
export const addComment = (id, content) =>
  request({
    url: `/repair/orders/${id}/comments`,
    method: "post",
    data: { content },
  });

// 报修人对已完成的工单进行评价（星级和评论内容）
export const evaluateOrder = (id, data) =>
  request({
      url: `/repair/orders/${id}/evaluation`,
      method: "post",
      data
  });

// 上传工单附件图片或文件
export const uploadAttachment = (id, file, recordId) => {
  const data = new FormData();
  data.append("file", file);
  if (recordId){
      data.append("recordId", recordId);
  }
  return request({
    url: `/repair/orders/${id}/attachments`,
    method: "post",
    data,
    headers: { "Content-Type": "multipart/form-data", repeatSubmit: false },
  });
};

// 获取所有启用的故障类型列表（用于前台下拉选择）
export const listCategories = () =>
  request({
    url: "/repair/categories",
    method: "get",
    headers: { isToken: false },
  });

// 根据故障分类获取对应负责团队的负荷摘要（判断是否爆单）
export const getWorkforceSummary = (categoryId) =>
  request({
    url: "/repair/workforce-summary",
    method: "get",
    params: { categoryId },
  });

// 获取当前维修师傅已接单负荷级别（判断接单是否繁忙）
export const getRepairerBusyLevel = () =>
  request({
    url: "/repair/repairer/busy-level",
    method: "get",
  });

// 报修人撤回待派单/待接单状态的工单为草稿状态
export const withdrawOrder = (id, data) =>
  request({
    url: `/repair/orders/${id}/withdraw`,
    method: "post",
    data,
  });

// 报修人将已被驳回的工单重新转为草稿状态进行编辑
export const reDraftOrder = (id, data) =>
  request({
    url: `/repair/orders/${id}/re-draft`,
    method: "post",
    data,
  });

// 报修人对因7天无响应自动确认完成的工单提起仲裁申请
export const requestAutoCompleteArbitration = (id, data) =>
  request({
    url: `/repair/orders/${id}/auto-complete-arbitration`,
    method: "post",
    data,
  });
