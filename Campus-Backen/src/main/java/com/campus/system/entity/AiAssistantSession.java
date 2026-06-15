package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

// AI 助手会话实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("ai_assistant_session")
public class AiAssistantSession extends BaseEntity {

    // 会话主键
    @TableId
    private Long sessionId;

    // 所属用户主键
    private Long userId;

    // 会话场景类型
    private String sceneType;

    // 会话标题
    private String title;

    // 会话过期时间
    private LocalDateTime expireTime;
}
