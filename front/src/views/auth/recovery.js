import { ElMessage } from "element-plus";
import CaptchaVerify from "@/components/CaptchaVerify";
import {
  sendVerificationCode,
  recoveryVerify,
  recoveryComplete,
} from "@/api/auth";

// 路由及状态变量定义
const router = useRouter();
const devCode = ref("");

// 找回密码表单数据
const form = reactive({
  target: "",
  captchaTicket: "",
  verificationId: "",
  verificationCode: "",
  recoveryTicket: "",
  newPassword: "",
});

// 发送验证码
async function send() {
  const r = await sendVerificationCode({
    scene: "RECOVERY",
    target: form.target,
    captchaTicket: form.captchaTicket,
  });
  form.verificationId = r.verificationId;
  devCode.value = r.developmentCode || "";
}

// 完成密码重置流程
async function complete() {
  const r = await recoveryVerify(form);
  form.recoveryTicket = r.recoveryTicket;
  await recoveryComplete(form);
  ElMessage.success("密码已重置");
  router.push("/login");
}
