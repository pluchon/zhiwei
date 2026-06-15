package com.campus.system.dto;

import lombok.Data;

// ORDER_LINK AI 单条推荐解析结果
@Data
public class AiOrderLinkRecommendationDTO {

    // 候选工单主键
    private Long orderId;

    // 是否推荐关联
    private boolean recommended;

    // 关联推荐理由
    private String reason;
}
