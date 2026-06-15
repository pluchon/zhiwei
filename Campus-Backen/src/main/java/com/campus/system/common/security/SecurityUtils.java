package com.campus.system.common.security;

import com.campus.system.common.exception.BusinessException;
import lombok.NoArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;

// 安全工具类，用于快速获取当前登录的用户信息。
@NoArgsConstructor
public final class SecurityUtils {
    /**
     * 获取当前请求的登录用户信息
     * @return CurrentUser 当前用户信息
     * @throws BusinessException 如果未登录或登录状态失效，则抛出异常
     */
    public static CurrentUser current() {
        // 从 Spring Security 线程上下文中获取认证的凭证信息
        Object value = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // 检查这个凭证是不是我们之前塞入的 CurrentUser 对象
        if (value instanceof CurrentUser user){
            return user;
        }
        // 如果不是，说明没有有效的登录状态，抛出业务异常交由全局异常处理器处理
        throw BusinessException.unauthorized("登录状态无效");
    }
}
