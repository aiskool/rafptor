package com.rafptor.api.onboarding;

import com.rafptor.api.config.AuthDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for remote agent onboarding.
 * Deployment runs asynchronously; progress is streamed via WebSocket.
 */
@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingAsyncLauncher asyncLauncher;
    private final OnboardingProgressStore progressStore;
    private final SimpMessagingTemplate messaging;

    public OnboardingController(
            OnboardingAsyncLauncher asyncLauncher,
            OnboardingProgressStore progressStore,
            SimpMessagingTemplate messaging) {
        this.asyncLauncher = asyncLauncher;
        this.progressStore = progressStore;
        this.messaging = messaging;
    }

    /**
     * Initiates an SSH agent deployment. Returns immediately; progress is
     * published to {@code /topic/onboarding/{connectionId}}.
     */
    @PostMapping("/connect")
    public ResponseEntity<Map<String, String>> connect(
            @Valid @RequestBody ConnectRequest req,
            Authentication auth) {
        String connectionId = UUID.randomUUID().toString();
        String tenantId = resolveTenantId(auth);
        asyncLauncher.deploy(req, connectionId, tenantId);
        return ResponseEntity.ok(Map.of(
                "connectionId", connectionId,
                "status", "connecting"
        ));
    }

    /**
     * Returns the last known progress event for a given connection.
     */
    @GetMapping("/{id}/progress")
    public ResponseEntity<OnboardingProgressEvent> progress(@PathVariable String id) {
        return progressStore.get(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Marks a completed deployment as ready to start migration.
     */
    @PostMapping("/start-migration")
    public ResponseEntity<Map<String, String>> startMigration(
            @RequestBody Map<String, String> body) {
        String connectionId = body.get("connectionId");
        if (connectionId == null || connectionId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        OnboardingProgressEvent event = OnboardingProgressEvent.of(
                connectionId, OnboardingState.COMPLETED, "Migration started");
        progressStore.put(connectionId, event);
        messaging.convertAndSend("/topic/onboarding/" + connectionId, event);
        return ResponseEntity.ok(Map.of("status", "migration_started", "connectionId", connectionId));
    }

    private String resolveTenantId(Authentication auth) {
        if (auth == null) return "unknown";
        if (auth.getDetails() instanceof AuthDetails details) {
            return details.tenantId();
        }
        return "unknown";
    }
}
