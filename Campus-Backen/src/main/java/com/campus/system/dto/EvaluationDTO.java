package com.campus.system.dto;

import lombok.Data;

// 工单评价请求参数
@Data
public class EvaluationDTO {

    // 星级评分，范围为 1 到 5
    private Integer star;

    // 评价内容，可为空，最多 50 字
    private String content;
}
