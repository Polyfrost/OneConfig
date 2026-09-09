package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun RetainedVisibility(
    visible: Boolean,
    enter: FiniteAnimationSpec<Float>,
    exit: FiniteAnimationSpec<Float>,
    modifier: Modifier = Modifier,
    alphaMultiplier: Float = 1f,
    hiddenScale: Float = 0.9f,
    openKey: Any? = null,
    content: @Composable (alpha: Float) -> Unit,
) {
    val progress = remember(openKey) { Animatable(0f) }
    LaunchedEffect(openKey, visible) {
        progress.animateTo(if (visible) 1f else 0f, if (visible) enter else exit)
    }

    val alpha = progress.value * alphaMultiplier
    val scale = hiddenScale + (1f - hiddenScale) * progress.value
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
