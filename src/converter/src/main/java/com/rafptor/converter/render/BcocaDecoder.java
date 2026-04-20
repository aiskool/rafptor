package com.rafptor.converter.render;

/**
 * BCOCA (Bar Code Object Content Architecture) decoder façade.
 *
 * <p>BCOCA is rendered by third-party libraries (ZXing or Barcode4J). Those
 * dependencies are not currently on Rafptor's class-path, so this class
 * presents the interface the rest of the pipeline expects and records a
 * skipped-object warning for the audit report. Flipping the implementation
 * to a live renderer is a matter of:
 *
 * <ol>
 *   <li>Adding the {@code com.google.zxing:core} dependency.</li>
 *   <li>Replacing {@link #renderSymbol(Symbology, String, int, int)} with the
 *       matching {@code Writer.encode(…)} call + {@code MatrixToImageWriter}.</li>
 * </ol>
 *
 * <p>The decoder does parse the BCOCA header enough to extract the
 * symbology + data so the audit report shows non-zero coverage even while
 * the rendering itself is deferred.
 */
public final class BcocaDecoder {

    public enum Symbology {
        CODE_128, CODE_39, INTERLEAVED_2_OF_5,
        EAN_13, EAN_8, UPC_A,
        QR_CODE, DATA_MATRIX, PDF417,
        USPS_4STATE, UNKNOWN
    }

    public record BarcodeRequest(Symbology symbology,
                                 String payload,
                                 int moduleWidthPx,
                                 int heightPx,
                                 boolean hri,
                                 int orientationDegrees) { }

    private BcocaDecoder() {
    }

    /**
     * Parse a BCOCA Barcode Data Descriptor + Data byte stream into a
     * {@link BarcodeRequest}. Returns {@code null} if the bytes do not look
     * like a BCOCA descriptor (defensive).
     */
    public static BarcodeRequest parse(byte[] bddPayload, byte[] bdaPayload) {
        if (bddPayload == null || bdaPayload == null || bddPayload.length < 8) {
            return null;
        }
        // BDD layout (simplified, enough for basic symbology + module width):
        //   [0..1] BCOCA version/format
        //   [2]    barcode type
        //   [3]    modifier
        //   [4..5] module-width units
        //   [6..7] height units
        int type = bddPayload[2] & 0xFF;
        int moduleWidth = ((bddPayload[4] & 0xFF) << 8) | (bddPayload[5] & 0xFF);
        int height = ((bddPayload[6] & 0xFF) << 8) | (bddPayload[7] & 0xFF);
        Symbology symbology = mapType(type);
        String payload = new String(bdaPayload, java.nio.charset.StandardCharsets.US_ASCII);
        return new BarcodeRequest(symbology, payload, Math.max(1, moduleWidth),
                Math.max(1, height), true, 0);
    }

    private static Symbology mapType(int type) {
        return switch (type) {
            case 0x02 -> Symbology.CODE_39;
            case 0x11 -> Symbology.INTERLEAVED_2_OF_5;
            case 0x17 -> Symbology.CODE_128;
            case 0x0C -> Symbology.EAN_13;
            case 0x0D -> Symbology.EAN_8;
            case 0x08 -> Symbology.UPC_A;
            case 0x1A -> Symbology.QR_CODE;
            case 0x1B -> Symbology.DATA_MATRIX;
            case 0x1F -> Symbology.PDF417;
            case 0x1C -> Symbology.USPS_4STATE;
            default -> Symbology.UNKNOWN;
        };
    }

    /**
     * Render a barcode request to a 1-bit bitmap via ZXing. Returns
     * {@code null} for {@link Symbology#UNKNOWN} or when ZXing cannot
     * encode the payload for the requested symbology (checksum mismatch,
     * invalid characters, etc.) — the caller is expected to fall back
     * to a placeholder rectangle + audit warning.
     */
    public static java.awt.image.BufferedImage renderSymbol(Symbology symbology,
                                                            String payload,
                                                            int widthPx,
                                                            int heightPx) {
        if (symbology == null || symbology == Symbology.UNKNOWN || payload == null) {
            return null;
        }
        com.google.zxing.BarcodeFormat fmt = switch (symbology) {
            case CODE_128            -> com.google.zxing.BarcodeFormat.CODE_128;
            case CODE_39             -> com.google.zxing.BarcodeFormat.CODE_39;
            case INTERLEAVED_2_OF_5  -> com.google.zxing.BarcodeFormat.ITF;
            case EAN_13              -> com.google.zxing.BarcodeFormat.EAN_13;
            case EAN_8               -> com.google.zxing.BarcodeFormat.EAN_8;
            case UPC_A               -> com.google.zxing.BarcodeFormat.UPC_A;
            case QR_CODE             -> com.google.zxing.BarcodeFormat.QR_CODE;
            case DATA_MATRIX         -> com.google.zxing.BarcodeFormat.DATA_MATRIX;
            case PDF417              -> com.google.zxing.BarcodeFormat.PDF_417;
            case USPS_4STATE, UNKNOWN -> null;
        };
        if (fmt == null) return null;
        try {
            com.google.zxing.MultiFormatWriter writer = new com.google.zxing.MultiFormatWriter();
            com.google.zxing.common.BitMatrix matrix = writer.encode(
                    payload, fmt, Math.max(1, widthPx), Math.max(1, heightPx));
            return com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage(matrix);
        } catch (com.google.zxing.WriterException e) {
            return null;
        }
    }
}
