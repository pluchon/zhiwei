package com.campus.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

// 行为验证码一次性票据响应数据
@Data
@AllArgsConstructor
public class CaptchaTicketVO {

    // 一次性票据
    private String captchaTicket;

    // 过期秒数
    private Integer expiresIn;
}
