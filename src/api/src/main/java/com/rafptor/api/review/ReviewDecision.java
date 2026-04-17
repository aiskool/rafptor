package com.rafptor.api.review;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "review_decisions")
public class ReviewDecision {

    public enum Outcome { APPROVED, REJECTED }

    @Id private String id;
    @Indexed private String tenantId;
    @Indexed private String documentId;
    private String reviewerId;
    private Outcome outcome;
    private String comment;
    private Instant decidedAt = Instant.now();

    public ReviewDecision() {}

    public ReviewDecision(String tenantId, String documentId, String reviewerId, Outcome outcome, String comment) {
        this.tenantId = tenantId;
        this.documentId = documentId;
        this.reviewerId = reviewerId;
        this.outcome = outcome;
        this.comment = comment;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
}
