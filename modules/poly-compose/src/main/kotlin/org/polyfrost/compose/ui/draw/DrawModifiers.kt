package org.polyfrost.compose.ui.draw

import androidx.compose.runtime.State
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.render.PolyFade
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

fun Modifier.fadeMask(mask: (PolyNode) -> PolyFade?) = element { fade = mask }
