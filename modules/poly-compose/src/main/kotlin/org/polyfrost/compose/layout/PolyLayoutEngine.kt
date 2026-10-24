package org.polyfrost.compose.layout

import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.ui.Alignment
import kotlin.math.max
import kotlin.math.min

object PolyLayoutEngine {
    private const val INF = Float.POSITIVE_INFINITY

    var pixelSnap = 0f
        private set

    var density = 1f
        private set

    fun layout(node: PolyNode, parentWidth: Float, parentHeight: Float) = layout(node, parentWidth, parentHeight, 0f)

    fun layout(node: PolyNode, parentWidth: Float, parentHeight: Float, pixelSnap: Float) {
        this.pixelSnap = pixelSnap
        density = (node as? org.polyfrost.compose.node.RootNode)?.density ?: 1f
        try {
            measure(node, parentWidth, parentHeight)
            arrange(node)
            placed(node)
        } finally {
            this.pixelSnap = 0f
        }
    }

    private fun resolve(size: PolySize, avail: Float): Float? = when (size) {
        is PolySize.Px -> size.value
        is PolySize.Percent -> if (avail.isFinite()) avail * size.value else null
        PolySize.Fill -> if (avail.isFinite()) avail else null
        PolySize.Wrap -> null
    }

    private fun clamp(v: Float, lo: Float, hi: Float) = max(lo, min(v, hi))

    private fun measure(node: PolyNode, availW: Float, availH: Float, exactW: Boolean = false, exactH: Boolean = false) {
        val s = node.style
        val knownW = if (exactW) availW else resolve(s.width, availW)?.let { clamp(it, s.minWidth, s.maxWidth) }
        val knownH = if (exactH) availH else resolve(s.height, availH)?.let { clamp(it, s.minHeight, s.maxHeight) }
        val innerW = max(0f, (knownW ?: min(availW, s.maxWidth)) - s.padding.horizontal)
        val innerH = max(0f, (knownH ?: min(availH, s.maxHeight)) - s.padding.vertical)

        when (s.layoutType) {
            LayoutType.Box -> for (c in node.children) {
                measure(c, innerW - c.style.margin.horizontal, innerH - c.style.margin.vertical)
            }
            LayoutType.Row -> linear(node, true, innerW, innerH)
            LayoutType.Column -> linear(node, false, innerH, innerW)
        }
        node.measureContent(innerW, innerH)

        node.width = knownW ?: clamp(wrap(node, true) + s.padding.horizontal, s.minWidth, s.maxWidth)
        node.height = knownH ?: clamp(wrap(node, false) + s.padding.vertical, s.minHeight, s.maxHeight)
        s.onMeasured?.invoke(node)
    }

    private fun linear(node: PolyNode, horizontal: Boolean, main: Float, cross: Float) {
        var n = 0
        var totalWeight = 0f
        for (c in node.children) { n++; totalWeight += c.style.weight }
        var remaining = main - if (n > 1) node.style.gap * (n - 1) else 0f
        for (c in node.children) {
            if (c.style.weight > 0f) continue
            val m = c.style.margin
            if (horizontal) {
                measure(c, max(0f, remaining - m.horizontal), cross - m.vertical)
                remaining -= c.width + m.horizontal
            } else {
                measure(c, cross - m.horizontal, max(0f, remaining - m.vertical))
                remaining -= c.height + m.vertical
            }
        }
        if (totalWeight <= 0f) return
        val free = max(0f, remaining)
        for (c in node.children) {
            if (c.style.weight <= 0f) continue
            val m = c.style.margin
            val share = if (free.isFinite()) max(0f, free * c.style.weight / totalWeight - if (horizontal) m.horizontal else m.vertical) else INF
            val exact = free.isFinite()
            if (horizontal) {
                measure(c, share, cross - m.vertical, exactW = exact && c.style.weightFill)
                if (exact && c.width > share) measure(c, share, cross - m.vertical, exactW = true)
            } else {
                measure(c, cross - m.horizontal, share, exactH = exact && c.style.weightFill)
                if (exact && c.height > share) measure(c, cross - m.horizontal, share, exactH = true)
            }
        }
    }

