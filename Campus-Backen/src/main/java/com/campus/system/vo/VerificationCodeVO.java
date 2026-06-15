package com.campus.system.vo;

import lombok.Data;

// 验证码发送响应数据
@Data
public class VerificationCodeVO {

    // 验证码编号
    private String verificationId;

    // 再次发送冷却秒数
    private Integer retryAfter;

    // 开发环境可选暴露的验证码
    private String developmentCode;
}
