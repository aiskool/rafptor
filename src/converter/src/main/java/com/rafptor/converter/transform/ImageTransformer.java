package com.rafptor.converter.transform;

import com.rafptor.converter.ir.IrImage;

import java.util.List;

/**
 * IOCA → IrImage transformer. Stubbed: the parser does not yet surface a typed
 * IOCA object, so this implementation records nothing and returns an empty list.
 * Callers append a warning to the conversion result.
 */
public final class ImageTransformer {

    public List<IrImage> transform() {
        return List.of();
    }

    public String notImplementedWarning() {
        return "IOCA image transformation is not implemented yet; images dropped.";
    }
}
