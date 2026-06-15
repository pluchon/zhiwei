import useSettingsStore from "@/store/modules/settings";

const settingsStore = useSettingsStore();

const visible = computed(() => settingsStore.footerVisible);
const content = computed(() => settingsStore.footerContent);
