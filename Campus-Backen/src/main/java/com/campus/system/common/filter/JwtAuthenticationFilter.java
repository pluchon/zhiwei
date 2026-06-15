package com.campus.system.common.filter;

import com.campus.system.common.security.CurrentUser;
import com.campus.system.common.security.JwtService;
import com.campus.system.common.security.payload.LoginSessionPayload;
import com.campus.system.entity.SysRole;
import com.campus.system.entity.SysUser;
import com.campus.system.mapper.SysRoleMapper;
import com.campus.system.mapper.SysUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

//JWT 只携带 sessionId；每次请求都从 Redis 与数据库实时校验账号状态、角色和安全戳
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwt;

    @Autowired
    private RedisTemplate<String, Object> redis;

    @Autowired
    private SysUserMapper users;

    @Autowired
    private SysRoleMapper roles;

    // 拦截并过滤请求。若请求头包含 "Bearer " 前缀的 Token，则尝试进行用户身份认证
    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain) throws ServletException, IOException {
        // 从请求头获取 Authorization 数据
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            // 提取 token 并执行身份认证逻辑
            authenticate(header);
        }
        // 继续执行过滤器链中的下一个过滤器
        chain.doFilter(request, response);
    }

    /**
     * 根据请求头中的 JWT 建立 Spring Security 上下文。
     * 如果 Redis 会话缺失或 securityStamp 不一致，说明令牌已失效，需要按未登录处理。
     */
    private void authenticate(String header) {
        try {
            // 1. 截取 "Bearer " 之后的部分，解析出 JWT 载荷中存储的唯一 sessionId
            String sessionId = jwt.parse(header.substring(7));
            // 2. 拿着 sessionId 从 Redis 中获取对应的登录会话负载数据
            Object raw = redis.opsForValue().get("login_tokens:" + sessionId);
            if (raw instanceof LoginSessionPayload session) {
                // 3. 成功找到会话后，执行加载 Spring Security 上下文的操作
                loadSecurityContext(sessionId, session);
            }
        } catch (Exception ignored) {
            // 4. 解析失败（Token过期、被篡改等），清除当前线程的安全上下文，视为未登录状态
            SecurityContextHolder.clearContext();
        }
    }

    // 从数据库实时加载用户信息，验证无误后将其写入 Spring Security 线程上下文
    private void loadSecurityContext(String sessionId, LoginSessionPayload session) {
        // 1. 从数据库查询最新的用户记录
        SysUser user = users.selectById(session.getUserId());
        // 2. 校验此会话在当前系统状态下是否依然有效（包括状态及安全戳的比对）
        if (!isSessionUsable(user, session)) {
            // 3. 校验失败，说明用户状态异常或已在别处改密强退，清除 Redis 缓存的会话，终止认证
            redis.delete("login_tokens:" + sessionId);
            return;
        }
        // 4. 校验通过，根据用户绑定的角色 ID 查出对应的角色信息
        SysRole role = roles.selectById(user.getRoleId());
        // 5. 组装当前登录用户的 Principal 载体 CurrentUser
        CurrentUser principal = new CurrentUser(user.getUserId(), user.getUserNo(), role.getRoleName(), user.getSecurityStamp());
        // 6. 构造 UsernamePasswordAuthenticationToken 并存入 Spring Security 上下文中，完成登记工作
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.getRoleName()))));
    }
    
    // 判断会话是否可用：用户必须存在、激活状态为1、账号未被冻结(状态为0)且数据库中的安全戳与Redis会话里的完全匹配
    private boolean isSessionUsable(SysUser user, LoginSessionPayload session) {
        return user != null && user.getActivationStatus() == 1 && user.getAccountStatus() == 0 && user.getSecurityStamp().equals(session.getSecurityStamp());
    }
}