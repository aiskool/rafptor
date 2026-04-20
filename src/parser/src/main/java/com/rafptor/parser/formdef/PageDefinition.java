package com.rafptor.parser.formdef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parsed AFP Page Definition — the resource that formats raw line data
 * (mainframe record-oriented text) into pages.
 *
 * <p>Line-data AFP is a huge slice of production AFP volume but the
 * transform requires orchestrating several records: PFC (Page Format
 * Control), CFI (Coded Font Index), PLF (Print Line Format), PTP
 * (Position Text Pointer). This class exposes the resolved layout for
 * the renderer; the full parsing of the resource envelope is deferred
 * to {@link com.rafptor.parser.modca.GenericEnvelope} + follow-up work,
 * but the data model is already in place so the conversion pipeline can
 * honour Page Definitions as soon as the parser produces them.
 */
public record PageDefinition(String name,
                             int linesPerPage,
                             int columnsPerLine,
                             int lineSpacingLunits,
                             int topMarginLunits,
                             int leftMarginLunits,
                             List<FieldBinding> fieldBindings) {

    /**
     * A single (record-column, screen-position, font) binding — ex: "column 10-40
     * of the input record = customer name, printed in Courier 12 at (x=72,y=144)".
     */
    public record FieldBinding(int recordColumnStart,
                               int recordColumnEnd,
                               int xLunits,
                               int yLunits,
                               int fontLocalId) {}

    public static PageDefinition defaults() {
        // Classic IBM mainframe default: 66 lines × 132 columns at 10 cpi / 6 lpi.
        return new PageDefinition("DEFAULT", 66, 132, 240, 0, 0, List.of());
    }

    public PageDefinition {
        if (name == null) name = "";
        fieldBindings = Collections.unmodifiableList(new ArrayList<>(fieldBindings));
    }
}
