package com.campus.system.dto;

import lombok.Data;

// 维修结果提交请求参数
@Data
public class RepairResultDTO {

    // 前端提交的乐观锁版本号
    private Integer version;

    // 维修结果描述
    private String description;
}
