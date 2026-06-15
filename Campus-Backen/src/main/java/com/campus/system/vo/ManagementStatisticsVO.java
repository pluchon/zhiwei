package com.campus.system.vo;

import java.util.List;
import lombok.Data;

// 管理统计汇总响应数据
@Data
public class ManagementStatisticsVO {

    // 统计时间范围类型
    private String rangeType;

    // 统计时间范围中文名称
    private String rangeTypeLabel;

    // 统计开始时间（展示用）
    private String rangeStart;

    // 统计结束时间（展示用）
    private String rangeEnd;

    // 维修效率统计
    private RepairEfficiencyStatVO repairEfficiency;

    // 维修次数较多的资产
    private List<AssetRepairRiskItemVO> topRepairedAssets;

    // 资产分类报修数量
    private List<AssetCategoryRepairStatVO> assetCategoryRepairs;

    // 未完成工单日趋势
    private List<StatisticsDistributionItemVO> unfinishedOrderTrend;
}
