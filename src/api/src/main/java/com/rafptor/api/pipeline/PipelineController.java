package com.rafptor.api.pipeline;

import com.rafptor.api.config.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pipelines")
public class PipelineController {

    private final PipelineService service;

    public PipelineController(PipelineService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PipelineJob> create(Authentication auth) {
        String tenant = TenantContext.get();
        return ResponseEntity.ok(service.create(tenant, auth.getName()));
    }

    @GetMapping
    public ResponseEntity<Page<PipelineJob>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.list(TenantContext.get(), PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PipelineJob> get(@PathVariable String id) {
        return service.findOne(TenantContext.get(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PipelineJob> cancel(@PathVariable String id) {
        return service.cancel(TenantContext.get(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
