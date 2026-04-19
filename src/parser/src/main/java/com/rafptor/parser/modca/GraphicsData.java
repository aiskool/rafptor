package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * Graphics Data (GAD) — structured field {@code 0xD3 0xEE 0xBB}. Carries the
 * raw GOCA drawing-order sequence; multiple GAD fields may appear inside a
 * single BGR/EGR pair and must be concatenated in document order.
 */
public record GraphicsData(StructuredFieldId id, byte[] data) implements AfpStructuredField {

    public GraphicsData {
        if (data == null) {
            data = new byte[0];
        } else {
            data = data.clone();
        }
    }

    public static GraphicsData parse(RawStructuredField raw) {
        return new GraphicsData(raw.id(), raw.data());
    }
}
