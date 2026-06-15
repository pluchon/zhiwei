package com.campus.system.dto;

import lombok.Data;

// 管理员编辑用户请求参数
@Data
public class AdminUpdateUserDTO {

    // 目标角色编码，可为空；不允许提升为 ADMIN
    private String roleCode;

    // 账号状态：0 正常，1 停用
    private Integer accountStatus;

    // 用户昵称，可为空
    private String nickName;

    // 用户手机号，可为空
    private String phoneNumber;
}
