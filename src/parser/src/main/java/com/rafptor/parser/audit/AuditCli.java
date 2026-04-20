package com.rafptor.parser.audit;

import com.rafptor.parser.RafptorParser;
import com.rafptor.parser.model.AfpDocument;
import com.rafptor.parser.model.AfpPage;
import com.rafptor.parser.model.OpaqueSf;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Standalone AFP audit tool.
 *
 * <pre>
 *   java -cp rafptor-parser.jar com.rafptor.parser.audit.AuditCli file.afp [--json] [--verbose]
 * </pre>
 *
 * <p>Parses the AFP file, then produces a report covering:
 *
 * <ul>
 *   <li>Byte coverage (total, used, envelope, ignored, unknown, partial)</li>
 *   <li>Structured-field inventory, grouped by status and mnemonic</li>
 *   <li>PTOCA opcode coverage per PTX block</li>
 *   <li>Pages, text runs, rules, fonts, images, graphics, resources, embedded objects</li>
 *   <li>Opaque SFs preserved for later analysis</li>
 * </ul>
 *
 * <p>The tool never prints decoded text — only structural counts and hex
 * identifiers — so it is safe to redirect output to files or CI logs.
 */
public final class AuditCli {

    private AuditCli() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: AuditCli <file.afp> [--json] [--verbose]");
            System.exit(64);
            return;
        }
        Path path = Path.of(args[0]);
        boolean json = false, verbose = false;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--json" -> json = true;
                case "--verbose", "-v" -> verbose = true;
                default -> {
                    System.err.println("unknown option: " + args[i]);
                    System.exit(64);
                }
            }
        }
        AfpDocument doc;
        try (InputStream in = Files.newInputStream(path)) {
            doc = new RafptorParser().parse(in);
        }
        if (json) {
            System.out.println(renderJson(path, doc));
        } else {
            renderText(path, doc, verbose, System.out);
        }
    }

    static void renderText(Path path, AfpDocument doc, boolean verbose, java.io.PrintStream out) {
        ByteAccountingReport r = doc.byteAccountingReport();
        out.println("========================================");
        out.printf("  RAFPTOR AFP AUDIT  %s%n", path.getFileName());
        out.println("========================================");
        out.printf("File:           %s%n", path);
        out.printf("Size:           %,d bytes%n", r.totalFileBytes());
        out.printf("Byte coverage:  %.2f%%  (used=%,d env=%,d ignored=%,d unknown=%,d partial=%,d)%n",
                r.coverage(), r.usedBytes(), r.envelopeBytes(),
                r.ignoredBytes(), r.unknownBytes(), r.partialBytes());
        out.printf("Pages:          %d%n", doc.pages().size());
        out.printf("Records:        %d%n", r.records().size());
        out.println();

        // SF inventory by mnemonic + status
        Map<String, long[]> byKey = new HashMap<>();
        for (ByteAccountant.SfRecord rec : r.records()) {
            String key = rec.mnemonic() + " (" + rec.idHex() + ") [" + rec.status() + "]";
            byKey.computeIfAbsent(key, k -> new long[]{0, 0});
            byKey.get(key)[0]++;
            byKey.get(key)[1] += rec.length();
        }
        out.println("Structured fields:");
        byKey.entrySet().stream()
                .sorted(Map.Entry.<String, long[]>comparingByValue((a, b) -> Long.compare(b[1], a[1])))
                .forEach(e -> out.printf("  %-50s count=%4d  bytes=%,10d%n",
                        e.getKey(), e.getValue()[0], e.getValue()[1]));
        out.println();

        // Opaque SFs (preserved)
        if (!doc.opaqueSfs().isEmpty()) {
            out.println("Opaque (preserved) SFs:");
            Map<String, int[]> byId = new HashMap<>();
            for (OpaqueSf o : doc.opaqueSfs()) {
                byId.computeIfAbsent(o.idHex(), k -> new int[]{0, 0});
                byId.get(o.idHex())[0]++;
                byId.get(o.idHex())[1] += o.originalLength();
            }
            byId.entrySet().stream()
                    .sorted(Map.Entry.<String, int[]>comparingByValue((a, b) -> Integer.compare(b[1], a[1])))
                    .forEach(e -> {
                        String mnemonic = SfRegistry.lookup(e.getKey())
                                .map(SfInfo::mnemonic).orElse("UNKNOWN");
                        out.printf("  %s (%s) count=%d  bytes=%d%n",
                                e.getKey(), mnemonic, e.getValue()[0], e.getValue()[1]);
                    });
            out.println();
        }

        // PTOCA opcode coverage
        if (!doc.ptocaReports().isEmpty()) {
            out.println("PTOCA opcode coverage:");
            int totalPtx = 0, ptxUsed = 0, ptxIgnored = 0, ptxUnknown = 0;
            Map<Integer, Integer> globalCounts = new HashMap<>();
            for (PtocaOpcodeReport pr : doc.ptocaReports()) {
                totalPtx += pr.totalPtxBytes();
                ptxUsed += pr.usedBytes();
                ptxIgnored += pr.ignoredBytes();
                ptxUnknown += pr.unknownBytes();
                for (Map.Entry<Integer, Integer> e : pr.opcodeCounts().entrySet()) {
                    globalCounts.merge(e.getKey(), e.getValue(), Integer::sum);
                }
            }
            double ptxCoverage = totalPtx == 0 ? 100.0 : (ptxUsed + ptxIgnored) * 100.0 / totalPtx;
            out.printf("  PTX blocks: %d   total bytes: %,d   coverage: %.2f%%%n",
                    doc.ptocaReports().size(), totalPtx, ptxCoverage);
            out.printf("  used=%,d  ignored=%,d  unknown=%,d%n", ptxUsed, ptxIgnored, ptxUnknown);
            List<Map.Entry<Integer, Integer>> opcodes = new ArrayList<>(globalCounts.entrySet());
            opcodes.sort(Comparator.<Map.Entry<Integer, Integer>>comparingInt(Map.Entry::getValue).reversed());
            out.println("  Opcodes:");
            for (Map.Entry<Integer, Integer> e : opcodes) {
                OpcodeInfo info = PtocaOpcodeRegistry.lookup(e.getKey()).orElse(OpcodeInfo.UNKNOWN);
                out.printf("    0x%02X %-6s %-32s count=%d  impl=%s%n",
                        e.getKey(), info.mnemonic(), info.description(),
                        e.getValue(), info.implemented() ? "yes" : "no");
            }
            out.println();
        }

        // Pages
        out.println("Pages:");
        int idx = 0;
        for (AfpPage p : doc.pages()) {
            out.printf("  [%d] name=%-10s runs=%d rules=%d images=%d graphics=%d%n",
                    idx++, p.name(),
                    p.textRuns().size(), p.rules().size(),
                    p.images().size(), p.graphics().size());
        }
        out.println();

        // Embedded objects
        if (!doc.embeddedObjects().isEmpty()) {
            out.println("Embedded objects:");
            for (Map.Entry<String, byte[]> e : doc.embeddedObjects().entrySet()) {
                out.printf("  %s (%s) %,d bytes%n",
                        e.getKey(), doc.embeddedObjectKind(e.getKey()), e.getValue().length);
            }
            out.println();
        }

        // Verdict
        out.println("Verdict:");
        if (r.coverage() >= 99.99) {
            out.println("  100% byte coverage — all source bytes accounted for.");
        } else {
            out.printf("  ⚠ %.2f%% byte coverage — %,d bytes unexplained.%n",
                    r.coverage(), r.unknownBytes() + r.ignoredBytes());
        }
        if (!r.bigIgnored().isEmpty()) {
            out.printf("  ⚠ %d SF(s) > 100 bytes ignored or unknown.%n", r.bigIgnored().size());
            if (verbose) {
                for (ByteAccountant.SfRecord rec : r.bigIgnored()) {
                    out.printf("    offset=%d len=%,d id=%s mnemonic=%s status=%s%n",
                            rec.offset(), rec.length(), rec.idHex(), rec.mnemonic(), rec.status());
                }
            }
        }
    }

    static String renderJson(Path path, AfpDocument doc) {
        ByteAccountingReport r = doc.byteAccountingReport();
        StringBuilder sb = new StringBuilder(1024);
        sb.append("{\n");
        sb.append("  \"file\": ").append(jsonString(path.toString())).append(",\n");
        sb.append("  \"size_bytes\": ").append(r.totalFileBytes()).append(",\n");
        sb.append(String.format(Locale.ROOT,
                "  \"byte_coverage\": %.4f,%n", r.coverage()));
        sb.append("  \"used_bytes\": ").append(r.usedBytes()).append(",\n");
        sb.append("  \"envelope_bytes\": ").append(r.envelopeBytes()).append(",\n");
        sb.append("  \"ignored_bytes\": ").append(r.ignoredBytes()).append(",\n");
        sb.append("  \"unknown_bytes\": ").append(r.unknownBytes()).append(",\n");
        sb.append("  \"partial_bytes\": ").append(r.partialBytes()).append(",\n");
        sb.append("  \"pages\": ").append(doc.pages().size()).append(",\n");
        sb.append("  \"sf_records\": ").append(r.records().size()).append(",\n");
        sb.append("  \"opaque_sfs\": ").append(doc.opaqueSfs().size()).append(",\n");
        sb.append("  \"ptx_blocks\": ").append(doc.ptocaReports().size()).append(",\n");
        sb.append("  \"embedded_objects\": ").append(doc.embeddedObjects().size()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String jsonString(String s) {
        StringBuilder b = new StringBuilder(s.length() + 2);
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        b.append('"');
        return b.toString();
    }
}
