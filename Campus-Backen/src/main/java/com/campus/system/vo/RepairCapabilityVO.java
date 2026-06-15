package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 维修能力响应数据
@Data
public class RepairCapabilityVO {

    // 维修能力主键
    private Long capabilityId;

    // 维修师傅用户主键
    private Long repairerId;

    // 可处理的故障类型主键
    private Long categoryId;

    // 创建时间
    private LocalDateTime createTime;

    // 最后更新时间
    private LocalDateTime updateTime;
}
