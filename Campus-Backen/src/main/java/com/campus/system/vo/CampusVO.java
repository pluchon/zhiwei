package com.campus.system.vo;

import java.time.LocalDateTime;
import lombok.Data;

// 校区响应数据
@Data
public class CampusVO {

    // 校区ID
    private Long campusId;

    // 校区展示名称
    private String campusName;

    // 校区说明
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
