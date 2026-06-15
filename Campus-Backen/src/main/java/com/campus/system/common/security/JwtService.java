package com.campus.system.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// JWT核心服务类
@Service
public class JwtService {

    // 从配置文件(application.yml)读取 JWT 签名的密钥。密钥必须足够长并且保密。
    @Value("${campus.jwt.secret}")
    private String secret;

    // token 有效期（天）
    @Value("${campus.jwt.ttl-days}")
    private long ttlDays;

    /**
     * 创建一个 JWT 字符串
     * @param sessionId 我们自己生成的唯一会话标识，存放在 Redis 中的 Key
     * @return 生成好的 JWT 字符串
     */
    public String create(String sessionId) {
        Date now = new Date();
        return Jwts.builder()
            // token 的主体（Subject）存放的就是我们的 sessionId
            .subject(sessionId)
            // 签发时间
            .issuedAt(now)
            // 过期时间 = 当前时间 + TTL（有效时长）
            .expiration(new Date(now.getTime() + ttl().toMillis()))
            // 签名，使用 HMAC SHA 算法，防止客户端伪造 token
            .signWith(key())
            .compact();
    }

    /**
     * 解析前端传来的 JWT 字符串
     * @param token 前端放在 Authorization 请求头里面的字符串
     * @return 解析出来的 sessionId。如果 token 伪造或者过期，会在这里抛出异常
     */
    public String parse(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload().getSubject();
    }

    //获取配置的过期时间（转换为 Duration 对象）
    public Duration ttl() {
        return Duration.ofDays(ttlDays);
    }

    // 根据配置的 Secret 字符串生成 JWT 加密用的安全密钥
    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}