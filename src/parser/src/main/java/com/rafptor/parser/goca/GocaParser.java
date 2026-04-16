package com.rafptor.parser.goca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** GOCA (Graphics) — stub; to be implemented in a later iteration. */
public final class GocaParser {

    private static final Logger LOG = LoggerFactory.getLogger(GocaParser.class);

    public void record(int payloadLength) {
        LOG.info("goca payload encountered (not yet implemented), bytes={}", payloadLength);
    }
}
