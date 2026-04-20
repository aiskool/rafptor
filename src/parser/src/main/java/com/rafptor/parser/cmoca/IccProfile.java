package com.rafptor.parser.cmoca;

/**
 * Phase 7 scaffold — holds the bytes of an ICC profile discovered inside an
 * AFP Object Container (CMOCA). The PDF renderer can embed the profile as
 * a PDICCBased color space and use it as the output intent, a requirement
 * for PDF/A-1b when the source is coloured.
 *
 * <p>Detection: an OCD whose Object Classification triplet declares the
 * ICC MIME type, or whose raw bytes begin with the ICC signature
 * {@code "acsp"} at offset 36.
 */
public record IccProfile(String name, byte[] bytes) {

    public static final byte[] ICC_SIG = {'a', 'c', 's', 'p'};

    public IccProfile {
        if (name == null) name = "";
        if (bytes == null) bytes = new byte[0];
    }

    public static boolean isIccProfile(byte[] candidate) {
        if (candidate == null || candidate.length < 40) return false;
        for (int i = 0; i < 4; i++) {
            if (candidate[36 + i] != ICC_SIG[i]) return false;
        }
        return true;
    }
}
