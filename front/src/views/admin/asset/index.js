import { ElMessage, ElMessageBox } from "element-plus";
import { Plus } from "@element-plus/icons-vue";
import { listEnabledAssetCategories } from "@/api/asset/category";
import {
  listAssets,
  getAsset,
  createAsset,
  updateAsset,
  changeAssetStatus,
  deleteAsset,
  restoreAsset,
  uploadAssetImage,
  getAssetRepairHistory,
} from "@/api/asset/index";
import { listLocationOptions } from "@/api/repair/location";
import {
  assetStatusOptions,
  assetStatusText,
  assetStatusType,
  formatAssetLocation,
} from "@/utils/repair";

// 页面状态
const loading = ref(false);
const saving = ref(false);
const historyLoading = ref(false);

const ASSET_NO_PREFIX = "AST-";

// 筛选与列表数据
const categories = ref([]);
const enabledCategories = ref([]);
const locations = ref([]);
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  assetNoSuffix: "",
  assetNameKeyword: "",
  assetCategoryId: null,
  campusId: null,
  buildingId: null,
  status: null,
  includeDeleted: false,
});
const data = reactive({ records: [], total: 0 });

// 对话框与表单
const dialog = ref(false);
const dialogMode = ref("create");
const form = reactive({
  assetName: "",
  assetCategoryId: null,
  campusId: null,
  buildingId: null,
  floor: "",
  room: "",
  locationDetail: "",
  description: "",
  enabledDate: null,
  purchaseDate: null,
  imageObjectKey: "",
  imageSignedUrl: "",
  version: null,
});
const imageFiles = ref([]);
let editingId = null;

// 详情弹窗
const detailVisible = ref(false);
const detail = ref(null);
const history = reactive({ records: [], total: 0 });
const historyQuery = reactive({ pageNum: 1, pageSize: 10 });
let currentAssetId = null;

const dialogTitle = computed(() =>
  dialogMode.value === "create" ? "新增资产" : "编辑资产",
);

const buildingOptions = computed(() => {
  const campus = locations.value.find((item) => item.campusId === query.campusId);
  return campus?.buildings || [];
});

const formBuildingOptions = computed(() => {
  const campus = locations.value.find((item) => item.campusId === form.campusId);
  return campus?.buildings || [];
});

function stripAssetNoPrefix(value) {
  const trimmed = (value || "").trim();
  if (!trimmed) {
    return "";
  }
  const normalized = trimmed.toUpperCase().startsWith(ASSET_NO_PREFIX)
    ? trimmed.slice(ASSET_NO_PREFIX.length)
    : trimmed;
  return normalized.trim();
}

function buildAssetNoParam() {
  const suffix = stripAssetNoPrefix(query.assetNoSuffix);
  if (!suffix) {
    return "";
  }
  return `${ASSET_NO_PREFIX}${suffix}`;
}

function syncAssetNoSuffix(value) {
  const next = stripAssetNoSuffix(value);
  if (next !== query.assetNoSuffix) {
    query.assetNoSuffix = next;
  }
}

function onAssetNoInput(value) {
  syncAssetNoSuffix(value);
}

function onAssetNoPaste(event) {
  const pasted = event.clipboardData?.getData("text") || "";
  if (!pasted) {
    return;
  }
  event.preventDefault();
  syncAssetNoSuffix(pasted);
}

function readErrorMessage(error) {
  return error?.response?.data?.message || error?.message || "操作失败";
}

// 加载筛选选项
async function loadOptions() {
  const [enabledList, locationList] = await Promise.all([
    listEnabledAssetCategories(),
    listLocationOptions(),
  ]);
  categories.value = enabledList;
  enabledCategories.value = enabledList;
  locations.value = locationList;
}

// 加载资产列表
async function load() {
  loading.value = true;
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    };
    const assetNo = buildAssetNoParam();
    if (assetNo) {
      params.assetNo = assetNo;
    }
    if (query.assetNameKeyword?.trim()) {
      params.assetNameKeyword = query.assetNameKeyword.trim();
    }
    if (query.assetCategoryId) {
      params.assetCategoryId = query.assetCategoryId;
    }
    if (query.campusId) {
      params.campusId = query.campusId;
    }
    if (query.buildingId) {
      params.buildingId = query.buildingId;
    }
    if (query.status) {
      params.status = query.status;
    }
    if (query.includeDeleted) {
      params.includeDeleted = true;
    }
    Object.assign(data, await listAssets(params));
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  load();
}

function resetFilters() {
  Object.assign(query, {
    pageNum: 1,
    pageSize: 10,
    assetNoSuffix: "",
    assetNameKeyword: "",
    assetCategoryId: null,
    campusId: null,
    buildingId: null,
    status: null,
    includeDeleted: false,
  });
  load();
}

function onCampusChange() {
  query.buildingId = null;
  search();
}

