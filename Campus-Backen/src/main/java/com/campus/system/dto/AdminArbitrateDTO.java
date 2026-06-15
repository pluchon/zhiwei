package com.campus.system.dto;

import lombok.Data;

// 管理员仲裁工单请求参数
@Data
public class AdminArbitrateDTO {

    // 前端提交的乐观锁版本号
    private Integer version;

    // 仲裁后的目标工单状态
    private Integer targetStatus;

    // 仲裁原因，写入系统评论与工单日志
    private String reason;
}
