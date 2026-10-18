@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.polyfrost.oneconfig.internal.ui.compose

import androidx.compose.foundation.gestures.LocalScrollConfig
import androidx.compose.foundation.gestures.ScrollConfig
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize

/** Override Compose's scrolling behaviour to get the same scrolling on every OS */
internal object FixedScrollConfig : ScrollConfig {
    override val isSmoothScrollingEnabled: Boolean get() = false

    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset {
        var delta = Offset.Zero
        event.changes.forEach { delta += it.scrollDelta }
        return delta * -density
    }

    fun provided(): ProvidedValue<*> = LocalScrollConfig provides this
}
