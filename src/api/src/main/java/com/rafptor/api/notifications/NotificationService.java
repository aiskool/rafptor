package com.rafptor.api.notifications;

/**
 * Email notification contract. The default implementation is a no-op;
 * a Resend-backed implementation will be wired in Phase 3.
 */
public interface NotificationService {

    void pipelineCompleted(String recipient, String jobId, int accepted, int review, int rejected);

    void documentsNeedReview(String recipient, int count);

    void pipelineFailed(String recipient, String jobId, String summary);
}
