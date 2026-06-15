package com.campus.system.vo;

import lombok.Data;

// 工单语义搜索单项
@Data
public class AiOrderSearchItemVO {

    // 工单主键
    private Long orderId;

    // 工单编号
    private String orderNo;

    // 标题
    private String title;

    // 状态
    private Integer status;

    // 状态中文
    private String statusLabel;

    // 位置摘要
    private String locationSummary;
}
