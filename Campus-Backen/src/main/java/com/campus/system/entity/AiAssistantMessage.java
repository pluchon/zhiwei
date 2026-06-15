package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// AI 助手会话消息实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("ai_assistant_message")
public class AiAssistantMessage extends BaseEntity {

    // 消息主键
    @TableId
    private Long messageId;

    // 所属会话主键
    private Long sessionId;

    // 消息角色
    private String role;

    // 消息内容摘要
    private String contentSummary;

    // 结构化附加数据（统计/导出预览等）
    private String extraJson;
}
