import ScrollPane from "./ScrollPane";
import { getNormalPath } from "@/utils/ruoyi";
import useTagsViewStore from "@/store/modules/tagsView";
import useSettingsStore from "@/store/modules/settings";
import usePermissionStore from "@/store/modules/permission";

const visible = ref(false);
const top = ref(0);
const left = ref(0);
const selectedTag = ref({});
const affixTags = ref([]);
const scrollPaneRef = ref(null);
const canScrollLeft = ref(false);
const canScrollRight = ref(false);
const isFullscreen = ref(false);
const hiddenElements = ref([]);

const { proxy } = getCurrentInstance();
const route = useRoute();
const router = useRouter();
const settingsStore = useSettingsStore();

const visitedViews = computed(() => useTagsViewStore().visitedViews);
const routes = computed(() => usePermissionStore().routes);
const theme = computed(() => useSettingsStore().theme);
const tagsIcon = computed(() => useSettingsStore().tagsIcon);
const tagsViewPersist = computed(() => useSettingsStore().tagsViewPersist);
const tagsViewStyle = computed(() => useSettingsStore().tagsViewStyle);

// 下拉菜单针对当前激活的 tag
const selectedDropdownTag = computed(
  () => visitedViews.value.find((v) => isActive(v)) || {},
);

watch(route, () => {
  addTags();
  moveToCurrentTag();
});

watch(visible, (value) => {
  if (value) {
    document.body.addEventListener("click", closeMenu);
  } else {
    document.body.removeEventListener("click", closeMenu);
  }
});

watch(visitedViews, () => {
  nextTick(() => updateArrowState());
});

onMounted(() => {
  initTags();
  addTags();
  window.addEventListener("resize", updateArrowState);
  window.addEventListener("keydown", handleKeyDown);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", updateArrowState);
  window.removeEventListener("keydown", handleKeyDown);
});

function handleKeyDown(event) {
  // 当按下Esc键且处于全屏状态时，退出全屏
  if (event.key === "Escape" && isFullscreen.value) {
    toggleFullscreen();
  }
}

function isActive(r) {
  return r.path === route.path;
}

function tagActiveStyle(tag) {
  if (!isActive(tag) || tagsViewStyle.value !== "card") return {};
  return {
    "background-color": theme.value,
    "border-color": theme.value,
  };
}

function isAffix(tag) {
  return tag && tag.meta && tag.meta.affix;
}

function isFirstView() {
  try {
    const tag =
      selectedTag.value && selectedTag.value.fullPath
        ? selectedTag.value
        : selectedDropdownTag.value;
    return (
      tag.fullPath === "/index" ||
      tag.fullPath === visitedViews.value[1].fullPath
    );
  } catch (err) {
    return false;
  }
}

function isLastView() {
  try {
    const tag =
      selectedTag.value && selectedTag.value.fullPath
        ? selectedTag.value
        : selectedDropdownTag.value;
    return (
      tag.fullPath ===
      visitedViews.value[visitedViews.value.length - 1].fullPath
    );
  } catch (err) {
    return false;
  }
}

function filterAffixTags(routes, basePath = "") {
  let tags = [];
  routes.forEach((route) => {
    if (route.meta && route.meta.affix) {
      const tagPath = getNormalPath(basePath + "/" + route.path);
      tags.push({
        fullPath: tagPath,
        path: tagPath,
        name: route.name,
        meta: { ...route.meta },
      });
    }
    if (route.children) {
      const tempTags = filterAffixTags(route.children, route.path);
      if (tempTags.length >= 1) {
        tags = [...tags, ...tempTags];
      }
    }
  });
  return tags;
}

function initTags() {
  if (tagsViewPersist.value) {
    useTagsViewStore().loadPersistedViews();
  }
  const res = filterAffixTags(routes.value);
  affixTags.value = res;
  for (const tag of res) {
    if (tag.name) {
      useTagsViewStore().addAffixView(tag);
    }
  }
}

function addTags() {
  const { name } = route;
  if (name) {
    useTagsViewStore().addView(route);
  }
}

function moveToCurrentTag() {
  nextTick(() => {
    for (const r of visitedViews.value) {
      if (r.path === route.path) {
        scrollPaneRef.value.moveToTarget(r);
        if (r.fullPath !== route.fullPath) {
          useTagsViewStore().updateVisitedView(route);
        }
      }
    }
  });
}

