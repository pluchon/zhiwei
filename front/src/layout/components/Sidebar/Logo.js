import logo from "@/assets/logo/logo.png";
import useSettingsStore from "@/store/modules/settings";
import variables from "@/assets/styles/variables.module.scss";

defineProps({
  collapse: {
    type: Boolean,
    required: true,
  },
});

const brandName = "知维";
const settingsStore = useSettingsStore();
const sideTheme = computed(() => settingsStore.sideTheme);

// 获取Logo背景色
const getLogoBackground = computed(() => {
  if (settingsStore.isDark) {
    return "var(--sidebar-bg)";
  }
  if (settingsStore.navType == 3) {
    return variables.menuLightBg;
  }
  return "#f4f6f9";
});

// 获取Logo文字颜色
const getLogoTextColor = computed(() => {
  if (settingsStore.isDark) {
    return "var(--sidebar-logo-text)";
  }
  if (settingsStore.navType == 3) {
    return variables.menuLightText;
  }
  return "#334155";
});
