package org.polyfrost.compose.foundation

import org.polyfrost.compose.foundation.interaction.MutableInteractionSource
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.element
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.layoutElement

fun Modifier.clickable(enabled: Boolean = true, onHover: ((Boolean) -> Unit)? = null, onClick: () -> Unit) = element {
    onPointer += {
        when (it.type) {
            PointerEventType.Enter -> { onHover?.invoke(true); false }
            PointerEventType.Exit -> { onHover?.invoke(false); false }
            PointerEventType.Press -> enabled && it.button == 0
            PointerEventType.Release -> { if (enabled && it.inside && it.button == 0) onClick(); enabled && it.button == 0 }
            else -> false
        }
    }
}

interface Indication

fun Modifier.clickable(
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    enabled: Boolean = true,
    onClick: () -> Unit,
) = element {
    onPointer += {
        when (it.type) {
            PointerEventType.Enter -> { interactionSource?.hovered?.value = true; false }
            PointerEventType.Exit -> { interactionSource?.hovered?.value = false; false }
            PointerEventType.Press -> (enabled && it.button == 0).also { p -> if (p) interactionSource?.pressed?.value = true }
            PointerEventType.Release -> {
                interactionSource?.pressed?.value = false
                if (enabled && it.inside && it.button == 0) onClick()
                enabled && it.button == 0
            }
            else -> false
        }
    }
}

fun Modifier.hoverable(interactionSource: MutableInteractionSource, enabled: Boolean = true) = element {
    onPointer += {
        when (it.type) {
            PointerEventType.Enter -> { if (enabled) interactionSource.hovered.value = true; false }
            PointerEventType.Exit -> { interactionSource.hovered.value = false; false }
            else -> false
        }
    }
}

fun Modifier.hoverable(onHover: (Boolean) -> Unit) = element {
    onPointer += {
        when (it.type) {
            PointerEventType.Enter -> { onHover(true); false }
            PointerEventType.Exit -> { onHover(false); false }
            else -> false
        }
    }
}

fun Modifier.focusable(request: (() -> Boolean)? = null, onFocus: ((Boolean) -> Unit)? = null) =
    layoutElement { focusable = true; this.onFocus = onFocus; focusRequest = request }

fun Modifier.focusable(enabled: Boolean = true, interactionSource: MutableInteractionSource?) =
    if (!enabled) this else layoutElement { focusable = true; onFocus = { f -> interactionSource?.focused?.value = f } }
