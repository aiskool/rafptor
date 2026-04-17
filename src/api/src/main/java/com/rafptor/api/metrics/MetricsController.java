package com.rafptor.api.metrics;

import com.rafptor.api.config.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService service;

    public MetricsController(MetricsService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public ResponseEntity<DashboardMetrics> overview() {
        return ResponseEntity.ok(service.overview(TenantContext.get()));
    }
}
