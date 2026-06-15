package com.campus.system.common.security.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 登录会话的载荷
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginSessionPayload {

    // 登录用户主键
    private Long userId;

    // 登录时的安全戳
    private String securityStamp;
}
