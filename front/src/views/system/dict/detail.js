import { listData } from "@/api/system/dict/data";

const props = defineProps({
  visible: { type: Boolean, default: false },
  row: { type: Object, default: () => ({}) },
});

const emit = defineEmits(["update:visible"]);

const loading = ref(false);
const dataList = ref([]);

const normalCount = computed(
  () => dataList.value.filter((r) => r.status === "0").length,
);
const disabledCount = computed(
  () => dataList.value.filter((r) => r.status !== "0").length,
);

watch(
  () => props.visible,
  (val) => {
    if (val) {
      loadData();
    } else {
      dataList.value = [];
    }
  },
);

function loadData() {
  if (!props.row?.dictType) return;
  loading.value = true;
  dataList.value = [];
  listData({ dictType: props.row.dictType, pageSize: 100, pageNum: 1 })
    .then((response) => {
      dataList.value = response.rows || [];
    })
    .catch(() => {})
    .finally(() => {
      loading.value = false;
    });
}
