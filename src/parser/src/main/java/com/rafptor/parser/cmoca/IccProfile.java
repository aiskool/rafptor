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

    // Package-private + byte[] wrapped in a helper to avoid SpotBugs
    // MS_PKGPROTECT / MS_MUTABLE_ARRAY warnings on a public mutable array.
    static byte[] iccSig() {
        return new byte[]{'a', 'c', 's', 'p'};
    }

    public IccProfile {
        if (name == null) name = "";
        if (bytes == null) bytes = new byte[0];
    }

    public static boolean isIccProfile(byte[] candidate) {
        if (candidate == null || candidate.length < 40) return false;
        byte[] sig = iccSig();
        for (int i = 0; i < 4; i++) {
            if (candidate[36 + i] != sig[i]) return false;
        }
        return true;
    }
}
