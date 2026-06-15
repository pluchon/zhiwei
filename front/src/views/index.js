import { ElMessage } from "element-plus";
import { getRepairDashboard } from "@/api/admin";
import { getReporterDashboard } from "@/api/repair/order";
import { getRepairerDashboard } from "@/api/repairer/index";
import { listNotifications } from "@/api/notification";
import CampusChart from "@/components/CampusChart/index.vue";
import useUserStore from "@/store/modules/user";
import {
  buildDonutOption,
  buildHorizontalBarOption,
  buildLineTrendOption,
  buildVerticalBarOption,
} from "@/utils/chartBuilders";
import { parseTime } from "@/utils/ruoyi";
import { acceptingStateText, statusText } from "@/utils/repair";

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const dashboard = ref(null);
const reporterDashboard = ref(null);
const repairerDashboard = ref(null);
const unreadCount = ref(0);
const rangeDays = ref(30);

const rangeOptions = [
  { value: 7, label: "最近 7 天" },
  { value: 30, label: "最近 30 天" },
  { value: 90, label: "最近 90 天" },
];

const isAdmin = computed(() => userStore.roles[0] === "ADMIN");
const isReporter = computed(() => ["STUDENT", "TEACHER"].includes(userStore.roles[0] || ""));
const isRepairer = computed(() => userStore.roles[0] === "REPAIRER");

const dashboardPageClass = computed(() => {
  if (isReporter.value) {
    return "dashboard-page--reporter";
  }
  if (isRepairer.value) {
    return "dashboard-page--repairer";
  }
  return "";
});

const reporterDisplayName = computed(() => userStore.nickName || userStore.userNo || "同学");
const repairerDisplayName = computed(() => userStore.nickName || userStore.userNo || "师傅");

const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 11) {
    return "早上好";
  }
  if (hour < 14) {
    return "中午好";
  }
  if (hour < 18) {
    return "下午好";
  }
  return "晚上好";
});

const greetingText = computed(() => `${greeting.value}，${reporterDisplayName.value}`);
const repairerGreetingText = computed(() => `${greeting.value}，${repairerDisplayName.value}`);

const repairerAcceptingLabel = computed(() => {
  const workStat = repairerDashboard.value?.workStat;
  return workStat?.acceptingStateLabel || acceptingStateText(workStat?.acceptingState);
});

const repairerStateTagClass = computed(() => {
  const state = repairerDashboard.value?.workStat?.acceptingState;
  if (state === "PAUSED") {
    return "repairer-state-tag--paused";
  }
  if (state === "BUSY") {
    return "repairer-state-tag--busy";
  }
  return "repairer-state-tag--available";
});

const dashboardCards = computed(() => {
  if (!dashboard.value) {
    return [];
  }
  const data = dashboard.value;
  const today = new Date();
  const todayFrom = `${parseTime(today, "{y}-{m}-{d}")} 00:00:00`;
  const todayTo = `${parseTime(today, "{y}-{m}-{d}")} 23:59:59`;
  return [
    { title: "待匹配", value: data.pendingDispatch, tone: "primary", query: { status: 1 } },
    { title: "待接单", value: data.pendingAccept, tone: "primary", query: { status: 2 } },
    { title: "已接单", value: data.accepted, tone: "info", query: { status: 3 } },
    { title: "处理中", value: data.processing, tone: "info", query: { status: 4 } },
    { title: "待确认", value: data.pendingConfirm, tone: "warning", query: { status: 5 } },
    { title: "待仲裁", value: data.pendingArbitration, tone: "danger", query: { status: 6 } },
    { title: "长时间未进展", value: data.longStagnant, tone: "danger", query: { longStagnant: "true" } },
    { title: "今日新增", value: data.todayCreated, tone: "accent", query: { createTimeFrom: todayFrom, createTimeTo: todayTo } },
    { title: "今日完成", value: data.todayCompleted, tone: "success", query: { completionTimeFrom: todayFrom, completionTimeTo: todayTo } },
  ];
});

const reporterCards = computed(() => {
  if (!reporterDashboard.value) {
    return [];
  }
  const data = reporterDashboard.value;
  return [
    {
      title: "进行中",
      value: data.inProgress,
      tone: "primary",
      hint: "维修流程尚未结束",
      action: () => openReporterList({ quickFilter: "REPORTER_PROCESSING" }),
    },
    {
      title: "待我确认",
      value: data.pendingConfirm,
      tone: "warning",
      hint: "维修已完成，等待确认",
      action: () => openReporterList({ quickFilter: "REPORTER_PENDING_CONFIRM" }),
    },
    {
      title: "已完成",
      value: data.completed,
      tone: "success",
      hint: "已结束的历史工单",
      action: () => openReporterList({ quickFilter: "REPORTER_ENDED" }),
    },
    {
      title: "草稿",
      value: data.draft,
      tone: "info",
      hint: "尚未提交的报修",
      action: () => openReporterList({ status: 0 }),
    },
    {
      title: "未读通知",
      value: unreadCount.value,
      tone: "accent",
      hint: "工单与系统提醒",
      action: () => router.push("/notifications"),
    },
  ];
});

