import { ElMessage } from "element-plus";
import { listRepairerAvailability } from "@/api/repairer/index";
import {
  acceptingStateOptions,
  acceptingStateText,
} from "@/utils/repair";

const loading = ref(false);
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  acceptingState: null,
});
const data = reactive({ records: [], total: 0 });

async function load() {
  loading.value = true;
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    };
    if (query.acceptingState) {
      params.acceptingState = query.acceptingState;
    }
    Object.assign(data, await listRepairerAvailability(params));
  } catch (error) {
    ElMessage.error(error?.message || "加载失败");
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  load();
}

function resetFilters() {
  query.pageNum = 1;
  query.acceptingState = null;
  load();
}

onMounted(load);
