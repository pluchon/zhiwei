import { ElMessage, ElMessageBox } from "element-plus";
import {
  adminListAssetCategories,
  createAssetCategory,
  updateAssetCategory,
  disableAssetCategory,
  exportAssetCategories,
} from "@/api/asset/category";
import { assetCategoryStatusOptions, assetCategoryStatusText } from "@/utils/repair";
import { parseTime, blobValidate } from "@/utils/ruoyi";
import { saveAs } from "file-saver";

// 页面加载与保存状态
const loading = ref(false);
const saving = ref(false);
const exporting = ref(false);
const exportMode = ref(false);
const tableRef = ref(null);
const selectedRows = ref([]);

// 分页数据与查询参数
const query = reactive({ pageNum: 1, pageSize: 10 });
const filters = reactive({ keyword: "", status: null });
const filtersApplied = reactive({ keyword: "", status: null });
const data = reactive({ records: [], total: 0 });

// 对话框状态
const dialog = ref(false);
const dialogMode = ref("create");
const form = reactive({ categoryName: "" });
let editingId = null;

const dialogTitle = computed(() =>
  dialogMode.value === "create" ? "新增分类" : "编辑分类",
);

const selectedCount = computed(() => selectedRows.value.length);

function buildParams({ includePagination = true } = {}) {
  const params = {};
  if (includePagination) {
    params.pageNum = query.pageNum;
    params.pageSize = query.pageSize;
  }
  const keyword = filtersApplied.keyword?.trim();
  if (keyword) {
    params.keyword = keyword;
  }
  if (filtersApplied.status !== null && filtersApplied.status !== "") {
    params.status = filtersApplied.status;
  }
  return params;
}

function formatTime(value) {
  return value ? parseTime(value) : "-";
}

function search() {
  filtersApplied.keyword = filters.keyword;
  filtersApplied.status = filters.status;
  query.pageNum = 1;
  if (exportMode.value) {
    clearSelection();
  }
  load();
}

function resetFilters() {
  filters.keyword = "";
  filters.status = null;
  search();
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

async function doExport(params) {
  exporting.value = true;
  try {
    const blob = await exportAssetCategories(params);
    if (!blobValidate(blob)) {
      const text = await blob.text();
      const result = JSON.parse(text);
      ElMessage.error(result.message || "导出失败，请缩小筛选范围");
      return;
    }
    saveAs(blob, `asset_categories_${parseTime(new Date(), "{y}{m}{d}{h}{i}{s}")}.xlsx`);
    ElMessage.success("导出成功");
    exitExportMode();
    await load();
  } catch (error) {
    ElMessage.error(readErrorMessage(error));
  } finally {
    exporting.value = false;
  }
}

async function exportFiltered() {
  if (!data.total) {
    ElMessage.warning("当前没有可导出的分类");
    return;
  }
  await doExport(buildParams({ includePagination: false }));
}

async function exportSelected() {
  const rows = tableRef.value?.getSelectionRows() ?? [];
  const ids = [
    ...new Set(
      rows
        .map((row) => row.assetCategoryId)
        .filter((id) => id !== null && id !== undefined && id !== ""),
    ),
  ];
  if (!ids.length) {
    ElMessage.warning("请先勾选要导出的分类");
    return;
  }
  await doExport({ assetCategoryIds: ids.map(String).join(",") });
}

// 加载资产分类列表
async function load() {
  loading.value = true;
  try {
    Object.assign(data, await adminListAssetCategories(buildParams()));
  } finally {
    loading.value = false;
  }
}

// 打开新增对话框
function openCreate() {
  dialogMode.value = "create";
  editingId = null;
  form.categoryName = "";
  dialog.value = true;
}

// 打开编辑对话框
function openEdit(row) {
  dialogMode.value = "edit";
  editingId = row.assetCategoryId;
  form.categoryName = row.categoryName;
  dialog.value = true;
}

// 提交新增或编辑表单
async function submitForm() {
  const name = form.categoryName?.trim();
  if (!name) {
    ElMessage.warning("请填写分类名称");
    return;
  }
  saving.value = true;
  try {
    if (dialogMode.value === "create") {
      await createAssetCategory({ categoryName: name });
      ElMessage.success("分类已新增");
    } else {
      await updateAssetCategory(editingId, { categoryName: name });
      ElMessage.success("分类已保存");
    }
    dialog.value = false;
    await load();
  } catch (error) {
    ElMessage.error(readErrorMessage(error));
  } finally {
    saving.value = false;
  }
}

function readErrorMessage(error) {
  return error?.response?.data?.message || error?.message || "操作失败";
}

async function confirmAndRun(message, runner) {
  try {
    await ElMessageBox.confirm(message, "操作确认", {
      type: "warning",
      confirmButtonText: "继续",
      cancelButtonText: "取消",
    });
    await runner();
  } catch (error) {
    if (error === "cancel" || error === "close") {
      return;
    }
    ElMessage.error(readErrorMessage(error));
  }
}

// 启用已停用的分类
function handleEnable(row) {
  confirmAndRun("启用后该分类可再次用于资产维护，是否继续？", async () => {
    await updateAssetCategory(row.assetCategoryId, { status: 0 });
    ElMessage.success("分类已启用");
    await load();
  });
}

// 停用分类
function handleDisable(row) {
  confirmAndRun("停用后该分类将不再出现在资产选择列表，是否继续？", async () => {
    await disableAssetCategory(row.assetCategoryId);
    ElMessage.success("分类已停用");
    await load();
  });
}

onMounted(load);
