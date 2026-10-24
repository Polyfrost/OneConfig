package org.polyfrost.oneconfig.internal.ui.components.dropdown

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.internal.ui.components.ShellPopup
import org.polyfrost.oneconfig.internal.ui.components.shellTransition
import javax.swing.Box

class DropdownPositionProvider(
    val contentOffset: DpOffset,
    val density: Density,
    val onPositionCalculated: (IntRect, IntRect) -> Unit = { _, _ -> }
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val contentOffsetX = with(density) { contentOffset.x.roundToPx() }
        val contentOffsetY = with(density) { contentOffset.y.roundToPx() }

        val toRight = anchorBounds.left + contentOffsetX
        val toLeft = anchorBounds.right - contentOffsetX - popupContentSize.width
        val toDisplayRight = windowSize.width - popupContentSize.width
        val toDisplayLeft = 0
        val x = if (layoutDirection == LayoutDirection.Ltr) {
            sequenceOf(toRight, toLeft, toDisplayRight)
        } else {
            sequenceOf(toLeft, toRight, toDisplayLeft)
        }.firstOrNull {
            it >= 0 && it + popupContentSize.width <= windowSize.width
        } ?: toLeft

        val spaceAbove = anchorBounds.top - contentOffsetY
        val spaceBelow = windowSize.height - anchorBounds.bottom - contentOffsetY
        var y = if (spaceBelow >= spaceAbove) {
            anchorBounds.bottom + contentOffsetY
        } else {
            anchorBounds.top - contentOffsetY - popupContentSize.height
        }

        if (y + popupContentSize.height > windowSize.height) {
            y = windowSize.height - popupContentSize.height
        }

        y = y.coerceAtLeast(0)

        onPositionCalculated(
            anchorBounds,
            IntRect(x, y, x + popupContentSize.width, y + popupContentSize.height)
        )
        return IntOffset(x, y)
    }
}

@Composable
fun SimpleDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    focusable: Boolean = true,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    pinAnchor: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val expandedStates = remember { MutableTransitionState(false) }
    expandedStates.targetState = expanded

    if (expandedStates.currentState || expandedStates.targetState) {
        val density = LocalDensity.current
        val pinnedAnchor = remember { mutableStateOf<IntRect?>(null) }
        val dropdownPositionProvider = remember(offset, density) { DropdownPositionProvider(offset, density) }
        val popupPositionProvider = if (!pinAnchor) dropdownPositionProvider else PinnedAnchorPositionProvider(dropdownPositionProvider, pinnedAnchor)

        ShellPopup(
            popupPositionProvider = popupPositionProvider,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = focusable)
        ) {
            SimpleDropdownMenuContent(
                expandedStates = expandedStates,
                modifier = modifier,
                content = content
            )
        }
    }
}

private class PinnedAnchorPositionProvider(
    private val provider: PopupPositionProvider,
    private val anchor: MutableState<IntRect?>,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val pinned = anchor.value ?: anchorBounds.also { anchor.value = it }
        return provider.calculatePosition(pinned, windowSize, layoutDirection, popupContentSize)
    }
}

@Composable
fun SimpleDropdownMenuContent(
    expandedStates: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberTransition(expandedStates, "SimpleDropDownMenu")
    val alpha by transition.animateFloat(transitionSpec = { tween() }) { expanded ->
        if (expanded) 1f else 0f
    }

    Box(
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha }
            .shellTransition()
            .then(modifier)
    ) {
        content()
    }
}
