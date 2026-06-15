import { getUnreadState } from "@/api/notification";
import {
  connectNotificationSse,
  disconnectNotificationSse,
} from "@/api/notification/sse";
import useUserStore from "@/store/modules/user";

// 定义 Pinia 通知状态存储仓储
const useNotificationStore = defineStore("notification", {
  state: () => ({
    hasUnread: false,
    sseConnected: false,
    sseRefreshHandler: null,
  }),
  actions: {
    // 异步拉取并刷新是否有未读通知的状态
    async refreshUnread() {
      const result = await getUnreadState();
      this.hasUnread = !!result?.hasUnread;
    },
    // 注册 SSE 推送后的页面刷新回调
    setSseRefreshHandler(handler) {
      this.sseRefreshHandler = handler;
    },
    // 建立 SSE 实时通知连接
    connectSse() {
      if (this.sseConnected) {
        return;
      }
      this.sseConnected = true;
      connectNotificationSse({
        onNotificationChanged: async () => {
          await this.refreshUnread();
          this.sseRefreshHandler?.();
        },
        onSessionInvalid: () => {
          this.disconnectSse();
          useUserStore().resetState();
          const redirect = encodeURIComponent(location.pathname + location.search);
          location.href = `/login?redirect=${redirect}`;
        },
      });
    },
    // 断开 SSE 连接
    disconnectSse() {
      disconnectNotificationSse();
      this.sseConnected = false;
    },
    // 重置未读标记与 SSE 状态
    reset() {
      this.disconnectSse();
      this.hasUnread = false;
      this.sseRefreshHandler = null;
    },
  },
});

export default useNotificationStore;
