import { ElMessage } from "element-plus";
import CaptchaVerify from "@/components/CaptchaVerify";
import {
  activationStart,
  activationComplete,
  sendVerificationCode,
} from "@/api/auth";

// 路由及状态变量定义
const router = useRouter();
const step = ref(0);
const maskedPhone = ref("");
const devCode = ref("");

// 激活表单数据
const form = reactive({
  userNo: "",
  initialPassword: "",
  activationTicket: "",
  target: "",
  captchaTicket: "",
  verificationId: "",
  verificationCode: "",
  newPassword: "",
});

// 第一步：验证账号和初始密码，获取激活票据和脱敏手机号
async function start() {
  const r = await activationStart(form);
  form.activationTicket = r.activationTicket;
  maskedPhone.value = r.maskedPhone;
  step.value = 1;
}

// 发送短信验证码
async function send() {
  const r = await sendVerificationCode({
    scene: "ACTIVATION",
    target: form.target,
    captchaTicket: form.captchaTicket,
  });
  form.verificationId = r.verificationId;
  devCode.value = r.developmentCode || "";
}

// 第二步：完成激活，校验短信并设置新密码
async function complete() {
  await activationComplete(form);
  ElMessage.success("账号激活成功");
  router.push("/login");
}
