const props = defineProps({
  visible: { type: Boolean, default: false },
  row: { type: Object, default: () => ({}) },
});

const emit = defineEmits(["update:visible"]);

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit("update:visible", val),
});

const { sys_oper_type } = useDict("sys_oper_type");

const form = computed(() => props.row || {});
const typeLabel = computed(
  () => selectDictLabel(sys_oper_type.value, form.value.businessType) || "-",
);

function formatJson(str) {
  if (!str) return "（无数据）";
  try {
    return JSON.stringify(JSON.parse(str), null, 2);
  } catch {
    return str;
  }
}

function copyText(str) {
  const text = formatJson(str);
  if (navigator.clipboard) {
    navigator.clipboard
      .writeText(text)
      .then(() =>
        ElMessage({ message: "已复制", type: "success", duration: 1500 }),
      );
  } else {
    const ta = document.createElement("textarea");
    ta.value = text;
    document.body.appendChild(ta);
    ta.select();
    document.execCommand("copy");
    document.body.removeChild(ta);
    ElMessage({ message: "已复制", type: "success", duration: 1500 });
  }
}
