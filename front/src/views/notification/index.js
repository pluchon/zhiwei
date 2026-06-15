import { ElMessage } from "element-plus";
import {
  listNotifications,
  readNotification,
  markNotificationsRead,
  markAllNotificationsRead,
} from "@/api/notification";
import useNotificationStore from "@/store/modules/notification";
import { notificationTypeText, notificationTypes } from "@/utils/repair";

const router = useRouter();
const notificationStore = useNotificationStore();

const loading = ref(false);
const submitting = ref(false);
const selectedIds = ref([]);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  isRead: null,
  notificationType: null,
});
const data = reactive({ records: [], total: 0 });

const readFilterOptions = [
  { label: "全部", value: null },
  { label: "未读", value: 0 },
  { label: "已读", value: 1 },
];

async function refreshUnread() {
  await notificationStore.refreshUnread();
}

async function load() {
  loading.value = true;
  try {
    const params = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    };
    if (query.isRead !== null && query.isRead !== "") {
      params.isRead = query.isRead;
    }
    if (query.notificationType !== null && query.notificationType !== "") {
      params.notificationType = query.notificationType;
    }
    Object.assign(data, await listNotifications(params));
    selectedIds.value = [];
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  load();
}

function onCheckChange(id, checked) {
  if (checked) {
    if (!selectedIds.value.includes(id)) {
      selectedIds.value.push(id);
    }
    return;
  }
  selectedIds.value = selectedIds.value.filter((item) => item !== id);
}

async function readOne(notification) {
  if (!notification.isRead) {
    await readNotification(notification.notificationId);
    await refreshUnread();
    await load();
  }
  if (notification.orderId) {
    router.push(`/repair/detail/${notification.orderId}`);
  }
}

async function batchRead() {
  if (!selectedIds.value.length) {
    ElMessage.warning("请先勾选通知");
    return;
  }
  submitting.value = true;
  try {
    await markNotificationsRead(selectedIds.value);
    ElMessage.success("已标记为已读");
    await refreshUnread();
    await load();
  } finally {
    submitting.value = false;
  }
}

async function readAll() {
  submitting.value = true;
  try {
    await markAllNotificationsRead();
    ElMessage.success("全部通知已标记为已读");
    await refreshUnread();
    await load();
  } finally {
    submitting.value = false;
  }
}

function handleSseRefresh() {
  refreshUnread();
  load();
}

onMounted(async () => {
  notificationStore.setSseRefreshHandler(handleSseRefresh);
  await refreshUnread();
  await load();
});

onBeforeUnmount(() => {
  notificationStore.setSseRefreshHandler(null);
  refreshUnread();
});
