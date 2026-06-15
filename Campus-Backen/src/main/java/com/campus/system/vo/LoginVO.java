package com.campus.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

// 登录成功响应数据
@Data
@AllArgsConstructor
public class LoginVO {

    // JWT 访问令牌
    private String token;

    // 令牌剩余有效秒数
    private Long expiresIn;
}
