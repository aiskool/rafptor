package com.rafptor.parser.ioca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IOCA (Image Object Content Architecture) — stub.
 *
 * <p>Full image parsing is scheduled for Phase 1 Task 1.3. This class currently
 * records the encounter only, so downstream modules can surface a "not-yet-supported"
 * marker rather than silently dropping content.
 */
public final class IocaParser {

    private static final Logger LOG = LoggerFactory.getLogger(IocaParser.class);

    public void record(int payloadLength) {
        LOG.info("ioca payload encountered (not yet implemented), bytes={}", payloadLength);
    }
}
