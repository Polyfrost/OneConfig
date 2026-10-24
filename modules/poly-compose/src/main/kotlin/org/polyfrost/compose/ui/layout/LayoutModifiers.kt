package org.polyfrost.compose.ui.layout

import org.polyfrost.compose.layout.PolyLayoutEngine
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.ui.Modifier
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

fun Modifier.layoutId(layoutId: Any?) = layoutElement { tag = layoutId }