function scrollLeft() {
  if (!canScrollLeft.value) return;
  scrollPaneRef.value.scrollToStart();
}

function scrollRight() {
  if (!canScrollRight.value) return;
  scrollPaneRef.value.scrollToEnd();
}

function updateArrowState() {
  nextTick(() => {
    if (scrollPaneRef.value) {
      const state = scrollPaneRef.value.getScrollState();
      canScrollLeft.value = state.canLeft;
      canScrollRight.value = state.canRight;
    }
  });
}

function toggleFullscreen() {
  const mainContainer = document.querySelector(".main-container");
  const navbar = document.querySelector(".navbar");
  const sidebar = document.querySelector(".sidebar-container");
  if (!mainContainer) return;

  if (!isFullscreen.value) {
    mainContainer.classList.add("fullscreen-mode");
    document.body.style.overflow = "hidden";
    const elementsToHide = [
      { el: navbar, originalDisplay: navbar?.style.display || "" },
      { el: sidebar, originalDisplay: sidebar?.style.display || "" },
    ];
    elementsToHide.forEach((item) => {
      if (item.el && item.el.style.display !== "none") {
        item.originalDisplay = item.el.style.display;
        item.el.style.display = "none";
        hiddenElements.value.push(item);
      }
    });
    isFullscreen.value = true;
  } else {
    mainContainer.classList.remove("fullscreen-mode");
    document.body.style.overflow = "";
    hiddenElements.value.forEach((item) => {
      if (item.el) {
        item.el.style.display = item.originalDisplay;
      }
    });
    hiddenElements.value = [];
    document.querySelector(".tags-action-btn").blur();
    isFullscreen.value = false;
  }
}

function handleDropdownCommand(command) {
  const tag = selectedDropdownTag.value;
  selectedTag.value = tag;
  switch (command) {
    case "refresh":
      refreshSelectedTag(tag);
      break;
    case "fullscreen":
      toggleFullscreen();
      break;
    case "close":
      closeSelectedTag(tag);
      break;
    case "closeOthers":
      closeOthersTags();
      break;
    case "closeLeft":
      closeLeftTags();
      break;
    case "closeRight":
      closeRightTags();
      break;
    case "closeAll":
      closeAllTags(tag);
      break;
  }
}

function refreshSelectedTag(view) {
  proxy.$tab.refreshPage(view);
  if (route.meta.link) {
    useTagsViewStore().delIframeView(route);
  }
}

function closeSelectedTag(view) {
  proxy.$tab.closePage(view).then(({ visitedViews }) => {
    if (isActive(view)) {
      toLastView(visitedViews, view);
    }
  });
}

function closeRightTags() {
  proxy.$tab.closeRightPage(selectedTag.value).then((visitedViews) => {
    if (!visitedViews.find((i) => i.fullPath === route.fullPath)) {
      toLastView(visitedViews);
    }
  });
}

function closeLeftTags() {
  proxy.$tab.closeLeftPage(selectedTag.value).then((visitedViews) => {
    if (!visitedViews.find((i) => i.fullPath === route.fullPath)) {
      toLastView(visitedViews);
    }
  });
}

function closeOthersTags() {
  router.push(selectedTag.value).catch(() => {});
  proxy.$tab.closeOtherPage(selectedTag.value).then(() => {
    moveToCurrentTag();
  });
}

function closeAllTags(view) {
  proxy.$tab.closeAllPage().then(({ visitedViews }) => {
    if (affixTags.value.some((tag) => tag.path === route.path)) {
      return;
    }
    toLastView(visitedViews, view);
  });
}

function toLastView(visitedViews, view) {
  const latestView = visitedViews.slice(-1)[0];
  if (latestView) {
    router.push(latestView.fullPath);
  } else {
    if (view && view.name === "Dashboard") {
      router.replace({ path: "/redirect" + view.fullPath });
    } else {
      router.push("/");
    }
  }
}

function openMenu(tag, e) {
  left.value = e.clientX;
  top.value = e.clientY;
  visible.value = true;
  selectedTag.value = tag;
}

function closeMenu() {
  visible.value = false;
}

function handleScroll() {
  closeMenu();
  updateArrowState();
}
