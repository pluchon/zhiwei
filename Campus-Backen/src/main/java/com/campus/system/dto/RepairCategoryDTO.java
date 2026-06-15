package com.campus.system.dto;

import lombok.Data;

// 故障类型维护请求参数
@Data
public class RepairCategoryDTO {

    // 故障类型名称
    private String categoryName;

    // 故障类型说明
    private String description;

    // 类型状态（0启用 1停用）
    private Integer status;
}
