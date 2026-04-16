package com.rafptor.converter.transform;

import com.rafptor.converter.ir.IrBarcode;

import java.util.List;

public final class BarcodeTransformer {

    public List<IrBarcode> transform() {
        return List.of();
    }

    public String notImplementedWarning() {
        return "BCOCA barcode transformation is not implemented yet; barcodes dropped.";
    }
}
