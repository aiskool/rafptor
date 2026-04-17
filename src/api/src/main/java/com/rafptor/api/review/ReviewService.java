package com.rafptor.api.review;

import com.rafptor.api.documents.Document;
import com.rafptor.api.documents.DocumentService;
import com.rafptor.api.documents.DocumentStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository repository;
    private final DocumentService documents;

    public ReviewService(ReviewRepository repository, DocumentService documents) {
        this.repository = repository;
        this.documents = documents;
    }

    public ReviewDecision approve(String tenantId, String documentId, String reviewerId, String comment) {
        Document doc = documents.findOne(tenantId, documentId)
                .orElseThrow(() -> new IllegalArgumentException("document not found"));
        doc.setStatus(DocumentStatus.ACCEPTED);
        doc.setUpdatedAt(Instant.now());
        documents.save(doc);
        return repository.save(new ReviewDecision(
                tenantId, documentId, reviewerId, ReviewDecision.Outcome.APPROVED, comment));
    }

    public ReviewDecision reject(String tenantId, String documentId, String reviewerId, String comment) {
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("a comment is required when rejecting");
        }
        Document doc = documents.findOne(tenantId, documentId)
                .orElseThrow(() -> new IllegalArgumentException("document not found"));
        doc.setStatus(DocumentStatus.REJECTED);
        doc.setUpdatedAt(Instant.now());
        documents.save(doc);
        return repository.save(new ReviewDecision(
                tenantId, documentId, reviewerId, ReviewDecision.Outcome.REJECTED, comment));
    }

    public List<ReviewDecision> history(String tenantId, String documentId) {
        return repository.findByTenantIdAndDocumentId(tenantId, documentId);
    }
}
