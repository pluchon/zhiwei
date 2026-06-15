import { listEnabledAssetCategories } from "@/api/asset/category";
import { listAssets, getAsset, getAssetRepairHistory } from "@/api/asset/index";
import { listLocationOptions } from "@/api/repair/location";
import useUserStore from "@/store/modules/user";
import {
  assetStatusOptions,
  assetStatusText,
  assetStatusType,
  formatAssetLocation,
} from "@/utils/repair";

const userStore = useUserStore();

const loading = ref(false);
const historyLoading = ref(false);
const categories = ref([]);
const locations = ref([]);
const detailVisible = ref(false);
const detail = ref(null);
const history = reactive({ records: [], total: 0 });
const historyQuery = reactive({ pageNum: 1, pageSize: 10 });
let currentAssetId = null;

const role = computed(() => userStore.roles[0] || "");
const isAdminOrRepairer = computed(() => ["ADMIN", "REPAIRER"].includes(role.value));

const ASSET_NO_PREFIX = "AST-";

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  assetNoSuffix: "",
  assetNameKeyword: "",
  assetCategoryId: null,
  campusId: null,
  buildingId: null,
  status: null,
});

const data = reactive({ records: [], total: 0 });

const buildingOptions = computed(() => {
  const campus = locations.value.find((item) => item.campusId === query.campusId);
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
  const next = stripAssetNoPrefix(value);
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

async function loadOptions() {
  const [categoryList, locationList] = await Promise.all([
    listEnabledAssetCategories(),
    listLocationOptions(),
  ]);
  categories.value = categoryList;
  locations.value = locationList;
}

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
  });
  load();
}

function onCampusChange() {
  query.buildingId = null;
  search();
}

async function loadHistory() {
  if (!currentAssetId) {
    return;
  }
  historyLoading.value = true;
  try {
    Object.assign(
      history,
      await getAssetRepairHistory(currentAssetId, historyQuery),
    );
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

onMounted(async () => {
  await loadOptions();
  await load();
});
