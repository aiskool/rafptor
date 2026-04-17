package com.rafptor.api.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    public RafptorUser register(String email, String rawPassword, String tenantId, Set<UserRole> roles) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (rawPassword == null || rawPassword.length() < 12) {
            throw new IllegalArgumentException("password must be at least 12 characters");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        String hash = encoder.encode(rawPassword);
        RafptorUser user = new RafptorUser(email, hash, tenantId, roles);
        return repository.save(user);
    }

    public Optional<RafptorUser> authenticate(String email, String rawPassword, String tenantId) {
        Optional<RafptorUser> user = repository.findByEmailAndTenantId(email, tenantId);
        if (user.isEmpty()) {
            return Optional.empty();
        }
        if (!encoder.matches(rawPassword, user.get().getPasswordHash())) {
            return Optional.empty();
        }
        user.get().setLastLogin(Instant.now());
        repository.save(user.get());
        return user;
    }

    public Optional<RafptorUser> findById(String id) {
        return repository.findById(id);
    }
}
