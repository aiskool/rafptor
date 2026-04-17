package com.rafptor.api.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider("test-secret-minimum-32-bytes-long-xxxxxxxxxxxxxxx");
        ReflectionTestUtils.invokeMethod(provider, "init");
    }

    @Test
    void rejectsTooShortSecret() {
        JwtTokenProvider bad = new JwtTokenProvider("short");
        assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(bad, "init"));
    }

    @Test
    void generatesAndParsesAccessToken() {
        RafptorUser user = new RafptorUser("a@b.com", "hash", "tenant-1", Set.of(UserRole.VIEWER));
        user.setId("u1");
        String token = provider.generateAccessToken(user);
        assertNotNull(token);
        Claims claims = provider.parseClaims(token);
        assertEquals("u1", claims.getSubject());
        assertEquals("tenant-1", claims.get("tenant_id", String.class));
        assertTrue(provider.extractRoles(claims).contains("VIEWER"));
    }

    @Test
    void refreshTokenCarriesType() {
        String token = provider.generateRefreshToken("u1");
        Claims claims = provider.parseClaims(token);
        assertEquals("refresh", claims.get("type", String.class));
        assertEquals("u1", claims.getSubject());
    }
}
