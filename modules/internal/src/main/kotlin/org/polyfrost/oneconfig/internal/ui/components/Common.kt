package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds

@Deprecated("Use rememberInteractionSource from org.polyfrost.oneconfig.api.ui.v1.components. Removed in OneConfig 1.6.0")
@Composable
fun rememberInteractionSource() = remember { MutableInteractionSource() }

@Deprecated("Use onClick from org.polyfrost.oneconfig.api.ui.v1.components. Removed in OneConfig 1.6.0")
fun Modifier.onClick(interactionSource: MutableInteractionSource, onClick: () -> Unit) =
    onClick(interactionSource, true, onClick)

@Deprecated("Use onClick from org.polyfrost.oneconfig.api.ui.v1.components. Removed in OneConfig 1.6.0")
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
 * Swallows press and release events before children see them so anything below cannot be clicked, dragged
 * or focused
 *
 * Hover move and scroll events are left alone so tooltips and page scrolling keep working
 *
 * @param keyboard also keeps focus and every key but tab and escape out
 */
fun Modifier.blockInteraction(blocked: Boolean = true, keyboard: Boolean = false): Modifier =
    if (!blocked) this else (
        if (keyboard) focusProperties { canFocus = false }.onPreviewKeyEvent { it.key != Key.Tab && it.key != Key.Escape }
        else this
        ).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press || event.type == PointerEventType.Release) {
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
