package com.campus.system.common.config;

import com.campus.system.common.result.ApiResponse;
import com.campus.system.common.filter.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// spring安全核心配置类
@Configuration
@EnableMethodSecurity
// 开启方法级别的权限控制，允许使用 @PreAuthorize 注解来做精细化控制
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwt;

    @Autowired
    private ObjectMapper json;

    // 自带盐值加密
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 配置好安全拦截链
    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        // 1. 关闭 CSRF 防护（因为我们使用 Header 携带 JWT 且不依赖 Cookie，天然不受跨站请求伪造攻击影响）
        http.csrf(AbstractHttpConfigurer::disable)
            // 2. 启用跨域支持（前后端分离项目必备，详见下方的 cors() 方法）
            .cors(c -> {})
            // 3. 将 Session 生成策略设为无状态（STATELESS）。因为我们完全依赖 JWT 和 Redis 来管理状态，不需要 Tomcat 自带的 JSESSIONID
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 4. 配置接口的访问权限规则
            .authorizeHttpRequests(
                a ->
                    a.requestMatchers("/auth/**", "/error") // 认证相关接口（登录、注册等）和 Spring Boot 全局错误页面
                        .permitAll() // 允许所有人匿名访问
                        .requestMatchers(HttpMethod.GET, "/repair/categories") // 允许所有人访问获取报修类别的接口
                        .permitAll()
                        .requestMatchers("/manual-account-recovery/*/verify-info")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/manual-account-recovery/*/verify-phone")
                        .permitAll()
                        .anyRequest() // 其他所有请求...
                        .authenticated()) // ...都必须通过认证（即必须登录并且提供了合法的 JWT）
            // 5. 把我们自定义的 JwtAuthenticationFilter 过滤器插到 Spring 原生的 UsernamePasswordAuthenticationFilter 过滤器之前
            // 这样每个请求过来都会先经过我们自己的 JWT 校验逻辑
            .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
            // 6. 配置异常处理器：遇到认证或授权失败时，返回给前端标准格式的 JSON 字符串，而不是抛出 403 的 HTML 错误页面
            .exceptionHandling(e ->
                        e.authenticationEntryPoint((q, r, x)
                                        -> write(r, json, 401, "请先登录")) // 场景：用户没带 Token 或 Token 失效去访问受保护接口
                        .accessDeniedHandler((q, r, x)
                                -> write(r, json, 403, "无权执行此操作"))); // 场景：用户虽然登录了，但可能由于角色不够（比如学生访问管理员接口）而被拦截
        return http.build();
    }

    // 同意写入JWT
    private void write(HttpServletResponse response, ObjectMapper json, int code, String message) throws IOException {
        response.setStatus(code);
        // 设置响应类型为 JSON 和字符编码为 UTF-8，防止中文乱码
        response.setContentType("application/json;charset=UTF-8");
        // 把 ApiResponse 对象序列化为 JSON 并写回到客户端
        json.writeValue(response.getWriter(), new ApiResponse<>(code, message, null, null));
    }

    // 允许跨域调用
    @Bean
    CorsConfigurationSource cors() {
        CorsConfiguration c = new CorsConfiguration();
        // 允许任何源（域名/IP）发起跨域请求（生产环境为了安全可以改为前端真实所在的地址，如 http://localhost:5173）
        c.setAllowedOriginPatterns(List.of("*"));
        // 允许任何 HTTP 动词（GET, POST, PUT, DELETE, OPTIONS 等）
        c.setAllowedMethods(List.of("*"));
        // 允许前端请求携带任何自定义 Header（比如我们的 Authorization 头部）
        c.setAllowedHeaders(List.of("*"));
        // 允许前端发送和携带凭证（例如跨域时允许带上 Cookie 虽然我们主要用 JWT）
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 把以上跨域规则应用到后端所有的接口路径（/**）上
        source.registerCorsConfiguration("/**", c);
        return source;
    }
}
