import { constantRoutes } from "@/router";
import { isHttp } from "@/utils/validate";
import useAppStore from "@/store/modules/app";
import useSettingsStore from "@/store/modules/settings";
import usePermissionStore from "@/store/modules/permission";

// 顶部栏初始数
const visibleNumber = ref(null);
// 当前激活菜单的 index
const currentIndex = ref(null);
// 隐藏侧边栏路由
const hideList = ["/index", "/user/profile"];

const appStore = useAppStore();
const settingsStore = useSettingsStore();
const permissionStore = usePermissionStore();
const route = useRoute();
const router = useRouter();

// 主题颜色
const theme = computed(() => settingsStore.theme);
// 所有的路由信息
const routers = computed(() => permissionStore.topbarRouters || []);

function resolveTopMenu(routeItem) {
  if (routeItem.path === "/" && routeItem.children?.length) {
    return routeItem.children[0];
  }
  if (routeItem.children?.length === 1 && routeItem.children[0].meta && !routeItem.meta) {
    return { ...routeItem, meta: routeItem.children[0].meta };
  }
  return routeItem;
}

// 顶部显示菜单
const topMenus = computed(() => {
  const menus = [];
  routers.value.forEach((menu) => {
    if (menu.hidden !== true) {
      menus.push(resolveTopMenu(menu));
    }
  });
  return menus;
});

// 设置子路由
const childrenMenus = computed(() => {
  const items = [];
  routers.value.forEach((parentRoute) => {
    if (!Array.isArray(parentRoute.children)) {
      return;
    }
    parentRoute.children.forEach((childRoute) => {
      const child = { ...childRoute };
      if (child.parentPath === undefined) {
        if (parentRoute.path === "/") {
          child.path = `/${child.path}`;
        } else if (!isHttp(child.path)) {
          child.path = child.path
            ? `${parentRoute.path}/${child.path}`.replace(/\/+/g, "/")
            : parentRoute.path;
        }
        child.parentPath = parentRoute.path;
      }
      items.push(child);
    });
  });
  return constantRoutes.concat(items);
});

function resolveActiveTopPath(path) {
  const matched = routers.value.find(
    (item) => !item.hidden && (item.path === path || path.startsWith(`${item.path}/`)),
  );
  if (!matched) {
    return path;
  }
  if (!matched.children || matched.children.length <= 1) {
    return matched.path;
  }
  return matched.path;
}

// 默认激活的菜单
const activeMenu = computed(() => {
  const path = route.path;
  let activePath = path;

  if (path !== undefined && path.lastIndexOf("/") > 0 && hideList.indexOf(path) === -1) {
    if (!route.meta.link) {
      activePath = resolveActiveTopPath(path);
      appStore.toggleSideBarHide(
        !routers.value.some(
          (item) =>
            !item.hidden &&
            item.path === activePath &&
            Array.isArray(item.children) &&
            item.children.length > 1,
        ),
      );
    }
  } else if (!route.children) {
    activePath = path;
    appStore.toggleSideBarHide(true);
  }

  activeRoutes(activePath);
  return activePath;
});

function setVisibleNumber() {
  const width = document.body.getBoundingClientRect().width / 3;
  visibleNumber.value = Math.max(1, parseInt(width / 85));
}

function handleSelect(key, keyPath) {
  currentIndex.value = key;
  const matchedRoute = routers.value.find((item) => item.path === key);
  if (isHttp(key)) {
    window.open(key, "_blank");
  } else if (!matchedRoute || !matchedRoute.children || matchedRoute.children.length <= 1) {
    const routeMenu = childrenMenus.value.find((item) => item.path === key);
    if (routeMenu && routeMenu.query) {
      const query = JSON.parse(routeMenu.query);
      router.push({ path: key, query });
    } else {
      router.push({ path: key });
    }
    appStore.toggleSideBarHide(true);
  } else {
    activeRoutes(key);
    appStore.toggleSideBarHide(false);
  }
}

function activeRoutes(key) {
  const routes = [];
  if (childrenMenus.value.length > 0) {
    childrenMenus.value.forEach((item) => {
      if (key === item.parentPath || (key === "index" && item.path === "")) {
        routes.push(item);
      }
    });
  }
  if (routes.length > 0) {
    permissionStore.setSidebarRouters(routes);
  } else {
    appStore.toggleSideBarHide(true);
  }
  return routes;
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
