package com.rafptor.parser.foca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FOCA (Font) — stub; font metric parsing is Module 4 territory. */
public final class FocaParser {

    private static final Logger LOG = LoggerFactory.getLogger(FocaParser.class);

    public void record(int payloadLength) {
        LOG.info("foca payload encountered (not yet implemented), bytes={}", payloadLength);
    }
}
