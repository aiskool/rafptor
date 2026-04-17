package com.rafptor.api.onboarding;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and validates short-lived agent tokens.
 * Only the SHA-256 digest of each token is persisted; the plaintext is returned
 * once to the caller and never stored.
 *
 * <p>TODO: replace SHA-256 HMAC with Ed25519 (BouncyCastle) for cryptographic
 * binding to the tenant keypair once key management is implemented.</p>
 */
@Service
public class AgentTokenService {

    private final AgentTokenRepository repository;

    public AgentTokenService(AgentTokenRepository repository) {
        this.repository = repository;
    }

    /**
     * Issues a new token tied to {@code tenantId} and {@code hostname}.
     *
     * @return the plaintext UUID v4 token — pass it directly to the agent command line
     */
    public String issue(String tenantId, String hostname) {
        String plainToken = UUID.randomUUID().toString();
        String hash = sha256Hex(plainToken);
        AgentToken doc = new AgentToken(hash, tenantId, hostname,
                Instant.now().plus(24, ChronoUnit.HOURS));
        repository.save(doc);
        return plainToken;
    }

    /**
     * Validates a submitted token by hashing it and looking up the stored digest.
     *
     * @param token plaintext token presented by the agent
     * @return the stored {@link AgentToken} if valid and not expired, empty otherwise
     */
    public Optional<AgentToken> validate(String token) {
        String hash = sha256Hex(token);
        return repository.findByTokenHash(hash)
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()));
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
