package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 报修人首页最近工单
@Data
public class ReporterDashboardRecentOrderVO {

    private Long orderId;

    private String title;

    private Integer status;

    private String statusLabel;

    private LocalDateTime createTime;
}
