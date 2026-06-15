import router from "./router";
import { ElMessage } from "element-plus";
import NProgress from "nprogress";
import "nprogress/nprogress.css";
import { getToken } from "@/utils/auth";
import { isHttp, isPathMatch } from "@/utils/validate";
import { isRelogin } from "@/utils/request";
import useUserStore from "@/store/modules/user";
import useSettingsStore from "@/store/modules/settings";
import usePermissionStore from "@/store/modules/permission";
import useNotificationStore from "@/store/modules/notification";

NProgress.configure({ showSpinner: false });

const whiteList = ["/login", "/activation", "/recovery", "/manual-recovery/*", "/401"];

const isWhiteList = (path) => {
  return whiteList.some((pattern) => isPathMatch(pattern, path));
};

function registerDynamicRoutes(accessRoutes) {
  accessRoutes.forEach((route) => {
    if (!isHttp(route.path)) {
      router.addRoute(route);
    }
  });
  if (!router.hasRoute("PathMatch404")) {
    router.addRoute({
      path: "/:pathMatch(.*)*",
      name: "PathMatch404",
      component: () => import("@/views/error/404.vue"),
      hidden: true,
    });
  }
}

router.beforeEach(async (to, from) => {
  NProgress.start();
  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title);
    if (to.path === "/login") {
      NProgress.done();
      return { path: "/" };
    }
    if (isWhiteList(to.path)) {
      return true;
    }
    const userStore = useUserStore();
    const permissionStore = usePermissionStore();
    if (userStore.roles.length === 0) {
      isRelogin.show = true;
      try {
        await userStore.getInfo();
        await useNotificationStore().refreshUnread();
        useNotificationStore().connectSse();
        isRelogin.show = false;
        const accessRoutes = await permissionStore.generateRoutes(userStore.roles);
        registerDynamicRoutes(accessRoutes);
        return { ...to, replace: true };
      } catch (err) {
        await userStore.logOut();
        ElMessage.error(err);
        return { path: "/" };
      }
    }
    if (permissionStore.addRoutes.length === 0) {
      const accessRoutes = await permissionStore.generateRoutes(userStore.roles);
      registerDynamicRoutes(accessRoutes);
      return { ...to, replace: true };
    }
    if (!useNotificationStore().sseConnected) {
      useNotificationStore().connectSse();
    }
    return true;
  } else {
    // 没有token
    if (isWhiteList(to.path)) {
      // 在免登录白名单，直接进入
      return true;
    }
    NProgress.done();
    return `/login?redirect=${to.fullPath}`; // 否则全部重定向到登录页
  }
});

router.afterEach(() => {
  NProgress.done();
});
