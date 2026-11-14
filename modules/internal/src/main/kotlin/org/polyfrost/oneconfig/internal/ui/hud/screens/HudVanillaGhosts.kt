package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import kotlin.math.abs
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.ui.hud.VanillaHudFootprint

private const val GHOST_FILL_ALPHA = 0.05f
private const val GHOST_LINE_ALPHA = 0.3f
private const val GHOST_JOIN_PX = 0.5f
private const val GUI_SIZE_EPSILON = 0.5f

private val ghostDashEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)

internal fun vanillaGhosts(): List<Rect> {
    if (!HudManager.inWorld || !VanillaHudFootprint.supported) return emptyList()
    if (abs(VanillaHudFootprint.measuredWidth - HudManager.guiScreenWidth) > GUI_SIZE_EPSILON ||
        abs(VanillaHudFootprint.measuredHeight - HudManager.guiScreenHeight) > GUI_SIZE_EPSILON
    ) return emptyList()
    return joinTouching(VanillaHudFootprint.rects + VanillaHudFootprint.sampleRects)

}

private fun joinTouching(rects: List<Rect>): List<Rect> {
    val out = ArrayList(rects)
    var merged = true
    while (merged) {
        merged = false
        loop@ for (i in out.indices) {
            for (j in i + 1 until out.size) {
                val a = out[i]
                val b = out[j]
                if (a.left > b.right + GHOST_JOIN_PX || b.left > a.right + GHOST_JOIN_PX ||
                    a.top > b.bottom + GHOST_JOIN_PX || b.top > a.bottom + GHOST_JOIN_PX
                ) continue
                out[i] = Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))
                out.removeAt(j)
                merged = true
                break@loop
            }
        }
    }
    return out
}

internal fun DrawScope.drawVanillaGhosts(ghosts: List<Rect>, mcToScreen: Float, base: Color, badgeRadius: Dp) {
    val radius = CornerRadius(badgeRadius.toPx())
    for (r in ghosts) {
        val topLeft = Offset(r.left * mcToScreen, r.top * mcToScreen)
        val size = Size(r.width * mcToScreen, r.height * mcToScreen)
        drawRoundRect(base.copy(alpha = base.alpha * GHOST_FILL_ALPHA), topLeft, size, radius)
        drawRoundRect(
            base.copy(alpha = base.alpha * GHOST_LINE_ALPHA), topLeft, size, radius,
            style = Stroke(width = 1f, pathEffect = ghostDashEffect),
        )
    }
}
