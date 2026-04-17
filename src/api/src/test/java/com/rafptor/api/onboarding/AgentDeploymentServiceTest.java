package com.rafptor.api.onboarding;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.scp.server.ScpCommandFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test: spins up an in-process Apache SSHD server, deploys against it,
 * and verifies credential wipe behaviour.
 */
class AgentDeploymentServiceTest {

    @TempDir
    Path tempDir;

    private SshServer sshd;
    private int sshPort;

    @BeforeEach
    void startSshServer() throws Exception {
        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(0);
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(
                tempDir.resolve("host.ser")));
        sshd.setPasswordAuthenticator((username, password, session) ->
                "testuser".equals(username) && "SuperSecret123".equals(password));
        sshd.setCommandFactory(new ScpCommandFactory.Builder()
                .withDelegate((channelSession, cmd) -> new NoopCommand())
                .build());
        sshd.setFileSystemFactory(new VirtualFileSystemFactory(tempDir));
        sshd.start();
        sshPort = sshd.getPort();
    }

    @AfterEach
    void stopSshServer() throws Exception {
        if (sshd != null && sshd.isOpen()) {
            sshd.stop(true);
        }
    }

    @Test
    void credentialsAreZeroedAfterDeploy() {
        char[] password = "SuperSecret123".toCharArray();

        ConnectRequest req = new ConnectRequest();
        req.setHostname("127.0.0.1");
        req.setPort(sshPort);
        req.setUsername("testuser");
        req.setCredential(password);
        req.setCredentialType(ConnectRequest.CredentialType.PASSWORD);
        req.setSystemType(ConnectRequest.SystemType.IBMI);

        AgentDeploymentService service = buildService();
        service.deploy(req, "test-conn-1", "tenant-test");

        char[] expected = new char[14];
        java.util.Arrays.fill(expected, (char) 0);
        assertArrayEquals(expected, password, "Credential array must be zeroed after deploy");
    }

    @Test
    void deployPublishesProgressEvents() {
        char[] password = "SuperSecret123".toCharArray();

        ConnectRequest req = new ConnectRequest();
        req.setHostname("127.0.0.1");
        req.setPort(sshPort);
        req.setUsername("testuser");
        req.setCredential(password);
        req.setCredentialType(ConnectRequest.CredentialType.PASSWORD);
        req.setSystemType(ConnectRequest.SystemType.IBMI);

        OnboardingProgressStore store = new OnboardingProgressStore();
        AgentDeploymentService service = buildService(store);
        service.deploy(req, "test-conn-2", "tenant-test");

        Optional<OnboardingProgressEvent> last = store.get("test-conn-2");
        assertTrue(last.isPresent(), "At least one progress event must be stored");
        assertNotNull(last.get().state());
    }

    private AgentDeploymentService buildService() {
        return buildService(new OnboardingProgressStore());
    }

    private AgentDeploymentService buildService(OnboardingProgressStore store) {
        AgentTokenRepository tokenRepo = mock(AgentTokenRepository.class);
        when(tokenRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        HostKeyRepository hostKeyRepo = mock(HostKeyRepository.class);
        when(hostKeyRepo.findByHostname(anyString())).thenReturn(Optional.empty());
        when(hostKeyRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        AgentTokenService tokenService = new AgentTokenService(tokenRepo);
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);

        return new AgentDeploymentService(tokenService, hostKeyRepo, store, messaging);
    }

    /** Shell command that does nothing — replaces real remote execution in tests. */
    private static class NoopCommand implements org.apache.sshd.server.command.Command {
        private org.apache.sshd.server.ExitCallback callback;

        @Override
        public void setInputStream(java.io.InputStream in) {}

        @Override
        public void setOutputStream(java.io.OutputStream out) {}

        @Override
        public void setErrorStream(java.io.OutputStream err) {}

        @Override
        public void setExitCallback(org.apache.sshd.server.ExitCallback c) {
            this.callback = c;
        }

        @Override
        public void start(org.apache.sshd.server.channel.ChannelSession channel,
                          org.apache.sshd.server.Environment env) {
            if (callback != null) callback.onExit(0);
        }

        @Override
        public void destroy(org.apache.sshd.server.channel.ChannelSession channel) {}
    }
}
