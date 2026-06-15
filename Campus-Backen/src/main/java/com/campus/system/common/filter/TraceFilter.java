package com.campus.system.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// 目的是让这一请求周期内产生的所有日志都包含相同的 TraceId，方便在高并发环境下定位和排查问题
@Component
public class TraceFilter extends OncePerRequestFilter {
  
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        // 1. 生成不带中划线的 32 位全局唯一 UUID，作为该请求的身份证 (TraceId)
        String traceId = UUID.randomUUID().toString().replace("-", "");
        // 2. 将 TraceId 放入 slf4j 日志框架的 MDC（映射诊断上下文）中，绑定到当前执行线程
        MDC.put("traceId", traceId);
        // 3. 在 HTTP 响应头中塞入 X-Trace-Id 属性，方便前端或客户端拿到此 ID 配合排查问题
        response.setHeader("X-Trace-Id", traceId);
        try {
            // 4. 继续执行过滤器链，处理具体的业务请求
            chain.doFilter(request, response);
        } finally {
            // 5. 关键步骤：请求处理完成后（即使中间报错抛出异常），必须在 finally 中清理当前线程的 MDC 缓存。
            // 因为 Tomcat 线程池会复用线程，如果不清理，该线程被分给下一个新请求时，会打印错误的 traceId 导致日志链路混乱。
            MDC.remove("traceId");
        }
    }
}
