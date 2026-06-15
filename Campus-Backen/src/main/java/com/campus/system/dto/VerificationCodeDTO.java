package com.campus.system.dto;

import lombok.Data;

// 短信/邮箱验证码发送请求
@Data
public class VerificationCodeDTO {

    // 验证码业务场景
    private String scene;

    // 接收验证码的手机号或邮箱
    private String target;

    // 行为验证码换取的一次性票据
    private String captchaTicket;
}
