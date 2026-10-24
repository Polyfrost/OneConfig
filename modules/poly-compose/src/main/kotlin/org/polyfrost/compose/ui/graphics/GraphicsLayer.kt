package org.polyfrost.compose.ui.graphics

import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.layoutElement

fun Modifier.graphicsLayer(
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    alpha: Float = 1f,
    translationX: Float = 0f,
    translationY: Float = 0f,
) = layoutElement {
    this.scaleX = scaleX
    this.scaleY = scaleY
    this.alpha *= alpha
    this.translateX = translationX
    this.translateY = translationY
}
