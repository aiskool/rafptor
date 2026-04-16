package com.rafptor.converter.font;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EbcdicDecoderTest {

    @Test
    void decodesIBM500() {
        byte[] bytes = "HELLO".getBytes(Charset.forName("IBM500"));
        assertEquals("HELLO", EbcdicDecoder.ibm500().decodeAll(bytes));
    }

    @Test
    void decodesFrenchIBM1147() {
        byte[] bytes = "ÉTÉ".getBytes(Charset.forName("IBM1147"));
        assertEquals("ÉTÉ", EbcdicDecoder.ibm1147().decodeAll(bytes));
    }
}
