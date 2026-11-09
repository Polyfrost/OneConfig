package org.polyfrost.oneconfig.internal.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyVideoModeFormatTest {
    @Test
    void readsBothFormats() {
        assertArrayEquals(new int[]{1920, 1080, 144}, LegacyVideoModeFormat.toSize("1920x1080@144:24"));
        assertArrayEquals(new int[]{1280, 720, 60}, LegacyVideoModeFormat.toSize("1280x720"));
        assertArrayEquals(new int[]{2560, 1440, 60}, LegacyVideoModeFormat.toSize(
            "{\"width\":2560,\"height\":1440,\"red_bits\":8,\"green_bits\":8,\"blue_bits\":8,\"refresh_rate\":59.94}"));
    }

    @Test
    void rejectsGarbage() {
        assertNull(LegacyVideoModeFormat.toSize(null));
        assertNull(LegacyVideoModeFormat.toSize(""));
        assertNull(LegacyVideoModeFormat.toSize("{\"width\":1920}"));
        assertNull(LegacyVideoModeFormat.toSize("0x0@60:24"));
    }

    @Test
    void detectsLegacyFormat() {
        assertTrue(LegacyVideoModeFormat.isLegacy("1920x1080@144:24"));
        assertFalse(LegacyVideoModeFormat.isLegacy(LegacyVideoModeFormat.toJson("1920x1080@144:24")));
        assertFalse(LegacyVideoModeFormat.isLegacy(null));
        assertFalse(LegacyVideoModeFormat.isLegacy("garbage"));
    }
}
