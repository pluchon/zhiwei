package com.campus.system.vo;

import java.util.List;
import lombok.Data;

// 报修位置选择项响应数据
@Data
public class LocationOptionVO {

    // 校区主键
    private Long campusId;

    // 校区名称
    private String campusName;

    // 校区描述
    private String description;

    // 楼栋列表快照
    private List<BuildingOptionVO> buildings;
}
