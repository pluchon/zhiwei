import useAppStore from "@/store/modules/app";
import useSettingsStore from "@/store/modules/settings";
import usePermissionStore from "@/store/modules/permission";
import { handleThemeStyle } from "@/utils/theme";

const { proxy } = getCurrentInstance();
const appStore = useAppStore();
const settingsStore = useSettingsStore();
const permissionStore = usePermissionStore();
const showSettings = ref(false);
const navType = ref(settingsStore.navType);
const theme = ref(settingsStore.theme);
const sideTheme = ref(settingsStore.sideTheme);
const tagsViewPersist = ref(settingsStore.tagsViewPersist);
const storeSettings = computed(() => settingsStore);
const predefineColors = ref([
  "#409EFF",
  "#ff4500",
  "#ff8c00",
  "#ffd700",
  "#90ee90",
  "#00ced1",
  "#1e90ff",
  "#c71585",
]);

/** 是否需要dynamicTitle */
function dynamicTitleChange() {
  useSettingsStore().setTitle(useSettingsStore().title);
}

function tagsViewPersistChange(val) {
  settingsStore.tagsViewPersist = val;
  tagsViewPersist.value = val;
}

function themeChange(val) {
  settingsStore.theme = val;
  handleThemeStyle(val);
}

function handleTheme(val) {
  settingsStore.sideTheme = val;
  sideTheme.value = val;
}

function handleNavType(val) {
  settingsStore.navType = val;
  navType.value = val;
}

/** 菜单导航设置 */
watch(
  () => navType,
  (val) => {
    if (val.value == 1) {
      appStore.sidebar.opened = true;
      appStore.toggleSideBarHide(false);
    }
    if (val.value == 2) {
      appStore.sidebar.opened = true;
    }
    if (val.value == 3) {
      appStore.sidebar.opened = false;
      appStore.toggleSideBarHide(true);
    }
    if ([1, 3].includes(val.value)) {
      permissionStore.setSidebarRouters(permissionStore.defaultRoutes);
    }
  },
  { immediate: true, deep: true },
);

function saveSetting() {
  proxy.$modal.loading("正在保存到本地，请稍候...");
  if (!tagsViewPersist.value) {
    proxy.$cache.local.remove("tags-view-visited");
  }
  let layoutSetting = {
    navType: storeSettings.value.navType,
    tagsView: storeSettings.value.tagsView,
    tagsIcon: storeSettings.value.tagsIcon,
    tagsViewStyle: storeSettings.value.tagsViewStyle,
    tagsViewPersist: storeSettings.value.tagsViewPersist,
    fixedHeader: storeSettings.value.fixedHeader,
    sidebarLogo: storeSettings.value.sidebarLogo,
    dynamicTitle: storeSettings.value.dynamicTitle,
    footerVisible: storeSettings.value.footerVisible,
    sideTheme: storeSettings.value.sideTheme,
    theme: storeSettings.value.theme,
  };
  localStorage.setItem("layout-setting", JSON.stringify(layoutSetting));
  setTimeout(proxy.$modal.closeLoading(), 1000);
}

function resetSetting() {
  proxy.$cache.local.remove("tags-view-visited");
  proxy.$modal.loading("正在清除设置缓存并刷新，请稍候...");
  localStorage.removeItem("layout-setting");
  setTimeout("window.location.reload()", 1000);
}

function openSetting() {
  showSettings.value = true;
}

defineExpose({
  openSetting,
});
