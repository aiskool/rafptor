package com.rafptor.parser;

import com.rafptor.parser.model.AfpDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the binary fixtures under {@code tests/fixtures/}.
 *
 * <p>Guarded by the {@code rafptor.fixtures.dir} system property so the test can be
 * skipped in isolated build environments. In CI, set
 * {@code -Drafptor.fixtures.dir=${workspace}/tests/fixtures}.
 */
@EnabledIfSystemProperty(named = "rafptor.fixtures.dir", matches = ".+")
class FixtureRoundTripTest {

    @Test
    void parses_minimal_fixture() throws IOException {
        Path path = fixturesDir().resolve("simple/minimal_document.afp");
        assertTrue(Files.exists(path), "fixture missing: " + path);
        try (InputStream in = Files.newInputStream(path)) {
            AfpDocument doc = new RafptorParser().parse(in);
            assertEquals("DOC00001", doc.name());
            assertEquals(1, doc.pages().size());
        }
    }

    private static Path fixturesDir() {
        String dir = System.getProperty("rafptor.fixtures.dir");
        return Paths.get(dir);
    }
}
