package com.campus.system.common.enums;

import com.campus.system.common.exception.BusinessException;
import lombok.Getter;

// 报修类型枚举
@Getter
public enum RepairType {

    NORMAL("NORMAL", "普通报修"),
    ASSET("ASSET", "资产报修");

    private final String code;
    private final String label;

    RepairType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static RepairType of(String code) {
        for (RepairType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw BusinessException.badRequest("未知报修类型");
    }
}
