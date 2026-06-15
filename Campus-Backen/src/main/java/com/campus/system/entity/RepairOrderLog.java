package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 工单流程日志实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("repair_order_log")
public class RepairOrderLog extends BaseEntity {

    // 工单日志主键
    @TableId
    private Long logId;

    // 所属工单主键
    private Long orderId;

    // 操作人用户主键
    private Long operatorId;

    // 流程动作编码
    private Integer action;

    // 变更前工单状态
    private Integer fromStatus;

    // 变更后工单状态
    private Integer toStatus;

    // 流程备注
    private String remark;
}
