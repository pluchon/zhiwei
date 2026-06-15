import usePermissionStore from "@/store/modules/permission";

const route = useRoute();
const router = useRouter();
const permissionStore = usePermissionStore();
const levelList = ref([]);

function getBreadcrumb() {
  // only show routes with meta.title
  let matched = [];
  const pathNum = findPathNum(route.path);
  // multi-level menu
  if (pathNum > 2) {
    const reg = /\/\w+/gi;
    const pathList = route.path.match(reg).map((item, index) => {
      if (index !== 0) item = item.slice(1);
      return item;
    });
    getMatched(pathList, permissionStore.defaultRoutes, matched);
  } else {
    matched = route.matched.filter((item) => item.meta && item.meta.title);
  }
  // 判断是否为首页
  if (!isDashboard(matched[0])) {
    matched = [{ path: "/index", meta: { title: "首页" } }].concat(matched);
  }
  levelList.value = matched.filter(
    (item) => item.meta && item.meta.title && item.meta.breadcrumb !== false,
  );
}

function findPathNum(str, char = "/") {
  let index = str.indexOf(char);
  let num = 0;
  while (index !== -1) {
    num++;
    index = str.indexOf(char, index + 1);
  }
  return num;
}

function getMatched(pathList, routeList, matched) {
  if (!Array.isArray(routeList)) {
    return;
  }

  let data = routeList.find(
    (item) =>
      item.path === pathList[0] ||
      (item.name += "").toLowerCase() === pathList[0],
  );
  if (data) {
    matched.push(data);
    if (data.children && pathList.length) {
      pathList.shift();
      getMatched(pathList, data.children, matched);
    }
  }
}

function isDashboard(route) {
  const name = route && route.name;
  if (!name) {
    return false;
  }
  return name.trim() === "Index";
}

function handleLink(item) {
  const { redirect, path } = item;
  if (redirect) {
    router.push(redirect);
    return;
  }
  router.push(path);
}

watchEffect(() => {
  // if you go to the redirect page, do not update the breadcrumbs
  if (route.path.startsWith("/redirect/")) {
    return;
  }
  getBreadcrumb();
});
getBreadcrumb();
