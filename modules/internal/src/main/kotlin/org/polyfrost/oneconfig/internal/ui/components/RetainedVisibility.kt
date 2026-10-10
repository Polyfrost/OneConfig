package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

class ShellTransition internal constructor(
    private val alphaProvider: () -> Float,
    val hiddenScale: Float,
) {
    val alpha: Float get() = alphaProvider()

    val scale: Float get() = hiddenScale + (1f - hiddenScale) * alpha
}

val LocalShellTransition = staticCompositionLocalOf { ShellTransition({ 1f }, 1f) }

@Composable
fun Modifier.shellTransition(): Modifier {
    val shell = LocalShellTransition.current
    return graphicsLayer {
        this.alpha = shell.alpha
        val shellScale = shell.scale
        scaleX = shellScale
        scaleY = shellScale
        this.transformOrigin = TransformOrigin.Center
    }
}

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

    val progressState = remember(progress) { derivedStateOf { progress.value } }
    val alphaMultiplierState = rememberUpdatedState(alphaMultiplier)
    val shellTransition = remember(progress, hiddenScale) {
        ShellTransition(
            alphaProvider = { progressState.value * alphaMultiplierState.value },
            hiddenScale = hiddenScale,
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
    ) {
        CompositionLocalProvider(LocalShellTransition provides shellTransition) {
            content(alpha)
        }
    }
}
