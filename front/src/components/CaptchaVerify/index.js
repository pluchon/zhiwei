import { captchaChallenge, captchaTicket } from "@/api/auth";
import { ElMessage } from "element-plus";

const props = defineProps({
  scene: { type: String, required: true },
  target: { type: String, default: "" },
  targetRequired: { type: Boolean, default: true },
  compact: { type: Boolean, default: false },
  block: { type: Boolean, default: false },
});
const emit = defineEmits(["verified"]);
const loading = ref(false);
const verified = ref(false);

async function verify() {
  if (props.targetRequired && !String(props.target || "").trim()) {
    ElMessage.warning("请先填写账号、手机号或邮箱");
    return;
  }

  loading.value = true;
  try {
    const challenge = await captchaChallenge({
      scene: props.scene,
      target: props.target,
    });
    const result = await captchaTicket({
      challengeId: challenge.challengeId,
      scene: props.scene,
      target: props.target,
    });
    verified.value = true;
    emit("verified", result.captchaTicket);
  } finally {
    loading.value = false;
  }
}

watch(
  () => props.target,
  () => {
    // 验证票据会绑定当前目标，账号或手机号变化后必须重新验证。
    verified.value = false;
    emit("verified", "");
  },
);
