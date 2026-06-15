package com.campus.system.service.impl;

import com.campus.system.common.security.SecurityUtils;
import com.campus.system.service.interfaces.SsePushService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// SSE 连接管理与轻量事件推送
@Service
public class SsePushServiceImpl implements SsePushService {

    private static final Logger log = LoggerFactory.getLogger(SsePushServiceImpl.class);

    private final Map<Long, List<SseEmitter>> connections = new ConcurrentHashMap<>();

    public SsePushServiceImpl() {
        ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public SseEmitter connect() {
        Long userId = SecurityUtils.current().userId();
        SseEmitter emitter = new SseEmitter(0L);
        connections.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(ex -> removeEmitter(userId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ex) {
            removeEmitter(userId, emitter);
        }
        return emitter;
    }

    @Override
    public void pushNotificationChanged(Long receiverId) {
        if (receiverId == null) {
            return;
        }
        List<SseEmitter> emitters = connections.get(receiverId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification-changed").data("refresh"));
            } catch (Exception ex) {
                removeEmitter(receiverId, emitter);
                log.warn("SSE 推送失败，receiverId={}", receiverId);
            }
        }
    }

    @Override
    public void disconnectUser(Long userId) {
        List<SseEmitter> emitters = connections.remove(userId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            emitter.complete();
        }
    }

    private void sendHeartbeat() {
        for (Map.Entry<Long, List<SseEmitter>> entry : connections.entrySet()) {
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                } catch (Exception ex) {
                    removeEmitter(entry.getKey(), emitter);
                }
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = connections.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            connections.remove(userId);
        }
    }
}
