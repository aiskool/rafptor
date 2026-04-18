package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * Image Raster Data (IRD) — structured field {@code 0xD3 0xEE 0xFB}.
 * Carries one chunk of the image payload; multiple IRDs concatenate to form
 * the full raster stream of a {@code BIM/EIM} envelope.
 */
public record ImageRasterData(StructuredFieldId id, byte[] data) implements AfpStructuredField {

    public ImageRasterData {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        data = data.clone();
    }

    @Override
    public byte[] data() {
        return data.clone();
    }

    public static ImageRasterData parse(RawStructuredField raw) {
        return new ImageRasterData(raw.id(), raw.data());
    }
}
