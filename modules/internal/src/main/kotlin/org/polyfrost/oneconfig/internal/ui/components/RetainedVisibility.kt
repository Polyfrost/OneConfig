package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

class ShellTransition internal constructor(
    private val progressProvider: () -> Float,
    private val alphaProvider: () -> Float,
    val hiddenScale: Float,
) {
    val alpha: Float get() = alphaProvider()

    val scale: Float get() = hiddenScale + (1f - hiddenScale) * progressProvider()
}

val LocalShellTransition = staticCompositionLocalOf { ShellTransition({ 1f }, { 1f }, 1f) }
val LocalShellPivot = staticCompositionLocalOf<State<Offset>?> { null }

@Composable
fun Modifier.shellTransition(): Modifier {
    val shell = LocalShellTransition.current
    val pivot = LocalShellPivot.current
    return graphicsLayer {
        this.alpha = shell.alpha
        val shellScale = shell.scale
        scaleX = shellScale
        scaleY = shellScale
        this.transformOrigin = pivot?.value?.toTransformOrigin(size) ?: TransformOrigin.Center
    }
}

private fun Offset.toTransformOrigin(size: Size): TransformOrigin? {
    if (this == Offset.Unspecified || size.width <= 0f || size.height <= 0f) return null
    return TransformOrigin(x / size.width, y / size.height)
}

class ShellPivotPositionProvider(
    private val delegate: PopupPositionProvider,
    private val pivot: MutableState<Offset>,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val position = delegate.calculatePosition(anchorBounds, windowSize, layoutDirection, popupContentSize)
        pivot.value = (anchorBounds.topLeft - position).toOffset()
        return position
    }
}

private class AlignmentOffsetPositionProvider(
    private val alignment: Alignment,
    private val offset: IntOffset,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchorAlignmentPoint = alignment.align(IntSize.Zero, anchorBounds.size, layoutDirection)
        val popupAlignmentPoint = -alignment.align(IntSize.Zero, popupContentSize, layoutDirection)
        val resolvedOffset = IntOffset(offset.x * (if (layoutDirection == LayoutDirection.Ltr) 1 else -1), offset.y)
        return anchorBounds.topLeft + anchorAlignmentPoint + popupAlignmentPoint + resolvedOffset
    }
}

@Composable
fun ShellPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit,
) {
    val pivot = remember { mutableStateOf(Offset.Unspecified) }
    val provider = remember(popupPositionProvider) { ShellPivotPositionProvider(popupPositionProvider, pivot) }
    Popup(
        popupPositionProvider = provider,
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        CompositionLocalProvider(LocalShellPivot provides pivot, content = content)
    }
}

@Composable
fun ShellPopup(
    alignment: Alignment = Alignment.TopStart,
    offset: IntOffset = IntOffset.Zero,
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    content: @Composable () -> Unit,
) {
    val provider = remember(alignment, offset) { AlignmentOffsetPositionProvider(alignment, offset) }
    ShellPopup(provider, onDismissRequest, properties, content)
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
            progressProvider = { progressState.value },
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
