import { ElMessage, ElMessageBox } from "element-plus";
import {
  createOrder,
  updateOrder,
  listCategories,
  getOrder,
  orderAction,
  checkDuplicate,
  uploadAttachment,
  getWorkforceSummary,
} from "@/api/repair/order";
import { listLocationOptions } from "@/api/repair/location";
import { listAssets, getAsset } from "@/api/asset/index";
import {
  busyLevelText,
  busyLevelType,
  repairTypeOptions,
  formatAssetLocation,
} from "@/utils/repair";
import { Plus, Search } from "@element-plus/icons-vue";

// 路由及常用服务
const route = useRoute();
const router = useRouter();

// 选项列表及加载状态
const categories = ref([]);
const locations = ref([]);
const files = ref([]);
const loading = ref(false);

// 负责团队负荷数据与加载状态
const workforce = ref(null);
const workforceLoading = ref(false);

// 编辑状态相关计算属性
const editId = computed(() => route.query.id || null);
const isEdit = computed(() => !!editId.value);

// 资产选择弹窗状态
const assetPickerVisible = ref(false);
const assetPickerLoading = ref(false);
const assetPickerKeyword = ref("");
const assetOptions = ref([]);
const selectedAsset = ref(null);

// 报修单表单表载荷
const form = reactive({
  requestId: crypto.randomUUID(),
  title: "",
  description: "",
  categoryId: null,
  repairType: "NORMAL",
  assetId: null,
  campusId: null,
  buildingId: null,
  floor: "",
  room: "",
  locationDetail: "",
  contactPhone: "",
  version: null,
});

const editOrderStatus = ref(null);
const isAssetRepair = computed(() => form.repairType === "ASSET");
const canChangeAsset = computed(
  () => !isEdit.value || [0, 1, 2].includes(editOrderStatus.value),
);
const assetConflict = computed(() => isAssetOccupiedByOther(selectedAsset.value));

// 计算当前选中的校区
const selectedCampus = computed(() =>
  locations.value.find((item) => item.campusId === form.campusId),
);

// 计算当前选中的校区可选楼栋
const buildingOptions = computed(() => selectedCampus.value?.buildings || []);

// 计算当前选中的楼栋
const selectedBuilding = computed(() =>
  buildingOptions.value.find((item) => item.buildingId === form.buildingId),
);

// 加载分类及位置选项列表
async function loadOptions() {
  const [categoryList, locationList] = await Promise.all([
    listCategories(),
    listLocationOptions(),
  ]);
  categories.value = categoryList;
  locations.value = locationList;
}

// 异步加载对应故障分类的师傅负荷情况
async function loadWorkforce() {
  if (!form.categoryId) {
    workforce.value = null;
    return;
  }
  workforceLoading.value = true;
  try {
    workforce.value = await getWorkforceSummary(form.categoryId);
  } finally {
    workforceLoading.value = false;
  }
}

// 如果是编辑模式，加载已有工单数据并回显
async function loadEditOrder() {
  if (!editId.value) {
    return;
  }
  loading.value = true;
  try {
    const detail = await getOrder(editId.value);
    const order = detail.order;
    if (![0, 1, 2].includes(order.status)) {
      ElMessage.warning("当前状态不可编辑位置信息");
      router.replace(`/repair/detail/${editId.value}`);
      return;
    }
    editOrderStatus.value = order.status;
    Object.assign(form, {
      requestId: order.requestId,
      title: order.title,
      description: order.description,
      categoryId: order.categoryId,
      repairType: order.repairType || "NORMAL",
      assetId: order.assetId || null,
      campusId: order.campusId,
      buildingId: order.buildingId,
      floor: order.floor || "",
      room: order.room || "",
      locationDetail: order.locationDetail || "",
      contactPhone: order.contactPhone || "",
      version: order.version,
    });
    if (form.assetId) {
      await loadSelectedAsset(form.assetId);
    }
    await loadWorkforce();
  } finally {
    loading.value = false;
  }
}

// 切换校区时清空已选楼栋
function onCampusChange() {
  form.buildingId = null;
}

// 切换报修类型
function onRepairTypeChange(value) {
  if (value !== "ASSET") {
    form.assetId = null;
    selectedAsset.value = null;
  }
}

// 加载已选资产详情
async function loadSelectedAsset(assetId) {
  try {
    selectedAsset.value = await getAsset(assetId);
  } catch {
    selectedAsset.value = null;
  }
}

// 搜索可选资产
async function searchAssets() {
  assetPickerLoading.value = true;
  try {
    const params = { pageNum: 1, pageSize: 20 };
    const keyword = assetPickerKeyword.value?.trim();
    if (keyword) {
      params.assetNameKeyword = keyword;
      if (/^AST-/i.test(keyword)) {
        params.assetNo = keyword;
        delete params.assetNameKeyword;
      }
    }
    const result = await listAssets(params);
    assetOptions.value = result.records || [];
  } finally {
    assetPickerLoading.value = false;
  }
}

function openAssetPicker() {
  assetPickerVisible.value = true;
  assetPickerKeyword.value = "";
  searchAssets();
}

