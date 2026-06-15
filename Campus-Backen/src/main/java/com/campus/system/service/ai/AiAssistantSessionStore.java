package com.campus.system.service.ai;

import com.campus.system.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

// AI 助手会话与消息 Redis 存储（临时数据，7 天过期，逻辑删除）
@Component
public class AiAssistantSessionStore {

    private static final int SESSION_LIMIT = 50;
    private static final int SESSION_TTL_DAYS = 7;

    private static final String SESSION_ID_SEQ = "ai:assistant:session:id-seq";
    private static final String MESSAGE_ID_SEQ = "ai:assistant:message:id-seq";
    private static final String SESSION_KEY_PREFIX = "ai:assistant:session:";
    private static final String MESSAGE_KEY_PREFIX = "ai:assistant:session:messages:";
    private static final String USER_INDEX_PREFIX = "ai:assistant:user:sessions:";

    @Autowired
    private RedisTemplate<String, Object> redis;

    public Long nextSessionId() {
        Long id = redis.opsForValue().increment(SESSION_ID_SEQ);
        return id == null ? System.currentTimeMillis() : id;
    }

    public Long nextMessageId() {
        Long id = redis.opsForValue().increment(MESSAGE_ID_SEQ);
        return id == null ? System.currentTimeMillis() : id;
    }

    public AiAssistantSessionRecord createSession(Long userId, String title) {
        LocalDateTime now = LocalDateTime.now();
        AiAssistantSessionRecord session = new AiAssistantSessionRecord();
        session.setSessionId(nextSessionId());
        session.setUserId(userId);
        session.setSceneType("MIXED");
        session.setTitle(title);
        session.setCreateTime(now);
        session.setUpdateTime(now);
        session.setExpireTime(now.plusDays(SESSION_TTL_DAYS));
        session.setDeleted(false);
        saveSession(session);
        return session;
    }

    public void saveSession(AiAssistantSessionRecord session) {
        if (session == null || session.getSessionId() == null || session.getUserId() == null) {
            return;
        }
        refreshExpireTime(session);
        redis.opsForValue().set(sessionKey(session.getSessionId()), session, ttlSeconds(session), TimeUnit.SECONDS);
        if (!session.isDeleted()) {
            redis.opsForZSet().add(userIndexKey(session.getUserId()), String.valueOf(session.getSessionId()), score(session.getUpdateTime()));
            redis.expire(userIndexKey(session.getUserId()), ttlSeconds(session), TimeUnit.SECONDS);
        }
    }

    public List<AiAssistantSessionRecord> listActiveSessions(Long userId) {
        Set<Object> ids = redis.opsForZSet().reverseRange(userIndexKey(userId), 0, SESSION_LIMIT - 1L);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        List<AiAssistantSessionRecord> rows = new ArrayList<>();
        for (Object rawId : ids) {
            Long sessionId = parseLong(rawId);
            if (sessionId == null) {
                continue;
            }
            AiAssistantSessionRecord session = getSession(sessionId);
            if (session == null || session.isDeleted() || !Objects.equals(session.getUserId(), userId)) {
                redis.opsForZSet().remove(userIndexKey(userId), String.valueOf(sessionId));
                continue;
            }
            if (!session.getExpireTime().isAfter(now)) {
                logicalDelete(userId, sessionId);
                continue;
            }
            rows.add(session);
        }
        return rows;
    }

    public AiAssistantSessionRecord requireOwnedSession(Long userId, Long sessionId) {
        if (sessionId == null) {
            throw BusinessException.badRequest("sessionId 不能为空");
        }
        AiAssistantSessionRecord session = getSession(sessionId);
        if (session == null || session.isDeleted() || !Objects.equals(session.getUserId(), userId)) {
            throw BusinessException.notFound("会话不存在");
        }
        if (!session.getExpireTime().isAfter(LocalDateTime.now())) {
            throw BusinessException.conflict("会话已过期，请新建对话");
        }
        return session;
    }

    public void logicalDelete(Long userId, Long sessionId) {
        AiAssistantSessionRecord session = getSession(sessionId);
        if (session == null || !Objects.equals(session.getUserId(), userId) || session.isDeleted()) {
            throw BusinessException.notFound("会话不存在");
        }
        session.setDeleted(true);
        session.setUpdateTime(LocalDateTime.now());
        redis.opsForValue().set(sessionKey(sessionId), session, ttlSeconds(session), TimeUnit.SECONDS);
        redis.opsForZSet().remove(userIndexKey(userId), String.valueOf(sessionId));
        redis.delete(messageKey(sessionId));
    }

    public void appendMessage(AiAssistantMessageRecord message) {
        if (message == null || message.getSessionId() == null) {
            return;
        }
        if (message.getMessageId() == null) {
            message.setMessageId(nextMessageId());
        }
        if (message.getCreateTime() == null) {
            message.setCreateTime(LocalDateTime.now());
        }
        String key = messageKey(message.getSessionId());
        redis.opsForList().rightPush(key, message);
        AiAssistantSessionRecord session = getSession(message.getSessionId());
        if (session != null && !session.isDeleted()) {
            redis.expire(key, ttlSeconds(session), TimeUnit.SECONDS);
        }
    }

    public List<AiAssistantMessageRecord> listMessages(Long sessionId) {
        List<Object> rows = redis.opsForList().range(messageKey(sessionId), 0, -1);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<AiAssistantMessageRecord> result = new ArrayList<>();
        for (Object row : rows) {
            if (row instanceof AiAssistantMessageRecord message) {
                result.add(message);
            }
        }
        result.sort(Comparator.comparing(AiAssistantMessageRecord::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    public List<AiAssistantMessageRecord> listRecentMessages(Long sessionId, int limit) {
        List<AiAssistantMessageRecord> rows = listMessages(sessionId);
        if (rows.size() <= limit) {
            return rows;
        }
        return new ArrayList<>(rows.subList(rows.size() - limit, rows.size()));
    }

    public String latestPreview(Long sessionId) {
        List<AiAssistantMessageRecord> rows = listMessages(sessionId);
        if (rows.isEmpty()) {
            return "暂无消息";
        }
        AiAssistantMessageRecord latest = rows.get(rows.size() - 1);
        String text = latest.getContentSummary() == null ? "" : latest.getContentSummary();
        return text.length() > 40 ? text.substring(0, 40) : text;
    }

    private AiAssistantSessionRecord getSession(Long sessionId) {
        Object raw = redis.opsForValue().get(sessionKey(sessionId));
        if (raw instanceof AiAssistantSessionRecord session) {
            return session;
        }
        return null;
    }

    private void refreshExpireTime(AiAssistantSessionRecord session) {
        LocalDateTime now = LocalDateTime.now();
        session.setUpdateTime(now);
        session.setExpireTime(now.plusDays(SESSION_TTL_DAYS));
    }

    private long ttlSeconds(AiAssistantSessionRecord session) {
        if (session.getExpireTime() == null) {
            return TimeUnit.DAYS.toSeconds(SESSION_TTL_DAYS);
        }
        long seconds = session.getExpireTime().atZone(ZoneId.systemDefault()).toEpochSecond()
                - LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
        return Math.max(seconds, 60);
    }

    private double score(LocalDateTime time) {
        if (time == null) {
            return System.currentTimeMillis();
        }
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private Long parseLong(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String sessionKey(Long sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String messageKey(Long sessionId) {
        return MESSAGE_KEY_PREFIX + sessionId;
    }

    private String userIndexKey(Long userId) {
        return USER_INDEX_PREFIX + userId;
    }
}
