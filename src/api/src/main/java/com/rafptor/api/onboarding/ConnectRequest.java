package com.rafptor.api.onboarding;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO carrying connection parameters for remote agent deployment.
 * Credentials are stored as char[] and must never appear in logs or serialised output.
 */
public class ConnectRequest {

    /** Target hostname or IP address. */
    @NotBlank
    private String hostname;

    /** SSH port — defaults to 22. */
    private int port = 22;

    /** Remote login username. */
    @NotBlank
    private String username;

    /** Credential bytes — char[] to allow explicit wiping after use. */
    private char[] credential;

    /** Whether the credential is a plaintext password or an SSH private key. */
    private CredentialType credentialType;

    /** Target mainframe operating system family. */
    private SystemType systemType;

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    @JsonIgnore
    public char[] getCredential() { return credential; }

    @JsonProperty("credential")
    public void setCredential(char[] credential) { this.credential = credential; }

    public CredentialType getCredentialType() { return credentialType; }
    public void setCredentialType(CredentialType credentialType) { this.credentialType = credentialType; }

    public SystemType getSystemType() { return systemType; }
    public void setSystemType(SystemType systemType) { this.systemType = systemType; }

    /** Explicitly excludes credential from any string representation. */
    @Override
    public String toString() {
        return "ConnectRequest{hostname='" + hostname + "', port=" + port
                + ", username='" + username + "', credentialType=" + credentialType
                + ", systemType=" + systemType + '}';
    }

    /** Supported credential types. */
    public enum CredentialType {
        PASSWORD,
        SSH_KEY
    }

    /** Supported remote system families. */
    public enum SystemType {
        IBMI,
        ZOS
    }
}
