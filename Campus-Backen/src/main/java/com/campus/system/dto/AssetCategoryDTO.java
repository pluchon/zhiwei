package com.campus.system.dto;

import lombok.Data;

// 资产分类维护请求参数
@Data
public class AssetCategoryDTO {

    // 分类名称
    private String categoryName;

    // 分类状态（0启用 1停用）
    private Integer status;
}
