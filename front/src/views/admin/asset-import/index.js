import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { listAssetImportBatches, uploadAssetImport, uploadAssetImportImages } from "@/api/asset/import";
import { parseTime } from "@/utils/ruoyi";

const router = useRouter();
const loading = ref(false);
const uploading = ref(false);
const uploadMode = ref("");
const uploadDateRange = ref([]);

const sourceTypeOptions = [
  { value: "EXCEL", label: "Excel 导入" },
  { value: "IMAGE", label: "图片导入" },
];

const pendingFilterOptions = [
  { value: null, label: "全部" },
  { value: true, label: "仅待审核" },
];

const query = reactive({
  pageNum: 1,
  pageSize: 10,
});

const filters = reactive({
  keyword: "",
  sourceType: null,
  createTimeFrom: null,
  createTimeTo: null,
  onlyPending: null,
});

const filtersApplied = reactive({
  keyword: "",
  sourceType: null,
  createTimeFrom: null,
  createTimeTo: null,
  onlyPending: null,
});

const data = reactive({ records: [], total: 0 });

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

function formatTime(value) {
  return value ? parseTime(value) : "-";
}

function buildParams() {
  const params = {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
  };
  const keyword = filtersApplied.keyword?.trim();
  if (keyword) {
    params.keyword = keyword;
  }
  if (filtersApplied.sourceType) {
    params.sourceType = filtersApplied.sourceType;
  }
  if (filtersApplied.createTimeFrom) {
    params.createTimeFrom = filtersApplied.createTimeFrom;
  }
  if (filtersApplied.createTimeTo) {
    params.createTimeTo = filtersApplied.createTimeTo;
  }
  if (filtersApplied.onlyPending === true) {
    params.onlyPending = true;
  }
  return params;
}

function applyFilters() {
  filtersApplied.keyword = filters.keyword;
  filtersApplied.sourceType = filters.sourceType;
  filtersApplied.onlyPending = filters.onlyPending;
  filtersApplied.createTimeFrom = formatDateBoundary(uploadDateRange.value?.[0]);
  filtersApplied.createTimeTo = formatDateBoundary(uploadDateRange.value?.[1], true);
}

async function load() {
  loading.value = true;
  try {
    Object.assign(data, await listAssetImportBatches(buildParams()));
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || "加载失败");
  } finally {
    loading.value = false;
  }
}

function search() {
  applyFilters();
  query.pageNum = 1;
  load();
}

function resetFilters() {
  filters.keyword = "";
  filters.sourceType = null;
  filters.onlyPending = null;
  uploadDateRange.value = [];
  search();
}

function openDetail(row) {
  router.push(`/admin/asset-import/${row.batchId}`);
}

async function onUploadChange(file) {
  if (!file?.raw) {
    return;
  }
  const name = file.name || "";
  if (!name.toLowerCase().endsWith(".xlsx")) {
    ElMessage.warning("仅支持 .xlsx 格式文件");
    return;
  }
  uploading.value = true;
  uploadMode.value = "excel";
  try {
    const batch = await uploadAssetImport(file.raw);
    ElMessage.success(`已上传，识别 ${batch.totalCount || 0} 条卡片`);
    await load();
    if (batch.batchId) {
      router.push(`/admin/asset-import/${batch.batchId}`);
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || "上传失败");
  } finally {
    uploading.value = false;
    uploadMode.value = "";
  }
}

async function onImageUploadChange(file, fileList) {
  if (!fileList?.length) {
    return;
  }
  if (fileList.length > 10) {
    ElMessage.warning("单次最多上传 10 张图片");
    return;
  }
  uploading.value = true;
  uploadMode.value = "image";
  try {
    const files = fileList.map((item) => item.raw).filter(Boolean);
    const batch = await uploadAssetImportImages(files);
    ElMessage.success(`已上传 ${batch.totalCount || files.length} 张图片`);
    await load();
    if (batch.batchId) {
      router.push(`/admin/asset-import/${batch.batchId}`);
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || "图片上传失败");
  } finally {
    uploading.value = false;
    uploadMode.value = "";
  }
}

onMounted(load);