    private fun wrap(node: PolyNode, horizontal: Boolean): Float {
        var n = 0
        var sum = 0f
        var maxOf = 0f
        for (c in node.children) {
            n++
            val size = if (horizontal) c.width + c.style.margin.horizontal else c.height + c.style.margin.vertical
            sum += size
            maxOf = max(maxOf, size)
        }
        if (n == 0) return if (horizontal) node.contentWidth else node.contentHeight
        val gaps = if (n > 1) node.style.gap * (n - 1) else 0f
        return when (node.style.layoutType) {
            LayoutType.Row -> if (horizontal) sum + gaps else maxOf
            LayoutType.Column -> if (horizontal) maxOf else sum + gaps
            LayoutType.Box -> maxOf
        }
    }

    private fun arrange(node: PolyNode) {
        val s = node.style
        val cx = node.x + s.padding.left
        val cy = node.y + s.padding.top
        val cw = max(0f, node.width - s.padding.horizontal)
        val ch = max(0f, node.height - s.padding.vertical)

        when (s.layoutType) {
            LayoutType.Row, LayoutType.Column -> {
                val horizontal = s.layoutType == LayoutType.Row
                var n = 0
                var used = 0f
                for (c in node.children) {
                    n++
                    used += if (horizontal) c.width + c.style.margin.horizontal else c.height + c.style.margin.vertical
                }
                if (n > 1) used += s.gap * (n - 1)
                val free = (if (horizontal) cw else ch) - used
                val slack = max(0f, free)
                var cursor = (if (horizontal) cx else cy) + when (s.arrangement) {
                    PolyArrangement.Start, PolyArrangement.SpaceBetween -> 0f
                    PolyArrangement.Center -> free / 2f
                    PolyArrangement.End -> free
                    PolyArrangement.SpaceEvenly -> slack / (n + 1)
                    PolyArrangement.SpaceAround -> if (n > 0) slack / n / 2f else 0f
                }
                val between = s.gap + when (s.arrangement) {
                    PolyArrangement.SpaceBetween -> if (n > 1) slack / (n - 1) else 0f
                    PolyArrangement.SpaceEvenly -> slack / (n + 1)
                    PolyArrangement.SpaceAround -> if (n > 0) slack / n else 0f
                    else -> 0f
                }
                for (child in node.children) {
                    val m = child.style.margin
                    if (horizontal) {
                        child.x = cursor + m.left
                        child.y = cy + cross(child, s.crossAlignment, ch, false)
                        cursor += child.width + m.horizontal + between
                    } else {
                        child.x = cx + cross(child, s.crossAlignment, cw, true)
                        child.y = cursor + m.top
                        cursor += child.height + m.vertical + between
                    }
                }
            }
            LayoutType.Box -> for (child in node.children) {
                if (child.style.positionMode == PolyPositionMode.Absolute) {
                    child.x = node.x + child.style.x
                    child.y = node.y + child.style.y
                } else {
                    val a = child.style.align ?: s.contentAlignment
                    child.x = cx + biased(a.horizontalBias, cw, child.width, child.style.margin, true)
                    child.y = cy + biased(a.verticalBias, ch, child.height, child.style.margin, false)
                }
            }
        }
        for (child in node.children) {
            child.x += child.style.offsetX
            child.y += child.style.offsetY
            arrange(child)
        }
    }

    private fun placed(node: PolyNode) {
        val pct = node.style.radiusPercent
        if (pct >= 0f) node.style.radius = min(node.width, node.height) * pct / 100f
        node.style.onPlaced?.invoke(node)
        for (c in node.children) placed(c)
    }

    private fun cross(child: PolyNode, parent: Float, container: Float, horizontal: Boolean): Float {
        val cs = child.style
        val bias = cs.crossBias.takeUnless { it.isNaN() }
            ?: cs.align?.let { if (horizontal) it.horizontalBias else it.verticalBias }
            ?: parent
        return biased(bias, container, if (horizontal) child.width else child.height, cs.margin, horizontal)
    }

    private fun biased(bias: Float, container: Float, child: Float, margin: PolyInsets, horizontal: Boolean): Float {
        val start = if (horizontal) margin.left else margin.top
        val end = if (horizontal) margin.right else margin.bottom
        return start + (container - child - start - end) * (bias + 1f) / 2f
    }
}
