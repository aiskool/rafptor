package com.rafptor.api.auth;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "users")
public class RafptorUser {

    @Id
    private String id;

    @Indexed
    private String email;

    private String passwordHash;

    @Indexed
    private String tenantId;

    private Set<UserRole> roles = new HashSet<>();

    private Instant createdAt = Instant.now();

    private Instant lastLogin;

    public RafptorUser() {
    }

    public RafptorUser(String email, String passwordHash, String tenantId, Set<UserRole> roles) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.tenantId = tenantId;
        this.roles = new HashSet<>(roles);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public Set<UserRole> getRoles() { return roles; }
    public void setRoles(Set<UserRole> roles) { this.roles = roles; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastLogin() { return lastLogin; }
    public void setLastLogin(Instant lastLogin) { this.lastLogin = lastLogin; }
}
