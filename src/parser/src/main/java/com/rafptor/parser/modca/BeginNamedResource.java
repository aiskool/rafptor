package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;

/**
 * Shared wire layout for the family of begin-markers that open a named
 * resource envelope in MO:DCA:
 * <ul>
 *   <li>{@code 0xD3 0xA8 0xA5} — BRS (Begin Resource)</li>
 *   <li>{@code 0xD3 0xA8 0xCE} — BFN (Begin Font)</li>
 *   <li>{@code 0xD3 0xA8 0x92} — BDG (Begin Document Environment Group)</li>
 * </ul>
 *
 * <p>Each of these carries the resource's 8-byte EBCDIC name in the first
 * bytes of its body. The remaining bytes (rotation, size, flags, triplets)
 * are not modelled here — only the name is harvested.
 */
public record BeginNamedResource(StructuredFieldId id, String resourceName)
        implements AfpStructuredField {

    public BeginNamedResource {
        if (resourceName == null) resourceName = "";
    }

    public static BeginNamedResource parse(RawStructuredField raw) {
        byte[] data = raw.data();
        String name = "";
        if (data.length >= 8) {
            name = ModcaUtil.decodeName(data, 0, 8).trim();
        }
        return new BeginNamedResource(raw.id(), name);
    }
}
