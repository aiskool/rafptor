package com.rafptor.converter.transform;

import com.rafptor.converter.ir.IrImage;
import com.rafptor.converter.ir.IrPage;
import com.rafptor.parser.model.AfpImageObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Transforms {@link AfpImageObject}s from the parser into {@link IrImage}
 * entries on the IR page. JPEG / BMP encodings are rendered end-to-end via
 * PDFBox; IOCA raster (FS45 and friends) is not decoded today — the image is
 * skipped and a warning is reported so the caller can surface it.
 */
public final class ImageTransformer {

    private final List<String> warnings = new ArrayList<>();

    public List<IrImage> transform(List<AfpImageObject> images, IrPage page) {
        warnings.clear();
        if (images == null || images.isEmpty() || page == null) {
            return List.of();
        }
        List<IrImage> out = new ArrayList<>(images.size());
        // Place each decoded image stacked in the top-left corner until
        // Include Object / Include Page Segment positioning is resolved.
        double cursor = 0;
        for (AfpImageObject img : images) {
            if (img.byteCount() == 0) continue;
            String format = switch (img.encoding()) {
                case JPEG -> "jpeg";
                case BMP -> "bmp";
                default -> {
                    warnings.add(String.format(
                            "IOCA image '%s' (%d bytes) not decoded — no free FS45 decoder",
                            img.name(), img.byteCount()));
                    yield null;
                }
            };
            if (format == null) continue;
            double wPt = Math.max(1, img.widthLUnits() > 0
                    ? page.toPoints(img.widthLUnits()) : 72);
            double hPt = Math.max(1, img.heightLUnits() > 0
                    ? page.toPoints(img.heightLUnits()) : 72);
            out.add(new IrImage(0, cursor, 0, img.rawBytes(), format, wPt, hPt,
                    page.afpResolution()));
            cursor += hPt + 4;
        }
        return out;
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }
}
