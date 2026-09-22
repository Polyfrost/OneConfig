package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds

@Composable
fun rememberInteractionSource() = remember { MutableInteractionSource() }

fun Modifier.onClick(interactionSource: MutableInteractionSource, onClick: () -> Unit) =
    onClick(interactionSource, true, onClick)

fun Modifier.onClick(interactionSource: MutableInteractionSource, enabled: Boolean, onClick: () -> Unit) = clickable(
    interactionSource = interactionSource,
    indication = null,
    enabled = enabled,
    onClick = {
        UiSounds.play(UiSoundEvent.CLICK)
        onClick()
    }
)

/**
 * Swallows press and release events before children see them so anything below cannot be clicked dragged
 * or focused
 *
 * Hover move and scroll events are left alone so tooltips and page scrolling keep working
 */
fun Modifier.blockInteraction(blocked: Boolean = true): Modifier =
    if (!blocked) this else pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Press || event.type == PointerEventType.Release) {
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
