import { constantRoutes, asyncRoutes } from "@/router";

function hasRouteRole(route, roles) {
  if (!route.roles || route.roles.length === 0) {
    return true;
  }
  return route.roles.some((role) => roles.includes(role));
}

function filterRoutes(routes, roles) {
  return routes.reduce((result, route) => {
    if (!hasRouteRole(route, roles)) {
      return result;
    }

    const nextRoute = { ...route };
    if (nextRoute.children) {
      nextRoute.children = filterRoutes(nextRoute.children, roles);
    }
    result.push(nextRoute);
    return result;
  }, []);
}

const usePermissionStore = defineStore("permission", {
  state: () => ({
    routes: [],
    addRoutes: [],
    defaultRoutes: [],
    topbarRouters: [],
    sidebarRouters: [],
  }),
  actions: {
    generateRoutes(roles) {
      const accessRoutes = filterRoutes(asyncRoutes, roles);
      this.addRoutes = accessRoutes;
      this.routes = constantRoutes.concat(accessRoutes);
      this.defaultRoutes = constantRoutes.concat(accessRoutes);
      this.topbarRouters = constantRoutes.concat(accessRoutes);
      this.sidebarRouters = constantRoutes.concat(accessRoutes);
      return Promise.resolve(accessRoutes);
    },
    setSidebarRouters(routes) {
      // RuoYi 布局组件会按导航模式动态切换侧边栏路由，这里统一收口，避免组件直接改状态。
      this.sidebarRouters = Array.isArray(routes) ? routes : [];
    },
    resetRoutes() {
      this.routes = constantRoutes;
      this.addRoutes = [];
      this.defaultRoutes = constantRoutes;
      this.topbarRouters = constantRoutes;
      this.sidebarRouters = constantRoutes;
    },
  },
});

export default usePermissionStore;
