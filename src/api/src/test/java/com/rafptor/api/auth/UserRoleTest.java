package com.rafptor.api.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserRoleTest {

    @Test
    void authorityPrefixesRole() {
        assertEquals("ROLE_ADMIN", UserRole.ADMIN.authority());
        assertEquals("ROLE_VIEWER", UserRole.VIEWER.authority());
    }
}
