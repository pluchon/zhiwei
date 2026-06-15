package com.campus.system.dto;

import lombok.Data;

// 行为验证码票据请求
@Data
public class CaptchaTicketDTO {

    // 行为验证码挑战编号
    private String challengeId;

    // 验证码业务场景
    private String scene;

    // 验证目标，如手机号、邮箱或账号
    private String target;
}
