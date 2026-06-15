package com.campus.system.common.enums;

import lombok.Getter;

// AI 业务场景类型枚举
@Getter
public enum AiSceneType {

    ASSET_RECOGNITION("ASSET_RECOGNITION", "资产识别"),
    DUPLICATE_REPAIR("DUPLICATE_REPAIR", "重复报修判断"),
    ORDER_LINK("ORDER_LINK", "工单关联推荐"),
    DISPATCH_ANALYSIS("DISPATCH_ANALYSIS", "派单辅助分析"),
    NL_STATISTICS("NL_STATISTICS", "自然语言查统计"),
    NL_EXPORT("NL_EXPORT", "自然语言导出解析"),
    ORDER_SEMANTIC_SEARCH("ORDER_SEMANTIC_SEARCH", "工单语义搜索"),
    ASSET_SEMANTIC_SEARCH("ASSET_SEMANTIC_SEARCH", "资产语义搜索"),
    SUGGESTION_SIMILARITY("SUGGESTION_SIMILARITY", "建议相似检测"),
    ASSET_CATEGORY_MATCH("ASSET_CATEGORY_MATCH", "资产分类语义匹配"),
    AVATAR_REVIEW("AVATAR_REVIEW", "头像审核");

    private final String code;
    private final String label;

    AiSceneType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
