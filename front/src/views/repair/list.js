import { ElMessage } from "element-plus";
import { ArrowDown } from "@element-plus/icons-vue";
import { listOrders, listAvailableOrders, listCategories } from "@/api/repair/order";
import { getRepairerAvailability } from "@/api/repairer/index";
import { listLocationOptions } from "@/api/repair/location";
import { exportOrders } from "@/api/admin";
import { analyzeDispatch } from "@/api/ai/repair-order";
import useUserStore from "@/store/modules/user";
import { parseTime, blobValidate } from "@/utils/ruoyi";
import { saveAs } from "file-saver";
import {
  repairStatuses,
  repairStatusType,
  statusText,
  repairerQuickFilters,
  reporterQuickFilters,
  longStagnantLabel,
  exportedFlagOptions,
  exportedFlagText,
  acceptingStateText,
} from "@/utils/repair";

const ORDER_NO_PREFIX = "RO";
const orderNoPrefix = ORDER_NO_PREFIX;

function stripOrderNoPrefix(value) {
  if (!value) {
    return "";
  }
  const trimmed = String(value).trim();
  const upper = trimmed.toUpperCase();
  if (upper.startsWith(ORDER_NO_PREFIX)) {
    return trimmed.slice(ORDER_NO_PREFIX.length).trim();
  }
  return trimmed;
}

function buildOrderNoParam() {
  const suffix = stripOrderNoPrefix(query.orderNoSuffix);
  if (!suffix) {
    return "";
  }
  return `${ORDER_NO_PREFIX}${suffix}`;
}

function syncOrderNoSuffix(value) {
  const next = stripOrderNoPrefix(value);
  if (next !== query.orderNoSuffix) {
    query.orderNoSuffix = next;
  }
}

function onOrderNoInput(value) {
  syncOrderNoSuffix(value);
}

function onOrderNoPaste(event) {
  const pasted = event.clipboardData?.getData("text") || "";
  if (!pasted) {
    return;
  }
  event.preventDefault();
  syncOrderNoSuffix(pasted);
}

function formatCreateTime(value) {
  return parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") || "-";
}

function createRepair() {
  router.push("/repair/create");
}

// 接收父组件传递的 Props 参数
const props = defineProps({
  available: Boolean,
  reporter: Boolean,
  title: { type: String, default: "工单列表" },
});

// 路由及 Pinia Store 实例
const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

// 页面基础交互状态
const loading = ref(false);
const exporting = ref(false);
const exportMode = ref(false);
const error = ref("");
const categories = ref([]);
const locations = ref([]);
const createDateRange = ref([]);
const completionDateRange = ref([]);
const availability = ref(null);
const availabilityLoading = ref(false);
const pausedBlocked = ref(false);
const advancedVisible = ref(false);
const tableRef = ref(null);
const selectedRows = ref([]);

const aiAnalysisVisible = ref(false);
const aiAnalysisLoading = ref(false);
const aiAnalysisText = ref("");
const aiAnalysisOrderNo = ref("");

function canShowListAiAnalysis(row) {
  return isAdmin.value && [1, 2].includes(row.status);
}

async function runListAiAnalysis(row, event) {
  event?.stopPropagation?.();
  aiAnalysisOrderNo.value = row.orderNo || "";
  aiAnalysisText.value = "";
  aiAnalysisVisible.value = true;
  aiAnalysisLoading.value = true;
  try {
    const resp = await analyzeDispatch(row.orderId);
    aiAnalysisText.value = resp.analysisText || "暂无分析结果";
  } catch (err) {
    ElMessage.error(err?.message || "AI 分析失败");
    aiAnalysisVisible.value = false;
  } finally {
    aiAnalysisLoading.value = false;
  }
}

// 权限相关的角色计算属性
const role = computed(() => userStore.roles[0] || "");
const isAdmin = computed(() => role.value === "ADMIN");
const isRepairer = computed(() => role.value === "REPAIRER");
const isReporter = computed(() => ["STUDENT", "TEACHER"].includes(role.value));
const selectedCount = computed(() => getSelectedRows().length);

function getSelectedRows() {
  const rows = tableRef.value?.getSelectionRows?.();
  if (Array.isArray(rows) && rows.length) {
    return rows;
  }
  return selectedRows.value;
}

// 根据角色返回不同的快速筛选项列表
const quickFilters = computed(() => {
  if (isRepairer.value) {
    return repairerQuickFilters;
  }
  if (isReporter.value) {
    return reporterQuickFilters;
  }
  return [];
});

