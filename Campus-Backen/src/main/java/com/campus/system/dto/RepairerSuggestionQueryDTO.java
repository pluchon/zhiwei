package com.campus.system.dto;

import lombok.Data;

// 维修师傅建议查询请求参数
@Data
public class RepairerSuggestionQueryDTO {

    // 建议状态
    private String status;

    // 建议分类
    private String category;
}
