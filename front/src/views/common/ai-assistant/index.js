import { ElMessage, ElMessageBox } from "element-plus";
import { Close, Delete, Edit, Plus } from "@element-plus/icons-vue";
import {
  aiAssistantChat,
  confirmAiExport,
  createAiAssistantSession,
  deleteAiAssistantSession,
  listAiAssistantMessages,
  listAiAssistantSessions,
  renameAiAssistantSession,
} from "@/api/ai/assistant";
import AiStatisticsPanel from "@/components/AiStatisticsPanel/index.vue";
import { blobValidate } from "@/utils/ruoyi";
import { saveAs } from "file-saver";
import useUserStore from "@/store/modules/user";

const router = useRouter();

const props = defineProps({
  modelValue: { type: Boolean, default: false },
});

const emit = defineEmits(["update:modelValue"]);

const userStore = useUserStore();
const isAdmin = computed(() => (userStore.roles[0] || "") === "ADMIN");

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit("update:modelValue", value),
});

const sessionList = ref([]);
const activeSessionId = ref(null);
const messages = ref([]);
const input = ref("");
const sending = ref(false);
const exportingToken = ref("");
const loadingSessions = ref(false);
const loadingMessages = ref(false);
const creatingSession = ref(false);
const chatScrollRef = ref(null);

function isUserMessage(role) {
  return String(role || "").toUpperCase() === "USER";
}

function formatSessionTime(value) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  const month = date.getMonth() + 1;
  const day = date.getDate();
  const hour = String(date.getHours()).padStart(2, "0");
  const minute = String(date.getMinutes()).padStart(2, "0");
  return `${month}/${day} ${hour}:${minute}`;
}

function formatMessageTime(value) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  const hour = String(date.getHours()).padStart(2, "0");
  const minute = String(date.getMinutes()).padStart(2, "0");
  return `${hour}:${minute}`;
}

async function loadSessions(preferredSessionId) {
  loadingSessions.value = true;
  try {
    const rows = (await listAiAssistantSessions()) || [];
    sessionList.value = rows;
    if (preferredSessionId && rows.some((item) => item.sessionId === preferredSessionId)) {
      activeSessionId.value = preferredSessionId;
      return;
    }
    if (activeSessionId.value && !rows.some((item) => item.sessionId === activeSessionId.value)) {
      activeSessionId.value = rows.length ? rows[0].sessionId : null;
      return;
    }
    if (!activeSessionId.value && rows.length) {
      activeSessionId.value = rows[0].sessionId;
    }
  } catch (error) {
    ElMessage.error(error?.message || "加载会话失败");
  } finally {
    loadingSessions.value = false;
  }
}

async function loadMessages(sessionId) {
  if (!sessionId) {
    messages.value = [];
    return;
  }
  loadingMessages.value = true;
  try {
    messages.value = (await listAiAssistantMessages(sessionId)) || [];
    await nextTick();
    scrollToBottom();
  } catch (error) {
    ElMessage.error(error?.message || "加载消息失败");
    messages.value = [];
  } finally {
    loadingMessages.value = false;
  }
}

async function handleOpen() {
  await loadSessions(activeSessionId.value);
  if (activeSessionId.value) {
    await loadMessages(activeSessionId.value);
  } else {
    messages.value = [];
  }
}

async function switchSession(sessionId) {
  if (sessionId === activeSessionId.value) {
    return;
  }
  activeSessionId.value = sessionId;
  await loadMessages(sessionId);
}

async function createNewSession() {
  creatingSession.value = true;
  try {
    const sessionId = await createAiAssistantSession();
    activeSessionId.value = sessionId;
    messages.value = [];
    input.value = "";
    await loadSessions(sessionId);
  } catch (error) {
    ElMessage.error(error?.message || "新建会话失败");
  } finally {
    creatingSession.value = false;
  }
}

async function renameSession(item) {
  try {
    const { value } = await ElMessageBox.prompt("请输入新的会话名称", "重命名会话", {
      confirmButtonText: "保存",
      cancelButtonText: "取消",
      inputValue: item.title || "新对话",
      inputPattern: /\S+/,
      inputErrorMessage: "名称不能为空",
    });
    await renameAiAssistantSession(item.sessionId, { title: value.trim() });
    ElMessage.success("已重命名");
    await loadSessions(activeSessionId.value);
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error?.message || "重命名失败");
    }
  }
}

async function confirmDeleteSession(item) {
  try {
    await ElMessageBox.confirm("删除后无法恢复，确定删除该会话吗？", "删除会话", {
      confirmButtonText: "删除",
      cancelButtonText: "取消",
      type: "warning",
    });
    const deletedId = item.sessionId;
    const wasActive = activeSessionId.value === deletedId;
    sessionList.value = sessionList.value.filter((row) => row.sessionId !== deletedId);
    if (wasActive) {
      activeSessionId.value = sessionList.value[0]?.sessionId ?? null;
      messages.value = [];
    }
    await deleteAiAssistantSession(deletedId);
    ElMessage.success("已删除");
    if (wasActive && activeSessionId.value) {
      await loadMessages(activeSessionId.value);
    }
    await loadSessions(activeSessionId.value);
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error?.message || "删除失败");
      await loadSessions(activeSessionId.value);
    }
  }
}

function scrollToBottom() {
  const el = chatScrollRef.value;
  if (el) {
    el.scrollTop = el.scrollHeight;
  }
}

async function send() {
  const text = input.value.trim();
  if (!text || sending.value) {
    return;
  }
  const optimistic = {
    role: "USER",
    text,
    createTime: new Date().toISOString(),
  };
  messages.value.push(optimistic);
  input.value = "";
  sending.value = true;
  await nextTick();
  scrollToBottom();
  try {
    const resp = await aiAssistantChat({
      sessionId: activeSessionId.value,
      message: text,
    });
    activeSessionId.value = resp.sessionId;
    if (resp.outOfScope) {
      ElMessage.warning(resp.replyText);
    }
    await loadSessions(resp.sessionId);
    await loadMessages(resp.sessionId);
  } catch (error) {
    messages.value.pop();
    input.value = text;
    ElMessage.error(error?.message || "发送失败");
  } finally {
    sending.value = false;
  }
}

async function confirmExport(preview) {
  if (!preview?.previewToken) {
    return;
  }
  if (preview.confirmDisabled) {
    ElMessage.warning(preview.confirmDisabledReason || "当前预览不可确认导出");
    return;
  }
  exportingToken.value = preview.previewToken;
  try {
    const blob = await confirmAiExport({ previewToken: preview.previewToken });
    if (!blobValidate(blob)) {
      throw new Error("导出失败");
    }
    saveAs(blob, preview.exportType === "ORDER" ? "orders.xlsx" : "statistics.xlsx");
    ElMessage.success("导出成功");
  } catch (error) {
    ElMessage.error(error?.message || "导出失败");
  } finally {
    exportingToken.value = "";
  }
}

function openOrderDetail(orderId) {
  visible.value = false;
  router.push(`/repair/detail/${orderId}`);
}

function openAssetDetail(assetId) {
  visible.value = false;
  router.push(`/admin/assets/${assetId}`);
}

function openSuggestionDetail(suggestionId) {
  visible.value = false;
  if (isAdmin.value) {
    router.push({ path: "/admin/suggestions", query: { suggestionId: String(suggestionId) } });
    return;
  }
  router.push("/repair/suggestion/mine");
}
