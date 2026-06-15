package com.campus.system.vo;

import lombok.Data;

// 整体维修力量响应数据
@Data
public class WorkforceSummaryVO {

    // 是否存在符合条件的维修师傅
    private boolean hasRepairer;

    // 繁忙程度编码
    private String busyLevel;

    // 繁忙程度中文名称
    private String busyLevelLabel;
}
