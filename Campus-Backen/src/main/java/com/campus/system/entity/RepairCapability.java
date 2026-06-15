package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 维修师傅能力实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("repairer_category_capability")
public class RepairCapability extends BaseEntity {

    // 维修能力主键
    @TableId
    private Long capabilityId;

    // 维修师傅用户主键
    private Long repairerId;

    // 可处理的故障类型主键
    private Long categoryId;
}
