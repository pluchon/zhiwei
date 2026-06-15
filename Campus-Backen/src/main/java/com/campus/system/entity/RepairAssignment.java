package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 工单接单记录实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("repair_assignment")
public class RepairAssignment extends BaseEntity {

    // 接单记录主键
    @TableId
    private Long assignmentId;

    // 工单主键
    private Long orderId;

    // 接单维修师傅用户主键
    private Long repairerId;

    // 接单状态（0处理中 1已退回 2已完成）
    private Integer status;

    // 维修师傅退回原因
    private String returnReason;

    // 接单来源（0主动接单 1管理员手动派单）
    private Integer assignmentSource;

    // 执行手动派单的管理员主键
    private Long operatorId;

    // 派单说明
    private String dispatchNote;

    // 能力不匹配派单原因
    private String capabilityMismatchReason;
}
