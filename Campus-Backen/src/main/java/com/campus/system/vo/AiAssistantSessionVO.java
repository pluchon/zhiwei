package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// AI 助手会话列表项
@Data
public class AiAssistantSessionVO {

    private Long sessionId;

    private String title;

    private String preview;

    private LocalDateTime updateTime;
}
