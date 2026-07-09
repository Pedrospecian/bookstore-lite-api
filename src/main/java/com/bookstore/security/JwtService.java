package com.bookstore.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(String userId, String role) {
        return buildToken(userId, role, accessTokenExpirationMs, "access");
    }

    public String generateRefreshToken(String userId) {
        return buildToken(userId, null, refreshTokenExpirationMs, "refresh");
    }

    private String buildToken(String subject, String role, long expirationMs, String tokenType) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(subject)
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    /**
     * Retorna as claims do token se ele for válido, ou lança JwtException/IllegalArgumentException.
     */
    public Claims parseClaims(String token) throws JwtException, IllegalArgumentException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type", String.class));
    }
}
