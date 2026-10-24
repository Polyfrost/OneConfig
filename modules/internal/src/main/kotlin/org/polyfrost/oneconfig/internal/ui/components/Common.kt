package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.polyfrost.compose.foundation.clickable
import org.polyfrost.compose.foundation.interaction.MutableInteractionSource
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.input.pointer.inert
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

@Suppress("UNUSED_PARAMETER")
fun Modifier.blockInteraction(blocked: Boolean = true, keyboard: Boolean = false): Modifier =
    if (!blocked) this else inert()
