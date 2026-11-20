package org.polyfrost.compose.layout

import org.polyfrost.compose.node.PolyNode

public object PolyLayoutEngine {
    private const val MIN_FIT_SCALE = 0.01f

    public fun layout(node: PolyNode, parentWidth: Float, parentHeight: Float) {
        measure(node, parentWidth, parentHeight)
        arrangeChildren(node)
    }

    private fun measure(node: PolyNode, parentWidth: Float, parentHeight: Float) {
        node.measureContent()
        for (child in node.children) {
            if (child.style.width != PolySize.Fill && child.style.height != PolySize.Fill) {
                measure(child, parentWidth, parentHeight)
            }
        }

        val style = node.style
        val naturalWidth = resolveSize(style.width, parentWidth, node, horizontal = true)
        val naturalHeight = resolveSize(style.height, parentHeight, node, horizontal = false)
        node.width = naturalWidth.constrained(style.minWidth, style.maxWidth)
        node.height = naturalHeight.constrained(style.minHeight, style.maxHeight)
        node.contentScale = if (style.fitContent) fitScale(node, naturalWidth, naturalHeight) else 1f

        val cw = (node.width - node.style.padding.horizontal).coerceAtLeast(0f)
        val ch = (node.height - node.style.padding.vertical).coerceAtLeast(0f)
        for (child in node.children) {
            if (child.style.width == PolySize.Fill || child.style.height == PolySize.Fill) {
                measure(child, cw, ch)
            }
        }
    }

    private fun fitScale(node: PolyNode, naturalWidth: Float, naturalHeight: Float): Float {
        val padding = node.style.padding
        val naturalContentWidth = naturalWidth - padding.horizontal
        val naturalContentHeight = naturalHeight - padding.vertical
        val contentWidth = (node.width - padding.horizontal).coerceAtLeast(0f)
        val contentHeight = (node.height - padding.vertical).coerceAtLeast(0f)
        var scale = 1f
        if (naturalContentWidth > contentWidth && naturalContentWidth > 0f) scale = minOf(scale, contentWidth / naturalContentWidth)
        if (naturalContentHeight > contentHeight && naturalContentHeight > 0f) scale = minOf(scale, contentHeight / naturalContentHeight)
        return scale.coerceAtLeast(MIN_FIT_SCALE)
    }

    private fun Float.constrained(min: Float, max: Float): Float {
        var value = this
        if (!max.isNaN()) value = minOf(value, max)
        if (!min.isNaN()) value = maxOf(value, min)
        return value
    }

    private fun resolveSize(size: PolySize, parentSize: Float, node: PolyNode, horizontal: Boolean): Float {
        return when (size) {
            is PolySize.Px -> size.value
            is PolySize.Percent -> parentSize * size.value
            PolySize.Fill -> parentSize
            PolySize.Wrap -> {
                val padding = if (horizontal) node.style.padding.horizontal else node.style.padding.vertical
                val gap = node.style.gap
                val n = node.children.size
                val gaps = if (n > 1) gap * (n - 1) else 0f
                val content = when (node.style.layoutType) {
                    LayoutType.Row -> if (horizontal)
                        node.children.fold(0f) { a, c -> a + c.width + c.style.margin.horizontal } + gaps
                    else
                        node.children.fold(0f) { a, c -> maxOf(a, c.height + c.style.margin.vertical) }
                    LayoutType.Column -> if (horizontal)
                        node.children.fold(0f) { a, c -> maxOf(a, c.width + c.style.margin.horizontal) }
                    else
                        node.children.fold(0f) { a, c -> a + c.height + c.style.margin.vertical } + gaps
                    LayoutType.Box -> if (horizontal)
                        node.children.fold(0f) { a, c -> maxOf(a, c.width + c.style.margin.horizontal) }
                    else
                        node.children.fold(0f) { a, c -> maxOf(a, c.height + c.style.margin.vertical) }
                }
                content + padding
            }
        }
    }

    private fun arrangeChildren(node: PolyNode) {
        val s = node.style
        val cx = node.x + s.padding.left
        val cy = node.y + s.padding.top
        val cw = (node.width - s.padding.horizontal).coerceAtLeast(0f) / node.contentScale
        val ch = (node.height - s.padding.vertical).coerceAtLeast(0f) / node.contentScale

        when (s.layoutType) {
            LayoutType.Row -> {
                var cursor = cx
                for ((i, child) in node.children.withIndex()) {
                    child.x = cursor + child.style.margin.left
                    child.y = cy + crossAxis(child.style.align, ch, child.height, child.style.margin, false)
                    arrangeChildren(child)
                    cursor += child.width + child.style.margin.horizontal
                    if (i < node.children.size - 1) cursor += s.gap
                }
            }
            LayoutType.Column -> {
                var cursor = cy
                for ((i, child) in node.children.withIndex()) {
                    child.x = cx + crossAxis(child.style.align, cw, child.width, child.style.margin, true)
                    child.y = cursor + child.style.margin.top
                    arrangeChildren(child)
                    cursor += child.height + child.style.margin.vertical
                    if (i < node.children.size - 1) cursor += s.gap
                }
            }
            LayoutType.Box -> {
                for (child in node.children) {
                    if (child.style.positionMode == PolyPositionMode.Absolute) {
                        child.x = node.x + child.style.x
                        child.y = node.y + child.style.y
                    } else {
                        child.x = cx + crossAxis(child.style.align, cw, child.width, child.style.margin, true)
                        child.y = cy + crossAxis(child.style.align, ch, child.height, child.style.margin, false)
                    }
                    arrangeChildren(child)
                }
            }
        }
    }

    private fun crossAxis(align: PolyAlign, container: Float, child: Float, margin: PolyInsets, horizontal: Boolean): Float {
        return if (horizontal) when (align) {
            PolyAlign.TopLeft, PolyAlign.Left, PolyAlign.BottomLeft -> margin.left
            PolyAlign.Top, PolyAlign.Center, PolyAlign.Bottom -> (container - child) / 2f
            PolyAlign.TopRight, PolyAlign.Right, PolyAlign.BottomRight -> container - child - margin.right
        } else when (align) {
            PolyAlign.TopLeft, PolyAlign.Top, PolyAlign.TopRight -> margin.top
            PolyAlign.Left, PolyAlign.Center, PolyAlign.Right -> (container - child) / 2f
            PolyAlign.BottomLeft, PolyAlign.Bottom, PolyAlign.BottomRight -> container - child - margin.bottom
        }
    }
}
