package com.campus.system.dto;

import lombok.Data;

// 修改密码请求
@Data
public class ChangePasswordDTO {

    // 当前密码
    private String oldPassword;

    // 新密码
    private String newPassword;
}
