package com.rafptor.parser.reader;


import java.util.ArrayList;
import java.util.List;

/**
 * Parses the triplet TLV structure used inside several Structured Fields.
 *
 * <p>Wire format per triplet:
 * <pre>
 *   offset 0 : 1 byte  total triplet length (>= 2)
 *   offset 1 : 1 byte  triplet identifier
 *   offset 2 : N-2     value bytes
 * </pre>
 */
public final class TripletParser {

    private TripletParser() {
    }

    public static List<Triplet> parseAll(byte[] data, int offset, int end) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (offset < 0 || end > data.length || offset > end) {
            throw new IllegalArgumentException("invalid offset/end");
        }
        List<Triplet> triplets = new ArrayList<>();
        int pos = offset;
        while (pos < end) {
            int length = data[pos] & 0xFF;
            if (length < 2) {
                // Padding / stop byte encountered — stop rather than abort the whole record.
                break;
            }
            if (pos + length > end) {
                // Truncated triplet at the tail: accept what was read so far.
                break;
            }
            int id = data[pos + 1] & 0xFF;
            byte[] value = new byte[length - 2];
            if (value.length > 0) {
                System.arraycopy(data, pos + 2, value, 0, value.length);
            }
            triplets.add(new Triplet(id, value));
            pos += length;
        }
        return triplets;
    }

    public record Triplet(int id, byte[] value) {
        public Triplet {
            if ((id & ~0xFF) != 0) {
                throw new IllegalArgumentException("triplet id must be a byte");
            }
            if (value == null) {
                throw new IllegalArgumentException("triplet value must not be null");
            }
            value = value.clone();
        }

        @Override
        public byte[] value() {
            return value.clone();
        }
    }
}
