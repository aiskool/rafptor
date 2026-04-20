package com.rafptor.parser.resources;

/**
 * Result of a successful {@link ResourceLibrary#resolve} call.
 */
public record ResolvedResource(String name,
                               ResourceType type,
                               byte[] data,
                               String sourcePath) {

    public ResolvedResource {
        if (name == null) name = "";
        if (data == null) data = new byte[0];
        if (sourcePath == null) sourcePath = "";
    }
}
