package com.campus.system.common.enums;

import com.campus.system.common.exception.BusinessException;
import lombok.Getter;

// AI 助手动态图表类型
@Getter
public enum AiChartType {

    BAR("BAR", "柱状图"),
    PIE("PIE", "饼图"),
    LINE("LINE", "折线图");

    private final String code;
    private final String label;

    AiChartType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AiChartType of(String code) {
        if (code == null || code.isBlank()) {
            return BAR;
        }
        String normalized = code.trim().toUpperCase();
        for (AiChartType value : values()) {
            if (value.code.equals(normalized)) {
                return value;
            }
        }
        throw BusinessException.badRequest("不支持的图表类型");
    }

    public static AiChartType fromMessage(String message) {
        if (message == null) {
            return BAR;
        }
        if (message.contains("饼") || message.contains("占比") || message.contains("比例")) {
            return PIE;
        }
        if (message.contains("折线") || message.contains("趋势") || message.contains("走势")) {
            return LINE;
        }
        return BAR;
    }
}
