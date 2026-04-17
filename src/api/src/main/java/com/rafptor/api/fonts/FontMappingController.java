package com.rafptor.api.fonts;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Placeholder for the font-mappings REST surface. The first real cut
 * reads the exporter JSON produced by Module 4; until then we expose an
 * empty list so the dashboard can exercise the endpoint.
 */
@RestController
@RequestMapping("/api/fonts")
public class FontMappingController {

    @GetMapping("/mappings")
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(List.of());
    }
}
