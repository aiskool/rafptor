package com.rafptor.api.metrics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies that MetricsDto serialises to the renamed JSON fields and nests
 * low-level scores under {@code technical_details}.
 */
class MetricsDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void renamedFieldsSerialiseCorrectly() throws Exception {
        DashboardMetrics internal = new DashboardMetrics(100, 80, 10, 5, 0.80, 0.92, 0.88, 0.75);
        MetricsDto dto = MetricsDto.from(internal);

        JsonNode json = mapper.readTree(mapper.writeValueAsString(dto));

        assertEquals(100, json.get("total_documents").asLong());
        assertEquals(0.80, json.get("conversion_success_rate").asDouble(), 0.001);
        assertEquals(0.92, json.get("fidelity_score").asDouble(), 0.001);
        assertEquals(10, json.get("documents_to_check").asLong());

        assertFalse(json.has("acceptanceRate"), "AFP-era camelCase must not appear");
        assertFalse(json.has("acceptance_rate"), "old acceptance_rate field must not appear");
        assertFalse(json.has("avg_composite_score"), "old avg_composite_score field must not appear");
        assertFalse(json.has("needs_review_count"), "old needs_review_count field must not appear");
    }

    @Test
    void technicalDetailsNestedCorrectly() throws Exception {
        DashboardMetrics internal = new DashboardMetrics(50, 40, 5, 3, 0.80, 0.91, 0.85, 0.78);
        MetricsDto dto = MetricsDto.from(internal);

        JsonNode json = mapper.readTree(mapper.writeValueAsString(dto));
        JsonNode tech = json.get("technical_details");

        assertNotNull(tech, "technical_details node must be present");
        assertEquals(0.91, tech.get("ssim_avg").asDouble(), 0.001);
        assertEquals(0.85, tech.get("structural_score").asDouble(), 0.001);
        assertEquals(0.78, tech.get("metadata_score").asDouble(), 0.001);

        assertFalse(json.has("ssim_avg"), "ssim_avg must not appear at top level");
        assertFalse(json.has("structural_score"), "structural_score must not appear at top level");
        assertFalse(json.has("metadata_score"), "metadata_score must not appear at top level");
    }
}
