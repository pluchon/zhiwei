package com.campus.system.service.ai;

import java.time.LocalDateTime;
import lombok.Data;

// AI 助手会话
@Data
public class AiAssistantSessionRecord {

    private Long sessionId;

    private Long userId;

    private String sceneType;

    private String title;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private boolean deleted;
}
