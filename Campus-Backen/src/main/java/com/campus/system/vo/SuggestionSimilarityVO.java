package com.campus.system.vo;

import lombok.Data;

// 建议相似检测响应
@Data
public class SuggestionSimilarityVO {

    // 是否存在相似建议
    private Boolean hasSimilar;

    // 是否为他人相似建议（true 时不展示详情）
    private Boolean othersSimilar;

    // 本人相似建议主键
    private Long suggestionId;

    // 本人相似建议标题
    private String title;

    // 本人相似建议状态
    private String status;

    // 本人相似建议状态中文
    private String statusLabel;

    // 提示文案
    private String message;
}
