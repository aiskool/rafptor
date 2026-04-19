package com.rafptor.parser.model;

/**
 * A GOCA (Graphics Object Content Architecture) object carried inside an
 * AFP page. The payload holds a sequence of GOCA Drawing Orders whose
 * interpretation is deferred to the converter so the parser stays
 * content-agnostic.
 *
 * <p>Fields with zero values mean "not supplied" — the converter uses the
 * page geometry when so. {@code xOriginLUnits} / {@code yOriginLUnits}
 * place the graphic object on the page in AFP L-units.
 */
public record AfpGraphicObject(
        String name,
        int xOriginLUnits,
        int yOriginLUnits,
        int widthLUnits,
        int heightLUnits,
        byte[] rawBytes) {

    public AfpGraphicObject {
        if (name == null) name = "";
        if (rawBytes == null) rawBytes = new byte[0];
    }

    public int byteCount() {
        return rawBytes.length;
    }
}
