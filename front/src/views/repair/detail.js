import { ElMessage, ElMessageBox } from "element-plus";
import {
  Bell,
  CircleCheck,
  CircleClose,
  Clock,
  Connection,
  Document,
  EditPen,
  Lock,
  Tools,
  User,
  Warning,
} from "@element-plus/icons-vue";
import { parseTime } from "@/utils/ruoyi";
import {
  getOrder,
  orderAction,
  addComment,
  evaluateOrder,
  getRepairerBusyLevel,
  withdrawOrder,
  reDraftOrder,
  requestAutoCompleteArbitration,
} from "@/api/repair/order";
import { adminCreate } from "@/api/admin";
import { getDispatchCandidates, adminDispatchOrder } from "@/api/admin";
import {
  getDuplicateDetail,
  confirmOrderAiLink,
  removeOrderAiLink,
  analyzeDispatch,
} from "@/api/ai/repair-order";
import { getAssetRepairHistory } from "@/api/asset/index";
import {
  statusText,
  busyLevelText,
  busyLevelType,
  formatLocationSnapshot,
  repairTypeText,
} from "@/utils/repair";
import useUserStore from "@/store/modules/user";

// 路由与 Pinia Store 实例
const route = useRoute();
const router = useRouter();
const store = useUserStore();

// 页面级加载与交互状态
const loading = ref(false);
const submitting = ref(false);

// 后台管理员手动指派对话框状态与表单
const dispatchVisible = ref(false);
const dispatchLoading = ref(false);
const candidates = ref([]);
const dispatchForm = reactive({
  repairerId: null,
  dispatchNote: "",
  capabilityMismatchReason: "",
});

// 工单完整详情数据（包含主体、分类、附件、评论、日志）
const detail = reactive({
  order: null,
  category: null,
  attachments: [],
  comments: [],
  logs: [],
  evaluation: null,
});

// 评论与评价表单数据
const comment = ref("");
const rating = ref(5);
const evaluation = ref("");

// 权限相关角色判断
const role = computed(() => store.roles[0]);
const reporter = computed(() => ["STUDENT", "TEACHER"].includes(role.value));
const admin = computed(() => role.value === "ADMIN");
const repairer = computed(() => role.value === "REPAIRER");
const isOwner = computed(
  () => reporter.value && detail.order?.reporterId === store.id,
);

// 获取工单的位置快照文本
const locationText = computed(() => formatLocationSnapshot(detail.order));

// 是否为资产报修工单
const hasAssetSnapshot = computed(
  () =>
    detail.order?.repairType === "ASSET" ||
    !!detail.order?.assetNoSnapshot,
);

// 管理员和维修师傅可查看完整维修历史
const canViewAssetHistory = computed(
  () => (admin.value || repairer.value) && !!detail.order?.assetId,
);

// 资产维修历史弹窗
const historyVisible = ref(false);
const historyLoading = ref(false);
const assetHistory = reactive({ records: [], total: 0 });
const historyQuery = reactive({ pageNum: 1, pageSize: 10 });

const duplicateDetail = ref(null);
const aiAnalysisText = ref("");
const aiAnalysisLoading = ref(false);
const dispatchAiText = ref("");
const dispatchAiLoading = ref(false);
const showAiAnalysis = computed(
  () => admin.value && [1, 2].includes(detail.order?.status),
);

const statusIconMap = {
  0: EditPen,
  1: Connection,
  2: Bell,
  3: User,
  4: Tools,
  5: Clock,
  6: Warning,
  7: CircleCheck,
  8: CircleClose,
  9: Lock,
};

const timelineStatusColor = {
  0: "#64748b",
  1: "#d97706",
  2: "#ea580c",
  3: "#2563eb",
  4: "#1d4ed8",
  5: "#ca8a04",
  6: "#dc2626",
  7: "#059669",
  8: "#e11d48",
  9: "#475569",
};

const currentStatusIcon = computed(() => {
  const status = detail.order?.status;
  return statusIconMap[status] || Document;
});

const attachmentPreviewList = computed(() =>
  (detail.attachments || [])
    .map((item) => item.signedUrl)
    .filter((url) => !!url),
);

