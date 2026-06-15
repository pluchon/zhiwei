import { ElMessage } from "element-plus";
import { getAssetRepairHistory } from "@/api/asset/index";
import {
  getManagementStatistics,
  exportManagementStatistics,
} from "@/api/statistics/management";
import CampusChart from "@/components/CampusChart/index.vue";
import { parseTime, blobValidate } from "@/utils/ruoyi";
import { saveAs } from "file-saver";
import {
  buildCategoryRepairOption,
  buildLineTrendOption,
  buildTopAssetBarOption,
} from "@/utils/chartBuilders";
import {
  statisticsRangeOptions,
  statisticsRangeText,
  assetStatusText,
  assetStatusType,
} from "@/utils/repair";

const loading = ref(false);
const exporting = ref(false);
const rangeType = ref("LAST_30_DAYS");
const stats = ref(null);

const historyVisible = ref(false);
const historyLoading = ref(false);
const history = reactive({ records: [], total: 0 });

const rangeLabel = computed(() =>
  stats.value?.rangeTypeLabel || statisticsRangeText(rangeType.value),
);

const rangeMeta = computed(() => {
  if (!stats.value?.rangeStart || !stats.value?.rangeEnd) {
    return rangeLabel.value;
  }
  return `${rangeLabel.value} · ${stats.value.rangeStart} 至 ${stats.value.rangeEnd}`;
});

const kpiCards = computed(() => {
  const efficiency = stats.value?.repairEfficiency || {};
  return [
    { title: "已完成工单", value: efficiency.completedCount ?? 0, tone: "success" },
    { title: "超 7 天才完成", value: efficiency.overSevenDaysCount ?? 0, tone: "danger" },
    { title: "未完成工单", value: efficiency.unfinishedCount ?? 0, tone: "warning" },
  ];
});

const unfinishedTrendOption = computed(() =>
  buildLineTrendOption("未完成工单", stats.value?.unfinishedOrderTrend || []),
);

const categoryChartOption = computed(() =>
  buildCategoryRepairOption(stats.value?.assetCategoryRepairs || []),
);

const topAssetChartOption = computed(() =>
  buildTopAssetBarOption(stats.value?.topRepairedAssets || []),
);

function formatTime(value) {
  return value ? parseTime(value) : "-";
}

function formatHistoryLine(row) {
  const parts = [
    row.orderNo || "-",
    row.categoryName || "未知类型",
    row.repairerRealName || row.repairerUserNo || "未分配",
    formatTime(row.completionTime),
  ];
  return parts.join(" · ");
}

async function load() {
  loading.value = true;
  try {
    stats.value = await getManagementStatistics({ rangeType: rangeType.value });
  } catch (error) {
    ElMessage.error(error?.message || "统计加载失败");
  } finally {
    loading.value = false;
  }
}

async function handleExport() {
  exporting.value = true;
  try {
    const blob = await exportManagementStatistics({ rangeType: rangeType.value });
    if (!blobValidate(blob)) {
      const text = await blob.text();
      const result = JSON.parse(text);
      ElMessage.error(result.message || "导出失败");
      return;
    }
    saveAs(blob, `management_statistics_${parseTime(new Date(), "{y}{m}{d}{h}{i}{s}")}.xlsx`);
    ElMessage.success("导出成功");
  } catch (error) {
    ElMessage.error(error?.message || "导出失败");
  } finally {
    exporting.value = false;
  }
}

async function openRepairHistory(row) {
  if (!row?.assetId) {
    return;
  }
  historyVisible.value = true;
  historyLoading.value = true;
  history.records = [];
  try {
    Object.assign(history, await getAssetRepairHistory(row.assetId, { pageNum: 1, pageSize: 20 }));
  } catch (error) {
    ElMessage.error(error?.message || "维修历史加载失败");
    historyVisible.value = false;
  } finally {
    historyLoading.value = false;
  }
}

function onRangeChange() {
  load();
}

onMounted(load);
