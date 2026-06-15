package com.campus.system.vo;

import lombok.Data;

// 建议语义搜索单项
@Data
public class AiSuggestionSearchItemVO {

    // 建议主键
    private Long suggestionId;

    // 标题
    private String title;

    // 状态
    private String status;

    // 状态中文
    private String statusLabel;

    // 分类
    private String category;
}
