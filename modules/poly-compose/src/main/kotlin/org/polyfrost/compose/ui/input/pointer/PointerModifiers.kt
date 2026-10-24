package org.polyfrost.compose.ui.input.pointer

import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.element
import org.polyfrost.compose.ui.layoutElement

fun Modifier.onPointerEvent(handler: (PointerEvent) -> Boolean) = element { onPointer += handler }

fun Modifier.pointerHoverIcon(icon: PointerIcon) = element { cursor = icon }

fun Modifier.inert(enabled: Boolean = true) = layoutElement { inert = enabled }
