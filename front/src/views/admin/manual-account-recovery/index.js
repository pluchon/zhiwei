import { ElMessage, ElMessageBox } from "element-plus";
import { adminList } from "@/api/admin";
import {
  listManualRecoveries,
  getManualRecovery,
  createManualRecovery,
  cancelManualRecovery,
  reviewManualRecovery,
} from "@/api/admin/manual-account-recovery";
import useUserStore from "@/store/modules/user";
import { parseTime } from "@/utils/ruoyi";
import {
  manualRecoveryStatusOptions,
  manualRecoveryStatusText,
  manualRecoveryStatusType,
} from "@/utils/repair";

const pendingFilterOptions = [
  { value: null, label: "全部" },
  { value: true, label: "仅待审批" },
];

const userStore = useUserStore();
const loading = ref(false);
const saving = ref(false);
const userLoading = ref(false);
const users = ref([]);
const createDateRange = ref([]);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
});

const filters = reactive({
  targetUserId: null,
  status: null,
  createTimeFrom: null,
  createTimeTo: null,
  onlyPending: null,
});

const filtersApplied = reactive({
  targetUserId: null,
  status: null,
  createTimeFrom: null,
  createTimeTo: null,
  onlyPending: null,
});

const data = reactive({ records: [], total: 0 });

const createDialog = ref(false);
const createForm = reactive({
  targetUserId: null,
  newPhone: "",
  identityCheckNote: "",
});

const detailVisible = ref(false);
const detail = ref(null);

const reviewDialog = ref(false);
const reviewForm = reactive({
  approved: true,
  reviewNote: "",
});
let reviewingId = null;

const currentAdminId = computed(() => Number(userStore.id) || null);

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

function applyFilters() {
  filtersApplied.targetUserId = filters.targetUserId;
  filtersApplied.status = filters.status;
  filtersApplied.onlyPending = filters.onlyPending;
  filtersApplied.createTimeFrom = formatDateBoundary(createDateRange.value?.[0]);
  filtersApplied.createTimeTo = formatDateBoundary(createDateRange.value?.[1], true);
}

function buildParams() {
  const params = {
    pageNum: query.pageNum,
    pageSize: query.pageSize,
  };
  if (filtersApplied.targetUserId) {
    params.targetUserId = filtersApplied.targetUserId;
  }
  if (filtersApplied.status) {
    params.status = filtersApplied.status;
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

async function load() {
  loading.value = true;
  try {
    Object.assign(data, await listManualRecoveries(buildParams()));
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
  filters.targetUserId = null;
  filters.status = null;
  filters.onlyPending = null;
  createDateRange.value = [];
  search();
}

let allUsers = [];

async function loadUsers(keyword) {
  userLoading.value = true;
  try {
    if (!allUsers.length) {
      const result = await adminList("users", {
        pageNum: 1,
        pageSize: 200,
      });
      allUsers = (result.records || []).filter((item) => [1, 2].includes(Number(item.roleId)));
    }
    const text = (keyword || "").trim().toLowerCase();
    if (!text) {
      users.value = allUsers.slice(0, 50);
      return;
    }
    users.value = allUsers.filter((item) => {
      const name = `${item.realName || ""} ${item.userNo || ""}`.toLowerCase();
      return name.includes(text);
    }).slice(0, 50);
  } finally {
    userLoading.value = false;
  }
}

function openCreate() {
  Object.assign(createForm, {
    targetUserId: null,
    newPhone: "",
    identityCheckNote: "",
  });
  createDialog.value = true;
  loadUsers();
}

function validateCreateForm() {
  if (!createForm.targetUserId) {
    ElMessage.warning("请选择目标用户");
    return false;
  }
  if (!createForm.newPhone?.trim()) {
    ElMessage.warning("请填写新手机号");
    return false;
  }
  if (!createForm.identityCheckNote?.trim()) {
    ElMessage.warning("请填写线下核验说明");
    return false;
  }
  return true;
}

async function submitCreate() {
  if (!validateCreateForm()) {
    return;
  }
  saving.value = true;
  try {
    await createManualRecovery({
      targetUserId: createForm.targetUserId,
      newPhone: createForm.newPhone.trim(),
      identityCheckNote: createForm.identityCheckNote.trim(),
    });
    ElMessage.success("申请已创建");
    createDialog.value = false;
    await load();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || "创建失败");
  } finally {
    saving.value = false;
  }
}

async function openDetail(row) {
  detailVisible.value = true;
  try {
    detail.value = await getManualRecovery(row.recoveryId);
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || "详情加载失败");
    detailVisible.value = false;
  }
}

function canReview(row) {
  return row.status === "PENDING" && row.applicantAdminId !== currentAdminId.value;
}

function canCancel(row) {
  return row.status === "PENDING" && row.applicantAdminId === currentAdminId.value;
}

function openReview(row) {
  reviewingId = row.recoveryId;
  Object.assign(reviewForm, {
    approved: true,
    reviewNote: "",
  });
  reviewDialog.value = true;
}

async function submitReview() {
  if (!reviewForm.reviewNote?.trim()) {
    ElMessage.warning("请填写审批处理说明");
    return;
  }
  saving.value = true;
  try {
    await reviewManualRecovery(reviewingId, {
      approved: reviewForm.approved,
      reviewNote: reviewForm.reviewNote.trim(),
    });
    ElMessage.success("审批已提交");
    reviewDialog.value = false;
    detailVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || "审批失败");
  } finally {
    saving.value = false;
  }
}

async function handleCancel(row) {
  try {
    await ElMessageBox.confirm("确认撤销该申请？", "撤销确认", { type: "warning" });
    await cancelManualRecovery(row.recoveryId);
    ElMessage.success("申请已撤销");
    detailVisible.value = false;
    await load();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error?.response?.data?.message || error?.message || "撤销失败");
    }
  }
}

onMounted(() => {
  loadUsers();
  load();
});
