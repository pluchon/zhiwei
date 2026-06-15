import { ElMessage, ElMessageBox } from "element-plus";
import {
  listMySuggestions,
  submitSuggestion,
  updateSuggestion,
  withdrawSuggestion,
  getSuggestion,
  checkSuggestionSimilarity,
} from "@/api/repair/suggestion";
import { parseTime } from "@/utils/ruoyi";
import {
  suggestionCategoryOptions,
  suggestionCategoryText,
  suggestionStatusText,
  suggestionStatusType,
} from "@/utils/repair";

const loading = ref(false);
const submitting = ref(false);
const detailVisible = ref(false);
const formVisible = ref(false);
const current = ref(null);
const editingId = ref(null);

const query = reactive({ pageNum: 1, pageSize: 10 });
const data = reactive({ records: [], total: 0 });

const form = reactive({
  category: "",
  title: "",
  content: "",
});

function canEdit(row) {
  return row.withdrawnFlag === 1 && !["ACCEPTED", "REJECTED"].includes(row.status);
}

function formatTime(value) {
  return parseTime(value, "{y}-{m}-{d} {h}:{i}:{s}") || "-";
}

function resetForm() {
  form.category = "";
  form.title = "";
  form.content = "";
  editingId.value = null;
}

async function load() {
  loading.value = true;
  try {
    Object.assign(data, await listMySuggestions(query));
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  resetForm();
  formVisible.value = true;
}

async function startEdit(row) {
  try {
    const detail = await getSuggestion(row.suggestionId);
    editingId.value = detail.suggestionId;
    form.category = detail.category;
    form.title = detail.title;
    form.content = detail.content;
    formVisible.value = true;
  } catch (error) {
    ElMessage.error(error?.message || "建议加载失败");
  }
}

async function viewDetail(row) {
  current.value = await getSuggestion(row.suggestionId);
  detailVisible.value = true;
}

function validateForm() {
  if (!form.category) {
    ElMessage.warning("请选择建议分类");
    return false;
  }
  if (!form.title?.trim()) {
    ElMessage.warning("请填写建议标题");
    return false;
  }
  if (!form.content?.trim()) {
    ElMessage.warning("请填写建议内容");
    return false;
  }
  return true;
}

async function submit() {
  if (!validateForm()) {
    return;
  }
  submitting.value = true;
  try {
    const payload = {
      category: form.category,
      title: form.title.trim(),
      content: form.content.trim(),
    };
    const similarity = await checkSuggestionSimilarity(payload, editingId.value || undefined);
    if (similarity?.hasSimilar) {
      if (similarity.othersSimilar) {
        await ElMessageBox.alert(similarity.message || "系统中存在内容相似的建议，如确为新建议可继续提交。", "相似内容提醒", {
          confirmButtonText: "继续提交",
        });
      } else {
        await ElMessageBox.confirm(
          `${similarity.message || "检测到您曾提交过相似建议。"}\n标题：${similarity.title || "-"}\n状态：${similarity.statusLabel || "-"}`,
          "相似内容提醒",
          {
            confirmButtonText: "继续提交",
            cancelButtonText: "返回修改",
            type: "warning",
          },
        );
      }
    }
    if (editingId.value) {
      await updateSuggestion(editingId.value, payload);
      ElMessage.success("建议已重新提交");
    } else {
      await submitSuggestion(payload);
      ElMessage.success("建议已提交");
    }
    formVisible.value = false;
    await load();
  } catch (error) {
    if (error !== "cancel" && error !== "close") {
      ElMessage.error(error?.message || "提交失败");
    }
  } finally {
    submitting.value = false;
  }
}

async function withdraw(row) {
  try {
    await ElMessageBox.confirm("撤回后可编辑并重新提交，是否继续？", "撤回确认", {
      type: "warning",
    });
    await withdrawSuggestion(row.suggestionId);
    ElMessage.success("建议已撤回");
    await load();
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(error?.message || "撤回失败");
    }
  }
}

onMounted(load);
