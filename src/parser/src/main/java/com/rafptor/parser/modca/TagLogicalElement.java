package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;
import com.rafptor.parser.reader.TripletParser;

import java.util.List;
import java.util.Optional;

/**
 * Tag Logical Element (TLE) — indexable metadata attached to a document or page.
 *
 * <p>TLE carries its key/value pair inside triplets:
 * <ul>
 *   <li>X'02' — Attribute Value (the value, EBCDIC)</li>
 *   <li>X'36' — Attribute Qualifier / name (the key, EBCDIC)</li>
 * </ul>
 */
public record TagLogicalElement(
        StructuredFieldId id,
        String attributeName,
        String attributeValue,
        List<TripletParser.Triplet> triplets) implements AfpStructuredField {

    public TagLogicalElement {
        triplets = List.copyOf(triplets);
    }

    public static TagLogicalElement parse(RawStructuredField raw) {
        byte[] data = raw.data();
        List<TripletParser.Triplet> triplets = TripletParser.parseAll(data, 0, data.length);
        String name = extractString(triplets, 0x02);
        String value = extractString(triplets, 0x36);
        return new TagLogicalElement(raw.id(), name, value, triplets);
    }

    public Optional<String> attributeNameOpt() {
        return Optional.ofNullable(attributeName);
    }

    public Optional<String> attributeValueOpt() {
        return Optional.ofNullable(attributeValue);
    }

    private static String extractString(List<TripletParser.Triplet> triplets, int tripletId) {
        for (TripletParser.Triplet t : triplets) {
            if (t.id() == tripletId) {
                return ModcaUtil.decodeName(t.value(), 0, t.value().length);
            }
        }
        return null;
    }
}
