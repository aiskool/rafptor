package com.rafptor.api.pipeline;

import com.rafptor.api.notifications.WebSocketNotifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class PipelineService {

    private final PipelineRepository repository;
    private final WebSocketNotifier notifier;

    public PipelineService(PipelineRepository repository, WebSocketNotifier notifier) {
        this.repository = repository;
        this.notifier = notifier;
    }

    public PipelineJob create(String tenantId, String userId) {
        PipelineJob job = new PipelineJob();
        job.setTenantId(tenantId);
        job.setCreatedBy(userId);
        PipelineJob saved = repository.save(job);
        notifier.pipelineStarted(saved.getId(), tenantId);
        return saved;
    }

    public Page<PipelineJob> list(String tenantId, Pageable pageable) {
        return repository.findByTenantId(tenantId, pageable);
    }

    public Optional<PipelineJob> findOne(String tenantId, String id) {
        return repository.findByIdAndTenantId(id, tenantId);
    }

    public Optional<PipelineJob> cancel(String tenantId, String id) {
        return repository.findByIdAndTenantId(id, tenantId).map(job -> {
            if (job.getStatus() != PipelineStatus.DONE && job.getStatus() != PipelineStatus.FAILED) {
                job.setStatus(PipelineStatus.CANCELLED);
                job.setFinishedAt(Instant.now());
                repository.save(job);
                notifier.pipelineCompleted(job.getId(), tenantId, job.getStatus());
            }
            return job;
        });
    }

    public PipelineJob updateProgress(String tenantId, String id, int progress, PipelineStatus status) {
        PipelineJob job = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("job not found: " + id));
        job.setProgress(Math.max(0, Math.min(100, progress)));
        job.setStatus(status);
        if (status == PipelineStatus.DONE || status == PipelineStatus.FAILED) {
            job.setFinishedAt(Instant.now());
        }
        PipelineJob saved = repository.save(job);
        notifier.pipelineProgress(saved.getId(), tenantId, saved.getProgress(), saved.getStatus());
        return saved;
    }
}
