package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 资产导入批次响应数据
@Data
public class AssetImportBatchVO {

    // 导入批次主键
    private Long batchId;

    // 上传文件名
    private String fileName;

    // 操作管理员主键
    private Long operatorId;

    // 操作管理员姓名
    private String operatorName;

    // 识别卡片总数
    private Integer totalCount;

    // 待审核卡片数量
    private Integer pendingCount;

    // 已确认入库卡片数量
    private Integer confirmedCount;

    // 已忽略卡片数量
    private Integer ignoredCount;

    // 导入来源类型
    private String sourceType;

    // 导入来源类型中文
    private String sourceTypeLabel;

    // 创建时间
    private LocalDateTime createTime;
}
