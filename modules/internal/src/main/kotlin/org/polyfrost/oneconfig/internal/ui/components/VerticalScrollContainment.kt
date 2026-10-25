package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity

/** keeps scroll input inside the child list. */
@Composable
internal fun Modifier.containVerticalScroll(scrollState: ScrollState): Modifier {
    return containVerticalScroll { scrollState.maxValue > 0 }
}

/**
 * catches wheel input at the list edges.
 * keep this before the scroll modifier so the list handles it first.
 */
@Composable
internal fun Modifier.containVerticalScroll(isScrollable: () -> Boolean): Modifier {
    val currentIsScrollable = rememberUpdatedState(isScrollable)
    val connection = remember {
        VerticalScrollContainmentConnection { currentIsScrollable.value() }
    }

    return nestedScroll(connection)
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (event.type == PointerEventType.Scroll && currentIsScrollable.value()) {
                        event.changes.forEach { change ->
                            if (change.scrollDelta.y != 0f) change.consume()
                        }
                    }
                }
            }
        }
}

private class VerticalScrollContainmentConnection(
    private val hasScrollableContent: () -> Boolean,
) : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        return if (hasScrollableContent()) Offset(0f, available.y) else Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return if (hasScrollableContent()) Velocity(0f, available.y) else Velocity.Zero
    }
}
