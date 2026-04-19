package com.rafptor.converter.transform;

import com.rafptor.converter.ir.IrImage;
import com.rafptor.converter.ir.IrPage;
import com.rafptor.converter.render.IocaDecoder;
import com.rafptor.parser.model.AfpImageObject;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Transforms {@link AfpImageObject}s from the parser into {@link IrImage}
 * entries on the IR page.
 *
 * <p>JPEG / BMP payloads are passed through to the renderer as-is. IOCA
 * Function-Set 45 payloads are routed through {@link IocaDecoder}:
 * <ul>
 *   <li>Uncompressed (1/8/24 bpp) → decoded natively.</li>
 *   <li>Group 4 (CCITT T.6) → wrapped in a minimal TIFF and read by the
 *       TwelveMonkeys ImageIO TIFF plugin.</li>
 *   <li>JBIG2 → read by the PDFBox jbig2-imageio plugin.</li>
 * </ul>
 *
 * <p>Whenever the decoder succeeds, the resulting {@link java.awt.image.BufferedImage}
 * is re-encoded as PNG for the renderer (so PdfRenderer.renderImage can use
 * the existing {@code ImageIO.read} → {@code LosslessFactory.createFromImage}
 * path unchanged). On failure a warning is accumulated.
 */
public final class ImageTransformer {

    private final List<String> warnings = new ArrayList<>();

    public List<IrImage> transform(List<AfpImageObject> images, IrPage page) {
        warnings.clear();
        if (images == null || images.isEmpty() || page == null) {
            return List.of();
        }
        List<IrImage> out = new ArrayList<>(images.size());
        double cursor = 0;
        for (AfpImageObject img : images) {
            if (img.byteCount() == 0) continue;
            IrImage ir = convert(img, page, cursor);
            if (ir == null) continue;
            out.add(ir);
            cursor += ir.height() + 4;
        }
        return out;
    }

    private IrImage convert(AfpImageObject img, IrPage page, double cursor) {
        byte[] raw = img.rawBytes();
        String format;
        byte[] data;
        double wPx;
        double hPx;

        switch (img.encoding()) {
            case JPEG -> { format = "jpeg"; data = raw; wPx = 0; hPx = 0; }
            case BMP  -> { format = "bmp";  data = raw; wPx = 0; hPx = 0; }
            default -> {
                // IOCA, unknown — run through the decoder.
                IocaDecoder.Decoded decoded = IocaDecoder.decode(img);
                if (decoded == null || decoded.image() == null) {
                    warnings.add(String.format(
                            "IOCA image '%s' (%d bytes) could not be decoded",
                            img.name(), img.byteCount()));
                    return null;
                }
                byte[] png = encodePng(decoded.image());
                if (png == null) {
                    warnings.add(String.format(
                            "IOCA image '%s' decoded but PNG re-encode failed",
                            img.name()));
                    return null;
                }
                format = "png";
                data = png;
                wPx = decoded.widthPx();
                hPx = decoded.heightPx();
            }
        }

        // Size in PDF points: if the parser knows L-units, convert; otherwise
        // fall back to the pixel size divided by 72 dpi heuristic.
        double wPt = img.widthLUnits() > 0 ? page.toPoints(img.widthLUnits())
                : wPx > 0 ? wPx * 72.0 / Math.max(1, page.afpResolution()) : 72;
        double hPt = img.heightLUnits() > 0 ? page.toPoints(img.heightLUnits())
                : hPx > 0 ? hPx * 72.0 / Math.max(1, page.afpResolution()) : 72;
        wPt = Math.max(1, wPt);
        hPt = Math.max(1, hPt);

        return new IrImage(0, cursor, 0, data, format, wPt, hPt, page.afpResolution());
    }

    private static byte[] encodePng(java.awt.image.BufferedImage img) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(img, "PNG", out)) return null;
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }
}
