package com.rafptor.api.audit;

import com.rafptor.api.config.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditRepository repository;

    public AuditController(AuditRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<Page<AuditEvent>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(repository.findByTenantId(TenantContext.get(), PageRequest.of(page, size)));
    }
}
