package com.campus.system.dto;

import java.time.LocalDateTime;
import lombok.Data;

// 资产导入批次列表查询参数
@Data
public class AssetImportBatchQueryDTO {

    // 文件名关键词
    private String keyword;

    // 来源类型：EXCEL / IMAGE
    private String sourceType;

    // 上传时间起
    private LocalDateTime createTimeFrom;

    // 上传时间止
    private LocalDateTime createTimeTo;

    // 仅显示仍有待审核卡片的批次
    private Boolean onlyPending;
}
