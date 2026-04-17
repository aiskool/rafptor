package com.rafptor.api.notifications;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default notification service: logs the intent without sending anything.
 * Replaced by a Resend-backed implementation via
 * Spring's {@code @Primary} once credentials are provisioned (Phase 3).
 */
@Component
public class NoopNotificationService implements NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NoopNotificationService.class);

    @Override
    public void pipelineCompleted(String recipient, String jobId, int accepted, int review, int rejected) {
        LOG.info("notif pipeline_completed recipient={} job={} accepted={} review={} rejected={}",
                recipient, jobId, accepted, review, rejected);
    }

    @Override
    public void documentsNeedReview(String recipient, int count) {
        LOG.info("notif documents_need_review recipient={} count={}", recipient, count);
    }

    @Override
    public void pipelineFailed(String recipient, String jobId, String summary) {
        LOG.info("notif pipeline_failed recipient={} job={} summary_len={}",
                recipient, jobId, summary == null ? 0 : summary.length());
    }
}
