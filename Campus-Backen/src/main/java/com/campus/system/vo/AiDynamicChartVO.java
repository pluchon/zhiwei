package com.campus.system.vo;

import java.util.List;
import lombok.Data;

// AI 助手动态图表响应数据
@Data
public class AiDynamicChartVO {

    // 图表类型（BAR / PIE / LINE）
    private String chartType;

    // 统计维度编码
    private String dimension;

    // 图表标题
    private String title;

    // 统计时间范围说明
    private String rangeLabel;

    // 数值单位
    private String unit;

    // 是否横向柱状图
    private Boolean horizontal;

    // 图表数据项
    private List<StatisticsDistributionItemVO> items;
}
