package com.campus.system.dto;

import lombok.Data;

// 账号人工恢复审批请求参数
@Data
public class ManualRecoveryReviewDTO {

    // 是否通过
    private Boolean approved;

    // 审批处理说明
    private String reviewNote;
}
