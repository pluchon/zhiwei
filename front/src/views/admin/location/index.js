import { ElMessage, ElMessageBox } from "element-plus";
import {
  adminListCampuses,
  adminListBuildings,
  adminCreateCampus,
  adminUpdateCampus,
  enableCampus,
  disableCampus,
  deleteCampus,
  restoreCampus,
  adminCreateBuilding,
  adminUpdateBuilding,
  enableBuilding,
  disableBuilding,
  deleteBuilding,
  restoreBuilding,
} from "@/api/repair/location";
import {
  locationDeleteStateOptions,
  locationDeleteStateText,
  locationStatusOptions,
  locationStatusText,
} from "@/utils/repair";

// 页面加载与交互状态变量
const loading = ref(false);
const buildingLoading = ref(false);
const saving = ref(false);
const suppressCampusChange = ref(false);
const campusTableRef = ref(null);

// 校区与楼栋数据列表
const campuses = ref([]);
const buildings = ref([]);
const selectedCampusId = ref(null);

// 筛选条件（applied 为实际生效的筛选）
const campusFilters = reactive({ status: null, deleteState: null });
const campusFiltersApplied = reactive({ status: null, deleteState: null });
const buildingFilters = reactive({ status: null, deleteState: null });
const buildingFiltersApplied = reactive({ status: null, deleteState: null });

// 对话框显示与模式控制（模式：create/edit，目标：campus/building）
const dialog = ref(false);
const dialogMode = ref("create");
const dialogTarget = ref("campus");

// 校区/楼栋表单实体
const form = reactive({
  campusName: "",
  buildingName: "",
  description: "",
});

const displayCampuses = computed(() =>
  campuses.value.filter((row) => matchesLocationFilter(row, campusFiltersApplied)),
);

const displayBuildings = computed(() =>
  buildings.value.filter((row) => matchesLocationFilter(row, buildingFiltersApplied)),
);

// 计算对话框标题
const dialogTitle = computed(() => {
  const action = dialogMode.value === "create" ? "新增" : "编辑";
  const target = dialogTarget.value === "campus" ? "校区" : "楼栋";
  return `${action}${target}`;
});

function matchesLocationFilter(row, filters) {
  if (filters.status !== null && filters.status !== "" && row.status !== filters.status) {
    return false;
  }
  if (
    filters.deleteState !== null &&
    filters.deleteState !== "" &&
    row.deleteState !== filters.deleteState
  ) {
    return false;
  }
  return true;
}

function syncAppliedFilters(source, target) {
  target.status = source.status;
  target.deleteState = source.deleteState;
}

function applyCampusFilters() {
  syncAppliedFilters(campusFilters, campusFiltersApplied);
  ensureCampusSelection();
  syncCampusTableCurrentRow();
}

function resetCampusFilters() {
  campusFilters.status = null;
  campusFilters.deleteState = null;
  applyCampusFilters();
}

function applyBuildingFilters() {
  syncAppliedFilters(buildingFilters, buildingFiltersApplied);
}

function resetBuildingFilters() {
  buildingFilters.status = null;
  buildingFilters.deleteState = null;
  applyBuildingFilters();
}

function ensureCampusSelection() {
  if (!campuses.value.length) {
    selectedCampusId.value = null;
    buildings.value = [];
    return;
  }
  const visible = displayCampuses.value;
  if (!visible.length) {
    return;
  }
  const stillVisible = visible.some(
    (item) => Number(item.campusId) === Number(selectedCampusId.value),
  );
  if (!stillVisible) {
    selectedCampusId.value = visible[0].campusId;
  }
}

function syncCampusTableCurrentRow() {
  nextTick(() => {
    if (!campusTableRef.value) {
      return;
    }
    const row = displayCampuses.value.find(
      (item) => Number(item.campusId) === Number(selectedCampusId.value),
    );
    suppressCampusChange.value = true;
    campusTableRef.value.setCurrentRow(row || null);
    suppressCampusChange.value = false;
  });
}

// 加载校区列表，并默认选中第一个校区
async function loadCampuses({ reloadBuildings = true } = {}) {
  loading.value = true;
  suppressCampusChange.value = true;
  try {
    campuses.value = await adminListCampuses();
    if (!selectedCampusId.value && campuses.value.length) {
      selectedCampusId.value = campuses.value[0].campusId;
    }
    ensureCampusSelection();
    syncCampusTableCurrentRow();
  } finally {
    suppressCampusChange.value = false;
    loading.value = false;
  }
  if (reloadBuildings) {
    await loadBuildings();
  }
}

// 根据当前选中的校区ID加载对应的楼栋列表
async function loadBuildings() {
  if (!selectedCampusId.value) {
    buildings.value = [];
    return;
  }
  buildingLoading.value = true;
  try {
    buildings.value = await adminListBuildings(selectedCampusId.value);
  } finally {
    buildingLoading.value = false;
  }
}

// 选中某个校区并加载其楼栋
function selectCampus(row) {
  if (suppressCampusChange.value || !row) {
    return;
  }
  if (Number(row.campusId) === Number(selectedCampusId.value)) {
    return;
  }
  selectedCampusId.value = row.campusId;
  loadBuildings();
}

