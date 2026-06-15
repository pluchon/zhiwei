package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 楼栋响应数据
@Data
public class BuildingVO {

    // 楼栋ID
    private Long buildingId;

    // 校区ID
    private Long campusId;

    // 楼栋展示名称
    private String buildingName;

    // 楼栋说明
    private String description;

    // 启用状态（0启用 1停用）
    private Integer status;

    // 删除状态
    private Integer deleteState;

    // 创建时间
    private LocalDateTime createTime;

    // 更新时间
    private LocalDateTime updateTime;
}
