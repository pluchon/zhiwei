package com.campus.system.vo;

import lombok.Data;

// 登录页公开概览数据
@Data
public class PortalSummaryVO {

    private long todayOrders;

    private long processingOrders;

    private long categoryCount;

    private long campusCount;
}
