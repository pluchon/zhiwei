import { ElMessage } from "element-plus";
import { getRepairerStatistics } from "@/api/repairer/index";
import CampusChart from "@/components/CampusChart/index.vue";
import {
  buildCompletionGaugeOption,
  buildPersonalRadarOption,
} from "@/utils/chartBuilders";
import {
  statisticsRangeOptions,
  statisticsRangeText,
  acceptingStateText,
} from "@/utils/repair";

const loading = ref(false);
const rangeType = ref("LAST_30_DAYS");
const stats = ref(null);

const radarChartOption = computed(() => buildPersonalRadarOption(stats.value || {}));
const gaugeChartOption = computed(() => buildCompletionGaugeOption(stats.value || {}));

function formatMinutes(value) {
  if (value == null || Number.isNaN(value)) {
    return "-";
  }
  return `${Number(value).toFixed(1)} 分钟`;
}

async function load() {
  loading.value = true;
  try {
    stats.value = await getRepairerStatistics({ rangeType: rangeType.value });
  } catch (error) {
    ElMessage.error(error?.message || "统计加载失败");
  } finally {
    loading.value = false;
  }
}

function onRangeChange() {
  load();
}

onMounted(load);
