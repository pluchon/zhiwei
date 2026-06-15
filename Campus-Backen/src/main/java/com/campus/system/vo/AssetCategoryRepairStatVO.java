package com.campus.system.vo;

import lombok.Data;

// 资产分类报修统计项响应数据
@Data
public class AssetCategoryRepairStatVO {

    // 资产分类名称
    private String categoryName;

    // 报修数量
    private Long repairCount;
}
