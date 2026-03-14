package org.polyfrost.compose.node

import org.polyfrost.compose.layout.PolyLayoutStyle
import org.polyfrost.compose.render.RenderContext

open class PolyNode {
    val children = mutableListOf<PolyNode>()
    val style = PolyLayoutStyle()
    var parent: PolyNode? = null
    var x = 0f
    var y = 0f
    var width = 0f
    var height = 0f

    open fun render(ctx: RenderContext) {
        val s = style
        if (s.background.alpha > 0) ctx.rect(x, y, width, height, s.background, s.radius)
        if (s.borderWidth > 0f && s.borderColor.alpha > 0) ctx.rectStroke(x, y, width, height, s.borderColor, s.borderWidth, s.radius)

        if (s.clipToBounds) {
            ctx.save()
            if (s.radius > 0f) ctx.clipRRect(x, y, width, height, s.radius) else ctx.clipRect(x, y, width, height)
        }
        children.forEach { it.render(ctx) }
        if (s.clipToBounds) ctx.restore()
    }
}
