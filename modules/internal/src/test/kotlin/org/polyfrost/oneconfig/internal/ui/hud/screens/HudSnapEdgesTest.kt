package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.ui.geometry.Rect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.polyfrost.oneconfig.api.hud.v1.HudAnchor
import org.polyfrost.oneconfig.api.hud.v1.HudSnapEdge

// uses HudSnapContext directly because HudDragSnapper.move needs a graphics backend
class HudSnapEdgesTest {
    private val screenW = 960f
    private val screenH = 540f
    private val threshold = 6f

    private fun rect(x: Float, y: Float, w: Float = 100f, h: Float = 20f) = Rect(x, y, x + w, y + h)

    /** Snaps [raw] as a drag would and returns where it lands with the edges on a guide there */
    private fun drop(
        raw: Rect,
        others: List<Rect> = emptyList(),
        prefer: HudAnchor = HudAnchor.TopLeft,
    ): Pair<Rect, Pair<HudSnapEdge, HudSnapEdge>> {
        val ctx = HudSnapContext(Rect(0f, 0f, screenW, screenH), others)
        val x = ctx.snapX(raw.left, raw.top, raw.width, raw.height, threshold) ?: raw.left
        val y = ctx.snapY(raw.left, raw.top, raw.width, raw.height, threshold) ?: raw.top
        val landed = rect(x, y, raw.width, raw.height)
        return landed to (ctx.edge(landed, true, prefer.edgeX) to ctx.edge(landed, false, prefer.edgeY))
    }

    @Test
    fun `screen centre snap is a centre edge`() {
        val (landed, edges) = drop(rect(433f, 303f))
        assertEquals(screenW / 2f, landed.center.x, 0.01f)
        assertEquals(HudSnapEdge.Center to HudSnapEdge.None, edges)
    }

    @Test
    fun `right edges lined up with another HUD is an end edge`() {
        val other = rect(380f, 100f, w = 140f)
        val (landed, edges) = drop(rect(418f, 303f), others = listOf(other))
        assertEquals(other.right, landed.right, 0.01f)
        assertEquals(HudSnapEdge.End, edges.first)
    }

    @Test
    fun `a free drop has no edges`() {
        val (_, edges) = drop(rect(331f, 343f))
        assertEquals(HudSnapEdge.None to HudSnapEdge.None, edges)
    }

    @Test
    fun `several matching edges keep the one the HUD already grows from`() {
        val other = rect(380f, 100f)
        val right = drop(rect(381f, 303f), others = listOf(other), prefer = HudAnchor.TopRight)
        val unrelated = drop(rect(381f, 303f), others = listOf(other), prefer = HudAnchor.Auto)
        assertEquals(HudSnapEdge.End, right.second.first)
        assertEquals(HudSnapEdge.Center, unrelated.second.first)
    }

    @Test
    fun `a HUD left where it began keeps its start edges`() {
        val start = rect(430f, 260f)
        val snapper = HudDragSnapper()
        snapper.begin(listOf(start), start, listOf(listOf(start)), screenW, screenH)
        val startEdges = HudSnapEdge.End to HudSnapEdge.Start
        assertEquals(startEdges, snapper.edges(start, HudAnchor.TopLeft, startEdges))
    }
}
