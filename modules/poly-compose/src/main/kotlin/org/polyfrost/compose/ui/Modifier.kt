package org.polyfrost.compose.ui

import org.polyfrost.compose.layout.PolyInsets
import org.polyfrost.compose.layout.PolyLayoutStyle
import org.polyfrost.compose.layout.PolyPositionMode
import org.polyfrost.compose.layout.PolySize
import org.polyfrost.compose.render.PolyColor

open class Modifier internal constructor(internal val block: PolyLayoutStyle.() -> Unit) {
    infix fun then(other: Modifier): Modifier = if (other === Companion) this else if (this === Companion) other else Modifier { block(); other.block(this) }

    fun applyTo(style: PolyLayoutStyle) {
        val lt = style.layoutType
        val g = style.gap
        val arr = style.arrangement
        val cross = style.crossAlignment
        val content = style.contentAlignment
        style.width = PolySize.Wrap
        style.height = PolySize.Wrap
        style.align = null
        style.crossBias = Float.NaN
        style.padding = PolyInsets()
        style.margin = PolyInsets()
        style.positionMode = PolyPositionMode.Relative
        style.x = 0f
        style.y = 0f
        style.background = PolyColor.TRANSPARENT
        style.radius = 0f
        style.radiusPercent = -1f
        style.borderColor = PolyColor.TRANSPARENT
        style.borderWidth = 0f
        style.gradient = null
        style.shadows = emptyList()
        style.clipToBounds = false
        style.alpha = 1f
        style.tag = null
        style.onPointer = emptyList()
        style.onKey = emptyList()
        style.borderColorBottom = null
        style.minWidth = 0f
        style.maxWidth = Float.POSITIVE_INFINITY
        style.minHeight = 0f
        style.maxHeight = Float.POSITIVE_INFINITY
        style.weight = 0f
        style.weightFill = true
        style.focusable = false
        style.onFocus = null
        style.onFocusChanged = emptyList()
        style.focusRequest = null
        style.inert = false
        style.onPlaced = null
        style.onMeasured = null
        style.scaleX = 1f
        style.scaleY = 1f
        style.translateX = 0f
        style.translateY = 0f
        style.offsetX = 0f
        style.offsetY = 0f
        style.cursor = null
        style.drawWithContent = null
        style.innerStarted = false
        style.layoutType = lt
        style.gap = g
        style.arrangement = arr
        style.crossAlignment = cross
        style.contentAlignment = content
        style.block()
    }

    companion object : Modifier({})
}

fun Modifier.element(block: PolyLayoutStyle.() -> Unit) = then(Modifier { innerStarted = true; block() })

fun Modifier.layoutElement(block: PolyLayoutStyle.() -> Unit) = then(Modifier(block))

internal fun PolyLayoutStyle.applyShape(shape: org.polyfrost.compose.ui.graphics.Shape) {
    if (shape === org.polyfrost.compose.ui.graphics.RectangleShape) return
    if (shape.percent >= 0f) radiusPercent = shape.percent else { radius = shape.radius; radiusPercent = -1f }
}
