import request from "@/utils/request";

// 获取当前用户的通知列表
export const listNotifications = (params) =>
  request({
    url: "/notifications",
    method: "get",
    params,
  });

// 获取是否有未读通知的状态
export const getUnreadState = () =>
  request({
    url: "/notifications/unread-state",
    method: "get",
  });

// 标记单条通知为已读
export const readNotification = (id) =>
  request({
    url: `/notifications/${id}/read`,
    method: "put",
  });

// 批量标记通知为已读
export const markNotificationsRead = (notificationIds) =>
  request({
    url: "/notifications/read-batch",
    method: "post",
    data: { notificationIds },
  });

// 一键标记所有通知为已读
export const markAllNotificationsRead = () =>
  request({
    url: "/notifications/read-all",
    method: "post",
  });
