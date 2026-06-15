package com.campus.system.dto;

import lombok.Data;

// 维修师傅建议提交与编辑请求参数
@Data
public class RepairerSuggestionSubmitDTO {

    // 建议分类
    private String category;

    // 建议标题
    private String title;

    // 建议内容
    private String content;
}
