package com.rafptor.converter.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A resolved overlay: a named group of IR elements rendered under (or over) the
 * page's variable content, depending on its zOrder.
 */
public final class IrOverlay extends IrElement {

    private final String name;
    private final List<IrElement> elements;

    public IrOverlay(String name, int zOrder, List<IrElement> elements) {
        super(0, 0, zOrder);
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (elements == null) {
            throw new IllegalArgumentException("elements must not be null");
        }
        this.name = name;
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements));
    }

    public String name() { return name; }
    public List<IrElement> elements() { return elements; }
}
