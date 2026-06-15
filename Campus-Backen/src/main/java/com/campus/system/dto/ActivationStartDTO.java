package com.campus.system.dto;

import lombok.Data;

// 账号激活第一步请求
@Data
public class ActivationStartDTO {

    // 待激活账号
    private String userNo;

    // 管理员分配的初始密码
    private String initialPassword;
}
