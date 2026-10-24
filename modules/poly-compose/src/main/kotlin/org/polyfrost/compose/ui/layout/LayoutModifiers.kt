package org.polyfrost.compose.ui.layout

import org.polyfrost.compose.layout.PolyLayoutEngine
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.geometry.Offset
import org.polyfrost.compose.ui.geometry.Rect
import org.polyfrost.compose.ui.geometry.Size
import org.polyfrost.compose.ui.layoutElement
import org.polyfrost.compose.ui.unit.IntSize
import kotlin.math.roundToInt

fun Modifier.onPlaced(onPlaced: (PolyNode) -> Unit) = layoutElement {
    val prev = this.onPlaced
    this.onPlaced = if (prev == null) onPlaced else { node -> prev(node); onPlaced(node) }
}

fun Modifier.onSizeChanged(onSizeChanged: (IntSize) -> Unit): Modifier {
    var last: IntSize? = null
    return onPlaced { node ->
        val d = PolyLayoutEngine.density
        val size = IntSize((node.width * d).roundToInt(), (node.height * d).roundToInt())
        if (size != last) {
            last = size
            onSizeChanged(size)
        }
    }
}

class LayoutCoordinates internal constructor(val size: IntSize, internal val origin: Offset)

fun LayoutCoordinates.positionInRoot() = origin
fun LayoutCoordinates.positionInWindow() = origin
fun LayoutCoordinates.boundsInRoot() = Rect(origin, Size(size.width.toFloat(), size.height.toFloat()))
fun LayoutCoordinates.boundsInWindow() = boundsInRoot()

fun Modifier.onGloballyPositioned(onGloballyPositioned: (LayoutCoordinates) -> Unit) = onPlaced { node ->
    val d = PolyLayoutEngine.density
    onGloballyPositioned(LayoutCoordinates(IntSize((node.width * d).roundToInt(), (node.height * d).roundToInt()), Offset(node.x * d, node.y * d)))
}

fun Modifier.layoutId(layoutId: Any?) = layoutElement { tag = layoutId }
