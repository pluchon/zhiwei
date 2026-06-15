package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 资产导入批次实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("asset_import_batch")
public class AssetImportBatch extends BaseEntity {

    // 导入批次主键
    @TableId
    private Long batchId;

    // 上传文件名
    private String fileName;

    // 操作管理员主键
    private Long operatorId;

    // 识别卡片总数
    private Integer totalCount;

    // 待审核卡片数量
    private Integer pendingCount;

    // 已确认入库卡片数量
    private Integer confirmedCount;

    // 已忽略卡片数量
    private Integer ignoredCount;

    // 导入来源类型（EXCEL / IMAGE）
    private String sourceType;
}
