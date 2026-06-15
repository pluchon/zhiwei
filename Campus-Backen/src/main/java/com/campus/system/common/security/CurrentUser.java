package com.campus.system.common.security;

// 存在spring security 上下文中，表示当前的用户的信息
public record CurrentUser(Long userId, String userNo, String roleCode, String securityStamp) {}
