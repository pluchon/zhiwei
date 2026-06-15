import { getToken } from "@/utils/auth";

// SSE 连接实例与重连定时器
let abortController = null;
let reconnectTimer = null;
let stopped = true;
let callbacks = {};

// 解析 SSE 文本块并分发事件
function dispatchSseEvents(chunk, onEvent) {
  const lines = chunk.split("\n");
  let eventName = "message";
  let data = "";
  for (const line of lines) {
    if (line.startsWith("event:")) {
      eventName = line.slice(6).trim();
    } else if (line.startsWith("data:")) {
      data += line.slice(5).trim();
    }
  }
  if (data) {
    onEvent(eventName, data);
  }
}

// 使用 fetch 建立带 Authorization 头的 SSE 连接
async function openStream() {
  const token = getToken();
  if (stopped || !token) {
    return;
  }
  abortController = new AbortController();
  const baseURL = import.meta.env.VITE_APP_BASE_API;
  try {
    const response = await fetch(`${baseURL}/notifications/stream`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
      },
      signal: abortController.signal,
    });
    if (response.status === 401 || response.status === 403) {
      callbacks.onSessionInvalid?.();
      return;
    }
    if (!response.ok || !response.body) {
      scheduleReconnect();
      return;
    }
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    while (!stopped) {
      const { done, value } = await reader.read();
      if (done) {
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      const parts = buffer.split("\n\n");
      buffer = parts.pop() || "";
      for (const part of parts) {
        dispatchSseEvents(part, (eventName) => {
          if (eventName === "notification-changed") {
            callbacks.onNotificationChanged?.();
          }
        });
      }
    }
    if (!stopped) {
      scheduleReconnect();
    }
  } catch (error) {
    if (!stopped && error?.name !== "AbortError") {
      scheduleReconnect();
    }
  }
}

// 断线后延迟重连
function scheduleReconnect() {
  if (stopped || reconnectTimer) {
    return;
  }
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = null;
    openStream();
  }, 3000);
}

// 建立 SSE 连接
export function connectNotificationSse(handlers = {}) {
  callbacks = handlers;
  stopped = false;
  disconnectNotificationSse(false);
  stopped = false;
  openStream();
}

// 关闭 SSE 连接
export function disconnectNotificationSse(resetStopped = true) {
  if (resetStopped) {
    stopped = true;
  }
  if (abortController) {
    abortController.abort();
    abortController = null;
  }
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
}
