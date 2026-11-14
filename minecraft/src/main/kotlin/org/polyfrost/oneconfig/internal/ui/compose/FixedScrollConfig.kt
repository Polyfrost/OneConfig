// Compose has no public way to override scrolling behaviour
@file:Suppress("INVISIBLE_REFERENCE")

package org.polyfrost.oneconfig.internal.ui.compose

import androidx.compose.foundation.gestures.LocalScrollConfig
import androidx.compose.foundation.gestures.ScrollConfig
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import org.polyfrost.oneconfig.internal.ui.components.WheelRouter

/** Override Compose's scrolling behaviour to get the same scrolling on every OS */
internal class FixedScrollConfig(private val router: WheelRouter) : ScrollConfig {
    override val isSmoothScrollingEnabled: Boolean get() = false

    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset {
        if (router.defersWheelScroll()) return Offset.Zero
        var delta = Offset.Zero
        event.changes.forEach { delta += it.scrollDelta }
        return delta * -density
    }

    fun provided(): ProvidedValue<*> = LocalScrollConfig provides this
}
