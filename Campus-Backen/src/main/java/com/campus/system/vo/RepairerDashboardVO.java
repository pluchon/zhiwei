package com.campus.system.vo;

import java.util.List;
import lombok.Data;

// 维修师傅首页看板
@Data
public class RepairerDashboardVO {

    private int rangeDays;

    private long inProgress;

    private long pendingConfirm;

    private long completed;

    private RepairerWorkStatVO workStat;

    private List<StatisticsDistributionItemVO> statusDistribution;

    private List<StatisticsDistributionItemVO> faultTypeDistribution;

    private List<StatisticsDistributionItemVO> completionTrend;

    private List<ReporterDashboardRecentOrderVO> recentOrders;
}
