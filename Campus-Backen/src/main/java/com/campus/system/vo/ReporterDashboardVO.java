package com.campus.system.vo;

import java.util.List;
import lombok.Data;

// 学生/教师报修首页看板
@Data
public class ReporterDashboardVO {

    private int rangeDays;

    private long inProgress;

    private long pendingConfirm;

    private long completed;

    private long draft;

    private List<StatisticsDistributionItemVO> statusDistribution;

    private List<StatisticsDistributionItemVO> faultTypeDistribution;

    private List<StatisticsDistributionItemVO> submitTrend;

    private List<ReporterDashboardRecentOrderVO> recentOrders;
}
