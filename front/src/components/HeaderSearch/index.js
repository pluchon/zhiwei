import Fuse from "fuse.js";
import { getNormalPath } from "@/utils/ruoyi";
import { isHttp } from "@/utils/validate";
import useSettingsStore from "@/store/modules/settings";
import usePermissionStore from "@/store/modules/permission";

const search = ref("");
const options = ref([]);
const searchPool = ref([]);
const activeIndex = ref(-1);
const show = ref(false);
const fuse = ref(undefined);
const headerSearchSelectRef = ref(null);
const router = useRouter();
const theme = computed(() => useSettingsStore().theme);
const routes = computed(() => usePermissionStore().defaultRoutes);

function click() {
  show.value = !show.value;
  if (show.value) {
    options.value = searchPool.value;
  }
}

function onDialogOpened() {
  nextTick(() => {
    if (headerSearchSelectRef.value) {
      headerSearchSelectRef.value.focus();
    }
  });
}

function close() {
  if (headerSearchSelectRef.value) {
    headerSearchSelectRef.value.blur();
  }

  search.value = "";
  options.value = searchPool.value;
  show.value = false;
  activeIndex.value = -1;
}

function change(value) {
  const path = value.path;
  const query = value.query;

  if (isHttp(path)) {
    // HTTP(S) 地址使用新窗口打开，避免影响当前管理页面状态。
    const httpIndex = path.indexOf("http");
    window.open(path.substr(httpIndex, path.length), "_blank");
  } else if (query) {
    router.push({
      path,
      query: JSON.parse(query),
    });
  } else {
    router.push(path);
  }

  search.value = "";
  options.value = searchPool.value;
  nextTick(() => {
    show.value = false;
  });
}

function initFuse(list) {
  fuse.value = new Fuse(list, {
    shouldSort: true,
    threshold: 0.2,
    distance: 100,
    minMatchCharLength: 1,
    keys: [
      {
        name: "title",
        weight: 0.7,
      },
      {
        name: "path",
        weight: 0.3,
      },
    ],
  });
}

function generateRoutes(routeList, basePath = "", prefixTitle = []) {
  let result = [];

  if (!Array.isArray(routeList)) {
    return result;
  }

  for (const route of routeList) {
    if (route.hidden) {
      continue;
    }

    const routePath =
      route.path.length > 0 && route.path[0] === "/"
        ? route.path
        : `/${route.path}`;
    const data = {
      path: !isHttp(route.path)
        ? getNormalPath(basePath + routePath)
        : route.path,
      title: [...prefixTitle],
      icon: "",
    };

    if (route.meta && route.meta.title) {
      data.title = [...data.title, route.meta.title];
      data.icon = route.meta.icon;
      if (route.redirect !== "noRedirect") {
        result.push(data);
      }
    }

    if (route.query) {
      data.query = route.query;
    }

    if (route.children) {
      const childRoutes = generateRoutes(route.children, data.path, data.title);
      if (childRoutes.length > 0) {
        result = [...result, ...childRoutes];
      }
    }
  }

  return result;
}

function querySearch(query) {
  activeIndex.value = -1;
  if (!query) {
    options.value = searchPool.value;
    return;
  }

  const normalizedQuery = query.toLowerCase();
  const pathMatches = searchPool.value.filter((item) =>
    item.path.toLowerCase().includes(normalizedQuery),
  );
  const fuseMatches = fuse.value.search(query).map((item) => item.item);
  const merged = [...pathMatches];

  fuseMatches.forEach((item) => {
    if (!merged.find((candidate) => candidate.path === item.path)) {
      merged.push(item);
    }
  });
  options.value = merged;
}

function activeStyle(index) {
  if (index !== activeIndex.value) {
    return {};
  }

  return {
    "background-color": theme.value,
    color: "#fff",
  };
}

function navigateResult(direction) {
  if (direction === "up") {
    activeIndex.value =
      activeIndex.value <= 0 ? options.value.length - 1 : activeIndex.value - 1;
  } else if (direction === "down") {
    activeIndex.value =
      activeIndex.value >= options.value.length - 1 ? 0 : activeIndex.value + 1;
  }
}

function selectActiveResult() {
  if (options.value.length > 0 && activeIndex.value >= 0) {
    change(options.value[activeIndex.value]);
  }
}

function highlightText(text) {
  if (!text) {
    return "";
  }
  if (!search.value) {
    return text;
  }

  const keyword = escapeRegExp(search.value);
  const expression = new RegExp(`(${keyword})`, "gi");

  // 拆分结束标签，避免外置脚本注入 SFC 时被 Vue 模板解析器提前识别。
  return text.replace(expression, '<span class="highlight">$1<' + "/span>");
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

onMounted(() => {
  searchPool.value = generateRoutes(routes.value || []);
});

watch(searchPool, (list) => {
  initFuse(list);
});