function formatTimelineTime(value) {
  if (!value) {
    return "-";
  }
  return parseTime(value, "{y}-{m}-{d} {h}:{i}") || String(value);
}

function timelineDotColor(status) {
  return timelineStatusColor[status] || "#64748b";
}

function timelineLineText(log) {
  const time = formatTimelineTime(log.createTime);
  const status = statusText(log.toStatus);
  const remark = log.remark?.trim() || "状态流转";
  return `${time}  ${status}  ${remark}`;
}

// 判断当前用户是否可以在当前状态下编辑位置信息
const canEditLocation = computed(() => {
  const status = detail.order?.status;
  return isOwner.value && [0, 1, 2].includes(status);
});

// 判断报修人是否可以在当前状态下申请仲裁（自动确认完成后7天内）
const canArbitrate = computed(() => {
  const order = detail.order;
  if (!order || !isOwner.value || order.status !== 7 || !order.autoCompletedTime) {
    return false;
  }
  const deadline = new Date(order.autoCompletedTime);
  deadline.setDate(deadline.getDate() + 7);
  return new Date() <= deadline;
});

// 已完成工单展示评价区域；报修人可提交，师傅只读查看
const isCompletedOrder = computed(() => detail.order?.status === 7);
const isAssignedRepairer = computed(
  () => repairer.value && detail.order?.currentRepairerId === store.id,
);
const showEvaluationSection = computed(() => {
  if (!isCompletedOrder.value) {
    return false;
  }
  if (isOwner.value) {
    return true;
  }
  return isAssignedRepairer.value;
});
const canSubmitEvaluation = computed(
  () => isOwner.value && isCompletedOrder.value && !detail.evaluation,
);
const showEvaluationPending = computed(
  () => showEvaluationSection.value && !canSubmitEvaluation.value && !detail.evaluation,
);

// 计算当前指派选中的师傅对象
const selectedCandidate = computed(() =>
  candidates.value.find((item) => item.userId === dispatchForm.repairerId),
);

// 从后端加载工单的完整详情
async function load() {
  loading.value = true;
  try {
    Object.assign(detail, await getOrder(route.params.id));
    if (admin.value && detail.order?.suspectedDuplicate === 1) {
      duplicateDetail.value = await getDuplicateDetail(route.params.id);
    } else {
      duplicateDetail.value = null;
    }
  } catch (error) {
    ElMessage.error(error.message || "加载失败");
  } finally {
    loading.value = false;
  }
}

async function runAiAnalysis() {
  aiAnalysisLoading.value = true;
  try {
    const resp = await analyzeDispatch(route.params.id);
    aiAnalysisText.value = resp.analysisText || "暂无分析结果";
  } catch (error) {
    ElMessage.error(error?.message || "AI 分析失败");
  } finally {
    aiAnalysisLoading.value = false;
  }
}

async function runDispatchAiAnalysis() {
  dispatchAiLoading.value = true;
  dispatchAiText.value = "";
  try {
    const resp = await analyzeDispatch(route.params.id);
    dispatchAiText.value = resp.analysisText || "暂无分析结果";
  } catch (error) {
    ElMessage.error(error?.message || "AI 分析失败");
  } finally {
    dispatchAiLoading.value = false;
  }
}

async function confirmLink(linkId) {
  try {
    await ElMessageBox.confirm("确认建立该工单关联？", "确认关联", { type: "warning" });
    await confirmOrderAiLink({ linkId });
    ElMessage.success("已确认关联");
    await load();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(error?.message || "操作失败");
    }
  }
}

async function removeLink(linkId) {
  try {
    await ElMessageBox.confirm("确认解除该工单关联？", "解除关联", { type: "warning" });
    await removeOrderAiLink(linkId);
    ElMessage.success("已解除关联");
    await load();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(error?.message || "操作失败");
    }
  }
}

