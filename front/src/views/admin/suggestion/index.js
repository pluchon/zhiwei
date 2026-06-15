import { ElMessage } from "element-plus";
import {
  listAdminSuggestions,
  getSuggestion,
  handleSuggestion,
} from "@/api/repair/suggestion";
import {
  suggestionCategoryOptions,
  suggestionCategoryText,
  suggestionStatusOptions,
  suggestionStatusText,
  suggestionStatusType,
} from "@/utils/repair";

const loading = ref(false);
const submitting = ref(false);
const detailVisible = ref(false);
const handleVisible = ref(false);
const current = ref(null);
let handlingId = null;

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null,
  category: null,
});

const data = reactive({ records: [], total: 0 });

const handleForm = reactive({
  status: "ACCEPTED",
  adminReply: "",
});

async function load() {
  loading.value = true;
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    };
    if (query.status) {
      params.status = query.status;
    }
    if (query.category) {
      params.category = query.category;
    }
    Object.assign(data, await listAdminSuggestions(params));
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  load();
}

function resetFilters() {
  query.status = null;
  query.category = null;
  search();
}

async function openDetail(row) {
  current.value = await getSuggestion(row.suggestionId);
  detailVisible.value = true;
}

function avatarFallback(name) {
  const text = (name || "").trim();
  return text ? text.slice(0, 1) : "师";
}

function openHandle(row) {
  handlingId = row.suggestionId;
  handleForm.status = "ACCEPTED";
  handleForm.adminReply = "";
  handleVisible.value = true;
}

async function submitHandle() {
  if (!handleForm.adminReply?.trim()) {
    ElMessage.warning("请填写回复说明");
    return;
  }
  submitting.value = true;
  try {
    await handleSuggestion(handlingId, {
      status: handleForm.status,
      adminReply: handleForm.adminReply.trim(),
    });
    ElMessage.success("建议已处理");
    handleVisible.value = false;
    await load();
  } catch (error) {
    ElMessage.error(error?.message || "处理失败");
  } finally {
    submitting.value = false;
  }
}

onMounted(load);

defineExpose({ load });