function onFormCampusChange() {
  form.buildingId = null;
}

function resetForm() {
  Object.assign(form, {
    assetName: "",
    assetCategoryId: null,
    campusId: null,
    buildingId: null,
    floor: "",
    room: "",
    locationDetail: "",
    description: "",
    enabledDate: null,
    purchaseDate: null,
    imageObjectKey: "",
    imageSignedUrl: "",
    version: null,
  });
  imageFiles.value = [];
  editingId = null;
}

function openCreate() {
  dialogMode.value = "create";
  resetForm();
  dialog.value = true;
}

async function openEdit(row) {
  dialogMode.value = "edit";
  resetForm();
  editingId = row.assetId;
  const asset = await getAsset(row.assetId);
  Object.assign(form, {
    assetName: asset.assetName,
    assetCategoryId: asset.assetCategoryId,
    campusId: asset.campusId,
    buildingId: asset.buildingId,
    floor: asset.floor || "",
    room: asset.room || "",
    locationDetail: asset.locationDetail || "",
    description: asset.description || "",
    enabledDate: asset.enabledDate || null,
    purchaseDate: asset.purchaseDate || null,
    imageObjectKey: asset.imageObjectKey || "",
    imageSignedUrl: asset.imageSignedUrl || "",
    version: asset.version,
  });
  dialog.value = true;
}

async function loadHistory() {
  if (!currentAssetId) {
    return;
  }
  historyLoading.value = true;
  try {
    Object.assign(history, await getAssetRepairHistory(currentAssetId, historyQuery));
  } finally {
    historyLoading.value = false;
  }
}

async function openDetail(row) {
  currentAssetId = row.assetId;
  historyQuery.pageNum = 1;
  detailVisible.value = true;
  detail.value = await getAsset(row.assetId);
  await loadHistory();
}

function onImageChange(file, fileList) {
  imageFiles.value = fileList.slice(-1);
}

function buildPayload() {
  return {
    assetName: form.assetName?.trim(),
    assetCategoryId: form.assetCategoryId,
    campusId: form.campusId,
    buildingId: form.buildingId || null,
    floor: form.floor?.trim() || "",
    room: form.room?.trim() || "",
    locationDetail: form.locationDetail?.trim() || "",
    description: form.description?.trim() || "",
    enabledDate: form.enabledDate || null,
    purchaseDate: form.purchaseDate || null,
    imageObjectKey: form.imageObjectKey || null,
    version: form.version,
  };
}

function validateForm() {
  if (!form.assetName?.trim()) {
    ElMessage.warning("请填写资产名称");
    return false;
  }
  if (!form.assetCategoryId) {
    ElMessage.warning("请选择资产分类");
    return false;
  }
  if (!form.campusId) {
    ElMessage.warning("请选择校区");
    return false;
  }
  return true;
}

async function submitForm() {
  if (!validateForm()) {
    return;
  }
  saving.value = true;
  try {
    if (imageFiles.value.length && imageFiles.value[0].raw) {
      form.imageObjectKey = await uploadAssetImage(imageFiles.value[0].raw);
    }
    const payload = buildPayload();
    if (dialogMode.value === "create") {
      await createAsset(payload);
      ElMessage.success("资产已新增");
    } else {
      await updateAsset(editingId, payload);
      ElMessage.success("资产已保存");
    }
    dialog.value = false;
    await load();
  } catch (error) {
    ElMessage.error(readErrorMessage(error));
  } finally {
    saving.value = false;
  }
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

function handleEnable(row) {
  confirmAndRun("启用后该资产将恢复为使用中，是否继续？", async () => {
    const { value } = await ElMessageBox.prompt("请填写变更原因", "启用资产", {
      inputType: "textarea",
    });
    await changeAssetStatus(row.assetId, {
      status: "IN_USE",
      changeReason: value,
      version: row.version,
    });
    ElMessage.success("资产已启用");
    await load();
  });
}

function handleDisable(row) {
  confirmAndRun("停用后该资产将不可用于新业务，是否继续？", async () => {
    const { value } = await ElMessageBox.prompt("请填写变更原因", "停用资产", {
      inputType: "textarea",
    });
    await changeAssetStatus(row.assetId, {
      status: "OUT_OF_SERVICE",
      changeReason: value,
      version: row.version,
    });
    ElMessage.success("资产已停用");
    await load();
  });
}

function handleDelete(row) {
  confirmAndRun("确认逻辑删除该资产？删除后可通过恢复功能找回。", async () => {
    await deleteAsset(row.assetId, row.version);
    ElMessage.success("资产已删除");
    await load();
  });
}

function handleRestore(row) {
  confirmAndRun("恢复前将校验编号、分类和位置，是否继续？", async () => {
    await restoreAsset(row.assetId);
    ElMessage.success("资产已恢复");
    await load();
  });
}

onMounted(async () => {
  await loadOptions();
  await load();
});
