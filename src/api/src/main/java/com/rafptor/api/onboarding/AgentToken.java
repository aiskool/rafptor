package com.rafptor.api.onboarding;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document storing the SHA-256 hash of an issued agent token with a 24-hour TTL.
 */
@Document(collection = "agentTokens")
public class AgentToken {

    @Id
    private String id;

    /** SHA-256 hex digest of the plain token — the plain token is never persisted. */
    @Indexed(unique = true)
    private String tokenHash;

    private String tenantId;

    private String hostname;

    /** TTL field: MongoDB will delete the document after this instant. */
    @Indexed(expireAfterSeconds = 0)
    private Instant expiresAt;

    private Instant createdAt = Instant.now();

    public AgentToken() {}

    public AgentToken(String tokenHash, String tenantId, String hostname, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.tenantId = tenantId;
        this.hostname = hostname;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
