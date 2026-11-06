package org.polyfrost.oneconfig.internal.ui.themes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlassOpacityTest {
    @Test
    void zeroOpacityRemovesGlassAndDefaultKeepsIt() {
        assertEquals(0f, StructsKt.glassOpacity(0f, 88f));
        assertEquals(0.5f, StructsKt.glassOpacity(44f, 88f));
        assertEquals(1f, StructsKt.glassOpacity(88f, 88f));
        assertEquals(1f, StructsKt.glassOpacity(100f, 88f));
    }
}
