package com.campus.system.common.enums;

import com.campus.system.common.exception.BusinessException;
import lombok.Getter;

// AI 识别状态枚举
@Getter
public enum AiRecognizeStatus {

    PENDING("PENDING", "识别中"),
    SUCCESS("SUCCESS", "识别成功"),
    FAILED("FAILED", "识别失败");

    private final String code;
    private final String label;

    AiRecognizeStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AiRecognizeStatus of(String code) {
        for (AiRecognizeStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw BusinessException.badRequest("未知 AI 识别状态");
    }
}
