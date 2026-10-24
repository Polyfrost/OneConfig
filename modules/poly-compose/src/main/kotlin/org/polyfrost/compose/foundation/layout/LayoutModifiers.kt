package org.polyfrost.compose.foundation.layout

import androidx.compose.runtime.State
import org.polyfrost.compose.layout.PolyInsets
import org.polyfrost.compose.layout.PolyPositionMode
import org.polyfrost.compose.layout.PolySize
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.element
import org.polyfrost.compose.ui.layoutElement
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.dp

data class PaddingValues(val start: Dp = 0.dp, val top: Dp = 0.dp, val end: Dp = 0.dp, val bottom: Dp = 0.dp) {
    constructor(all: Dp) : this(all, all, all, all)
    constructor(horizontal: Dp, vertical: Dp) : this(horizontal, vertical, horizontal, vertical)

    internal fun toInsets() = PolyInsets(start.value, top.value, end.value, bottom.value)
}

private fun px(size: Dp) = PolySize.Px(size.value)

private fun fraction(f: Float): PolySize = if (f >= 1f) PolySize.Fill else PolySize.Percent(f)

fun Modifier.size(size: Dp) = element { width = px(size); height = px(size) }
fun Modifier.size(width: Dp, height: Dp) = element { this.width = px(width); this.height = px(height) }
fun Modifier.width(width: Dp) = element { this.width = px(width) }
fun Modifier.height(height: Dp) = element { this.height = px(height) }
fun Modifier.fillMaxSize(fraction: Float = 1f) = element { width = fraction(fraction); height = fraction(fraction) }
fun Modifier.fillMaxWidth(fraction: Float = 1f) = element { width = fraction(fraction) }
fun Modifier.fillMaxHeight(fraction: Float = 1f) = element { height = fraction(fraction) }
fun Modifier.wrapContentSize() = element { width = PolySize.Wrap; height = PolySize.Wrap }
fun Modifier.wrapContentWidth() = element { width = PolySize.Wrap }
fun Modifier.wrapContentHeight() = element { height = PolySize.Wrap }

fun Modifier.widthIn(min: Dp = Dp.Unspecified, max: Dp = Dp.Unspecified) = element {
    if (!min.value.isNaN()) minWidth = min.value
    if (!max.value.isNaN()) maxWidth = max.value
}

fun Modifier.heightIn(min: Dp = Dp.Unspecified, max: Dp = Dp.Unspecified) = element {
    if (!min.value.isNaN()) minHeight = min.value
    if (!max.value.isNaN()) maxHeight = max.value
}

fun Modifier.sizeIn(minWidth: Dp = Dp.Unspecified, minHeight: Dp = Dp.Unspecified, maxWidth: Dp = Dp.Unspecified, maxHeight: Dp = Dp.Unspecified) =
    widthIn(minWidth, maxWidth).heightIn(minHeight, maxHeight)

fun Modifier.padding(start: Dp = 0.dp, top: Dp = 0.dp, end: Dp = 0.dp, bottom: Dp = 0.dp) =
    padding(PolyInsets(start.value, top.value, end.value, bottom.value))

fun Modifier.padding(horizontal: Dp = 0.dp, vertical: Dp = 0.dp) = padding(PolyInsets(horizontal.value, vertical.value))
fun Modifier.padding(all: Dp) = padding(PolyInsets(all.value))
fun Modifier.padding(paddingValues: PaddingValues) = padding(paddingValues.toInsets())

private fun Modifier.padding(insets: PolyInsets) = layoutElement {
    if (innerStarted) padding = padding + insets else margin = margin + insets
}

private operator fun PolyInsets.plus(o: PolyInsets) = PolyInsets(left + o.left, top + o.top, right + o.right, bottom + o.bottom)

fun Modifier.align(alignment: Alignment) = layoutElement { align = alignment }
fun Modifier.align(alignment: Alignment.Vertical) = layoutElement { crossBias = alignment.bias }
fun Modifier.align(alignment: Alignment.Horizontal) = layoutElement { crossBias = alignment.bias }

fun Modifier.weight(weight: Float, fill: Boolean = true) = layoutElement { this.weight = weight; weightFill = fill }

fun Modifier.absoluteOffset(x: Dp = 0.dp, y: Dp = 0.dp) = layoutElement {
    positionMode = PolyPositionMode.Absolute
    this.x = x.value
    this.y = y.value
}

fun Modifier.offset(x: Dp = 0.dp, y: Dp = 0.dp) = layoutElement { offsetX += x.value; offsetY += y.value }

fun Modifier.offsetX(x: State<Float>, target: Float) = layoutElement { animatedOffsetX = x; offsetXTarget = target }
