package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 操作日志响应数据
@Data
public class SysOperationLogVO {

    // 操作日志主键
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

    // 创建时间
    private LocalDateTime createTime;

    // 最后更新时间
    private LocalDateTime updateTime;
}
