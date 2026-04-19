package com.rafptor.parser.modca;

import com.rafptor.parser.model.RawStructuredField;
import com.rafptor.parser.model.StructuredFieldId;
import org.junit.jupiter.api.Test;

import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapDataResourceTest {

    @Test
    void extractsArialBoldRgFromAfpworldSample() {
        // Wire-format of one MDR repeating group as observed in the AFPWorld
        // Continuing Health Coverage sample. RG length 77.
        String hex = ""
                + "004D"                                 // RG length = 77
                + "18100041000000A800060725120004010133000000000000" // triplet 0x10 (24 bytes)
                + "108B00200000B400000000000003000100" // wait, we need size at bytes 2-3
                + "0601000004B0"                          // triplet 0x01
                + "18" + "02DE0000" + "41007200690061006C002000420069006c00" // 0x18=24 bytes triplet 0x02 name
                + "05" + "02BE0001";                      // 0x05=5 bytes triplet 0x02 id
        // The above is approximate; instead use the *exact* RG we saw in the
        // diagnostics (len 77, id=0x10 24B, id=0x8B 16B, id=0x01 6B,
        // id=0x02 24B for name, id=0x02 5B for local id).
        // Exact RG#1 bytes lifted verbatim from the AFPWorld Continuing Health
        // Coverage sample — 77-byte repeating group that binds local font id 1
        // to "Arial Bold" at 9pt.
        String exactRg = "004d181000410000a80006072b12000401013300000000000000"
                + "108b002000b4000000000003000100000601000004b0"
                + "1802de000041007200690061006c00200042006f006c00640502be0001";
        byte[] bytes = HexFormat.of().parseHex(exactRg);
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xAB, 0xC3), 0, bytes);
        MapDataResource mdr = MapDataResource.parse(raw);
        List<MapDataResource.FontEntry> entries = mdr.fontEntries();
        assertEquals(1, entries.size(), "one RG → one font entry");
        MapDataResource.FontEntry e = entries.get(0);
        assertEquals(1, e.localId());
        assertEquals("Arial Bold", e.fontName());
        // Size triplet 0x8B bytes 4-5 = 0x00 0xB4 = 180 → 9pt (180/20)
        assertEquals(9.0, e.pointSize(), 0.01);
    }

    @Test
    void tolerantOnEmptyPayload() {
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xAB, 0xC3), 0, new byte[0]);
        MapDataResource mdr = MapDataResource.parse(raw);
        assertTrue(mdr.fontEntries().isEmpty());
    }

    @Test
    void toleratesMalformedRepeatingGroupLength() {
        byte[] data = new byte[]{0x00, 0x02, 0x00, 0x00};
        RawStructuredField raw = new RawStructuredField(
                StructuredFieldId.of(0xD3, 0xAB, 0xC3), 0, data);
        MapDataResource mdr = MapDataResource.parse(raw);
        assertTrue(mdr.fontEntries().isEmpty());
    }
}
