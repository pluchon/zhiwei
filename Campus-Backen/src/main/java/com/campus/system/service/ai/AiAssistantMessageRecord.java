package com.campus.system.service.ai;

import java.time.LocalDateTime;
import lombok.Data;

// AI 助手消息（Redis 存储）
@Data
public class AiAssistantMessageRecord {

    private Long messageId;

    private Long sessionId;

    private String role;

    private String contentSummary;

    private String extraJson;

    private LocalDateTime createTime;
}
