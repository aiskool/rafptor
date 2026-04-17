package com.rafptor.api.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditRepository extends MongoRepository<AuditEvent, String> {
    Page<AuditEvent> findByTenantId(String tenantId, Pageable pageable);
}
