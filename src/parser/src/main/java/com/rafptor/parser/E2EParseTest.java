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
            System.err.println("Usage: E2EParseTest <afp-streams-dir>");
            System.exit(1);
        }
        Path streamsDir = Path.of(args[0]);
        if (!Files.isDirectory(streamsDir)) {
            System.err.println("Not a directory: " + streamsDir);
            System.exit(1);
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
}
