package com.rafptor.parser.bcoca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** BCOCA (Bar Code) — stub; to be implemented in a later iteration. */
public final class BcocaParser {

    private static final Logger LOG = LoggerFactory.getLogger(BcocaParser.class);

    public void record(int payloadLength) {
        LOG.info("bcoca payload encountered (not yet implemented), bytes={}", payloadLength);
    }
}
