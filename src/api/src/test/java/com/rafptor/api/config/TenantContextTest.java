package com.rafptor.api.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantContextTest {

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void setThenGet() {
        TenantContext.set("tenant-1");
        assertEquals("tenant-1", TenantContext.get());
    }

    @Test
    void getOrNullIsLenient() {
        assertNull(TenantContext.getOrNull());
    }

    @Test
    void getRaisesWhenUnset() {
        assertThrows(IllegalStateException.class, TenantContext::get);
    }
}
