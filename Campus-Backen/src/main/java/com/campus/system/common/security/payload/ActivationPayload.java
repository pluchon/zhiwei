package com.campus.system.common.security.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 激活的票据载荷
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivationPayload {

    // 待激活用户主键
    private Long userId;

    // 激活验证码发送目标，通常为学校预留手机号。
    private String target;
}