// 重置对话框表单数据
function resetForm() {
  form.campusName = "";
  form.buildingName = "";
  form.description = "";
  delete form.campusId;
  delete form.buildingId;
}

// 打开新增校区对话框
function openCampusCreate() {
  dialogTarget.value = "campus";
  dialogMode.value = "create";
  resetForm();
  dialog.value = true;
}

// 打开编辑校区对话框
function openCampusEdit(row) {
  dialogTarget.value = "campus";
  dialogMode.value = "edit";
  resetForm();
  form.campusId = row.campusId;
  form.campusName = row.campusName;
  form.description = row.description || "";
  dialog.value = true;
}

// 打开新增楼栋对话框
function openBuildingCreate() {
  if (!selectedCampusId.value) {
    ElMessage.warning("请先选择校区");
    return;
  }
  dialogTarget.value = "building";
  dialogMode.value = "create";
  resetForm();
  dialog.value = true;
}

// 打开编辑楼栋对话框
function openBuildingEdit(row) {
  dialogTarget.value = "building";
  dialogMode.value = "edit";
  resetForm();
  form.buildingId = row.buildingId;
  form.buildingName = row.buildingName;
  form.description = row.description || "";
  dialog.value = true;
}

// 去除字符串两端空格
function trimName(value) {
  return value == null ? "" : String(value).trim();
}

// 提交新增或更新校区/楼栋表单
async function submitForm() {
  saving.value = true;
  try {
    if (dialogTarget.value === "campus") {
      const payload = {
        campusName: trimName(form.campusName),
        description: trimName(form.description),
      };
      if (!payload.campusName) {
        ElMessage.warning("请填写校区名称");
        return;
      }
      if (dialogMode.value === "create") {
        await adminCreateCampus(payload);
        ElMessage.success("校区已新增");
      } else {
        await adminUpdateCampus(form.campusId, payload);
        ElMessage.success("校区已保存");
      }
    } else {
      const payload = {
        campusId: selectedCampusId.value,
        buildingName: trimName(form.buildingName),
        description: trimName(form.description),
      };
      if (!payload.buildingName) {
        ElMessage.warning("请填写楼栋名称");
        return;
      }
      if (dialogMode.value === "create") {
        await adminCreateBuilding(payload);
        ElMessage.success("楼栋已新增");
      } else {
        await adminUpdateBuilding(form.buildingId, payload);
        ElMessage.success("楼栋已保存");
      }
    }
    dialog.value = false;
    await loadCampuses({ reloadBuildings: dialogTarget.value === "building" });
    if (dialogTarget.value === "campus") {
      await loadBuildings();
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || "保存失败");
  } finally {
    saving.value = false;
  }
}

function readErrorMessage(error) {
  return error?.response?.data?.message || error?.message || "操作失败";
}

// 执行校区相关接口动作（启用/停用/删除/恢复）
async function runCampusAction(row, action, successText) {
  await action(row.campusId);
  ElMessage.success(successText);
  await loadCampuses();
}

// 执行楼栋相关接口动作（启用/停用/删除/恢复）
async function runBuildingAction(row, action, successText) {
  await action(row.buildingId);
  ElMessage.success(successText);
  await loadBuildings();
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

// 处理启用校区点击事件
function handleEnableCampus(row) {
  confirmAndRun("启用后该校区可再次用于报修选择，是否继续？", () =>
    runCampusAction(row, enableCampus, "校区已启用"),
  );
}

// 处理停用校区点击事件
function handleDisableCampus(row) {
  confirmAndRun("停用后报修人将无法选择该校区，是否继续？", () =>
    runCampusAction(row, disableCampus, "校区已停用"),
  );
}

// 处理删除校区点击事件
function handleDeleteCampus(row) {
  confirmAndRun(
    "删除校区前请先停用并处理其下所有楼栋。确认继续删除该校区？",
    () => runCampusAction(row, deleteCampus, "校区已删除"),
  );
}

// 处理恢复已删除校区点击事件
function handleRestoreCampus(row) {
  confirmAndRun("恢复后该校区将重新出现在维护列表中，是否继续？", () =>
    runCampusAction(row, restoreCampus, "校区已恢复"),
  );
}

// 处理启用楼栋点击事件
function handleEnableBuilding(row) {
  confirmAndRun("启用后该楼栋可再次用于报修选择，是否继续？", () =>
    runBuildingAction(row, enableBuilding, "楼栋已启用"),
  );
}

// 处理停用楼栋点击事件
function handleDisableBuilding(row) {
  confirmAndRun("停用后报修人将无法选择该楼栋，是否继续？", () =>
    runBuildingAction(row, disableBuilding, "楼栋已停用"),
  );
}

// 处理删除楼栋点击事件
function handleDeleteBuilding(row) {
  confirmAndRun("删除楼栋前请确认没有进行中的关联工单，是否继续？", () =>
    runBuildingAction(row, deleteBuilding, "楼栋已删除"),
  );
}

// 处理恢复已删除楼栋点击事件
function handleRestoreBuilding(row) {
  confirmAndRun("恢复后该楼栋将重新出现在维护列表中，是否继续？", () =>
    runBuildingAction(row, restoreBuilding, "楼栋已恢复"),
  );
}

// 组件挂载时初始化加载校区列表
onMounted(loadCampuses);
