package com.rafptor.api.onboarding;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Proves that no log event at any level exposes the plaintext credential.
 */
class CredentialWipeTest {

    private static final String SECRET = "SuperSecret123";

    private CapturingAppender appender;
    private Logger rootLogger;

    @BeforeEach
    void attachAppender() {
        appender = new CapturingAppender();
        appender.start();
        rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(appender);
        rootLogger.setLevel(Level.TRACE);
    }

    @AfterEach
    void detachAppender() {
        rootLogger.detachAppender(appender);
    }

    @Test
    void credentialNeverAppearsInLogs() {
        char[] password = SECRET.toCharArray();

        ConnectRequest req = new ConnectRequest();
        req.setHostname("127.0.0.1");
        req.setPort(9999);  // no server — connection will fail immediately
        req.setUsername("user");
        req.setCredential(password);
        req.setCredentialType(ConnectRequest.CredentialType.PASSWORD);
        req.setSystemType(ConnectRequest.SystemType.IBMI);

        AgentTokenRepository tokenRepo = mock(AgentTokenRepository.class);
        when(tokenRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        HostKeyRepository hostKeyRepo = mock(HostKeyRepository.class);
        when(hostKeyRepo.findByHostname(anyString())).thenReturn(Optional.empty());
        when(hostKeyRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        AgentTokenService tokenService = new AgentTokenService(tokenRepo);
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        OnboardingProgressStore store = new OnboardingProgressStore();
        AgentDeploymentService service = new AgentDeploymentService(
                tokenService, hostKeyRepo, store, messaging);

        service.deploy(req, "wipe-test-conn", "tenant-test");

        for (ILoggingEvent event : appender.events) {
            String msg = event.getFormattedMessage();
            assertTrue(!msg.contains(SECRET),
                    "Log event must not contain plaintext credential. Offending message: " + msg);
        }
    }

    /** Logback appender that captures all events for inspection. */
    private static class CapturingAppender extends AppenderBase<ILoggingEvent> {

        final List<ILoggingEvent> events = new ArrayList<>();

        @Override
        protected void append(ILoggingEvent event) {
            events.add(event);
        }
    }
}
