package com.campus.system.dto;

import lombok.Data;

// 账号激活第二步完成请求
@Data
public class ActivationCompleteDTO {

    // 激活第一步生成的票据
    private String activationTicket;

    // 验证码编号
    private String verificationId;

    // 用户输入的验证码
    private String verificationCode;

    // 用户设置的新密码
    private String newPassword;
}
