import useAppStore from "@/store/modules/app";

const appStore = useAppStore();
const size = computed(() => appStore.size);
const { proxy } = getCurrentInstance();
const sizeOptions = ref([
  { label: "较大", value: "large" },
  { label: "默认", value: "default" },
  { label: "稍小", value: "small" },
]);

function handleSetSize(size) {
  proxy.$modal.loading("正在设置布局大小，请稍候...");
  appStore.setSize(size);
  setTimeout("window.location.reload()", 1000);
}
