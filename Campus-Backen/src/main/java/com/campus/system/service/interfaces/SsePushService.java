package com.campus.system.service.interfaces;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// SSE 推送业务接口
public interface SsePushService {

    SseEmitter connect();

    void pushNotificationChanged(Long receiverId);

    void disconnectUser(Long userId);
}
