import { disposeCampusChart, initCampusChart } from "@/utils/campusChart";

const props = defineProps({
  option: {
    type: Object,
    default: null,
  },
  height: {
    type: String,
    default: "320px",
  },
});

const rootRef = ref(null);
let chartInstance = null;
let resizeObserver = null;

function resizeChart() {
  chartInstance?.resize();
}

function renderOption(option) {
  if (!chartInstance || !option) {
    return;
  }
  chartInstance.setOption(option, true);
  nextTick(() => {
    resizeChart();
  });
}

onMounted(() => {
  chartInstance = initCampusChart(rootRef.value);
  renderOption(props.option);
  if (rootRef.value && typeof ResizeObserver !== "undefined") {
    resizeObserver = new ResizeObserver(() => {
      resizeChart();
    });
    resizeObserver.observe(rootRef.value);
  }
  window.addEventListener("resize", resizeChart);
});

watch(
  () => props.option,
  (option) => {
    renderOption(option);
  },
  { deep: true },
);

watch(
  () => props.height,
  () => {
    nextTick(() => {
      resizeChart();
    });
  },
);

onUnmounted(() => {
  resizeObserver?.disconnect();
  resizeObserver = null;
  window.removeEventListener("resize", resizeChart);
  disposeCampusChart(chartInstance);
  chartInstance = null;
});
