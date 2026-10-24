package org.polyfrost.oneconfig.internal.ui.components.dropdown

import androidx.compose.runtime.*
import org.polyfrost.compose.animation.core.*
import org.polyfrost.compose.foundation.layout.*
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.window.Popup
import org.polyfrost.compose.ui.window.PopupProperties

@Composable
fun SimpleDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    focusable: Boolean = true,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val alpha by animateFloatAsState(if (expanded) 1f else 0f, tween())

    if (expanded || alpha != 0f) {
        Popup(
            alignment = Alignment.TopStart,
            offset = offset,
            onDismissRequest = if (expanded) onDismissRequest else null,
            properties = PopupProperties(focusable = focusable),
            anchor = Alignment.BottomStart,
            roomier = true,
        ) {
            SimpleDropdownMenuContent(
                alpha = alpha,
                modifier = modifier,
                content = content
            )
        }
    }
}

@Composable
fun SimpleDropdownMenuContent(
    alpha: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .alpha(alpha)
            .then(modifier)
    ) {
        content()
    }
}