// 计算当前选中校区下的楼栋选项
const buildingOptions = computed(() => {
  const campus = locations.value.find((item) => item.campusId === query.campusId);
  return campus?.buildings || [];
});

// 工单分页查询表单数据
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNoSuffix: "",
  titleKeyword: "",
  status: null,
  categoryId: null,
  campusId: null,
  buildingId: null,
  reporterKeyword: "",
  repairerKeyword: "",
  assetNo: "",
  assetNameKeyword: "",
  exportedFlag: null,
  suspectedDuplicate: null,
  longStagnant: null,
  quickFilter: "",
  createTimeFrom: null,
  createTimeTo: null,
  completionTimeFrom: null,
  completionTimeTo: null,
});

// 分页列表展示数据源
const data = reactive({ records: [], total: 0 });

// 格式化日期选择的边界值，补充当天起始时间或结束时间
function formatDateBoundary(value, endOfDay = false) {
  if (!value) {
    return null;
  }
  const suffix = endOfDay ? " 23:59:59" : " 00:00:00";
  if (typeof value === "string" && value.includes(":")) {
    return value;
  }
  return `${parseTime(value, "{y}-{m}-{d}")}${suffix}`;
}

// 构建接口请求参数，排除空值或空字符串
function buildParams({ includePagination = true } = {}) {
  const params = {};
  if (includePagination) {
    params.pageNum = query.pageNum;
    params.pageSize = query.pageSize;
  }
  const fields = [
    "titleKeyword",
    "status",
    "categoryId",
    "campusId",
    "buildingId",
    "reporterKeyword",
    "repairerKeyword",
    "assetNo",
    "assetNameKeyword",
    "exportedFlag",
    "suspectedDuplicate",
    "quickFilter",
    "createTimeFrom",
    "createTimeTo",
    "completionTimeFrom",
    "completionTimeTo",
  ];
  fields.forEach((field) => {
    const value = query[field];
    if (value !== null && value !== undefined && value !== "") {
      params[field] = value;
    }
  });
  const orderNo = buildOrderNoParam();
  if (orderNo) {
    params.orderNo = orderNo;
  }
  if (query.longStagnant === true) {
    params.longStagnant = true;
  }
  return params;
}

async function loadAvailability() {
  if (!props.available || !isRepairer.value) {
    pausedBlocked.value = false;
    availability.value = null;
    return;
  }
  availabilityLoading.value = true;
  try {
    availability.value = await getRepairerAvailability();
    pausedBlocked.value = availability.value?.acceptingState === "PAUSED";
  } catch (err) {
    error.value = err.message || "接单状态加载失败";
    pausedBlocked.value = false;
  } finally {
    availabilityLoading.value = false;
  }
}

// 异步加载工单列表（区分“待接单池”与“普通查询列表”）
async function load() {
  if (props.available && isRepairer.value && pausedBlocked.value) {
    Object.assign(data, { records: [], total: 0 });
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    const result = await (props.available
      ? listAvailableOrders(buildParams())
      : listOrders(buildParams()));
    Object.assign(data, result);
  } catch (err) {
    error.value = err.message || "加载失败";
  } finally {
    loading.value = false;
  }
}

// 触发搜索操作，重置页码为1
function search() {
  query.pageNum = 1;
  load();
}

// 重置所有搜索过滤参数
function resetFilters() {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    orderNoSuffix: "",
    titleKeyword: "",
    status: null,
    categoryId: null,
    campusId: null,
    buildingId: null,
    reporterKeyword: "",
    repairerKeyword: "",
    assetNo: "",
    assetNameKeyword: "",
    exportedFlag: null,
    suspectedDuplicate: null,
    longStagnant: null,
    quickFilter: "",
    createTimeFrom: null,
    createTimeTo: null,
    completionTimeFrom: null,
    completionTimeTo: null,
  });
  createDateRange.value = [];
  completionDateRange.value = [];
  advancedVisible.value = false;
  if (exportMode.value) {
    clearSelection();
  }
  load();
}

function toggleAdvanced() {
  advancedVisible.value = !advancedVisible.value;
}

// 应用或切换快捷过滤标签
function applyQuickFilter(value) {
  query.quickFilter = query.quickFilter === value ? "" : value;
  search();
}

// 变更校区选择时自动清空选中的楼栋并触发搜索
function onCampusChange() {
  query.buildingId = null;
  search();
}

