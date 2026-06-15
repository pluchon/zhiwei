package com.campus.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

// 账号激活第一步响应数据
@Data
@AllArgsConstructor
public class ActivationStartVO {

    // 激活票据
    private String activationTicket;

    // 脱敏后的手机号
    private String maskedPhone;
}
