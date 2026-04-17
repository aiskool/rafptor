package com.rafptor.api.review;

import com.rafptor.api.config.TenantContext;
import com.rafptor.api.documents.Document;
import com.rafptor.api.documents.DocumentService;
import com.rafptor.api.documents.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@PreAuthorize("hasAnyRole('REVIEWER','ADMIN')")
public class ReviewController {

    private final ReviewService reviewService;
    private final DocumentService documents;

    public ReviewController(ReviewService reviewService, DocumentService documents) {
        this.reviewService = reviewService;
        this.documents = documents;
    }

    public record CommentRequest(String comment) {}

    @GetMapping
    public ResponseEntity<Page<Document>> queue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                documents.list(TenantContext.get(), DocumentStatus.REVIEW, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> get(@PathVariable String id) {
        return documents.findOne(TenantContext.get(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ReviewDecision>> history(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.history(TenantContext.get(), id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ReviewDecision> approve(
            @PathVariable String id,
            @RequestBody(required = false) CommentRequest body,
            Authentication auth) {
        String comment = body == null ? null : body.comment();
        return ResponseEntity.ok(
                reviewService.approve(TenantContext.get(), id, auth.getName(), comment));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ReviewDecision> reject(
            @PathVariable String id,
            @RequestBody CommentRequest body,
            Authentication auth) {
        if (body == null || body.comment() == null || body.comment().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(
                reviewService.reject(TenantContext.get(), id, auth.getName(), body.comment()));
    }
}
