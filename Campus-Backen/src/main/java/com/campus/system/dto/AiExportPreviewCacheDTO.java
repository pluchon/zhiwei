package com.campus.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// AI 导出预览 Redis 缓存载荷
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiExportPreviewCacheDTO {

    // 导出类型（ORDER / STATISTICS）
    private String exportType;

    // 统计范围类型
    private String rangeType;

    // 已校验的工单导出查询条件
    private RepairOrderQueryDTO orderQuery;
}
