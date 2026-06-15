package com.campus.system.common.security.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 短信或邮件验证码的 Redis 载荷。
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationPayload {

    // 验证码绑定的业务场景。
    private String scene;

    // 验证码发送目标，通常为手机号或邮箱。
    private String target;

    // 开发阶段明文验证码；生产环境后续应改为摘要值。
    private String code;

    // 已校验失败次数，用于限制暴力尝试。
    private Integer errors;
}
