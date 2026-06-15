package com.campus.system.dto;

import lombok.Data;

// 自然语言导出解析结果
@Data
public class AiNlExportParseResultDTO {

    // 是否解析成功
    private boolean success;

    // 已校验查询条件
    private RepairOrderQueryDTO query;

    // 筛选条件摘要
    private String filterSummary;

    // 错误提示
    private String errorMessage;

    public static AiNlExportParseResultDTO success(RepairOrderQueryDTO query, String filterSummary) {
        AiNlExportParseResultDTO dto = new AiNlExportParseResultDTO();
        dto.setSuccess(true);
        dto.setQuery(query);
        dto.setFilterSummary(filterSummary);
        return dto;
    }

    public static AiNlExportParseResultDTO error(String message) {
        AiNlExportParseResultDTO dto = new AiNlExportParseResultDTO();
        dto.setSuccess(false);
        dto.setErrorMessage(message);
        return dto;
    }
}
