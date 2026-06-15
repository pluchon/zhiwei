package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 系统操作审计日志实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("sys_operation_log")
public class SysOperationLog extends BaseEntity {

    // 操作日志主键
    @TableId
    private Long operationLogId;

    // 操作人用户主键
    private Long operatorId;

    // 操作类型
    private String operationType;

    // 操作目标类型
    private String targetType;

    // 操作目标主键
    private Long targetId;

    // 操作描述
    private String description;

    // 操作来源 IP
    private String operationIp;
}
