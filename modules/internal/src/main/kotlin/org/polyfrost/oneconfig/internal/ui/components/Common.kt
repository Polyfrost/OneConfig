package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.clickable
import org.polyfrost.compose.foundation.interaction.MutableInteractionSource
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.LinearGradient
import org.polyfrost.compose.ui.graphics.RadialGradient
import org.polyfrost.compose.ui.graphics.RectangleShape
import org.polyfrost.compose.ui.graphics.Shape
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

fun Modifier.background(brush: Brush, shape: Shape = RectangleShape, alpha: Float): Modifier = when {
    alpha <= 0f -> this
    alpha >= 1f -> background(brush, shape)
    else -> background(brush.withAlpha(alpha), shape)
}

private fun Brush.withAlpha(alpha: Float): Brush = when (this) {
    is LinearGradient -> copy(colors = colors.map { it.withAlpha(it.alphaF * alpha) })
    is RadialGradient -> copy(colors = colors.map { it.withAlpha(it.alphaF * alpha) })
}
