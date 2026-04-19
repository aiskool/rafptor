package com.rafptor.parser;

import com.rafptor.parser.model.AfpDocument;
import com.rafptor.parser.model.AfpPage;
import com.rafptor.parser.ptoca.PtocaTextRun;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class E2EParseTest {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: E2EParseTest <afp-streams-dir> [--emit-text-json <out-dir>]");
            System.exit(1);
        }
        Path streamsDir = Path.of(args[0]);
        if (!Files.isDirectory(streamsDir)) {
            System.err.println("Not a directory: " + streamsDir);
            System.exit(1);
        }

        Path textOut = null;
        for (int i = 1; i < args.length - 1; i++) {
            if ("--emit-text-json".equals(args[i])) {
                textOut = Path.of(args[i + 1]);
            }
        }
        if (textOut != null) {
            Files.createDirectories(textOut);
        }

        RafptorParser parser = new RafptorParser();
        int ok = 0, ko = 0;
        try (Stream<Path> files = Files.list(streamsDir)) {
            for (Path afp : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".afp")).sorted()::iterator) {
                System.out.println();
                System.out.println("=== Parsing: " + afp.getFileName() + " ===");
                try (InputStream in = Files.newInputStream(afp)) {
                    AfpDocument doc = parser.parse(in);
                    System.out.println("  Document:   " + doc.name());
                    System.out.println("  Pages:      " + doc.pages().size());
                    System.out.println("  Resources:  " + doc.resourceReferences().size());
                    if (!doc.tags().isEmpty()) {
                        System.out.println("  Tags (TLE):");
                        doc.tags().forEach((k, v) -> System.out.println("    " + k + " = " + v));
                    }
                    int i = 0;
                    for (AfpPage page : doc.pages()) {
                        i++;
                        int textChars = 0;
                        for (PtocaTextRun r : page.textRuns()) {
                            textChars += r.text() == null ? 0 : r.text().length();
                        }
                        System.out.println("  Page " + i + " (" + page.name() + "): "
                                + page.textRuns().size() + " text runs, "
                                + textChars + " chars, "
                                + page.resourceReferences().size() + " res refs");
                    }
                    if (textOut != null) {
                        writeTextJson(textOut, afp, doc);
                    }
                    System.out.println("  SUCCESS");
                    ok++;
                } catch (Exception e) {
                    System.out.println("  FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    ko++;
                }
            }
        }
        System.out.println();
        System.out.println("=== Parse summary: " + ok + " ok, " + ko + " failed ===");
        if (ko > 0) System.exit(2);
    }

    /**
     * Serialise the concatenated PTOCA text of every page as a JSON array
     * (one entry per page). The QA pipeline accepts this format via
     * {@code rafptor-qa validate --afp-text}.
     */
    private static void writeTextJson(Path outDir, Path afp, AfpDocument doc) throws java.io.IOException {
        String base = afp.getFileName().toString();
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        Path out = outDir.resolve(base + ".text.json");
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean firstPage = true;
        for (AfpPage page : doc.pages()) {
            if (!firstPage) sb.append(',');
            firstPage = false;
            StringBuilder pageText = new StringBuilder();
            for (PtocaTextRun r : page.textRuns()) {
                if (r.text() == null || r.text().isEmpty()) continue;
                if (pageText.length() > 0) pageText.append('\n');
                pageText.append(r.text());
            }
            sb.append(jsonString(pageText.toString()));
        }
        sb.append(']');
        Files.writeString(out, sb.toString());
    }

    private static String jsonString(String raw) {
        StringBuilder out = new StringBuilder(raw.length() + 2);
        out.append('"');
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            switch (ch) {
                case '\\' -> out.append("\\\\");
                case '"'  -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (ch < 0x20) {
                        out.append(String.format("\\u%04x", (int) ch));
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        out.append('"');
        return out.toString();
    }
}