// 统一控制各个流转动作按钮的显示与隐藏权限
function can(action) {
  const status = detail.order?.status;
  if (action === "accept") {
    return repairer.value && status === 2;
  }
  if (action === "start") {
    return repairer.value && status === 3;
  }
  if (action === "result") {
    return repairer.value && status === 4;
  }
  if (action === "return") {
    return repairer.value && [3, 4].includes(status);
  }
  if (action === "confirm") {
    return isOwner.value && status === 5;
  }
  if (action === "unresolved") {
    return isOwner.value && status === 5 && !detail.order.autoCompletedTime;
  }
  if (action === "submit") {
    return isOwner.value && status === 0;
  }
  if (action === "withdraw") {
    return isOwner.value && [1, 2].includes(status);
  }
  if (action === "reDraft") {
    return isOwner.value && status === 8;
  }
  if (action === "dispatch") {
    return admin.value && [1, 2].includes(status);
  }
  if (action === "arbitration") {
    return canArbitrate.value;
  }
  if (action === "evaluate") {
    return canSubmitEvaluation.value;
  }
  return false;
}

// 提交工单状态流转接口（自动带上乐观锁版本号）
async function runAction(action, data = {}) {
  submitting.value = true;
  try {
    await orderAction(route.params.id, action, {
      version: detail.order.version,
      ...data,
    });
    ElMessage.success("操作成功");
    if (action === "return") {
      router.push("/repair/mine");
    } else {
      await load();
    }
  } catch (error) {
    ElMessage.error(error.message || "操作失败，请刷新后重试");
    await load();
  } finally {
    submitting.value = false;
  }
}

// 师傅接单动作（若繁忙则弹出二次确认）
async function acceptOrder() {
  submitting.value = true;
  try {
    let confirmMessage = "确认接取该工单？";
    try {
      const busyLevel = await getRepairerBusyLevel();
      if (busyLevel === "BUSY") {
        confirmMessage = "您当前接的工单较多，请慎重接取。确认继续接单？";
      }
    } catch (error) {
      // 繁忙提示失败不阻断接单
    }
    await ElMessageBox.confirm(confirmMessage, "接单确认", {
      type: "warning",
      confirmButtonText: "继续接单",
      cancelButtonText: "取消",
    });
    await orderAction(route.params.id, "accept", {
      version: detail.order.version,
    });
    ElMessage.success("接单成功");
    await load();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(error.message || "接单失败，请刷新后重试");
      await load();
    }
  } finally {
    submitting.value = false;
  }
}

// 弹出输入框，输入原因或说明后再执行动作
async function promptAction(action, label, field = "reason") {
  const { value } = await ElMessageBox.prompt(label, "确认操作", {
    inputType: "textarea",
    confirmButtonText: "确认",
    cancelButtonText: "取消",
  });
  const payload =
    action === "result" ? { description: value } : { [field]: value };
  await runAction(action, payload);
}

// 报修人撤回工单为草稿
async function confirmWithdraw() {
  try {
    await ElMessageBox.confirm("撤回后工单将回到草稿，可继续编辑。确认撤回？", "撤回确认", {
      type: "warning",
    });
  } catch {
    return;
  }
  submitting.value = true;
  try {
    await withdrawOrder(route.params.id, { version: detail.order.version });
    ElMessage.success("已撤回为草稿");
    await load();
  } catch (error) {
    ElMessage.error(error.message || "撤回失败，请刷新后重试");
    await load();
  } finally {
    submitting.value = false;
  }
}

// 报修人将已驳回工单重新转为草稿进行编辑
async function confirmReDraft() {
  try {
    await ElMessageBox.confirm("确认将已驳回工单重新转为草稿？", "转草稿确认", {
      type: "warning",
    });
  } catch {
    return;
  }
  submitting.value = true;
  try {
    await reDraftOrder(route.params.id, { version: detail.order.version });
    ElMessage.success("已转为草稿");
    await load();
  } catch (error) {
    ElMessage.error(error.message || "操作失败，请刷新后重试");
    await load();
  } finally {
    submitting.value = false;
  }
}

// 报修人申请自动确认完成的仲裁
async function requestArbitration() {
  const { value } = await ElMessageBox.prompt(
    "请填写仲裁申请说明",
    "申请仲裁",
    { inputType: "textarea" },
  );
  submitting.value = true;
  try {
    await requestAutoCompleteArbitration(route.params.id, {
      reason: value,
      version: detail.order.version,
    });
    ElMessage.success("仲裁申请已提交");
    await load();
  } catch (error) {
    ElMessage.error(error.message || "申请失败，请刷新后重试");
    await load();
  } finally {
    submitting.value = false;
  }
}

