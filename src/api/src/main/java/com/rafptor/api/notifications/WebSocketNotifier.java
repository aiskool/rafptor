package com.rafptor.api.notifications;

import com.rafptor.api.pipeline.PipelineStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WebSocketNotifier {

    private final SimpMessagingTemplate template;

    public WebSocketNotifier(SimpMessagingTemplate template) {
        this.template = template;
    }

    public void pipelineStarted(String jobId, String tenantId) {
        template.convertAndSend("/topic/pipelines/" + jobId, Map.of(
                "event", "PIPELINE_STARTED",
                "job_id", jobId,
                "tenant_id", tenantId
        ));
    }

    public void pipelineProgress(String jobId, String tenantId, int progress, PipelineStatus status) {
        template.convertAndSend("/topic/pipelines/" + jobId, Map.of(
                "event", "PIPELINE_PROGRESS",
                "job_id", jobId,
                "tenant_id", tenantId,
                "progress", progress,
                "status", status.name()
        ));
    }

    public void pipelineCompleted(String jobId, String tenantId, PipelineStatus status) {
        template.convertAndSend("/topic/pipelines/" + jobId, Map.of(
                "event", "PIPELINE_COMPLETED",
                "job_id", jobId,
                "tenant_id", tenantId,
                "status", status.name()
        ));
    }

    public void documentReview(String documentId, String tenantId) {
        template.convertAndSend("/topic/reviews", Map.of(
                "event", "DOCUMENT_REVIEW",
                "document_id", documentId,
                "tenant_id", tenantId
        ));
    }
}
