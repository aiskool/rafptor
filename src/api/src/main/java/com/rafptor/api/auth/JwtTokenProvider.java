package com.rafptor.api.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    static final int MIN_SECRET_BYTES = 32;

    private final String secretRaw;
    private SecretKey signingKey;

    public JwtTokenProvider(@Value("${rafptor.security.jwt-secret:}") String secretRaw) {
        this.secretRaw = secretRaw;
    }

    @PostConstruct
    void init() {
        byte[] bytes = secretRaw.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "rafptor.security.jwt-secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(bytes);
    }

    public String generateAccessToken(RafptorUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("tenant_id", user.getTenantId())
                .claim("roles", user.getRoles().stream().map(Enum::name).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ACCESS_TOKEN_TTL)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("type", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(REFRESH_TOKEN_TTL)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        Object raw = claims.get("roles");
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
