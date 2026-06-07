package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds

@Composable
fun rememberInteractionSource() = remember { MutableInteractionSource() }

fun Modifier.onClick(interactionSource: MutableInteractionSource, onClick: () -> Unit) = clickable(
    interactionSource = interactionSource,
    indication = null,
    onClick = {
        UiSounds.play(UiSoundEvent.CLICK)
        onClick()
    }
)
