export const repairStatuses = [
  "草稿",
  "待匹配",
  "待接单",
  "已接单",
  "处理中",
  "待确认",
  "待仲裁",
  "已完成",
  "已驳回",
  "已关闭",
];

export const repairStatusType = [
  "info",
  "warning",
  "warning",
  "primary",
  "primary",
  "warning",
  "danger",
  "success",
  "danger",
  "info",
];

export const statusText = (value) => repairStatuses[value] || "未知状态";

export const busyLevels = {
  IDLE: { label: "空闲", type: "success" },
  MODERATE: { label: "适中", type: "warning" },
  BUSY: { label: "繁忙", type: "danger" },
};

export const busyLevelText = (code, fallback) =>
  busyLevels[code]?.label || fallback || "未知";

export const busyLevelType = (code) => busyLevels[code]?.type || "info";

export const repairerQuickFilters = [
  { value: "REPAIRER_PROCESSING", label: "我的处理中" },
  { value: "REPAIRER_PENDING_CONFIRM", label: "我的待确认" },
  { value: "REPAIRER_COMPLETED", label: "已完成" },
];

export const reporterQuickFilters = [
  { value: "REPORTER_PROCESSING", label: "处理中" },
  { value: "REPORTER_PENDING_CONFIRM", label: "待我确认" },
  { value: "REPORTER_ENDED", label: "已结束" },
];

export const longStagnantLabel = "长时间未进展";

// 通知类型与后端 NotificationType 枚举保持同步（code 一一对应）
export const notificationTypes = [
  { value: 0, label: "工单状态通知" },
  { value: 1, label: "维修三天提醒" },
  { value: 2, label: "维修七天提醒" },
  { value: 3, label: "待确认三天提醒" },
  { value: 4, label: "待确认七天提醒" },
  { value: 5, label: "待确认二十七天提醒" },
  { value: 6, label: "自动完成通知" },
  { value: 7, label: "长时间未进展提醒" },
  { value: 8, label: "仲裁申请通知" },
  { value: 9, label: "建议提交通知" },
  { value: 10, label: "建议处理通知" },
  { value: 11, label: "头像审核通知" },
];

export const notificationTypeText = (value) =>
  notificationTypes.find((item) => item.value === value)?.label || "其他通知";

export const locationStatusOptions = [
  { value: 0, label: "启用", type: "success" },
  { value: 1, label: "停用", type: "danger" },
];

export const locationDeleteStateOptions = [
  { value: 0, label: "正常", type: "success" },
  { value: 1, label: "已删除", type: "info" },
];

export const locationStatusText = (value) =>
  locationStatusOptions.find((item) => item.value === value)?.label || value;

export const locationDeleteStateText = (value) =>
  locationDeleteStateOptions.find((item) => item.value === value)?.label ||
  value;

export const formatLocationSnapshot = (order) => {
  if (!order) {
    return "";
  }
  const parts = [
    order.campus,
    order.building,
    order.floor,
    order.room,
    order.locationDetail,
  ].filter((part) => part && String(part).trim());
  return parts.join(" ");
};

// 资产状态选项，与后端 AssetStatus 枚举保持同步
export const assetStatusOptions = [
  { value: "IN_USE", label: "使用中", type: "success" },
  { value: "UNDER_REPAIR", label: "维修中", type: "warning" },
  { value: "OUT_OF_SERVICE", label: "停用", type: "danger" },
];

export const assetStatusText = (code, fallback) =>
  assetStatusOptions.find((item) => item.value === code)?.label ||
  fallback ||
  code ||
  "未知";

export const assetStatusType = (code) =>
  assetStatusOptions.find((item) => item.value === code)?.type || "info";

// 报修类型选项，与后端 RepairType 枚举保持同步
export const repairTypeOptions = [
  { value: "NORMAL", label: "普通报修" },
  { value: "ASSET", label: "资产报修" },
];

export const repairTypeText = (code, fallback) =>
  repairTypeOptions.find((item) => item.value === code)?.label ||
  fallback ||
  "普通报修";

// 维修师傅建议状态
export const suggestionStatusOptions = [
  { value: "PENDING", label: "待处理", type: "warning" },
  { value: "ACCEPTED", label: "已采纳", type: "success" },
  { value: "REJECTED", label: "未采纳", type: "info" },
];

