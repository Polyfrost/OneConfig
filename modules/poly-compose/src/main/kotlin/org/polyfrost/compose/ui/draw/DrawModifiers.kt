package org.polyfrost.compose.ui.draw

import androidx.compose.runtime.State
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.render.PolyFade
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.applyShape
import org.polyfrost.compose.ui.element
import org.polyfrost.compose.ui.graphics.RectangleShape
import org.polyfrost.compose.ui.graphics.Shape
import org.polyfrost.compose.ui.graphics.shadow.Shadow
import org.polyfrost.compose.ui.layoutElement

fun Modifier.alpha(alpha: Float) = layoutElement { this.alpha = alpha }

fun Modifier.alpha(alpha: State<Float>, target: Float) = layoutElement { this.alpha = target; animatedAlpha = alpha }

fun Modifier.clip(shape: Shape = RectangleShape) = element { clipToBounds = true; applyShape(shape) }

fun Modifier.dropShadow(shape: Shape, shadow: Shadow) = element { shadows = shadows + shadow; applyShape(shape) }

fun Modifier.scale(scale: Float) = layoutElement { scaleX = scale; scaleY = scale }

class ContentDrawScope(val ctx: RenderContext, private val node: PolyNode) {
    val x get() = node.x
    val y get() = node.y
    val width get() = node.width
    val height get() = node.height

    fun drawContent() = node.render(ctx)
}

fun Modifier.drawWithContent(onDraw: ContentDrawScope.() -> Unit) = element { drawWithContent = onDraw }

fun Modifier.fadeMask(mask: (PolyNode) -> PolyFade?) = element { fade = mask }
