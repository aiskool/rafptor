package com.rafptor.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshService;

    public AuthController(
            UserService userService,
            JwtTokenProvider tokenProvider,
            RefreshTokenService refreshService) {
        this.userService = userService;
        this.tokenProvider = tokenProvider;
        this.refreshService = refreshService;
    }

    public record LoginRequest(@Email String email, @NotBlank String password, @NotBlank String tenantId) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record TokenPair(String accessToken, String refreshToken) {}

    @PostMapping("/login")
    public ResponseEntity<TokenPair> login(@RequestBody LoginRequest req) {
        return userService.authenticate(req.email(), req.password(), req.tenantId())
                .map(user -> {
                    String access = tokenProvider.generateAccessToken(user);
                    String refresh = refreshService.issue(user);
                    return ResponseEntity.ok(new TokenPair(access, refresh));
                })
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@RequestBody RefreshRequest req) {
        return refreshService.validate(req.refreshToken())
                .flatMap(rt -> userService.findById(rt.getUserId()))
                .map(user -> {
                    String access = tokenProvider.generateAccessToken(user);
                    return ResponseEntity.ok(new TokenPair(access, req.refreshToken()));
                })
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest req) {
        refreshService.revoke(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).build();
        }
        String userId = auth.getName();
        return userService.findById(userId)
                .map(user -> ResponseEntity.ok(Map.<String, Object>of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "tenantId", user.getTenantId(),
                        "roles", user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                        "lastLogin", user.getLastLogin() == null ? Instant.EPOCH : user.getLastLogin()
                )))
                .orElseGet(() -> ResponseEntity.status(404).build());
    }
}
