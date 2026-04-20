package com.rafptor.converter.transform;

import com.rafptor.converter.ir.IrMetadata;
import com.rafptor.parser.audit.ByteAccountingReport;
import com.rafptor.parser.model.AfpDocument;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MetadataExtractor {

    public IrMetadata extract(AfpDocument afp) {
        if (afp == null) {
            return new IrMetadata("", 0, java.util.Map.of());
        }
        Map<String, String> tags = new LinkedHashMap<>(afp.tags());
        ByteAccountingReport report = afp.byteAccountingReport();
        if (report != null) {
            // Expose parse diagnostics as PDF custom metadata. These keys are
            // Rafptor-specific (prefixed) and are safe to show operators that
            // need to audit whether every source byte was accounted for.
            tags.put("RafptorByteCoverage",
                    String.format("%.2f%%", report.coverage()));
            tags.put("RafptorUsedBytes", Long.toString(report.usedBytes()));
            tags.put("RafptorUnknownBytes", Long.toString(report.unknownBytes()));
            tags.put("RafptorIgnoredBytes", Long.toString(report.ignoredBytes()));
            tags.put("RafptorSfCount", Integer.toString(report.records().size()));
            if (report.coverage() < 100.0) {
                tags.put("RafptorWarnings",
                        String.format("low-byte-coverage (%.2f%%); %d opaque-SFs",
                                report.coverage(), afp.opaqueSfs().size()));
            }
        }
        return new IrMetadata(afp.name(), afp.pages().size(), tags);
    }
}
