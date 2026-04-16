package com.rafptor.parser.reader;

import com.rafptor.parser.ParserLimits;
import com.rafptor.parser.exception.MalformedFieldException;
import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Reads an AFP binary stream and emits a sequence of {@link RawStructuredField}.
 *
 * <p>Wire format per record:
 * <pre>
 *   offset 0  : 1 byte  carriage control (0x5A)
 *   offset 1  : 2 bytes SF length (big-endian, unsigned, includes these 8 header bytes)
 *   offset 3  : 3 bytes SF identifier (class, type, category)
 *   offset 6  : 1 byte  flags
 *   offset 7  : 2 bytes reserved / extension
 *   offset 9+ : N bytes SF data
 * </pre>
 *
 * <p><b>Security:</b> every length is validated against {@link ParserLimits#maxFieldSize()}
 * BEFORE any allocation; total bytes consumed are tracked against {@link ParserLimits#maxDocumentSize()}.
 * The reader never logs payload bytes — only structural metadata (id, size, offset).
 */
public final class RecordReader implements Iterator<RawStructuredField> {

    private static final Logger LOG = LoggerFactory.getLogger(RecordReader.class);

    public static final int CARRIAGE_CONTROL = 0x5A;
    public static final int HEADER_SIZE = 8;

    private final InputStream input;
    private final ParserLimits limits;
    private long bytesConsumed;
    private long recordsRead;
    private RawStructuredField nextField;
    private boolean exhausted;

    public RecordReader(InputStream input, ParserLimits limits) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        if (limits == null) {
            throw new IllegalArgumentException("limits must not be null");
        }
        this.input = input;
        this.limits = limits;
    }

    @Override
    public boolean hasNext() {
        if (exhausted) {
            return false;
        }
        if (nextField != null) {
            return true;
        }
        try {
            nextField = readNext();
        } catch (IOException e) {
            throw new MalformedFieldException("I/O error at record " + recordsRead, e);
        }
        if (nextField == null) {
            exhausted = true;
            return false;
        }
        return true;
    }

    @Override
    public RawStructuredField next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        RawStructuredField out = nextField;
        nextField = null;
        return out;
    }

    public long recordsRead() {
        return recordsRead;
    }

    public long bytesConsumed() {
        return bytesConsumed;
    }

    private RawStructuredField readNext() throws IOException {
        int cc = input.read();
        if (cc == -1) {
            return null;
        }
        if (cc != CARRIAGE_CONTROL) {
            throw new MalformedFieldException(
                    "Missing 0x5A carriage control at offset " + bytesConsumed
                            + " (got 0x" + Integer.toHexString(cc) + ")");
        }
        bytesConsumed++;
        incrementDocumentSize(0);

        int lengthHi = readOrFail("length-hi");
        int lengthLo = readOrFail("length-lo");
        int declaredLength = (lengthHi << 8) | lengthLo;

        if (declaredLength < HEADER_SIZE) {
            throw new MalformedFieldException(
                    "Declared length " + declaredLength + " < header size at record " + recordsRead);
        }
        if (declaredLength > limits.maxFieldSize()) {
            throw new MalformedFieldException(
                    "Declared length " + declaredLength + " exceeds maxFieldSize "
                            + limits.maxFieldSize() + " at record " + recordsRead);
        }

        int idClass = readOrFail("id-class");
        int idType = readOrFail("id-type");
        int idCategory = readOrFail("id-category");
        int flags = readOrFail("flags");
        int reservedHi = readOrFail("reserved-hi");
        int reservedLo = readOrFail("reserved-lo");
        // reserved bytes are not validated (spec allows extensions)
        if (reservedHi < 0 || reservedLo < 0) {
            throw new MalformedFieldException("Truncated reserved bytes");
        }

        int dataLength = declaredLength - HEADER_SIZE;
        byte[] data = readExact(dataLength);

        bytesConsumed += (long) (declaredLength - 1); // -1: cc already counted
        incrementDocumentSize(declaredLength - 1);
        recordsRead++;

        StructuredFieldId id = new StructuredFieldId(idClass, idType, idCategory);
        if (LOG.isDebugEnabled()) {
            LOG.debug("record={} id={} flags=0x{} dataLen={}",
                    recordsRead, id.toHex(), String.format("%02X", flags), dataLength);
        }
        return new RawStructuredField(id, flags, data);
    }

    private int readOrFail(String label) throws IOException {
        int b = input.read();
        if (b == -1) {
            throw new MalformedFieldException(
                    "Truncated stream while reading " + label + " at offset " + bytesConsumed);
        }
        return b;
    }

    private byte[] readExact(int n) throws IOException {
        if (n < 0) {
            throw new MalformedFieldException("Negative data length " + n);
        }
        if (n == 0) {
            return new byte[0];
        }
        byte[] buf = new byte[n];
        int got = 0;
        while (got < n) {
            int r = input.read(buf, got, n - got);
            if (r == -1) {
                throw new MalformedFieldException(
                        "Truncated payload: expected " + n + " bytes, got " + got);
            }
            got += r;
        }
        return buf;
    }

    private void incrementDocumentSize(long delta) {
        long next = bytesConsumed + delta;
        if (next < 0 || next > limits.maxDocumentSize()) {
            throw new MalformedFieldException(
                    "Document size exceeds maxDocumentSize " + limits.maxDocumentSize());
        }
    }
}
