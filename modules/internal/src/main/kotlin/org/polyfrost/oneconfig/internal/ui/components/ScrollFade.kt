package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.shell.LocalShell

private val ScrollFadeLength = 32.dp

/**
 * Fades a page's list out toward the top and bottom edges that can still scroll, like the changelog's version list
 *
 * Unlike the kit's fadingEdges, content is drawn offscreen only while an edge is faded, and not while a mod card is
 * being dragged since the offscreen layer would clip the dragged card to the page
 */
@Composable
internal fun Modifier.scrollFade(state: ScrollableState): Modifier {
    val top by animateFloatAsState(if (state.canScrollBackward) 1f else 0f)
    val bottom by animateFloatAsState(if (state.canScrollForward) 1f else 0f)
    val shell = LocalShell.current
    return graphicsLayer {
        compositingStrategy = if ((top > 0f || bottom > 0f) && !shell.layout.modDragging) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }.drawWithContent {
        drawContent()
        if ((top <= 0f && bottom <= 0f) || shell.layout.modDragging) return@drawWithContent
        val edge = ScrollFadeLength.toPx().coerceAtMost(size.height / 2f) / size.height
        drawRect(
            Brush.verticalGradient(
                0f to Color.Black.copy(1f - top),
                edge to Color.Black,
                1f - edge to Color.Black,
                1f to Color.Black.copy(1f - bottom),
            ),
            blendMode = BlendMode.DstIn,
        )
    }
}
