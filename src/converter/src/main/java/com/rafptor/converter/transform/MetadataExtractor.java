package com.rafptor.converter.transform;

import com.rafptor.converter.ir.IrMetadata;
import com.rafptor.parser.model.AfpDocument;

public final class MetadataExtractor {

    public IrMetadata extract(AfpDocument afp) {
        if (afp == null) {
            return new IrMetadata("", 0, java.util.Map.of());
        }
        return new IrMetadata(afp.name(), afp.pages().size(), afp.tags());
    }
}
