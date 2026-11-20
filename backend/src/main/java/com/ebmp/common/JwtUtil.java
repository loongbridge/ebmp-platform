package com.ebmp.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expire-hours:12}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600_000L;
    }

    public String generate(AuthUser user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("realName", user.getRealName())
                .claim("deptName", user.getDeptName())
                .claim("roles", user.getRoles())
                .claim("permissions", user.getPermissions())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(key)
                .compact();
    }

    @SuppressWarnings("unchecked")
    public AuthUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
        AuthUser user = new AuthUser();
        user.setId(Long.valueOf(claims.getSubject()));
        user.setUsername(claims.get("username", String.class));
        user.setRealName(claims.get("realName", String.class));
        user.setDeptName(claims.get("deptName", String.class));
        user.setRoles((List<String>) claims.get("roles", List.class));
        user.setPermissions((List<String>) claims.get("permissions", List.class));
        return user;
    }
}
