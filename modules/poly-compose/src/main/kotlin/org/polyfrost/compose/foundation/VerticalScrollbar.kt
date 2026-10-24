package org.polyfrost.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.animation.core.tween
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.dp

@Composable
fun VerticalScrollbar(
    state: ScrollState,
    color: PolyColor,
    hoverColor: PolyColor,
    modifier: Modifier = Modifier,
    thickness: Dp = 8.dp,
    minThumb: Dp = 24.dp,
    radius: Dp = thickness / 2,
    hoverMillis: Int = 300,
) {
    var hovered by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val mix by animateFloatAsState(if (hovered || dragging) 1f else 0f, tween(hoverMillis))
    val grab = remember { floatArrayOf(0f) }

    Canvas(
        modifier.width(thickness).onPointerEvent { e ->
            when (e.type) {
                PointerEventType.Enter -> { hovered = true; false }
                PointerEventType.Exit -> { hovered = false; false }
                PointerEventType.Press -> {
                    if (e.button != 0 || state.maxValue <= 0f) return@onPointerEvent false
                    val (len, pos) = ScrollState.thumb(state, e.node.height, minThumb.value)
                    val local = e.y - e.node.y
                    when {
                        local < pos -> state.animateBy(-state.viewport)
                        local > pos + len -> state.animateBy(state.viewport)
                        else -> { grab[0] = local - pos; dragging = true }
                    }
                    true
                }
                PointerEventType.Move -> {
                    if (dragging) {
                        val (len, _) = ScrollState.thumb(state, e.node.height, minThumb.value)
                        val room = e.node.height - len
                        if (room > 0f) state.scrollTo((e.y - e.node.y - grab[0]) / room * state.maxValue)
                    }
                    true
                }
                PointerEventType.Release -> { dragging = false; true }
                else -> false
            }
        },
    ) { x, y, w, h ->
        if (state.maxValue <= 0f) return@Canvas
        val (len, pos) = ScrollState.thumb(state, h, minThumb.value)
        rect(x, y + pos, w, len, color.lerp(hoverColor, mix), radius.value)
    }
}
