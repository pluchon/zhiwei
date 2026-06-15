package com.campus.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

// 行为验证码挑战响应数据
@Data
@AllArgsConstructor
public class CaptchaChallengeVO {

    // 挑战编号
    private String challengeId;

    // 验证码类型
    private String captchaType;

    // 过期秒数
    private Integer expiresIn;
}
