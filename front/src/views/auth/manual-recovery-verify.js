import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import CaptchaVerify from "@/components/CaptchaVerify";
import { sendVerificationCode } from "@/api/auth";
import {
  getManualRecoveryVerifyInfo,
  verifyManualRecoveryPhone,
} from "@/api/admin/manual-account-recovery";
import { manualRecoveryStatusText } from "@/utils/repair";

const route = useRoute();
const router = useRouter();
const recoveryId = computed(() => route.params.recoveryId);

const loading = ref(true);
const submitting = ref(false);
const blocked = ref(false);
const blockMessage = ref("");
const info = ref(null);
const devCode = ref("");

const form = reactive({
  target: "",
  captchaTicket: "",
  verificationId: "",
  verificationCode: "",
});

async function loadInfo() {
  loading.value = true;
  blocked.value = false;
  blockMessage.value = "";
  try {
    info.value = await getManualRecoveryVerifyInfo(recoveryId.value);
  } catch (error) {
    blocked.value = true;
    blockMessage.value = error?.message || "无法继续验证";
    info.value = null;
  } finally {
    loading.value = false;
  }
}

async function send() {
  if (!form.target?.trim()) {
    ElMessage.warning("请填写新手机号");
    return;
  }
  const result = await sendVerificationCode({
    scene: "MANUAL_RECOVERY",
    target: form.target.trim(),
    captchaTicket: form.captchaTicket,
  });
  form.verificationId = result.verificationId;
  devCode.value = result.developmentCode || "";
}

async function submit() {
  if (!form.verificationId || !form.verificationCode?.trim()) {
    ElMessage.warning("请先发送并填写验证码");
    return;
  }
  submitting.value = true;
  try {
    await verifyManualRecoveryPhone(recoveryId.value, {
      verificationId: form.verificationId,
      verificationCode: form.verificationCode.trim(),
    });
    ElMessage.success("验证成功，请使用新手机号重新登录");
    router.push("/login");
  } catch (error) {
    ElMessage.error(error?.message || "验证失败");
    await loadInfo();
  } finally {
    submitting.value = false;
  }
}

onMounted(loadInfo);
