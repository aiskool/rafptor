package com.rafptor.api.documents;

import com.rafptor.api.config.TenantContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<Document>> list(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.list(TenantContext.get(), status, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Document> get(@PathVariable String id) {
        return service.findOne(TenantContext.get(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String id) {
        return service.findOne(TenantContext.get(), id)
                .map(doc -> {
                    if (doc.getPdfPath() == null || doc.getPdfPath().isBlank()) {
                        return ResponseEntity.notFound().<Resource>build();
                    }
                    Path p = Path.of(doc.getPdfPath());
                    Resource r = new FileSystemResource(p);
                    if (!r.exists()) {
                        return ResponseEntity.notFound().<Resource>build();
                    }
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .body(r);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
