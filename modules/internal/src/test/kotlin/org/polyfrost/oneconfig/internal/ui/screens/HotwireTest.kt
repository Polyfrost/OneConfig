package org.polyfrost.oneconfig.internal.ui.screens

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HotwireTest {
    @Test
    fun noMoreModsArePickedThanASwitchTakes() {
        val picked = mutableSetOf("a", "b")
        assertFalse(picked.pick("c", most = 2))
        assertTrue(picked.pick("a", most = 2))
        assertEquals(setOf("b"), picked)
    }
}
