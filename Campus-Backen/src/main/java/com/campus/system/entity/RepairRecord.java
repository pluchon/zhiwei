package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 维修结果记录实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("repair_record")
public class RepairRecord extends BaseEntity {

    // 维修记录主键
    @TableId
    private Long recordId;

    // 工单主键
    private Long orderId;

    // 提交结果的维修师傅用户主键
    private Long repairerId;

    // 维修结果描述
    private String resultDescription;

    // 当前工单的维修尝试序号
    private Integer attemptNo;
}
