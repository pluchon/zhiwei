package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 待确认周期实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("repair_confirmation_cycle")
public class RepairConfirmationCycle extends BaseEntity {

    // 待确认周期主键
    @TableId
    private Long confirmationCycleId;

    // 工单主键
    private Long orderId;

    // 报修人用户主键
    private Long reporterId;

    // 本次进入待确认的时间
    private LocalDateTime startTime;

    // 本周期结束时间
    private LocalDateTime endTime;

    // 活动周期标记（1活动中 结束后置空）
    private Integer activeFlag;

    // 三天提醒是否已发送
    private Integer threeDayReminded;

    // 七天提醒是否已发送
    private Integer sevenDayReminded;

    // 二十七天提醒是否已发送
    private Integer twentySevenDayReminded;

    // 本周期是否已自动完成
    private Integer autoCompleted;
}
