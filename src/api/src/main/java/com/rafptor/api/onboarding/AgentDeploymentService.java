package com.rafptor.api.onboarding;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.scp.client.ScpClient;
import org.apache.sshd.scp.client.ScpClientCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * Deploys the Rafptor agent binary to a remote host over SSH/SCP using a TOFU host-key model.
 */
@Service
public class AgentDeploymentService {

    private static final Logger log = LoggerFactory.getLogger(AgentDeploymentService.class);

    private static final long SSH_TIMEOUT_MS = 10_000L;
    private static final String REMOTE_AGENT_PATH = "/tmp/rafptor-agent";

    private final AgentTokenService tokenService;
    private final HostKeyRepository hostKeyRepository;
    private final OnboardingProgressStore progressStore;
    private final SimpMessagingTemplate messaging;

    @Value("${rafptor.api.base-url:http://localhost:8080}")
    private String apiBaseUrl = "http://localhost:8080";

    public AgentDeploymentService(
            AgentTokenService tokenService,
            HostKeyRepository hostKeyRepository,
            OnboardingProgressStore progressStore,
            SimpMessagingTemplate messaging) {
        this.tokenService = tokenService;
        this.hostKeyRepository = hostKeyRepository;
        this.progressStore = progressStore;
        this.messaging = messaging;
    }

    /**
     * Opens an SSH connection, uploads the agent binary, and starts it remotely.
     * Progress events are published to {@code /topic/onboarding/{connectionId}} at each step.
     * Credentials are zeroed in a {@code finally} block regardless of outcome.
     *
     * @param req          validated connection parameters (credential will be wiped on return)
     * @param connectionId unique deployment identifier used for WebSocket routing
     * @param tenantId     owning tenant — used when issuing the agent token
     */
    public void deploy(ConnectRequest req, String connectionId, String tenantId) {
        SshClient client = SshClient.setUpDefaultClient();
        ClientSession session = null;
        try {
            emit(connectionId, OnboardingState.CONNECTING, "Connecting to " + req.getHostname());

            client.setServerKeyVerifier((sshClientSession, remoteAddress, serverKey) -> {
                String fingerprint = KeyUtils.getFingerPrint(serverKey);
                String hostname = req.getHostname();
                return verifyHostKey(hostname, req.getPort(), fingerprint);
            });
            client.start();

            session = client.connect(req.getUsername(), req.getHostname(), req.getPort())
                    .verify(SSH_TIMEOUT_MS)
                    .getSession();

            emit(connectionId, OnboardingState.AUTHENTICATING, "Authenticating as " + req.getUsername());

            authenticateSession(session, req);
            session.auth().verify(SSH_TIMEOUT_MS);

            emit(connectionId, OnboardingState.DEPLOYING, "Uploading agent binary");

            Path localBinary = resolveLocalBinary(req.getSystemType());
            ScpClientCreator.instance()
                    .createScpClient(session)
                    .upload(localBinary, REMOTE_AGENT_PATH, ScpClient.Option.PreserveAttributes);

            session.executeRemoteCommand("chmod +x " + REMOTE_AGENT_PATH);

            emit(connectionId, OnboardingState.STARTING, "Starting remote agent");

            String token = tokenService.issue(tenantId, req.getHostname());
            String launchCmd = "nohup " + REMOTE_AGENT_PATH
                    + " --token " + token
                    + " --server " + apiBaseUrl
                    + " > /tmp/rafptor-agent.log 2>&1 &";
            session.executeRemoteCommand(launchCmd);

            emit(connectionId, OnboardingState.SCANNING, "Agent started — awaiting initial scan");

        } catch (Exception e) {
            String userMessage = "Deployment failed: " + sanitiseMessage(e.getMessage());
            log.error("Agent deployment error for connection {}: {}", connectionId, e.getMessage());
            emit(connectionId, OnboardingState.ERROR, userMessage);
        } finally {
            if (req.getCredential() != null) {
                Arrays.fill(req.getCredential(), (char) 0);
            }
            if (session != null) {
                try { session.close(); } catch (IOException ignored) {}
            }
            client.stop();
        }
    }

    private void authenticateSession(ClientSession session, ConnectRequest req) throws IOException {
        if (req.getCredentialType() == ConnectRequest.CredentialType.PASSWORD) {
            session.addPasswordIdentity(new String(req.getCredential()));
        } else {
            try {
                Path keyPath = Path.of(new String(req.getCredential()));
                var keyPairs = org.apache.sshd.common.util.security.SecurityUtils
                        .getKeyPairResourceParser()
                        .loadKeyPairs(null, keyPath, null);
                if (keyPairs != null) {
                    keyPairs.forEach(session::addPublicKeyIdentity);
                }
            } catch (Exception e) {
                throw new IOException("Failed to load SSH key", e);
            }
        }
    }

    private boolean verifyHostKey(String hostname, int port, String fingerprint) {
        return hostKeyRepository.findByHostname(hostname)
                .map(stored -> {
                    if (stored.getFingerprint().equals(fingerprint)) {
                        return true;
                    }
                    log.warn("Host key mismatch for {} — expected {} but got {}",
                            hostname, stored.getFingerprint(), fingerprint);
                    return false;
                })
                .orElseGet(() -> {
                    log.info("TOFU: trusting new host key for {}:{}", hostname, port);
                    hostKeyRepository.save(new HostKey(hostname, port, fingerprint));
                    return true;
                });
    }

    /**
     * Resolves the path to the agent binary on the local classpath.
     * Falls back to the linux-amd64 stub for local integration tests.
     */
    Path resolveLocalBinary(ConnectRequest.SystemType systemType) throws IOException {
        String name = binaryName(systemType);
        URL resource = getClass().getClassLoader().getResource("agents/" + name);
        if (resource == null) {
            resource = getClass().getClassLoader().getResource("agents/rafptor-agent-linux-amd64");
        }
        if (resource == null) {
            throw new IOException("Agent binary not found in classpath: agents/" + name);
        }
        Path tmp = Files.createTempFile("rafptor-agent-", "");
        try (InputStream in = resource.openStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        tmp.toFile().setExecutable(true);
        return tmp;
    }

    private static String binaryName(ConnectRequest.SystemType systemType) {
        if (systemType == null) {
            return "rafptor-agent-linux-amd64";
        }
        return switch (systemType) {
            case IBMI -> "rafptor-agent-ibmi-ppc64";
            case ZOS -> "rafptor-agent-zos-s390x";
        };
    }

    private void emit(String connectionId, OnboardingState state, String message) {
        OnboardingProgressEvent event = OnboardingProgressEvent.of(connectionId, state, message);
        progressStore.put(connectionId, event);
        messaging.convertAndSend("/topic/onboarding/" + connectionId, event);
    }

    private static String sanitiseMessage(String raw) {
        if (raw == null) return "unknown error";
        return raw.replaceAll("(?i)(password|credential|token|secret)[^\\s]*", "[REDACTED]");
    }
}
