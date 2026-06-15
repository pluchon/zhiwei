package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// AI 场景级审计日志实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("ai_audit_log")
public class AiAuditLog extends BaseEntity {

    // 审计日志主键
    @TableId
    private Long auditId;

    // 操作人用户主键
    private Long operatorId;

    // AI 业务场景类型
    private String sceneType;

    // 目标对象类型
    private String targetType;

    // 目标对象主键
    private Long targetId;

    // 执行结果状态
    private String resultStatus;

    // 失败原因摘要
    private String failureReason;
}
