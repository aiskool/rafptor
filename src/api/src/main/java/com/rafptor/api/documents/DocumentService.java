package com.rafptor.api.documents;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository repository;

    public DocumentService(DocumentRepository repository) {
        this.repository = repository;
    }

    public Page<Document> list(String tenantId, DocumentStatus status, Pageable pageable) {
        if (status == null) {
            return repository.findByTenantId(tenantId, pageable);
        }
        return repository.findByTenantIdAndStatus(tenantId, status, pageable);
    }

    public Optional<Document> findOne(String tenantId, String id) {
        return repository.findByIdAndTenantId(id, tenantId);
    }

    public Document save(Document doc) {
        return repository.save(doc);
    }

    public long count(String tenantId, DocumentStatus status) {
        return repository.countByTenantIdAndStatus(tenantId, status);
    }
}
