package com.campus.system.dto;

import lombok.Data;

// 维修师傅退回工单请求参数
@Data
public class RepairReturnDTO {

    // 前端提交的乐观锁版本号
    private Integer version;

    // 退回原因
    private String reason;
}
