package com.campus.system.dto;

import lombok.Data;

// AI 助手对话请求
@Data
public class AiAssistantChatDTO {

    // 会话主键，首次为空
    private Long sessionId;

    // 场景类型（STATISTICS / EXPORT）
    private String sceneType;

    // 用户输入
    private String message;
}