// 判断资产是否被其他工单占用
function isAssetOccupiedByOther(asset) {
  if (!asset?.hasActiveOrder) {
    return false;
  }
  if (!isEdit.value) {
    return true;
  }
  return Number(asset.activeOrderId) !== Number(editId.value);
}

// 选择资产并带入位置信息
function chooseAsset(asset) {
  if (isAssetOccupiedByOther(asset)) {
    ElMessage.warning("该资产已被其他未结束工单占用，暂不可选择");
    return;
  }
  selectedAsset.value = asset;
  form.assetId = asset.assetId;
  form.campusId = asset.campusId;
  form.buildingId = asset.buildingId || null;
  form.floor = asset.floor || "";
  form.room = asset.room || "";
  form.locationDetail = asset.locationDetail || "";
  assetPickerVisible.value = false;
}

function clearAsset() {
  selectedAsset.value = null;
  form.assetId = null;
}

const DESCRIPTION_MAX = 50;
const LOCATION_DETAIL_MAX = 50;

// 验证表单必填项
function validateForm() {
  if (!form.title?.trim()) {
    ElMessage.warning("请填写报修标题");
    return false;
  }
  if (!form.description?.trim()) {
    ElMessage.warning("请填写故障描述");
    return false;
  }
  if (form.description.trim().length > DESCRIPTION_MAX) {
    ElMessage.warning(`故障描述不能超过 ${DESCRIPTION_MAX} 字`);
    return false;
  }
  if (!form.categoryId) {
    ElMessage.warning("请选择故障类型");
    return false;
  }
  if (!form.campusId) {
    ElMessage.warning("请选择校区");
    return false;
  }
  if (form.locationDetail?.trim().length > LOCATION_DETAIL_MAX) {
    ElMessage.warning(`具体位置描述不能超过 ${LOCATION_DETAIL_MAX} 字`);
    return false;
  }
  if (form.repairType === "ASSET") {
    if (!form.assetId) {
      ElMessage.warning("资产报修必须选择资产");
      return false;
    }
    if (assetConflict.value) {
      ElMessage.warning("所选资产存在未结束关联工单，暂不可提交");
      return false;
    }
  } else if (!form.buildingId && !form.locationDetail?.trim()) {
    ElMessage.warning("未选择楼栋时，请填写具体位置描述");
    return false;
  }
  return true;
}

// 组装提交的接口载荷
function buildPayload() {
  const payload = {
    requestId: form.requestId,
    title: form.title.trim(),
    description: form.description.trim().slice(0, DESCRIPTION_MAX),
    categoryId: form.categoryId,
    repairType: form.repairType,
    assetId: form.repairType === "ASSET" ? form.assetId : null,
    campusId: form.campusId,
    buildingId: form.buildingId || null,
    floor: form.floor?.trim() || "",
    room: form.room?.trim() || "",
    locationDetail: (form.locationDetail?.trim() || "").slice(0, LOCATION_DETAIL_MAX),
    contactPhone: form.contactPhone?.trim() || "",
  };
  if (isEdit.value) {
    payload.version = form.version;
  }
  return payload;
}

// 保存/更新工单，并批量上传附件
async function persistOrder() {
  if (isEdit.value) {
    return updateOrder(editId.value, buildPayload());
  }
  const order = await createOrder(buildPayload());
  for (const file of files.value) {
    await uploadAttachment(order.orderId, file.raw);
  }
  return order;
}

// 保存为草稿
async function saveDraft() {
  if (!validateForm()) {
    return;
  }
  loading.value = true;
  try {
    const order = await persistOrder();
    if (isEdit.value) {
      ElMessage.success("草稿已保存");
      router.push(`/repair/detail/${editId.value}`);
      return;
    }
    ElMessage.success("草稿已保存");
    router.push(`/repair/detail/${order.orderId}`);
  } finally {
    loading.value = false;
  }
}

// 提交工单（先保存，然后执行流转动作 submit）
async function submit() {
  if (!validateForm()) {
    return;
  }
  loading.value = true;
  try {
    const order = await persistOrder();
    const orderId = isEdit.value ? editId.value : order.orderId;
    const version = isEdit.value ? form.version : order.version;
    try {
      const duplicate = await checkDuplicate(orderId);
      if (duplicate?.suspected && duplicate?.reporterReminder) {
        await ElMessageBox.confirm(duplicate.reporterReminder, "重复报修提醒", {
          confirmButtonText: "继续提交",
          cancelButtonText: "取消",
          type: "warning",
        });
      }
    } catch (error) {
      if (error === "cancel") {
        return;
      }
    }
    const result = await orderAction(orderId, "submit", { version });
    if (result?.duplicateReminder) {
      ElMessage.warning(result.duplicateReminder);
    }
    ElMessage.success("报修已提交");
    router.push(`/repair/detail/${orderId}`);
  } finally {
    loading.value = false;
  }
}

// 监听故障分类变化，自动更新团队负荷情况
watch(
  () => form.categoryId,
  () => {
    loadWorkforce();
  },
);

// 挂载组件时并行加载基础选项并加载编辑数据
onMounted(async () => {
  await loadOptions();
  await loadEditOrder();
});
