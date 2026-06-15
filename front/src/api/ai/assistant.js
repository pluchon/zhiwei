import request from "@/utils/request";

export const listAiAssistantSessions = () =>
  request({
    url: "/ai/assistant/sessions",
    method: "get",
  });

export const createAiAssistantSession = () =>
  request({
    url: "/ai/assistant/sessions",
    method: "post",
  });

export const renameAiAssistantSession = (sessionId, data) =>
  request({
    url: `/ai/assistant/sessions/${sessionId}/rename`,
    method: "post",
    data,
    headers: { repeatSubmit: false },
  });

export const deleteAiAssistantSession = (sessionId) =>
  request({
    url: `/ai/assistant/sessions/${sessionId}/delete`,
    method: "post",
    headers: { repeatSubmit: false },
  });

export const listAiAssistantMessages = (sessionId) =>
  request({
    url: `/ai/assistant/sessions/${sessionId}/messages`,
    method: "get",
  });

export const aiAssistantChat = (data) =>
  request({
    url: "/ai/assistant/chat",
    method: "post",
    data,
  });

export const confirmAiExport = (data) =>
  request({
    url: "/ai/assistant/export/confirm",
    method: "post",
    data,
    responseType: "blob",
  });
