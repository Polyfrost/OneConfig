package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * fades and scales [content] in and out, and keeps it composed while it is hidden
 *
 * AnimatedVisibility disposes its content when the exit ends, which for a retained scene hands
 * back the whole cost of an open. this animates the same way and simply stops drawing.
 *
 * the chain is the same shape either way on purpose: a modifier that comes and goes adds and
 * removes nodes Compose indexes for hit testing, and toggling one took the scene down.
 */
@Composable
fun RetainedVisibility(
    visible: Boolean,
    enter: FiniteAnimationSpec<Float>,
    exit: FiniteAnimationSpec<Float>,
    modifier: Modifier = Modifier,
    alphaMultiplier: Float = 1f,
    hiddenScale: Float = 0.9f,
    content: @Composable (alpha: Float) -> Unit,
) {
    val transition = updateTransition(visible, label = "retainedVisibility")
    val spec: @Composable Transition.Segment<Boolean>.() -> FiniteAnimationSpec<Float> = {
        if (targetState) enter else exit
    }
    val progress by transition.animateFloat(spec, "retainedVisibilityAlpha") { if (it) 1f else 0f }
    val scale by transition.animateFloat(spec, "retainedVisibilityScale") { if (it) 1f else hiddenScale }

    val alpha = progress * alphaMultiplier
    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
    ) {
        content(alpha)
    }
}
