package com.rafptor.parser.audit;

import java.util.List;

/**
 * Immutable snapshot of the byte accounting for one AFP parse.
 */
public record ByteAccountingReport(long totalFileBytes,
                                   long usedBytes,
                                   long envelopeBytes,
                                   long ignoredBytes,
                                   long unknownBytes,
                                   long partialBytes,
                                   double coverage,
                                   List<ByteAccountant.SfRecord> bigIgnored,
                                   List<ByteAccountant.SfRecord> records) {

    public ByteAccountingReport {
        if (bigIgnored == null) bigIgnored = List.of();
        if (records == null) records = List.of();
    }

    public long accountedBytes() {
        return usedBytes + envelopeBytes + ignoredBytes + unknownBytes + partialBytes;
    }

    public long unaccountedBytes() {
        return Math.max(0, totalFileBytes - accountedBytes());
    }

    public boolean hasWarnings() {
        return coverage < 100.0 || !bigIgnored.isEmpty() || unknownBytes > 0;
    }
}