const repairerCards = computed(() => {
  if (!repairerDashboard.value) {
    return [];
  }
  const data = repairerDashboard.value;
  const workStat = data.workStat || {};
  return [
    {
      title: "处理中",
      value: data.inProgress,
      tone: "primary",
      action: () => openRepairerList({ quickFilter: "REPAIRER_PROCESSING" }),
    },
    {
      title: "待确认",
      value: data.pendingConfirm,
      tone: "warning",
      action: () => openRepairerList({ quickFilter: "REPAIRER_PENDING_CONFIRM" }),
    },
    {
      title: "已完成",
      value: data.completed,
      tone: "success",
      action: () => openRepairerList({ quickFilter: "REPAIRER_COMPLETED" }),
    },
    {
      title: "范围接单",
      value: workStat.acceptCount ?? 0,
      tone: "info",
      action: () => openRepairerList(),
    },
    {
      title: "未读通知",
      value: unreadCount.value,
      tone: "accent",
      action: () => router.push("/notifications"),
    },
  ];
});

function formatStatusItems(items = []) {
  return items.map((item) => ({
    name: formatStatusDistributionName(item.name),
    count: item.count,
  }));
}

function formatStatusDistributionName(name) {
  const statusIndex = Number(name);
  if (!Number.isNaN(statusIndex) && statusText(statusIndex)) {
    return statusText(statusIndex);
  }
  return name;
}

const donutChartOptions = {
  legendPosition: "right",
  legendNameOnly: true,
  tooltipPercentOnly: true,
};

const statusChartOption = computed(() =>
  buildDonutOption(
    "状态分布",
    formatStatusItems(dashboard.value?.currentStatusDistribution),
    donutChartOptions,
  ),
);

const faultTypeChartOption = computed(() =>
  buildDonutOption("故障类型", dashboard.value?.faultTypeDistribution || [], donutChartOptions),
);

const campusChartOption = computed(() =>
  buildVerticalBarOption("校区分布", dashboard.value?.campusDistribution || []),
);

const buildingChartOption = computed(() =>
  buildHorizontalBarOption("楼栋分布", dashboard.value?.buildingDistribution || [], { maxItems: 8 }),
);

const reporterStatusChartOption = computed(() =>
  buildDonutOption("我的报修状态", reporterDashboard.value?.statusDistribution || [], {
    legendPosition: "right",
    legendNameOnly: true,
    tooltipPercentOnly: true,
  }),
);

const reporterFaultChartOption = computed(() =>
  buildDonutOption("报修类型", reporterDashboard.value?.faultTypeDistribution || [], {
    legendPosition: "bottom",
    legendNameOnly: true,
    tooltipPercentOnly: true,
    compact: true,
  }),
);

const reporterTrendChartOption = computed(() =>
  buildLineTrendOption("提交次数", reporterDashboard.value?.submitTrend || [], { unit: "次" }),
);

const repairerStatusChartOption = computed(() =>
  buildDonutOption("我的工单状态", repairerDashboard.value?.statusDistribution || [], {
    legendPosition: "right",
    legendNameOnly: true,
    tooltipPercentOnly: true,
  }),
);

const repairerFaultChartOption = computed(() =>
  buildDonutOption("维修类型", repairerDashboard.value?.faultTypeDistribution || [], {
    legendPosition: "bottom",
    legendNameOnly: true,
    tooltipPercentOnly: true,
    compact: true,
  }),
);

const repairerTrendChartOption = computed(() =>
  buildLineTrendOption("完成单数", repairerDashboard.value?.completionTrend || [], { unit: "单" }),
);

async function loadUnreadCount() {
  try {
    const result = await listNotifications({ pageNum: 1, pageSize: 1, isRead: 0 });
    unreadCount.value = Number(result?.total || 0);
  } catch {
    unreadCount.value = 0;
  }
}

async function loadDashboard() {
  if (isAdmin.value) {
    loading.value = true;
    try {
      dashboard.value = await getRepairDashboard(rangeDays.value);
    } catch (error) {
      ElMessage.error(error.message || "看板加载失败");
    } finally {
      loading.value = false;
    }
    return;
  }
  if (isReporter.value) {
    loading.value = true;
    try {
      await loadUnreadCount();
      reporterDashboard.value = await getReporterDashboard(rangeDays.value);
    } catch (error) {
      ElMessage.error(error.message || "首页加载失败");
    } finally {
      loading.value = false;
    }
    return;
  }
  if (isRepairer.value) {
    loading.value = true;
    try {
      await loadUnreadCount();
      repairerDashboard.value = await getRepairerDashboard(rangeDays.value);
    } catch (error) {
      ElMessage.error(error.message || "首页加载失败");
    } finally {
      loading.value = false;
    }
  }
}

function onRangeChange() {
  loadDashboard();
}

function openCard(card) {
  router.push({ path: "/admin/orders", query: card.query });
}

function openReporterList(query = {}) {
  router.push({ path: "/repair/mine", query });
}

function openRepairCreate() {
  router.push("/repair/create");
}

function openRepairerList(query = {}) {
  router.push({ path: "/work-order/mine", query });
}

function openAvailableOrders() {
  router.push("/work-order/available");
}

function openOrderDetail(orderId) {
  router.push(`/repair/detail/${orderId}`);
}

function formatRecentTime(value) {
  return parseTime(value, "{m}-{d} {h}:{i}");
}

onMounted(loadDashboard);
