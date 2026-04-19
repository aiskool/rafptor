package com.rafptor.converter.render;

import com.rafptor.parser.model.AfpGraphicObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GocaDecoderTest {

    @Test
    void returnsEmptyForNullOrEmpty() {
        assertTrue(GocaDecoder.decode(null).isEmpty());
        AfpGraphicObject g = new AfpGraphicObject("g", 0, 0, 0, 0, new byte[0]);
        assertTrue(GocaDecoder.decode(g).isEmpty());
    }

    @Test
    void decodesSetPositionThenLine() {
        // GSPS (21) 00 10 00 20 → current = (16, 32)
        // GLINE long-form (81 04) 00 50 00 60 → line to (80, 96)
        byte[] data = new byte[]{
                0x21, 0x00, 0x10, 0x00, 0x20,
                (byte) 0x81, 0x04, 0x00, 0x50, 0x00, 0x60
        };
        List<GocaDecoder.DrawOrder> orders = GocaDecoder.decodeOrders(data);
        assertEquals(1, orders.size());
        assertTrue(orders.get(0) instanceof GocaDecoder.Line);
        GocaDecoder.Line l = (GocaDecoder.Line) orders.get(0);
        assertEquals(16, l.x1());
        assertEquals(32, l.y1());
        assertEquals(80, l.x2());
        assertEquals(96, l.y2());
    }

    @Test
    void decodesSetLineWidthAndColor() {
        // GSLW (19) 03 → line width 3
        // GSCOL (0A) 02 → colour index 2 (red)
        byte[] data = new byte[]{0x19, 0x03, 0x0A, 0x02};
        List<GocaDecoder.DrawOrder> orders = GocaDecoder.decodeOrders(data);
        assertEquals(2, orders.size());
        assertTrue(orders.get(0) instanceof GocaDecoder.SetLineWidth);
        assertTrue(orders.get(1) instanceof GocaDecoder.SetColor);
        assertEquals("#FF0000", ((GocaDecoder.SetColor) orders.get(1)).hexRgb());
    }

    @Test
    void decodesLineTypeAndRoundedBox() {
        // GSLT (18) 02 → short-dash line
        // GSPS (21) 00 00 00 00 → (0,0)
        // GCBOX (E1 08) 00 64 00 C8 00 0A 00 0A → rounded box to (100, 200) r=(10,10)
        byte[] data = new byte[]{
                0x18, 0x02,
                0x21, 0x00, 0x00, 0x00, 0x00,
                (byte) 0xE1, 0x08, 0x00, 0x64, 0x00, (byte) 0xC8, 0x00, 0x0A, 0x00, 0x0A
        };
        List<GocaDecoder.DrawOrder> orders = GocaDecoder.decodeOrders(data);
        assertEquals(2, orders.size());
        GocaDecoder.SetLineType slt = (GocaDecoder.SetLineType) orders.get(0);
        assertEquals(GocaDecoder.LineType.SHORT_DASH, slt.pattern());
        GocaDecoder.RoundedRect rr = (GocaDecoder.RoundedRect) orders.get(1);
        assertEquals(100, rr.width());
        assertEquals(200, rr.height());
        assertEquals(10, rr.rx());
        assertEquals(10, rr.ry());
    }

    @Test
    void decodesBoxOrder() {
        // GSPS 00 00 00 00 → position (0,0)
        // GBOX long-form (C0 04) 00 64 00 C8 → box to (100, 200)
        byte[] data = new byte[]{
                0x21, 0x00, 0x00, 0x00, 0x00,
                (byte) 0xC0, 0x04, 0x00, 0x64, 0x00, (byte) 0xC8
        };
        List<GocaDecoder.DrawOrder> orders = GocaDecoder.decodeOrders(data);
        assertEquals(1, orders.size());
        GocaDecoder.Rect r = (GocaDecoder.Rect) orders.get(0);
        assertEquals(0, r.x());
        assertEquals(0, r.y());
        assertEquals(100, r.width());
        assertEquals(200, r.height());
    }
}
