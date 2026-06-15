package com.campus.system.dto;

import lombok.Data;

// 找回密码验证请求
@Data
public class RecoveryVerifyDTO {

    // 验证码编号
    private String verificationId;

    // 用户输入的验证码
    private String verificationCode;
}
