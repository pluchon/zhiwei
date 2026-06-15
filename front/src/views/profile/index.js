import { ElMessage, ElMessageBox } from "element-plus";
import useUserStore from "@/store/modules/user";
import { changePassword, uploadProfileAvatar } from "@/api/auth";
import { getInfo } from "@/api/login";
import {
  getRepairerAvailability,
  updateRepairerAvailability,
} from "@/api/repairer/index";
import defAva from "@/assets/images/profile.jpg";

const store = useUserStore();
const form = reactive({ oldPassword: "", newPassword: "" });

const profile = reactive({
  user: null,
  roleCode: "",
  roleLabel: "",
  avatarUrl: "",
});

const avatarUploading = ref(false);
const isRepairer = computed(() => (profile.roleCode || store.roles[0]) === "REPAIRER");
const availabilityLoading = ref(false);
const availabilitySaving = ref(false);
const availability = ref(null);

const pauseDialog = ref(false);
const pauseForm = reactive({
  pauseReason: "",
  expectedResumeTime: null,
});

const displayAvatar = computed(() => profile.avatarUrl || store.avatar || defAva);

const avatarFallback = computed(() => {
  const name = profile.user?.realName || profile.user?.nickName || store.nickName || "?";
  return name.slice(0, 1);
});

async function loadProfile() {
  const result = await getInfo();
  profile.user = result.user || result;
  profile.roleCode = result.roleCode || store.roles[0] || "";
  profile.roleLabel = result.roleLabel || profile.roleCode;
  profile.avatarUrl = result.avatarUrl || profile.user?.avatar || store.avatar || defAva;
  store.nickName = profile.user?.nickName || profile.user?.realName || store.nickName;
  store.avatar = profile.avatarUrl;
}

async function handleAvatarUpload({ file }) {
  avatarUploading.value = true;
  try {
    const result = await uploadProfileAvatar(file);
    profile.avatarUrl = result.avatarUrl;
    store.avatar = result.avatarUrl;
    ElMessage.success(result.message || "头像已更新");
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || "头像上传失败");
  } finally {
    avatarUploading.value = false;
  }
}

async function loadAvailability() {
  if (!isRepairer.value) {
    return;
  }
  availabilityLoading.value = true;
  try {
    availability.value = await getRepairerAvailability();
  } catch (error) {
    ElMessage.error(error?.message || "接单状态加载失败");
  } finally {
    availabilityLoading.value = false;
  }
}

function openPauseDialog() {
  Object.assign(pauseForm, {
    pauseReason: "",
    expectedResumeTime: null,
  });
  pauseDialog.value = true;
}

function switchToPaused() {
  if (availability.value?.acceptingState === "PAUSED" || availabilitySaving.value) {
    return;
  }
  openPauseDialog();
}

function switchToAvailable() {
  if (availability.value?.acceptingState === "AVAILABLE" || availabilitySaving.value) {
    return;
  }
  handleResume();
}

async function submitPause() {
  if (!pauseForm.pauseReason?.trim()) {
    ElMessage.warning("请填写暂停原因");
    return;
  }
  availabilitySaving.value = true;
  try {
    availability.value = await updateRepairerAvailability({
      acceptingState: "PAUSED",
      pauseReason: pauseForm.pauseReason.trim(),
      expectedResumeTime: pauseForm.expectedResumeTime || null,
    });
    ElMessage.success("已暂停接单");
    pauseDialog.value = false;
  } catch (error) {
    ElMessage.error(error?.message || "操作失败");
  } finally {
    availabilitySaving.value = false;
  }
}

async function handleResume() {
  try {
    await ElMessageBox.confirm("恢复后将重新出现在待接工单匹配中，是否继续？", "恢复接单", {
      type: "warning",
    });
    availabilitySaving.value = true;
    availability.value = await updateRepairerAvailability({
      acceptingState: "AVAILABLE",
    });
    ElMessage.success("已恢复接单");
  } catch (error) {
    if (error !== "cancel") {
      ElMessage.error(error?.message || "操作失败");
    }
  } finally {
    availabilitySaving.value = false;
  }
}

async function submit() {
  await changePassword(form);
  ElMessage.success("密码已修改，请重新登录");
  store.resetState();
  location.href = "/login";
}

onMounted(async () => {
  await loadProfile();
  await loadAvailability();
});
