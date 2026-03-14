package org.polyfrost.compose.runtime

import androidx.compose.runtime.AbstractApplier
import org.polyfrost.compose.node.PolyNode

class PolyApplier(root: PolyNode) : AbstractApplier<PolyNode>(root) {
    override fun insertTopDown(index: Int, instance: PolyNode) {
        instance.parent = current
        current.children.add(index, instance)
    }

    override fun insertBottomUp(index: Int, instance: PolyNode) {}

    override fun remove(index: Int, count: Int) {
        repeat(count) { current.children.removeAt(index).also { it.parent = null } }
    }

    override fun move(from: Int, to: Int, count: Int) {
        val dest = if (to > from) to - count else to
        repeat(count) { current.children.add(dest + it, current.children.removeAt(from)) }
    }

    override fun onClear() {
        root.children.forEach { it.parent = null }
        root.children.clear()
    }
}
