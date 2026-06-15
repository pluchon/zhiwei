package com.campus.system.dto;

import lombok.Data;

// 维修能力配置请求参数
@Data
public class RepairCapabilityDTO {

    // 维修师傅用户主键
    private Long repairerId;

    // 可处理的故障类型主键
    private Long categoryId;
}
