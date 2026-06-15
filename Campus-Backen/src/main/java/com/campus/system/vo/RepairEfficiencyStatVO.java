package com.campus.system.vo;

import lombok.Data;

// 维修效率统计响应数据
@Data
public class RepairEfficiencyStatVO {

    // 已完成工单数量
    private Long completedCount;

    // 平均首次接单时长（分钟）
    private Double avgFirstAcceptMinutes;

    // 平均维修处理时长（分钟）
    private Double avgProcessMinutes;

    // 平均完成时长（分钟）
    private Double avgCompletionMinutes;

    // 完成耗时超过三天的工单数量
    private Long overThreeDaysCount;

    // 完成耗时超过七天的工单数量
    private Long overSevenDaysCount;

    // 当前未完成工单数量
    private Long unfinishedCount;
}
