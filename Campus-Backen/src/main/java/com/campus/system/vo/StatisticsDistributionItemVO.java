package com.campus.system.vo;

import lombok.Data;

// 统计分布项响应数据
@Data
public class StatisticsDistributionItemVO {

    // 分布项名称
    private String name;

    // 数量
    private long count;
}
