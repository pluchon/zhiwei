package com.campus.system.dto;

import lombok.Data;

// 管理员创建普通用户请求参数
@Data
public class AdminCreateUserDTO {

    // 登录账号，通常为学工号或系统分配账号
    private String userNo;

    // 用户真实姓名
    private String realName;

    // 页面展示昵称
    private String nickName;

    // 固定角色编码，禁止传入 ADMIN
    private String roleCode;

    // 用户手机号
    private String phoneNumber;

    // 学生家长手机号，可为空
    private String parentPhone;

    // 初始密码，仅用于创建账号时写入加密摘要
    private String initialPassword;
}
