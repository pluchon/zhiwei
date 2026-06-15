package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// AI 助手历史消息
@Data
public class AiAssistantHistoryMessageVO {

    private Long messageId;

    private String role;

    private String text;

    private AiStatisticsQueryResultVO statisticsResult;

    private AiExportPreviewVO exportPreview;

    private AiOrderSearchResultVO orderSearchResult;

    private AiAssetSearchResultVO assetSearchResult;

    private AiSuggestionSearchResultVO suggestionSearchResult;

    private LocalDateTime createTime;
}
