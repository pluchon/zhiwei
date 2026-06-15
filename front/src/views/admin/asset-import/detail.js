import { ElMessage, ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { listEnabledAssetCategories } from "@/api/asset/category";
import { listLocationOptions } from "@/api/repair/location";
import {
  getAssetImportBatch,
  listAssetImportItems,
  updateAssetImportItem,
  ignoreAssetImportItem,
  confirmAssetImportItem,
  confirmAssetImportBatch,
  deleteAssetImportBatch,
} from "@/api/asset/import";
import {
  importItemStatusOptions,
  importItemStatusText,
  importItemStatusType,
} from "@/utils/repair";

const route = useRoute();
const router = useRouter();
const batchId = computed(() => Number(route.params.batchId));

const loading = ref(false);
const batchLoading = ref(false);
const saving = ref(false);
const confirming = ref(false);
const batch = ref(null);
const categories = ref([]);
const locations = ref([]);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null,
});
const data = reactive({ records: [], total: 0 });

const editDialog = ref(false);
const editForm = reactive({
  assetName: "",
  assetCategoryId: null,
  purchaseDate: null,
  enabledDate: null,
  assetDescription: "",
  locationText: "",
  campusId: null,
  buildingId: null,
  floor: "",
  room: "",
  locationDetail: "",
});
let editingItemId = null;

const confirmResultVisible = ref(false);
const confirmResult = ref(null);

const formBuildingOptions = computed(() => {
  const campus = locations.value.find((item) => item.campusId === editForm.campusId);
  return campus?.buildings || [];
});

async function loadBatch() {
  batchLoading.value = true;
  try {
    batch.value = await getAssetImportBatch(batchId.value);
  } catch (error) {
    ElMessage.error(error?.message || "批次加载失败");
  } finally {
    batchLoading.value = false;
  }
}

async function loadItems() {
  loading.value = true;
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    };
    if (query.status) {
      params.status = query.status;
    }
    Object.assign(data, await listAssetImportItems(batchId.value, params));
  } catch (error) {
    ElMessage.error(error?.message || "卡片加载失败");
  } finally {
    loading.value = false;
  }
}

async function loadOptions() {
  const [categoryList, locationList] = await Promise.all([
    listEnabledAssetCategories(),
    listLocationOptions(),
  ]);
  categories.value = categoryList;
  locations.value = locationList;
}

function search() {
  query.pageNum = 1;
  loadItems();
}

function openEdit(row) {
  if (row.status !== "PENDING") {
    return;
  }
  editingItemId = row.itemId;
  Object.assign(editForm, {
    assetName: row.assetName || "",
    assetCategoryId: row.assetCategoryId || null,
    purchaseDate: row.purchaseDate || null,
    enabledDate: row.enabledDate || null,
    assetDescription: row.assetDescription || "",
    locationText: row.locationText || "",
    campusId: row.campusId || null,
    buildingId: row.buildingId || null,
    floor: row.floor || "",
    room: row.room || "",
    locationDetail: row.locationDetail || "",
  });
  editDialog.value = true;
}

function onFormCampusChange() {
  editForm.buildingId = null;
}

function buildEditPayload() {
  return {
    assetName: editForm.assetName?.trim(),
    assetCategoryId: editForm.assetCategoryId,
    purchaseDate: editForm.purchaseDate || null,
    enabledDate: editForm.enabledDate || null,
    assetDescription: editForm.assetDescription?.trim() || "",
    locationText: editForm.locationText?.trim() || "",
    campusId: editForm.campusId,
    buildingId: editForm.buildingId || null,
    floor: editForm.floor?.trim() || "",
    room: editForm.room?.trim() || "",
    locationDetail: editForm.locationDetail?.trim() || "",
  };
}

function validateEditForm() {
  if (!editForm.assetName?.trim()) {
    ElMessage.warning("请填写资产名称");
    return false;
  }
  if (!editForm.assetCategoryId) {
    ElMessage.warning("请选择资产分类");
    return false;
  }
  if (!editForm.campusId) {
    ElMessage.warning("请选择校区");
    return false;
  }
  return true;
}

async function submitEdit() {
  if (!validateEditForm()) {
    return;
  }
  saving.value = true;
  try {
    await updateAssetImportItem(editingItemId, buildEditPayload());
    ElMessage.success("卡片已保存");
    editDialog.value = false;
    await Promise.all([loadBatch(), loadItems()]);
  } catch (error) {
    ElMessage.error(error?.message || "保存失败");
    await loadItems();
  } finally {
    saving.value = false;
  }
}

async function handleIgnore(row) {
  if (row.status !== "PENDING") {
    return;
  }
  try {
    await ElMessageBox.confirm("确认忽略该卡片？忽略后不可再确认入库。", "操作确认", {
      type: "warning",
    });
    await ignoreAssetImportItem(row.itemId);
    ElMessage.success("已忽略");
    await Promise.all([loadBatch(), loadItems()]);
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(error?.message || "操作失败");
      await loadItems();
    }
  }
}

async function handleConfirm(row) {
  if (row.status !== "PENDING") {
    return;
  }
  confirming.value = true;
  try {
    const result = await confirmAssetImportItem(row.itemId);
    confirmResult.value = result;
    confirmResultVisible.value = true;
    await Promise.all([loadBatch(), loadItems()]);
  } catch (error) {
    ElMessage.error(error?.message || "确认失败");
    await loadItems();
  } finally {
    confirming.value = false;
  }
}

async function handleBatchConfirm() {
  const pendingCount = batch.value?.pendingCount || 0;
  if (!pendingCount) {
    ElMessage.warning("当前批次没有待审核卡片");
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确认批量入库该批次全部 ${pendingCount} 条待审核卡片？`,
      "批量确认",
      { type: "warning" },
    );
    confirming.value = true;
    const result = await confirmAssetImportBatch({ batchId: batchId.value });
    confirmResult.value = result;
    confirmResultVisible.value = true;
    await Promise.all([loadBatch(), loadItems()]);
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(error?.message || "批量确认失败");
      await loadItems();
    }
  } finally {
    confirming.value = false;
  }
}

async function handleDeleteBatch() {
  try {
    await ElMessageBox.confirm(
      "确认删除该批次及所有未入库卡片？此操作不可恢复。",
      "删除批次",
      { type: "warning" },
    );
    await deleteAssetImportBatch(batchId.value);
    ElMessage.success("批次已删除");
    router.push("/admin/asset-import");
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(error?.message || "删除失败");
    }
  }
}

function goBack() {
  router.push("/admin/asset-import");
}

onMounted(async () => {
  await loadOptions();
  await Promise.all([loadBatch(), loadItems()]);
});
