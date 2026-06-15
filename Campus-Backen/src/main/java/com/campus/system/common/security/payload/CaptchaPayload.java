package com.campus.system.common.security.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 行为验证码挑战和业务票据的 Redis 载荷。
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaPayload {

    // 票据绑定的业务场景，例如 LOGIN_PASSWORD、ACTIVATION
    private String scene;

    // 票据绑定的业务目标，例如账号、手机号或邮箱；允许为空字符串。
    private String target;
}
