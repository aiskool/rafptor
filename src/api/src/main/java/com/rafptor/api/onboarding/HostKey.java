package com.rafptor.api.onboarding;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document persisting a known SSH host key fingerprint (TOFU model).
 */
@Document(collection = "hostKeys")
public class HostKey {

    @Id
    private String id;

    @Indexed(unique = true)
    private String hostname;

    private int port;

    /** SHA-256 fingerprint as reported by Apache SSHD. */
    private String fingerprint;

    private Instant firstSeenAt = Instant.now();

    public HostKey() {}

    public HostKey(String hostname, int port, String fingerprint) {
        this.hostname = hostname;
        this.port = port;
        this.fingerprint = fingerprint;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public Instant getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(Instant firstSeenAt) { this.firstSeenAt = firstSeenAt; }
}
