package org.polyfrost.compose.composables

import org.polyfrost.compose.layout.*
import org.polyfrost.compose.render.PolyColor

public open class PolyModifier internal constructor(internal val block: PolyLayoutStyle.() -> Unit) {
    public fun then(other: PolyModifier): PolyModifier = PolyModifier { block(); other.block(this) }

    public fun applyTo(style: PolyLayoutStyle) {
        val lt = style.layoutType
        val g = style.gap
        style.width = PolySize.Wrap
        style.height = PolySize.Wrap
        style.align = PolyAlign.TopLeft
        style.padding = PolyInsets()
        style.margin = PolyInsets()
        style.positionMode = PolyPositionMode.Relative
        style.x = 0f
        style.y = 0f
        style.background = PolyColor.TRANSPARENT
        style.radius = 0f
        style.borderColor = PolyColor.TRANSPARENT
        style.borderWidth = 0f
        style.clipToBounds = false
        style.alpha = 1f
        style.fade = null
        style.tag = null
        style.layoutType = lt
        style.gap = g
        style.block()
    }

    public companion object : PolyModifier({})
}

public fun PolyModifier.size(w: Float, h: Float): PolyModifier = then(PolyModifier { width = PolySize.Px(w); height = PolySize.Px(h) })
public fun PolyModifier.size(w: PolySize, h: PolySize): PolyModifier = then(PolyModifier { width = w; height = h })
public fun PolyModifier.width(w: Float): PolyModifier = then(PolyModifier { width = PolySize.Px(w) })
public fun PolyModifier.width(w: PolySize): PolyModifier = then(PolyModifier { width = w })
public fun PolyModifier.height(h: Float): PolyModifier = then(PolyModifier { height = PolySize.Px(h) })
public fun PolyModifier.height(h: PolySize): PolyModifier = then(PolyModifier { height = h })
public fun PolyModifier.fillWidth(): PolyModifier = then(PolyModifier { width = PolySize.Fill })
public fun PolyModifier.fillHeight(): PolyModifier = then(PolyModifier { height = PolySize.Fill })
public fun PolyModifier.fill(): PolyModifier = then(PolyModifier { width = PolySize.Fill; height = PolySize.Fill })
public fun PolyModifier.wrapWidth(): PolyModifier = then(PolyModifier { width = PolySize.Wrap })
public fun PolyModifier.wrapHeight(): PolyModifier = then(PolyModifier { height = PolySize.Wrap })

public fun PolyModifier.padding(all: Float): PolyModifier = then(PolyModifier { padding = PolyInsets(all) })
public fun PolyModifier.padding(horizontal: Float = 0f, vertical: Float = 0f): PolyModifier = then(PolyModifier { padding = PolyInsets(horizontal, vertical) })
public fun PolyModifier.padding(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f): PolyModifier = then(PolyModifier { padding = PolyInsets(left, top, right, bottom) })
public fun PolyModifier.padding(insets: PolyInsets): PolyModifier = then(PolyModifier { padding = insets })

public fun PolyModifier.margin(all: Float): PolyModifier = then(PolyModifier { margin = PolyInsets(all) })
public fun PolyModifier.margin(horizontal: Float = 0f, vertical: Float = 0f): PolyModifier = then(PolyModifier { margin = PolyInsets(horizontal, vertical) })
public fun PolyModifier.margin(left: Float = 0f, top: Float = 0f, right: Float = 0f, bottom: Float = 0f): PolyModifier = then(PolyModifier { margin = PolyInsets(left, top, right, bottom) })
public fun PolyModifier.margin(insets: PolyInsets): PolyModifier = then(PolyModifier { margin = insets })

public fun PolyModifier.background(color: PolyColor, radius: Float = 0f): PolyModifier = then(PolyModifier { background = color; this.radius = radius })
public fun PolyModifier.border(color: PolyColor, width: Float = 1f, radius: Float = 0f): PolyModifier = then(PolyModifier { borderColor = color; borderWidth = width; if (radius > 0f) this.radius = radius })
public fun PolyModifier.radius(r: Float): PolyModifier = then(PolyModifier { radius = r })
public fun PolyModifier.clip(radius: Float = 0f): PolyModifier = then(PolyModifier { clipToBounds = true; if (radius > 0f) this.radius = radius })
public fun PolyModifier.alpha(a: Float): PolyModifier = then(PolyModifier { alpha = a })
public fun PolyModifier.fade(alpha: (() -> Float)?): PolyModifier = then(PolyModifier { fade = alpha })
public fun PolyModifier.align(a: PolyAlign): PolyModifier = then(PolyModifier { align = a })

public fun PolyModifier.tag(value: Any?): PolyModifier = then(PolyModifier { tag = value })

public fun PolyModifier.absoluteAt(x: Float, y: Float): PolyModifier = then(PolyModifier {
    positionMode = PolyPositionMode.Absolute
    this.x = x
    this.y = y
})
