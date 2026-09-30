package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.polyfrost.compose.composables.PolyRect
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.runtime.PolyComposeClock
import org.polyfrost.compose.runtime.PolyComposeRuntime

class PolyComposeChangeTrackingTest {
    @Test
    fun reportsOnlyFramesThatChangeTheTree() {
        val clock = PolyComposeClock()
        val changing = PolyComposeRuntime(clock)
        val idle = PolyComposeRuntime(clock)
        var color by mutableIntStateOf(0xFF000000.toInt())
        changing.setContent { PolyRect(PolyColor(color)) }
        idle.setContent { PolyRect(PolyColor(0xFF00FF00.toInt())) }

        assertTrue(changing.consumeChanges(), "initial composition")
        assertTrue(idle.consumeChanges(), "initial composition")

        clock.frame()
        assertFalse(changing.consumeChanges(), "frame without state changes")

        color = 0xFFFFFFFF.toInt()
        clock.frame()
        assertTrue(changing.consumeChanges(), "recomposed with a new color")
        assertFalse(changing.consumeChanges(), "changes are consumed once")
        assertFalse(idle.consumeChanges(), "another runtime on the same clock is untouched")
    }
}
