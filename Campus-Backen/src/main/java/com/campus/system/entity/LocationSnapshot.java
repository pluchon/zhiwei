package com.campus.system.entity;

import lombok.Data;

// 工单位置快照载荷
@Data
public class LocationSnapshot {

    // 校区主键
    private Long campusId;

    // 校区名称快照
    private String campusName;

    // 校区说明快照
    private String campusDescription;

    // 楼栋主键，可为空
    private Long buildingId;

    // 楼栋名称快照，可为空。
    private String buildingName;

    // 楼栋说明快照，可为空
    private String buildingDescription;

}
