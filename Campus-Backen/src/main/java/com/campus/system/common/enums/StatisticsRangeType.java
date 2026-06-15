package com.campus.system.common.enums;

import com.campus.system.common.exception.BusinessException;
import lombok.Getter;

// 管理统计时间范围枚举
@Getter
public enum StatisticsRangeType {

    LAST_7_DAYS("LAST_7_DAYS", "最近七天", 7),
    LAST_30_DAYS("LAST_30_DAYS", "最近三十天", 30),
    LAST_90_DAYS("LAST_90_DAYS", "最近九十天", 90),
    CURRENT_YEAR("CURRENT_YEAR", "本年度", 0);

    private final String code;
    private final String label;
    private final int days;

    StatisticsRangeType(String code, String label, int days) {
        this.code = code;
        this.label = label;
        this.days = days;
    }

    public static StatisticsRangeType of(String code) {
        if (code == null || code.isBlank()) {
            return LAST_30_DAYS;
        }
        for (StatisticsRangeType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw BusinessException.badRequest("未知统计时间范围");
    }
}
