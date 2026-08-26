package com.rit.performance.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyMigratingPasswordEncoderTest {
    private final LegacyMigratingPasswordEncoder encoder = new LegacyMigratingPasswordEncoder();

    @Test
    void acceptsLegacyPasswordAndRequiresUpgrade() {
        assertTrue(encoder.matches("temporary-password", "temporary-password"));
        assertTrue(encoder.upgradeEncoding("temporary-password"));
    }

    @Test
    void storesNewPasswordsWithBcrypt() {
        String encoded = encoder.encode("strong-password");

        assertTrue(encoder.matches("strong-password", encoded));
        assertFalse(encoder.matches("wrong-password", encoded));
        assertFalse(encoder.upgradeEncoding(encoded));
    }
}
