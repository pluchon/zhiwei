package com.campus.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.system.common.dbCommon.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

// 资产分类实体
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("asset_category")
public class AssetCategory extends BaseEntity {

    // 资产分类主键
    @TableId
    private Long assetCategoryId;

    // 分类名称
    private String categoryName;

    // 标准化名称，用于唯一约束
    private String normalizedName;

    // 分类状态（0启用 1停用）
    private Integer status;
}
