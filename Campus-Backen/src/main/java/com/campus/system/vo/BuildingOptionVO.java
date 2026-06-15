package com.campus.system.vo;

import lombok.Data;

// 楼栋选择项
@Data
public class BuildingOptionVO {

    // 楼栋主键
    private Long buildingId;

    // 楼栋名称
    private String buildingName;

    // 楼栋描述
    private String description;
}
