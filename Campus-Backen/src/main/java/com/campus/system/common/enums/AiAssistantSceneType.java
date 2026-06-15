package com.campus.system.common.enums;

import com.campus.system.common.exception.BusinessException;
import lombok.Getter;

// AI 助手会话场景类型枚举
@Getter
public enum AiAssistantSceneType {

    STATISTICS("STATISTICS", "自然语言查统计"),
    EXPORT("EXPORT", "自然语言导出预览"),
    ORDER_SEARCH("ORDER_SEARCH", "历史工单语义搜索"),
    ASSET_SEARCH("ASSET_SEARCH", "资产语义搜索"),
    SUGGESTION_SEARCH("SUGGESTION_SEARCH", "建议语义搜索");

    private final String code;
    private final String label;

    AiAssistantSceneType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AiAssistantSceneType of(String code) {
        for (AiAssistantSceneType value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw BusinessException.badRequest("未知 AI 助手场景");
    }
}
