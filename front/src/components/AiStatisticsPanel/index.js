import CampusChart from "@/components/CampusChart/index.vue";
import {
  buildAiStatisticsDetailCharts,
  buildAiStatisticsPreview,
} from "@/utils/chartBuilders";

const props = defineProps({
  result: {
    type: Object,
    default: null,
  },
});

const detailOpen = ref(false);

const summary = computed(() => props.result?.summary || "");
const preview = computed(() => buildAiStatisticsPreview(props.result));
const detailCharts = computed(() => buildAiStatisticsDetailCharts(props.result));
