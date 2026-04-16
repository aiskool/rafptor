package com.rafptor.parser.reader;

import com.rafptor.parser.exception.MalformedFieldException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void rejects_length_below_two() {
        byte[] data = new byte[]{0x01, 0x00};
        assertThrows(MalformedFieldException.class,
                () -> TripletParser.parseAll(data, 0, data.length));
    }

    @Test
    void rejects_truncated_triplet() {
        byte[] data = new byte[]{0x05, 0x02, (byte) 'A'};
        assertThrows(MalformedFieldException.class,
                () -> TripletParser.parseAll(data, 0, data.length));
    }

    @Test
    void accepts_empty_range() {
        List<TripletParser.Triplet> triplets = TripletParser.parseAll(new byte[0], 0, 0);
        assertTrue(triplets.isEmpty());
    }
}
