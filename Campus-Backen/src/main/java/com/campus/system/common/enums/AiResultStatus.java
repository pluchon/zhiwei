package com.campus.system.common.enums;

import lombok.Getter;

// AI 调用结果状态枚举
@Getter
public enum AiResultStatus {

    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    DEGRADED("DEGRADED", "降级");

    private final String code;
    private final String label;

    AiResultStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
