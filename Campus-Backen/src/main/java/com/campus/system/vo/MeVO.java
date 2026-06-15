package com.campus.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 当前登录用户响应数据
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeVO {

    // 用户公开信息
    private UserVO user;

    // 当前角色编码
    private String roleCode;

    // 前端权限框架使用的角色数组
    private String[] roles;

    // 可访问的头像地址
    private String avatarUrl;

    // 角色中文名称
    private String roleLabel;
}
