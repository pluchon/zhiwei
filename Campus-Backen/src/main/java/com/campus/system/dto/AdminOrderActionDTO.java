package com.campus.system.dto;

import lombok.Data;

// 管理员处理工单请求参数
@Data
public class AdminOrderActionDTO {

    // 前端提交的乐观锁版本号
    private Integer version;

    // 驳回、关闭等管理操作原因
    private String reason;
}
