/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 */

package org.polyfrost.oneconfig.api.config.v1;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.annotations.Text;
import org.polyfrost.oneconfig.api.config.v1.collect.impl.OneConfigCollector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextTest {
    @Test
    void collectsSecureMetadata() {
        Tree tree = new OneConfigCollector().collect(new SecureTextConfig());

        Property<?> plain = tree.getProp("plain");
        assertEquals(false, plain.getMetadata("secure"));
        assertEquals(Character.valueOf('*'), plain.getMetadata("secureMask"));
        assertEquals(Integer.valueOf(0), plain.getMetadata("secureShowLast"));
        assertNull(plain.getMetadata("secureShowChars"));

        Property<?> key = tree.getProp("key");
        assertEquals(true, key.getMetadata("secure"));
        assertEquals(Character.valueOf('#'), key.getMetadata("secureMask"));
        assertEquals(Integer.valueOf(4), key.getMetadata("secureShowLast"));
        assertEquals("-", key.getMetadata("secureShowChars"));
    }

    @Test
    void rejectsNegativeShowLast() {
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> new OneConfigCollector().collect(new InvalidShowLastConfig())
        );
        assertTrue(hasMessage(exception, "@Text field 'key' has a negative secureShowLast"));
    }

    private static boolean hasMessage(Throwable throwable, String text) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(text)) return true;
        }
        return false;
    }

    private static final class SecureTextConfig extends Config {
        @Text(title = "Plain")
        private String plain = "";

        @Text(title = "Key", secure = true, secureMask = '#', secureShowLast = 4, secureShowChars = "-")
        private String key = "";

        private SecureTextConfig() {
            super("secure-text-test.json", "Secure Text Test", Category.OTHER);
        }
    }

    private static final class InvalidShowLastConfig extends Config {
        @Text(title = "Key", secure = true, secureShowLast = -1)
        private String key = "";

        private InvalidShowLastConfig() {
            super("invalid-secure-text-test.json", "Invalid Secure Text Test", Category.OTHER);
        }
    }
}
