package com.rafptor.api.review;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReviewRepository extends MongoRepository<ReviewDecision, String> {
    List<ReviewDecision> findByTenantIdAndDocumentId(String tenantId, String documentId);
}
