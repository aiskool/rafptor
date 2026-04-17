package com.rafptor.api.auth;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtTokenProvider tokenProvider;

    public RefreshTokenService(RefreshTokenRepository repository, JwtTokenProvider tokenProvider) {
        this.repository = repository;
        this.tokenProvider = tokenProvider;
    }

    public String issue(RafptorUser user) {
        String token = tokenProvider.generateRefreshToken(user.getId());
        Instant expiresAt = Instant.now().plus(JwtTokenProvider.REFRESH_TOKEN_TTL);
        RefreshToken doc = new RefreshToken(token, user.getId(), user.getTenantId(), expiresAt);
        repository.save(doc);
        return token;
    }

    public Optional<RefreshToken> validate(String token) {
        Optional<RefreshToken> stored = repository.findByToken(token);
        if (stored.isEmpty()) {
            return Optional.empty();
        }
        RefreshToken rt = stored.get();
        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return stored;
    }

    public void revoke(String token) {
        repository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            repository.save(rt);
        });
    }

    public void revokeAllForUser(String userId) {
        repository.deleteByUserId(userId);
    }
}
