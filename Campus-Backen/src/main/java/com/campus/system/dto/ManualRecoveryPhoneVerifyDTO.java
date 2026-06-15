package com.campus.system.dto;

import lombok.Data;

// 人工恢复新手机号验证请求参数
@Data
public class ManualRecoveryPhoneVerifyDTO {

    // 验证码编号
    private String verificationId;

    // 短信验证码
    private String verificationCode;
}
