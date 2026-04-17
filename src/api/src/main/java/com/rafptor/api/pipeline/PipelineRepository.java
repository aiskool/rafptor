package com.rafptor.api.pipeline;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PipelineRepository extends MongoRepository<PipelineJob, String> {
    Page<PipelineJob> findByTenantId(String tenantId, Pageable pageable);
    Optional<PipelineJob> findByIdAndTenantId(String id, String tenantId);
}
