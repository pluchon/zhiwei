package com.campus.system.dto;

import lombok.Data;

// 后台资产分类列表查询参数
@Data
public class AssetCategoryQueryDTO {

    // 分类名称关键词（模糊匹配）
    private String keyword;

    // 状态：0 启用，1 停用
    private Integer status;

    // 指定导出的分类主键，逗号分隔
    private String assetCategoryIds;
}
