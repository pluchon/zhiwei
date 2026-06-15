package com.campus.system.dto;

import lombok.Data;

// 资产状态手动变更请求参数
@Data
public class AssetStatusChangeDTO {

    // 目标状态
    private String status;

    // 变更原因
    private String changeReason;

    // 乐观锁版本号
    private Integer version;
}
