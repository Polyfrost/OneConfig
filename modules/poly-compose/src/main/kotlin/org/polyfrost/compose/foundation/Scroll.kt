package org.polyfrost.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.element
import org.polyfrost.compose.ui.input.pointer.PointerEventType

@Composable
fun rememberScrollState(initial: Float = 0f) = remember { ScrollState(initial) }

fun Modifier.verticalScroll(state: ScrollState) = element {
    scroll = state
    clipToBounds = true
    onPointer += { it.type == PointerEventType.Scroll && state.wheel(it.scroll) }
}

fun Modifier.horizontalScroll(state: ScrollState) = element {
    scroll = state
    scrollHorizontal = true
    clipToBounds = true
    onPointer += { it.type == PointerEventType.Scroll && state.wheel(it.scroll) }
}
