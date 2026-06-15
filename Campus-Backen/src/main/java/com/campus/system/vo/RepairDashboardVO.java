package com.campus.system.vo;

import lombok.Data;

// 管理员报修看板响应数据
@Data
public class RepairDashboardVO {

    // 待派单工单数
    private long pendingDispatch;

    // 待接单工单数
    private long pendingAccept;

    // 已接单工单数
    private long accepted;

    // 处理中工单数
    private long processing;

    // 待确认工单数
    private long pendingConfirm;

    // 待仲裁工单数
    private long pendingArbitration;

    // 长时间滞留未处理工单数
    private long longStagnant;

    // 今日新增工单数
    private long todayCreated;

    // 今日完成工单数
    private long todayCompleted;

    // 统计时间范围天数
    private int rangeDays;

    // 故障类型分布
    private java.util.List<StatisticsDistributionItemVO> faultTypeDistribution;

    // 校区分布
    private java.util.List<StatisticsDistributionItemVO> campusDistribution;

    // 楼栋分布
    private java.util.List<StatisticsDistributionItemVO> buildingDistribution;

    // 当前各工单状态数量
    private java.util.List<StatisticsDistributionItemVO> currentStatusDistribution;
}
