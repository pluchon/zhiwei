import { ElMessageBox } from "element-plus";
import { MagicStick } from "@element-plus/icons-vue";
import Breadcrumb from "@/components/Breadcrumb";
import TopNav from "./TopNav";
import TopBar from "./TopBar";
import Logo from "./Sidebar/Logo";
import Hamburger from "@/components/Hamburger";
import Screenfull from "@/components/Screenfull";
import SizeSelect from "@/components/SizeSelect";
import AiAssistant from "@/views/common/ai-assistant/index.vue";
import useAppStore from "@/store/modules/app";
import useUserStore from "@/store/modules/user";
import useSettingsStore from "@/store/modules/settings";
const appStore = useAppStore();
const userStore = useUserStore();
const settingsStore = useSettingsStore();

const aiAssistantVisible = ref(false);
const canUseAiAssistant = computed(() => {
  const role = userStore.roles[0] || "";
  return role === "ADMIN" || role === "REPAIRER";
});

function toggleSideBar() {
  appStore.toggleSideBar();
}

function handleCommand(command) {
  switch (command) {
    case "setLayout":
      setLayout();
      break;
    case "logout":
      logout();
      break;
    default:
      break;
  }
}

function logout() {
  ElMessageBox.confirm("确定注销并退出系统吗？", "提示", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(() => {
      userStore.logOut().then(() => {
        location.href = "/index";
      });
    })
    .catch(() => {});
}

const emits = defineEmits(["setLayout"]);
function setLayout() {
  emits("setLayout");
}
