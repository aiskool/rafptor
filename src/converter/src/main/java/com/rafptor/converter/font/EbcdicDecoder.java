package com.rafptor.converter.font;

/**
 * Thin wrapper that delegates to the parser's EBCDIC decoder. Re-exposed here
 * so the converter doesn't need to import parser internals from every call site.
 */
public final class EbcdicDecoder {

    private final com.rafptor.parser.ptoca.EbcdicDecoder inner;

    public EbcdicDecoder(String charsetName) {
        this.inner = new com.rafptor.parser.ptoca.EbcdicDecoder(charsetName);
    }

    public static EbcdicDecoder ibm500() {
        return new EbcdicDecoder("IBM500");
    }

    public static EbcdicDecoder ibm1147() {
        return new EbcdicDecoder("IBM1147");
    }

    public String decode(byte[] data, int offset, int length) {
        return inner.decode(data, offset, length);
    }

    public String decodeAll(byte[] data) {
        return inner.decodeAll(data);
    }
}
