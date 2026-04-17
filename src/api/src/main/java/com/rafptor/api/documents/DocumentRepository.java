package com.rafptor.api.documents;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DocumentRepository extends MongoRepository<Document, String> {
    Page<Document> findByTenantId(String tenantId, Pageable pageable);
    Page<Document> findByTenantIdAndStatus(String tenantId, DocumentStatus status, Pageable pageable);
    Optional<Document> findByIdAndTenantId(String id, String tenantId);
    long countByTenantIdAndStatus(String tenantId, DocumentStatus status);
}
