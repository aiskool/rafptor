package com.rafptor.parser.formdef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parsed AFP Form Definition — the resource that describes paper size,
 * duplex mode, tray, page-offset and N-up placement.
 *
 * <p>A Form Definition is an AFP resource on its own (BFM/EFM envelope,
 * SF id {@code D3 A8 CD}) containing one or more Medium Maps (BMM/EMM)
 * plus Medium Descriptor (MDD), Medium Modification Control (MMC) and
 * Page Position (PGP) records.
 *
 * <p>This class captures the fully resolved effect for the renderer:
 *
 * <ul>
 *   <li>{@link #pageWidthPt()} / {@link #pageHeightPt()} — PDF page size.</li>
 *   <li>{@link #duplex()} — simplex / duplex-long / duplex-short for
 *       page ordering inside the resulting PDF.</li>
 *   <li>{@link #pagePositions()} — list of PGP entries (N-up offsets).</li>
 * </ul>
 */
public record FormDefinition(String name,
                             double pageWidthPt,
                             double pageHeightPt,
                             Duplex duplex,
                             List<PagePosition> pagePositions) {

    public enum Duplex { SIMPLEX, DUPLEX_NORMAL, DUPLEX_TUMBLE }

    public record PagePosition(double xOffsetPt, double yOffsetPt,
                               int orientationDegrees) {}

    public static FormDefinition defaults() {
        return new FormDefinition("DEFAULT", 612.0, 792.0, Duplex.SIMPLEX, List.of());
    }

    public FormDefinition {
        if (name == null) name = "";
        if (duplex == null) duplex = Duplex.SIMPLEX;
        pagePositions = Collections.unmodifiableList(new ArrayList<>(pagePositions));
    }
}
