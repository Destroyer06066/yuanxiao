package com.campus.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.access-token-validity-seconds:7200}")
    private long accessTokenValiditySeconds;

    // 生产环境应使用 RSA 密钥对，这里先用简单密钥（开发/测试用）
    private static final String DEV_SECRET = "campus-platform-jwt-secret-key-for-dev-only-32bytes!";
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        // 开发/测试环境使用对称密钥
        this.secretKey = Keys.hmacShaKeyFor(DEV_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(AccountPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenValiditySeconds);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(principal.getAccountId().toString())
                .claim("role", principal.getRole())
                .claim("schoolId", principal.getSchoolId() != null ? principal.getSchoolId().toString() : null)
                .claim("realName", principal.getRealName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public AccountPrincipal parseToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new AccountPrincipal(
                    UUID.fromString(claims.getSubject()),
                    claims.get("role", String.class),
                    claims.get("schoolId", String.class) != null
                            ? UUID.fromString(claims.get("schoolId", String.class)) : null,
                    claims.get("realName", String.class),
                    claims.getId()
            );
        } catch (JwtException e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
