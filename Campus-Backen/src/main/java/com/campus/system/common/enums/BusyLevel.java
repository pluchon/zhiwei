package com.campus.system.common.enums;

import lombok.Getter;

// 维修师傅的繁忙程度展示
@Getter
public enum BusyLevel {

    IDLE("IDLE", "空闲"),
    MODERATE("MODERATE", "适中"),
    BUSY("BUSY", "繁忙");

    private final String code;
    private final String label;

    BusyLevel(String code, String label) {
        this.code = code;
        this.label = label;
    }

    // 根据状态码反查枚举类
    public static BusyLevel fromCount(int count) {
        if (count <= 2) {
            return IDLE;
        }
        if (count <= 5) {
            return MODERATE;
        }
        return BUSY;
    }
}