export const suggestionStatusText = (code, fallback) =>
  suggestionStatusOptions.find((item) => item.value === code)?.label ||
  fallback ||
  "未知";

export const suggestionStatusType = (code) =>
  suggestionStatusOptions.find((item) => item.value === code)?.type || "info";

// 维修师傅建议分类
export const suggestionCategoryOptions = [
  { value: "FAULT_TYPE", label: "新增或调整故障类型" },
  { value: "ASSET_INFO", label: "资产信息错误或缺失" },
  { value: "REPAIR_PROCESS", label: "报修与维修流程改进" },
  { value: "OTHER", label: "其他建议" },
];

export const suggestionCategoryText = (code, fallback) =>
  suggestionCategoryOptions.find((item) => item.value === code)?.label ||
  fallback ||
  code ||
  "其他";

// 资产分类启用状态
export const assetCategoryStatusOptions = [
  { value: 0, label: "启用", type: "success" },
  { value: 1, label: "停用", type: "danger" },
];

export const assetCategoryStatusText = (value) =>
  assetCategoryStatusOptions.find((item) => item.value === value)?.label ||
  value;

// 工单导出标记
export const exportedFlagOptions = [
  { value: 0, label: "未导出" },
  { value: 1, label: "已导出" },
];

export const exportedFlagText = (value) =>
  exportedFlagOptions.find((item) => item.value === value)?.label || "-";

// 格式化资产位置文本
export const formatAssetLocation = (asset) => {
  if (!asset) {
    return "";
  }
  const parts = [
    asset.campusName,
    asset.buildingName,
    asset.floor,
    asset.room,
    asset.locationDetail,
  ].filter((part) => part && String(part).trim());
  return parts.join(" ");
};

// 维修师傅接单状态选项
export const acceptingStateOptions = [
  { value: "AVAILABLE", label: "可接单", type: "success" },
  { value: "PAUSED", label: "暂停接单", type: "warning" },
];

export const acceptingStateText = (code, fallback) =>
  acceptingStateOptions.find((item) => item.value === code)?.label ||
  fallback ||
  code ||
  "未知";

// 待审核资产卡片状态选项
export const importItemStatusOptions = [
  { value: "PENDING", label: "待审核", type: "warning" },
  { value: "CONFIRMED", label: "已确认入库", type: "success" },
  { value: "IGNORED", label: "已忽略", type: "info" },
];

export const importItemStatusText = (code, fallback) =>
  importItemStatusOptions.find((item) => item.value === code)?.label ||
  fallback ||
  code ||
  "未知";

export const importItemStatusType = (code) =>
  importItemStatusOptions.find((item) => item.value === code)?.type || "info";

// 账号人工恢复申请状态选项
export const manualRecoveryStatusOptions = [
  { value: "PENDING", label: "待复核", type: "warning" },
  { value: "APPROVED", label: "已通过", type: "primary" },
  { value: "COMPLETED", label: "已完成", type: "success" },
  { value: "REJECTED", label: "已驳回", type: "danger" },
  { value: "EXPIRED", label: "已过期", type: "info" },
];

export const manualRecoveryStatusText = (code, fallback) =>
  manualRecoveryStatusOptions.find((item) => item.value === code)?.label ||
  fallback ||
  code ||
  "未知";

export const manualRecoveryStatusType = (code) =>
  manualRecoveryStatusOptions.find((item) => item.value === code)?.type || "info";

// 管理统计时间范围选项
export const statisticsRangeOptions = [
  { value: "LAST_7_DAYS", label: "最近七天" },
  { value: "LAST_30_DAYS", label: "最近三十天" },
  { value: "LAST_90_DAYS", label: "最近九十天" },
  { value: "CURRENT_YEAR", label: "本年度" },
];

export const statisticsRangeText = (code, fallback) =>
  statisticsRangeOptions.find((item) => item.value === code)?.label ||
  fallback ||
  code ||
  "最近三十天";

// 格式化已购入时长
export const formatPurchaseDuration = (years, months) => {
  if (years == null && months == null) {
    return "-";
  }
  const y = years || 0;
  const m = months || 0;
  if (y === 0 && m === 0) {
    return "不足1个月";
  }
  const parts = [];
  if (y > 0) {
    parts.push(`${y}年`);
  }
  if (m > 0) {
    parts.push(`${m}个月`);
  }
  return parts.join("") || "-";
};
