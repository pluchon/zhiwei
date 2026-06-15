import CaptchaVerify from "@/components/CaptchaVerify";
import { fetchPortalSummary, sendVerificationCode } from "@/api/auth";
import useUserStore from "@/store/modules/user";
import { ElMessage } from "element-plus";

const router = useRouter();
const route = useRoute();
const store = useUserStore();

const loginType = ref("password");
const loading = ref(false);
const devCode = ref("");
const portalSummary = ref(null);

const statItems = [
  { key: "todayOrders", label: "今日报修" },
  { key: "processingOrders", label: "处理中" },
  { key: "campusCount", label: "覆盖校区" },
];

const displayStats = reactive({
  todayOrders: 0,
  processingOrders: 0,
  campusCount: 0,
});

const pointer = reactive({ x: 0, y: 0 });

const password = reactive({
  userNo: "",
  password: "",
  captchaTicket: "",
});

const phone = reactive({
  target: "",
  captchaTicket: "",
  verificationId: "",
  verificationCode: "",
});

function onMouseMove(event) {
  const x = (event.clientX / window.innerWidth - 0.5) * 2;
  const y = (event.clientY / window.innerHeight - 0.5) * 2;
  pointer.x = x;
  pointer.y = y;
}

function parallaxStyle(factor) {
  return {
    transform: `translate3d(${pointer.x * factor * 24}px, ${pointer.y * factor * 24}px, 0)`,
  };
}

function animateStat(key, target) {
  const end = Number(target) || 0;
  const duration = 900;
  const start = performance.now();
  const from = displayStats[key];

  function frame(now) {
    const progress = Math.min((now - start) / duration, 1);
    const eased = 1 - (1 - progress) ** 3;
    displayStats[key] = Math.round(from + (end - from) * eased);
    if (progress < 1) {
      requestAnimationFrame(frame);
    }
  }

  requestAnimationFrame(frame);
}

function syncStats(summary) {
  if (!summary) {
    return;
  }
  statItems.forEach(({ key }) => {
    animateStat(key, summary[key] ?? 0);
  });
}

onMounted(async () => {
  try {
    portalSummary.value = await fetchPortalSummary();
    syncStats(portalSummary.value);
  } catch {
    // 概览数据加载失败时不影响登录
  }
});

async function sendCode() {
  if (!phone.target) {
    ElMessage.warning("请先填写手机号");
    return;
  }

  if (!phone.captchaTicket) {
    ElMessage.warning("请先完成验证");
    return;
  }

  try {
    const r = await sendVerificationCode({
      scene: "LOGIN_SMS",
      target: phone.target,
      captchaTicket: phone.captchaTicket,
    });
    phone.verificationId = r.verificationId;
    devCode.value = r.developmentCode || "";
  } catch {
    // 请求拦截器已统一展示后端业务错误
  }
}

async function submitPassword() {
  if (!password.userNo || !password.password) {
    ElMessage.warning("请填写账号和密码");
    return;
  }

  if (!password.captchaTicket) {
    ElMessage.warning("请先完成验证");
    return;
  }

  loading.value = true;
  try {
    await store.passwordLogin(password);
    router.push(route.query.redirect || "/");
  } catch {
    // 登录失败由请求拦截器展示
  } finally {
    loading.value = false;
  }
}

async function submitPhone() {
  if (!phone.target || !phone.verificationId || !phone.verificationCode) {
    ElMessage.warning("请填写手机号并完成验证码校验");
    return;
  }

  loading.value = true;
  try {
    await store.phoneLogin(phone);
    router.push(route.query.redirect || "/");
  } catch {
    // 登录失败由请求拦截器展示
  } finally {
    loading.value = false;
  }
}
