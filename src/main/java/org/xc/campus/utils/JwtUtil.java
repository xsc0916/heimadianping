package org.xc.campus.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT 工具：签发/解析登录态。会话本体存 Redis（可主动踢下线），JWT 只携带 userId。
 */
@Component
public class JwtUtil {

    private static Key KEY;
    private static long EXPIRE_SECONDS;

    public JwtUtil(@Value("${campus.jwt.secret}") String secret,
                   @Value("${campus.jwt.expire}") long expireSeconds) {
        KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        EXPIRE_SECONDS = expireSeconds;
    }

    /** 签发 token，subject 为 userId */
    public static String createToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + EXPIRE_SECONDS * 1000))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析 token，非法/过期返回 null（由调用方决定是否 401） */
    public static Long parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(KEY).build()
                    .parseClaimsJws(token).getBody();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}
