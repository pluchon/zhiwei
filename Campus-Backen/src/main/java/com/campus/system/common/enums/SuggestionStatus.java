package com.campus.system.common.enums;

import com.campus.system.common.exception.BusinessException;
import lombok.Getter;

// 维修师傅建议状态
@Getter
public enum SuggestionStatus {

    PENDING("PENDING", "待处理"),
    ACCEPTED("ACCEPTED", "已采纳"),
    REJECTED("REJECTED", "未采纳");

    private final String code;
    private final String label;

    SuggestionStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static SuggestionStatus of(String code) {
        for (SuggestionStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw BusinessException.badRequest("未知建议状态");
    }
}
