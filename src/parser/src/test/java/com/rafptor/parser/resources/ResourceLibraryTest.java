package com.rafptor.parser.resources;

import com.rafptor.parser.model.AfpDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ResourceLibraryTest {

    @Test
    void inline_library_returns_embedded_object() {
        AfpDocument doc = new AfpDocument("D");
        doc.putEmbeddedObject("LOGO1", new byte[]{0x01, 0x02, 0x03}, "JPEG");
        ResourceLibrary lib = new InlineResourceLibrary(doc);
        Optional<ResolvedResource> r = lib.resolve("LOGO1", ResourceType.OBJECT_CONTAINER);
        assertTrue(r.isPresent());
        assertEquals("LOGO1", r.get().name());
        assertEquals(3, r.get().data().length);
    }

    @Test
    void filesystem_library_resolves_case_insensitive(@TempDir Path root) throws IOException {
        Path overlays = root.resolve("OVLYLIB");
        Files.createDirectories(overlays);
        Files.write(overlays.resolve("HEADER01.OVL"), new byte[]{0x10, 0x20});
        ResourceLibrary lib = new FileSystemResourceLibrary(root);
        Optional<ResolvedResource> r = lib.resolve("header01", ResourceType.OVERLAY);
        assertTrue(r.isPresent());
        assertEquals(2, r.get().data().length);
    }

    @Test
    void composite_falls_back_to_second_library() {
        AfpDocument doc = new AfpDocument("D");
        ResourceLibrary composite = new CompositeResourceLibrary(
                new InlineResourceLibrary(doc),
                ResourceLibrary.EMPTY);
        assertTrue(composite.resolve("NOPE", ResourceType.OVERLAY).isEmpty());
    }

    @Test
    void empty_library_always_returns_empty() {
        assertTrue(ResourceLibrary.EMPTY.resolve("x", ResourceType.OVERLAY).isEmpty());
    }
}
