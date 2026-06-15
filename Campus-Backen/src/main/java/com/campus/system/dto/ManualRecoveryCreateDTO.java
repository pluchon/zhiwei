package com.campus.system.dto;

import lombok.Data;

// 创建账号人工恢复申请请求参数
@Data
public class ManualRecoveryCreateDTO {

    // 目标用户主键
    private Long targetUserId;

    // 新主手机号
    private String newPhone;

    // 线下身份核验说明
    private String identityCheckNote;
}
