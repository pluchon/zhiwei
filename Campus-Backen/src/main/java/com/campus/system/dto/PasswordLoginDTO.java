package com.campus.system.dto;

import lombok.Data;

// 账号密码登录请求
@Data
public class PasswordLoginDTO {

    // 登录账号
    private String userNo;

    // 登录密码明文，仅用于本次校验
    private String password;

    // 行为验证码换取的一次性票据
    private String captchaTicket;
}
