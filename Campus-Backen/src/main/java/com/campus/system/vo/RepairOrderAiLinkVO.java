package com.campus.system.vo;

import lombok.Data;

// 工单 AI 关联推荐响应
@Data
public class RepairOrderAiLinkVO {

    // 关联记录主键
    private Long linkId;

    // 当前工单主键
    private Long sourceOrderId;

    // 关联历史工单主键
    private Long targetOrderId;

    // 关联历史工单编号
    private String targetOrderNo;

    // 关联历史工单标题
    private String targetOrderTitle;

    // 关联类型
    private String linkType;

    // AI 推荐理由
    private String aiReason;

    // 管理员是否已确认
    private Integer confirmed;
}
