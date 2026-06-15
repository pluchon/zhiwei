package com.campus.system.vo;

import lombok.Data;

// 资产语义搜索单项
@Data
public class AiAssetSearchItemVO {

    // 资产主键
    private Long assetId;

    // 资产编号
    private String assetNo;

    // 资产名称
    private String assetName;

    // 分类名称
    private String categoryName;

    // 位置摘要
    private String locationSummary;
}
