package com.campus.system.dto;

import lombok.Data;

// 手机验证码登录请求
@Data
public class PhoneLoginDTO {

    // 验证码编号
    private String verificationId;

    // 用户输入的验证码
    private String verificationCode;
}
