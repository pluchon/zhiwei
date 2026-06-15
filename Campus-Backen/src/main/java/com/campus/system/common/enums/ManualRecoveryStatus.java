package com.campus.system.common.enums;

import com.campus.system.common.exception.BusinessException;
import lombok.Getter;

// 账号人工恢复申请状态枚举
@Getter
public enum ManualRecoveryStatus {

    PENDING("PENDING", "待复核"),
    APPROVED("APPROVED", "已通过"),
    COMPLETED("COMPLETED", "已完成"),
    REJECTED("REJECTED", "已驳回"),
    EXPIRED("EXPIRED", "已过期");

    private final String code;
    private final String label;

    ManualRecoveryStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ManualRecoveryStatus of(String code) {
        for (ManualRecoveryStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw BusinessException.badRequest("未知恢复申请状态");
    }
}
