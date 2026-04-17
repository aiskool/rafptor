package com.rafptor.api.bundles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook called by the Go transport receiver when a .rpb bundle has been accepted.
 * For Phase 1 we only acknowledge the call — the pipeline kick-off lives in
 * PipelineService and will be wired in the next iteration.
 */
@RestController
@RequestMapping("/api/bundles")
public class BundleWebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(BundleWebhookController.class);

    public record BundlePayload(String bundleId, String clientId, String path, long sizeBytes) {}

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "X-Rafptor-Signature", required = false) String signature,
            @RequestBody BundlePayload payload) {
        // Signature verification lives in a dedicated service (not wired in this cut).
        LOG.info(
                "bundle_received bundle_id={} client_id={} size_bytes={} signature_present={}",
                payload.bundleId(),
                payload.clientId(),
                payload.sizeBytes(),
                signature != null);
        return ResponseEntity.accepted().build();
    }
}
