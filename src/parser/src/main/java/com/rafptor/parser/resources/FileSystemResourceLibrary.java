package com.rafptor.parser.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * File-system backed {@link ResourceLibrary}. Looks for resources in a
 * configured root directory using the standard IBM / ACIF directory
 * conventions:
 *
 * <ul>
 *   <li>{@code overlays/}, {@code OVLYLIB/}</li>
 *   <li>{@code fonts/}, {@code FONTLIB/}, {@code FNTLIB/}</li>
 *   <li>{@code segments/}, {@code PSEGLIB/}</li>
 *   <li>{@code formdef/}, {@code FDEFLIB/}</li>
 *   <li>{@code pagedef/}, {@code PDEFLIB/}</li>
 * </ul>
 *
 * <p>Case-insensitive on name + trimmed, with the {@code .afp / .ovl /
 * .fnt / .pseg / .all / .ttf} extensions tried in order.
 */
public final class FileSystemResourceLibrary implements ResourceLibrary {

    private final Path root;

    public FileSystemResourceLibrary(Path root) {
        if (root == null) throw new IllegalArgumentException("root must not be null");
        this.root = root;
    }

    @Override
    public Optional<ResolvedResource> resolve(String name, ResourceType type) {
        if (name == null || name.isBlank()) return Optional.empty();
        String trimmed = name.trim();
        for (String subdir : subdirsFor(type)) {
            for (String ext : extensionsFor(type)) {
                Optional<Path> hit = findCaseInsensitive(root.resolve(subdir), trimmed + ext);
                if (hit.isPresent()) {
                    try {
                        byte[] data = Files.readAllBytes(hit.get());
                        return Optional.of(new ResolvedResource(
                                trimmed, type, data, hit.get().toString()));
                    } catch (IOException ignored) {
                        return Optional.empty();
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Path> findCaseInsensitive(Path dir, String fileName) {
        if (!Files.isDirectory(dir)) return Optional.empty();
        try {
            return Files.list(dir)
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(fileName))
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static List<String> subdirsFor(ResourceType type) {
        return switch (type) {
            case OVERLAY         -> List.of("overlays", "OVLYLIB", ".");
            case PAGE_SEGMENT    -> List.of("segments", "PSEGLIB", ".");
            case CODED_FONT,
                 CHARACTER_SET,
                 CODE_PAGE,
                 TRUETYPE_FONT   -> List.of("fonts", "FONTLIB", "FNTLIB", ".");
            case FORM_DEFINITION -> List.of("formdef", "FDEFLIB", ".");
            case PAGE_DEFINITION -> List.of("pagedef", "PDEFLIB", ".");
            case OBJECT_CONTAINER -> List.of("objects", "USERLIB", ".");
        };
    }

    private static List<String> extensionsFor(ResourceType type) {
        return switch (type) {
            case OVERLAY        -> List.of(".ovl", ".afp", "");
            case PAGE_SEGMENT   -> List.of(".pseg", ".afp", "");
            case CODED_FONT     -> List.of(".fnt", ".afp", "");
            case CHARACTER_SET  -> List.of(".240", ".300", "");
            case CODE_PAGE      -> List.of(".240", ".300", "");
            case TRUETYPE_FONT  -> List.of(".ttf", ".otf", "");
            case FORM_DEFINITION -> List.of(".fdef", ".afp", "");
            case PAGE_DEFINITION -> List.of(".pdef", ".afp", "");
            case OBJECT_CONTAINER -> List.of("", ".jpg", ".png", ".tif", ".pdf");
        };
    }
}
