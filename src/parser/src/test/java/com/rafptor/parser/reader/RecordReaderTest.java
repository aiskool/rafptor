package com.rafptor.parser.reader;

import com.rafptor.parser.AfpTestFileGenerator;
import com.rafptor.parser.ParserLimits;
import com.rafptor.parser.exception.MalformedFieldException;
import com.rafptor.parser.model.RawStructuredField;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordReaderTest {

    @Test
    void reads_minimal_document_in_order() {
        byte[] stream = AfpTestFileGenerator.createMinimalDocument();
        RecordReader reader = new RecordReader(new ByteArrayInputStream(stream), ParserLimits.defaults());

        List<RawStructuredField> records = new ArrayList<>();
        while (reader.hasNext()) {
            records.add(reader.next());
        }
        assertEquals(8, records.size());
        assertEquals(0xD3, records.get(0).id().classByte());
        assertEquals(0xA8, records.get(0).id().typeByte());
        assertEquals(0xA8, records.get(0).id().categoryByte());
        assertEquals(records.size(), reader.recordsRead());
    }

    @Test
    void rejects_stream_without_carriage_control() {
        byte[] bad = AfpTestFileGenerator.createStreamMissingCarriageControl();
        RecordReader reader = new RecordReader(new ByteArrayInputStream(bad), ParserLimits.defaults());
        assertThrows(MalformedFieldException.class, reader::hasNext);
    }

    @Test
    void rejects_oversized_field() {
        byte[] over = AfpTestFileGenerator.createOversizedField(2_000_000);
        ParserLimits limits = new ParserLimits(
                1_000_000,
                ParserLimits.DEFAULT_MAX_DOCUMENT_SIZE,
                ParserLimits.DEFAULT_MAX_NESTING_DEPTH,
                ParserLimits.DEFAULT_PARSE_TIMEOUT_MILLIS);
        RecordReader reader = new RecordReader(new ByteArrayInputStream(over), limits);
        assertThrows(MalformedFieldException.class, reader::hasNext);
    }

    @Test
    void rejects_truncated_stream() {
        byte[] truncated = AfpTestFileGenerator.createTruncatedStream();
        RecordReader reader = new RecordReader(new ByteArrayInputStream(truncated), ParserLimits.defaults());
        assertThrows(MalformedFieldException.class, () -> {
            while (reader.hasNext()) {
                reader.next();
            }
        });
    }

    @Test
    void returns_false_on_empty_stream() {
        RecordReader reader = new RecordReader(new ByteArrayInputStream(new byte[0]), ParserLimits.defaults());
        assertFalse(reader.hasNext());
        assertEquals(0L, reader.recordsRead());
    }

    @Test
    void rejects_declared_length_smaller_than_header() {
        byte[] bad = new byte[]{0x5A, 0x00, 0x03, (byte) 0xD3, (byte) 0xA8, (byte) 0xA8, 0x00, 0x00, 0x00};
        RecordReader reader = new RecordReader(new ByteArrayInputStream(bad), ParserLimits.defaults());
        assertThrows(MalformedFieldException.class, reader::hasNext);
    }

    @Test
    void tracks_bytes_consumed() {
        byte[] stream = AfpTestFileGenerator.createMinimalDocument();
        RecordReader reader = new RecordReader(new ByteArrayInputStream(stream), ParserLimits.defaults());
        while (reader.hasNext()) {
            reader.next();
        }
        assertEquals(stream.length, reader.bytesConsumed());
    }

    @Test
    void preserves_payload_contents() {
        byte[] stream = AfpTestFileGenerator.createMinimalDocument();
        RecordReader reader = new RecordReader(new ByteArrayInputStream(stream), ParserLimits.defaults());
        assertTrue(reader.hasNext());
        RawStructuredField first = reader.next();
        assertEquals(8, first.dataLength());
    }
}
