package com.campus.system.vo;

import java.time.LocalDate;
import lombok.Data;

// 资产维修风险统计项响应数据
@Data
public class AssetRepairRiskItemVO {

    // 资产主键
    private Long assetId;

    // 资产编号
    private String assetNo;

    // 资产名称
    private String assetName;

    // 启用日期
    private LocalDate enabledDate;

    // 资产分类名称
    private String assetCategoryName;

    // 资产状态
    private String status;

    // 资产状态中文名称
    private String statusLabel;

    // 购入日期
    private LocalDate purchaseDate;

    // 已购入年数
    private Integer purchaseYears;

    // 已购入月数（扣除整年后剩余月数）
    private Integer purchaseMonths;

    // 维修次数
    private Long repairCount;
}
