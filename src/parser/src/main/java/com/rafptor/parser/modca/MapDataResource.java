package com.rafptor.parser.modca;

import com.rafptor.parser.model.AfpStructuredField;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;
import com.rafptor.parser.reader.TripletParser;

import java.util.List;

public record MapDataResource(StructuredFieldId id, List<TripletParser.Triplet> triplets)
        implements AfpStructuredField {

    public MapDataResource {
        triplets = List.copyOf(triplets);
    }

    public static MapDataResource parse(RawStructuredField raw) {
        byte[] data = raw.data();
        List<TripletParser.Triplet> triplets = TripletParser.parseAll(data, 0, data.length);
        return new MapDataResource(raw.id(), triplets);
    }
}
