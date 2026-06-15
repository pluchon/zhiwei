package com.campus.system.dto;

import lombok.Data;

// 只需要乐观锁版本号的状态流转请求参数
@Data
public class VersionDTO {

    // 前端提交的乐观锁版本号
    private Integer version;
}
