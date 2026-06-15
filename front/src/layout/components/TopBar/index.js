import SidebarItem from "../Sidebar/SidebarItem";
import useAppStore from "@/store/modules/app";
import useSettingsStore from "@/store/modules/settings";
import usePermissionStore from "@/store/modules/permission";

const route = useRoute();
const appStore = useAppStore();
const settingsStore = useSettingsStore();
const permissionStore = usePermissionStore();

const sidebarRouters = computed(() => permissionStore.sidebarRouters);
const theme = computed(() => settingsStore.theme);
const device = computed(() => appStore.device);
const activeMenu = computed(() => {
  const { meta, path } = route;
  if (meta.activeMenu) {
    return meta.activeMenu;
  }
  return path;
});

const visibleNumber = ref(5);
const topMenus = computed(() => {
  return permissionStore.sidebarRouters
    .filter((f) => !f.hidden)
    .slice(0, visibleNumber.value);
});
const moreRoutes = computed(() => {
  return permissionStore.sidebarRouters
    .filter((f) => !f.hidden)
    .slice(visibleNumber.value);
});
function setVisibleNumber() {
  const width = document.body.getBoundingClientRect().width / 3;
  visibleNumber.value = Math.max(1, parseInt(width / 85));
}

onMounted(() => {
  window.addEventListener("resize", setVisibleNumber);
});
onBeforeUnmount(() => {
  window.removeEventListener("resize", setVisibleNumber);
});

onMounted(() => {
  setVisibleNumber();
});
