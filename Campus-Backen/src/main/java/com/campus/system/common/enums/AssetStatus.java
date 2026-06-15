package com.campus.system.common.enums;

import com.campus.system.common.exception.BusinessException;
import lombok.Getter;

// 资产状态枚举
@Getter
public enum AssetStatus {

    IN_USE("IN_USE", "使用中"),
    UNDER_REPAIR("UNDER_REPAIR", "维修中"),
    OUT_OF_SERVICE("OUT_OF_SERVICE", "停用");

    private final String code;
    private final String label;

    AssetStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AssetStatus of(String code) {
        for (AssetStatus value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw BusinessException.badRequest("未知资产状态");
    }
}
