package com.campus.system.common.enums;

import com.campus.system.common.exception.BusinessException;
import lombok.Getter;

// 维修师傅接单可用状态枚举
@Getter
public enum RepairerAcceptingState {

    AVAILABLE("AVAILABLE", "可接单"),
    PAUSED("PAUSED", "暂停接单");

    private final String code;
    private final String label;

    RepairerAcceptingState(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static RepairerAcceptingState of(String code) {
        for (RepairerAcceptingState value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw BusinessException.badRequest("未知接单状态");
    }
}
