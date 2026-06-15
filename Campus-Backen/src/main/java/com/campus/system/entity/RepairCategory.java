package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 报修故障类型实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("repair_category")
public class RepairCategory extends BaseEntity {

    // 故障类型主键
    @TableId
    private Long categoryId;

    // 故障类型名称
    private String categoryName;

    // 故障类型说明
    private String description;

    // 故障类型状态（0启用 1停用）
    private Integer status;
}
