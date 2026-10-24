package org.polyfrost.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.polyfrost.compose.layout.LayoutType
import org.polyfrost.compose.node.CanvasNode
import org.polyfrost.compose.node.ImageNode
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.runtime.PolyApplier
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.geometry.Size
import org.polyfrost.compose.ui.graphics.drawscope.DrawScope
import org.polyfrost.compose.ui.graphics.ColorFilter
import org.polyfrost.compose.ui.graphics.FilterQuality
import org.polyfrost.compose.ui.graphics.ImageBitmap

@Composable
fun Image(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = FilterQuality.Low,
) {
    ComposeNode<ImageNode, PolyApplier>(
        factory = { ImageNode(bitmap).also { it.style.layoutType = LayoutType.Box } },
        update = {
            set(bitmap) { image = it; remeasure() }
            set(colorFilter) { tint = it?.color }
            set(filterQuality) { this.filterQuality = it }
            set(modifier) { it.applyTo(style); autoWidth = false; autoHeight = false; remeasure() }
        },
    )
}

@Composable
fun Canvas(
    modifier: Modifier,
    live: Boolean = false,
    onDraw: RenderContext.(x: Float, y: Float, width: Float, height: Float) -> Unit,
) {
    ComposeNode<CanvasNode, PolyApplier>(
        factory = { CanvasNode() },
        update = {
            set(live) { this.live = it }
            set(modifier) { it.applyTo(style) }
            set(onDraw) { this.onDraw = it }
        },
    )
}

@Composable
fun Canvas(modifier: Modifier, live: Boolean = false, onDraw: DrawScope.() -> Unit) =
    Canvas(modifier, live) { x, y, w, h -> DrawScope(this, x, y, Size(w, h)).onDraw() }
