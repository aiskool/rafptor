package com.rafptor.api.metrics;

import com.rafptor.api.documents.DocumentRepository;
import com.rafptor.api.documents.DocumentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final DocumentRepository documents;

    public MetricsService(DocumentRepository documents) {
        this.documents = documents;
    }

    public DashboardMetrics overview(String tenantId) {
        long accepted = documents.countByTenantIdAndStatus(tenantId, DocumentStatus.ACCEPTED);
        long review = documents.countByTenantIdAndStatus(tenantId, DocumentStatus.REVIEW);
        long rejected = documents.countByTenantIdAndStatus(tenantId, DocumentStatus.REJECTED);
        long total = accepted + review + rejected
                + documents.countByTenantIdAndStatus(tenantId, DocumentStatus.RECEIVED)
                + documents.countByTenantIdAndStatus(tenantId, DocumentStatus.PARSED)
                + documents.countByTenantIdAndStatus(tenantId, DocumentStatus.CONVERTED)
                + documents.countByTenantIdAndStatus(tenantId, DocumentStatus.VALIDATED);
        double acceptance = total == 0 ? 0.0 : (double) accepted / (double) total;
        double avgScore = documents.findByTenantId(tenantId, Pageable.ofSize(200))
                .stream()
                .mapToDouble(d -> d.getCompositeScore())
                .average()
                .orElse(0.0);
        return new DashboardMetrics(total, accepted, review, rejected, acceptance, avgScore);
    }
}