// 管理员执行相关流转动作（如驳回、关闭、仲裁等）
async function adminAction(action) {
  const { value } = await ElMessageBox.prompt("请填写原因", "管理员操作", {
    inputType: "textarea",
  });
  submitting.value = true;
  try {
    await adminCreate(`orders/${route.params.id}/${action}`, {
      version: detail.order.version,
      reason: value,
    });
    ElMessage.success("操作成功");
    await load();
  } catch (error) {
    ElMessage.error(error.message || "操作失败，请刷新后重试");
    await load();
  } finally {
    submitting.value = false;
  }
}

// 打开管理员手动指派对话框并加载指派候选人
async function openDispatch() {
  dispatchLoading.value = true;
  dispatchVisible.value = true;
  dispatchForm.repairerId = null;
  dispatchForm.dispatchNote = "";
  dispatchForm.capabilityMismatchReason = "";
  dispatchAiText.value = "";
  try {
    candidates.value = await getDispatchCandidates(route.params.id);
  } catch (error) {
    dispatchVisible.value = false;
    ElMessage.error(error.message || "无法加载派单候选");
  } finally {
    dispatchLoading.value = false;
  }
}

// 提交管理员手动指派工单
async function submitDispatch() {
  if (!dispatchForm.repairerId) {
    ElMessage.warning("请选择维修师傅");
    return;
  }
  if (!dispatchForm.dispatchNote?.trim()) {
    ElMessage.warning("请填写派单说明");
    return;
  }
  if (
    selectedCandidate.value &&
    !selectedCandidate.value.hasCapability &&
    !dispatchForm.capabilityMismatchReason?.trim()
  ) {
    ElMessage.warning("能力不匹配时必须填写原因");
    return;
  }
  try {
    await ElMessageBox.confirm(
      "请确认已与维修师傅沟通，并确认是否继续手动派单。",
      "派单确认",
      { type: "warning" },
    );
  } catch {
    return;
  }
  submitting.value = true;
  try {
    await adminDispatchOrder(route.params.id, {
      repairerId: dispatchForm.repairerId,
      dispatchNote: dispatchForm.dispatchNote.trim(),
      capabilityMismatchReason: dispatchForm.capabilityMismatchReason?.trim() || "",
      version: detail.order.version,
    });
    ElMessage.success("派单成功");
    dispatchVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error(error.message || "派单失败，请刷新后重试");
    await load();
  } finally {
    submitting.value = false;
  }
}

// 跳转到编辑页面
function goEdit() {
  router.push(`/repair/create?id=${route.params.id}`);
}

// 加载资产维修历史
async function loadAssetHistory() {
  if (!detail.order?.assetId) {
    return;
  }
  historyLoading.value = true;
  try {
    Object.assign(
      assetHistory,
      await getAssetRepairHistory(detail.order.assetId, historyQuery),
    );
  } catch (error) {
    ElMessage.error(error.message || "维修历史加载失败");
  } finally {
    historyLoading.value = false;
  }
}

// 打开资产维修历史弹窗
async function openAssetHistory() {
  historyQuery.pageNum = 1;
  historyVisible.value = true;
  await loadAssetHistory();
}

// 发送工单详情下方的讨论评论
async function sendComment() {
  if (!comment.value?.trim()) {
    return;
  }
  await addComment(route.params.id, comment.value.trim());
  comment.value = "";
  load();
}

// 报修人提交对本次报修师傅服务的评分与评价
async function evaluate() {
  const content = evaluation.value?.trim() || "";
  if (content.length > 50) {
    ElMessage.warning("评价内容不能超过50字");
    return;
  }
  submitting.value = true;
  try {
    await evaluateOrder(route.params.id, {
      star: rating.value,
      content: content || undefined,
    });
    ElMessage.success("评价已提交");
    await load();
  } catch (error) {
    ElMessage.error(error?.message || "提交失败");
  } finally {
    submitting.value = false;
  }
}

// 组件挂载时自动加载工单详情数据
onMounted(load);
