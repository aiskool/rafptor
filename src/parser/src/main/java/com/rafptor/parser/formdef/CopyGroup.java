package com.rafptor.parser.formdef;

import java.util.List;

/**
 * A Copy Group — the mechanism by which one logical document produces
 * multiple variants (original, customer copy, archive …) each with their
 * own overlay, tray selection and copy count.
 *
 * <p>Structurally a Copy Group lives inside a Form Definition and binds
 * a range of pages to a Medium Modification Control (MMC) record.
 */
public record CopyGroup(String name,
                        int copyCount,
                        int trayId,
                        List<String> overlayNames,
                        int firstPageIndex,
                        int lastPageIndex) {

    public static CopyGroup defaults() {
        return new CopyGroup("ORIGINAL", 1, 0, List.of(), 0, Integer.MAX_VALUE);
    }

    public CopyGroup {
        if (name == null) name = "";
        if (overlayNames == null) overlayNames = List.of();
    }
}
