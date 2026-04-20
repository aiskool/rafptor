package com.rafptor.parser.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tracks every structured field read from an AFP stream so that the final
 * parse report can prove the parser did not silently discard any byte.
 *
 * <p>This class is deliberately cheap: one record (two longs + three
 * references + one enum) per SF. On a 10 MB document with ~5 000 SFs, the
 * overhead is around 250 KB of heap and a handful of milliseconds.
 *
 * <p>Thread-safety: none. A parser is expected to own one accountant per parse.
 */
public final class ByteAccountant {

    /** Status assigned to each SF relative to what the parser extracted from it. */
    public enum SfStatus {
        /** Parser produced an IR element from this SF (text, image, rule …). */
        PARSED_USED,
        /** Parser recognised the SF but did not produce IR (ex: NOP, comment). */
        PARSED_IGNORED,
        /** Structural envelope (BDT/EDT, BPG/EPG, BAG/EAG, …) — no content expected. */
        ENVELOPE_ONLY,
        /** Identifier not in the SfRegistry — preserved as OpaqueSf. */
        UNKNOWN,
        /** Parsed but some internal data (triplets, opcodes) was dropped. */
        PARTIAL
    }

    public record SfRecord(long offset,
                           int length,
                           String idHex,
                           String mnemonic,
                           SfStatus status,
                           int pageIndex) {

        public SfRecord {
            if (length < 0) {
                throw new IllegalArgumentException("length must be >= 0");
            }
            if (idHex == null) idHex = "";
            if (mnemonic == null) mnemonic = "";
            if (status == null) status = SfStatus.UNKNOWN;
        }
    }

    private final List<SfRecord> records = new ArrayList<>();
    private long totalFileBytes;

    public void setTotalFileBytes(long totalFileBytes) {
        if (totalFileBytes < 0) {
            throw new IllegalArgumentException("totalFileBytes must be >= 0");
        }
        this.totalFileBytes = totalFileBytes;
    }

    public long totalFileBytes() {
        return totalFileBytes;
    }

    public void register(long offset, int length, String idHex, String mnemonic,
                         SfStatus status, int pageIndex) {
        records.add(new SfRecord(offset, length, idHex, mnemonic, status, pageIndex));
    }

    /**
     * Upgrade the status of the most-recently-registered SF. The parser uses
     * this after a dispatch to mark a SF that produced IR content as
     * {@code PARSED_USED} (the initial register call used a conservative
     * default).
     */
    public void markLastStatus(SfStatus status) {
        if (records.isEmpty()) return;
        SfRecord last = records.get(records.size() - 1);
        records.set(records.size() - 1,
                new SfRecord(last.offset, last.length, last.idHex, last.mnemonic, status, last.pageIndex));
    }

    public int size() {
        return records.size();
    }

    public List<SfRecord> records() {
        return Collections.unmodifiableList(records);
    }

    /** Materialise aggregated totals + big-ignored list. */
    public ByteAccountingReport generateReport() {
        long used = 0, envelope = 0, ignored = 0, unknown = 0, partial = 0;
        List<SfRecord> bigIgnored = new ArrayList<>();
        for (SfRecord r : records) {
            switch (r.status) {
                case PARSED_USED      -> used += r.length;
                case ENVELOPE_ONLY    -> envelope += r.length;
                case PARSED_IGNORED   -> {
                    ignored += r.length;
                    if (r.length > 100) bigIgnored.add(r);
                }
                case UNKNOWN          -> {
                    unknown += r.length;
                    if (r.length > 100) bigIgnored.add(r);
                }
                case PARTIAL          -> partial += r.length;
            }
        }
        double coverage = totalFileBytes == 0
                ? 100.0
                : (used + envelope + partial) * 100.0 / totalFileBytes;
        return new ByteAccountingReport(
                totalFileBytes, used, envelope, ignored, unknown, partial,
                coverage,
                Collections.unmodifiableList(bigIgnored),
                Collections.unmodifiableList(new ArrayList<>(records)));
    }
}
