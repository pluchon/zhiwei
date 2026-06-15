package com.campus.system.vo;

import lombok.Data;

// 派单辅助 AI 分析响应
@Data
public class RepairOrderAiAnalysisVO {

    // 文字分析内容
    private String analysisText;

    // 是否因 AI 不可用而降级
    private Boolean degraded;
}
