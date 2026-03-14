package org.polyfrost.compose.composables

import org.polyfrost.compose.layout.*
import org.polyfrost.compose.render.PolyColor

open class PolyModifier internal constructor(internal val block: PolyLayoutStyle.() -> Unit) {
    fun then(other: PolyModifier) = PolyModifier { block(); other.block(this) }
    fun applyTo(style: PolyLayoutStyle) = style.block()

    companion object : PolyModifier({})
}

fun PolyModifier.size(w: Float, h: Float) = then(PolyModifier { width = PolySize.Px(w); height = PolySize.Px(h) })
fun PolyModifier.size(w: PolySize, h: PolySize) = then(PolyModifier { width = w; height = h })
fun PolyModifier.width(w: Float) = then(PolyModifier { width = PolySize.Px(w) })
fun PolyModifier.width(w: PolySize) = then(PolyModifier { width = w })
fun PolyModifier.height(h: Float) = then(PolyModifier { height = PolySize.Px(h) })
fun PolyModifier.height(h: PolySize) = then(PolyModifier { height = h })
fun PolyModifier.fillWidth() = then(PolyModifier { width = PolySize.Fill })
fun PolyModifier.fillHeight() = then(PolyModifier { height = PolySize.Fill })
fun PolyModifier.fill() = then(PolyModifier { width = PolySize.Fill; height = PolySize.Fill })
fun PolyModifier.wrapWidth() = then(PolyModifier { width = PolySize.Wrap })
fun PolyModifier.wrapHeight() = then(PolyModifier { height = PolySize.Wrap })

fun PolyModifier.padding(all: Float) = then(PolyModifier { padding = PolyInsets(all) })
fun PolyModifier.padding(horizontal: Float = 0f, vertical: Float = 0f) = then(PolyModifier { padding = PolyInsets(horizontal, vertical) })
fun PolyModifier.padding(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f) = then(PolyModifier { padding = PolyInsets(left, top, right, bottom) })
fun PolyModifier.padding(insets: PolyInsets) = then(PolyModifier { padding = insets })

fun PolyModifier.margin(all: Float) = then(PolyModifier { margin = PolyInsets(all) })
fun PolyModifier.margin(horizontal: Float = 0f, vertical: Float = 0f) = then(PolyModifier { margin = PolyInsets(horizontal, vertical) })
fun PolyModifier.margin(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f) = then(PolyModifier { margin = PolyInsets(left, top, right, bottom) })
fun PolyModifier.margin(insets: PolyInsets) = then(PolyModifier { margin = insets })

fun PolyModifier.background(color: PolyColor, radius: Float = 0f) = then(PolyModifier { background = color; this.radius = radius })
fun PolyModifier.border(color: PolyColor, width: Float = 1f, radius: Float = 0f) = then(PolyModifier { borderColor = color; borderWidth = width; if (radius > 0f) this.radius = radius })
fun PolyModifier.radius(r: Float) = then(PolyModifier { radius = r })
fun PolyModifier.clip(radius: Float = 0f) = then(PolyModifier { clipToBounds = true; if (radius > 0f) this.radius = radius })
fun PolyModifier.alpha(a: Float) = then(PolyModifier { alpha = a })
fun PolyModifier.align(a: PolyAlign) = then(PolyModifier { align = a })

fun PolyModifier.absoluteAt(x: Float, y: Float) = then(PolyModifier {
    positionMode = PolyPositionMode.Absolute
    this.x = x
    this.y = y
})
