package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 维修周期实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("repair_work_cycle")
public class RepairWorkCycle extends BaseEntity {

    // 维修周期主键
    @TableId
    private Long workCycleId;

    // 工单主键
    private Long orderId;

    // 当前维修师傅用户主键
    private Long repairerId;

    // 周期开始时间
    private LocalDateTime startTime;

    // 周期结束时间
    private LocalDateTime endTime;

    // 活动周期标记（1活动中 结束后置空）
    private Integer activeFlag;

    // 三天提醒是否已发送
    private Integer threeDayReminded;

    // 七天提醒是否已发送
    private Integer sevenDayReminded;
}
