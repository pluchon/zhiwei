import defaultSettings from "@/settings";
import { useDynamicTitle } from "@/utils/dynamicTitle";

document.documentElement.classList.remove("dark");

const {
  sideTheme,
  showSettings,
  navType,
  tagsView,
  tagsViewPersist,
  tagsIcon,
  tagsViewStyle,
  fixedHeader,
  sidebarLogo,
  dynamicTitle,
  footerVisible,
  footerContent,
} = defaultSettings;

const storageSetting = JSON.parse(localStorage.getItem("layout-setting")) || "";

const useSettingsStore = defineStore("settings", {
  state: () => ({
    title: "",
    theme: storageSetting.theme || "#1D4ED8",
    sideTheme: storageSetting.sideTheme || sideTheme,
    showSettings: showSettings,
    navType:
      storageSetting.navType === undefined ? navType : storageSetting.navType,
    tagsView:
      storageSetting.tagsView === undefined
        ? tagsView
        : storageSetting.tagsView,
    tagsViewPersist:
      storageSetting.tagsViewPersist === undefined
        ? tagsViewPersist
        : storageSetting.tagsViewPersist,
    tagsIcon:
      storageSetting.tagsIcon === undefined
        ? tagsIcon
        : storageSetting.tagsIcon,
    tagsViewStyle:
      storageSetting.tagsViewStyle === undefined
        ? tagsViewStyle
        : storageSetting.tagsViewStyle,
    fixedHeader:
      storageSetting.fixedHeader === undefined
        ? fixedHeader
        : storageSetting.fixedHeader,
    sidebarLogo:
      storageSetting.sidebarLogo === undefined
        ? sidebarLogo
        : storageSetting.sidebarLogo,
    dynamicTitle:
      storageSetting.dynamicTitle === undefined
        ? dynamicTitle
        : storageSetting.dynamicTitle,
    footerVisible:
      storageSetting.footerVisible === undefined
        ? footerVisible
        : storageSetting.footerVisible,
    footerContent: footerContent,
    isDark: false,
  }),
  actions: {
    // 修改布局设置
    changeSetting(data) {
      const { key, value } = data;
      if (this.hasOwnProperty(key)) {
        this[key] = value;
      }
    },
    // 设置网页标题
    setTitle(title) {
      this.title = title;
      useDynamicTitle();
    },
  },
});

export default useSettingsStore;