// 创建日期范围选择器值变更监听
function onCreateDateChange(range) {
  query.createTimeFrom = formatDateBoundary(range?.[0]);
  query.createTimeTo = formatDateBoundary(range?.[1], true);
  search();
}

// 完成日期范围选择器值变更监听
function onCompletionDateChange(range) {
  query.completionTimeFrom = formatDateBoundary(range?.[0]);
  query.completionTimeTo = formatDateBoundary(range?.[1], true);
  search();
}

function onRowClick(row, column) {
  if (column?.label === "AI 辅助") {
    return;
  }
  if (exportMode.value) {
    if (column?.type === "selection") {
      return;
    }
    tableRef.value?.toggleRowSelection(row);
    return;
  }
  open(row);
}

function onSelectionChange(rows) {
  selectedRows.value = rows;
}

function enterExportMode() {
  exportMode.value = true;
  selectedRows.value = [];
  nextTick(() => {
    tableRef.value?.clearSelection();
  });
}

function exitExportMode() {
  exportMode.value = false;
  selectedRows.value = [];
  nextTick(() => {
    tableRef.value?.clearSelection();
  });
}

function selectCurrentPage() {
  data.records.forEach((row) => {
    tableRef.value?.toggleRowSelection(row, true, true);
  });
}

function clearSelection() {
  selectedRows.value = [];
  tableRef.value?.clearSelection();
}

// 点击打开单条工单详情
function open(row) {
  router.push(`/repair/detail/${row.orderId}`);
}

async function doExport(params) {
  exporting.value = true;
  try {
    const blob = await exportOrders(params);
    if (!blobValidate(blob)) {
      const text = await blob.text();
      const result = JSON.parse(text);
      ElMessage.error(result.message || "导出失败，请缩小筛选范围");
      return;
    }
    saveAs(blob, `repair_orders_${parseTime(new Date(), "{y}{m}{d}{h}{i}{s}")}.xlsx`);
    ElMessage.success("导出成功");
    exitExportMode();
    await load();
  } catch (err) {
    ElMessage.error(err?.message || "导出失败");
  } finally {
    exporting.value = false;
  }
}

async function exportFiltered() {
  if (!data.total) {
    ElMessage.warning("当前没有可导出的工单");
    return;
  }
  await doExport(buildParams({ includePagination: false }));
}

async function exportSelected() {
  const rows = getSelectedRows();
  const orderIds = [
    ...new Set(
      rows
        .map((row) => row.orderId)
        .filter((id) => id !== null && id !== undefined && id !== ""),
    ),
  ];
  if (!orderIds.length) {
    ElMessage.warning("请先勾选要导出的工单");
    return;
  }
  await doExport({ orderIds: orderIds.map(String).join(",") });
}

// 解析路由中的 Query 传参（例如从仪表盘卡片点入时），回显到筛选表单中
function applyRouteQuery() {
  const routeQuery = route.query;
  if (!Object.keys(routeQuery).length) {
    return;
  }
  if (routeQuery.status !== undefined) {
    query.status = Number(routeQuery.status);
  }
  if (routeQuery.longStagnant !== undefined) {
    query.longStagnant = routeQuery.longStagnant === "true";
  }
  if (routeQuery.quickFilter) {
    query.quickFilter = String(routeQuery.quickFilter);
  }
  if (routeQuery.createTimeFrom) {
    query.createTimeFrom = String(routeQuery.createTimeFrom);
    createDateRange.value = [routeQuery.createTimeFrom, routeQuery.createTimeTo];
  }
  if (routeQuery.completionTimeFrom) {
    query.completionTimeFrom = String(routeQuery.completionTimeFrom);
    completionDateRange.value = [
      routeQuery.completionTimeFrom,
      routeQuery.completionTimeTo,
    ];
    advancedVisible.value = true;
  }
  if (routeQuery.longStagnant !== undefined) {
    advancedVisible.value = true;
  }
  router.replace({ path: route.path, query: {} });
}

// 页面加载时的统一初始化入口（加载过滤下拉项、应用路由 Query、触发首次加载）
async function init() {
  try {
    const [categoryList, locationList] = await Promise.all([
      listCategories(),
      listLocationOptions(),
    ]);
    categories.value = categoryList;
    locations.value = locationList;
  } catch (err) {
    error.value = err.message || "筛选数据加载失败";
  }
  applyRouteQuery();
  await loadAvailability();
  await load();
}

// 组件挂载时调用初始化
onMounted(init);
