import AdminSuggestionPanel from "./index.vue";

const panelRef = ref(null);

onMounted(() => {
  panelRef.value?.load?.();
});
