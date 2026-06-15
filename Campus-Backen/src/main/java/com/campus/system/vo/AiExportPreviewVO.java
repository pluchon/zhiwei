package com.campus.system.vo;

import lombok.Data;

// 自然语言导出预览响应
@Data
public class AiExportPreviewVO {

    // 预览令牌，确认导出时使用
    private String previewToken;

    // 导出类型（ORDER / STATISTICS）
    private String exportType;

    // 预览说明
    private String previewSummary;

    // 预计导出条数
    private Integer estimatedCount;

    // 筛选条件摘要
    private String filterSummary;

    // 是否禁止确认导出
    private Boolean confirmDisabled;

    // 禁止确认原因
    private String confirmDisabledReason;

    // 预览过期秒数
    private Integer expireSeconds;
}
