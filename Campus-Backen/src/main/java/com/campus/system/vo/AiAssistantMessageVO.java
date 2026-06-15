package com.campus.system.vo;

import lombok.Data;

// AI 助手消息响应
@Data
public class AiAssistantMessageVO {

    // 会话主键
    private Long sessionId;

    // 助手回复文本
    private String replyText;

    // 结构化统计结果
    private AiStatisticsQueryResultVO statisticsResult;

    // 导出预览
    private AiExportPreviewVO exportPreview;

    // 工单语义搜索结果
    private AiOrderSearchResultVO orderSearchResult;

    // 资产语义搜索结果
    private AiAssetSearchResultVO assetSearchResult;

    // 建议语义搜索结果
    private AiSuggestionSearchResultVO suggestionSearchResult;

    // 是否超出权限或解析失败
    private Boolean outOfScope;
}
