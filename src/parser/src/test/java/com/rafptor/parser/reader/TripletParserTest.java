package com.rafptor.parser.reader;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripletParserTest {

    @Test
    void parses_two_triplets() {
        byte[] data = new byte[]{0x04, 0x02, (byte) 'A', (byte) 'B', 0x03, 0x36, (byte) 'X'};
        List<TripletParser.Triplet> triplets = TripletParser.parseAll(data, 0, data.length);
        assertEquals(2, triplets.size());
        assertEquals(0x02, triplets.get(0).id());
        assertArrayEquals(new byte[]{'A', 'B'}, triplets.get(0).value());
        assertEquals(0x36, triplets.get(1).id());
        assertArrayEquals(new byte[]{'X'}, triplets.get(1).value());
    }

    @Test
    void tolerates_length_below_two_as_padding() {
        // 0x01 is an invalid triplet length; treat as padding and stop.
        byte[] data = new byte[]{0x01, 0x00};
        List<TripletParser.Triplet> triplets = TripletParser.parseAll(data, 0, data.length);
        assertTrue(triplets.isEmpty());
    }

    @Test
    void tolerates_truncated_triplet_at_tail() {
        // Valid triplet followed by a truncated declaration: keep what was parsed.
        byte[] data = new byte[]{0x04, 0x02, 'A', 'B', 0x05, 0x02, 'X'};
        List<TripletParser.Triplet> triplets = TripletParser.parseAll(data, 0, data.length);
        assertEquals(1, triplets.size());
        assertEquals(0x02, triplets.get(0).id());
    }

    @Test
    void accepts_empty_range() {
        List<TripletParser.Triplet> triplets = TripletParser.parseAll(new byte[0], 0, 0);
        assertTrue(triplets.isEmpty());
    }
}
