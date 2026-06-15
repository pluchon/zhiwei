package com.campus.system.dto;

import lombok.Data;

// 变更联系方式请求
@Data
public class ChangeContactDTO {

    // 当前身份验证码编号
    private String identityVerificationId;

    // 当前身份验证码
    private String identityCode;

    // 新联系方式验证码编号
    private String newVerificationId;

    // 新联系方式验证码
    private String newCode;
}
