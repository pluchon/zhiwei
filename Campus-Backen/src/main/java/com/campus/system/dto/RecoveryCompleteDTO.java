package com.campus.system.dto;

import lombok.Data;

// 找回密码重置新密码请求
@Data
public class RecoveryCompleteDTO {

    // 找回密码第一步生成的恢复票据
    private String recoveryTicket;

    // 用户设置的新密码
    private String newPassword;
}
