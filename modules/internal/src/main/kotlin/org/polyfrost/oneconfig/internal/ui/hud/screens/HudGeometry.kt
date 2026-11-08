package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager

private const val MIN_VISIBLE_PX = 12f

internal fun designerSelection(hud: Hud): List<Hud> {
    val selection = HudDesignSession.activeSelection
    return if (hud in selection) selection else listOf(hud)
}

internal fun selectionOrigin(huds: Collection<Hud>): Offset? {
    if (huds.isEmpty()) return null
    return Offset(huds.minOf { it.x }, huds.minOf { it.y })
}

internal fun moveHudsBy(huds: Collection<Hud>, dx: Float, dy: Float): Boolean {
    if (huds.isEmpty()) return false
    if (huds.any { it.locked }) {
        notifyHudLocked()
        return false
    }
    if (dx == 0f && dy == 0f) return true
    val free = huds.filter { !it.isAnchored }
    val cdx = clampGroupDelta(dx, free, HudManager.guiScreenWidth, { it.x }, { it.scaledWidth })
    val cdy = clampGroupDelta(dy, free, HudManager.guiScreenHeight, { it.y }, { it.scaledHeight })
    if (cdx == 0f && cdy == 0f) return true
    val oneOff = moveSession == null
    if (oneOff) {
        HudManager.setMergeExclusions(huds)
        for (hud in huds) hud.onEditorDragStart()
    }
    try {
        val targets = huds.map { Triple(it, it.x + cdx, it.y + cdy) }
        Snapshot.withMutableSnapshot { applyHudTargets(targets) }
    } finally {
        if (oneOff) {
            HudManager.setMergeExclusions(emptySet())
            for (hud in huds) hud.onEditorDragEnd()
        }
    }
    return true
}

private var moveSession: List<Hud>? = null

internal fun beginHudMove(huds: Collection<Hud>) {
    endHudMove()
    val session = huds.toList()
    moveSession = session
    HudManager.setMergeExclusions(session)
    for (hud in session) hud.onEditorDragStart()
}

internal fun endHudMove() {
    val session = moveSession ?: return
    moveSession = null
    HudManager.setMergeExclusions(emptySet())
    for (hud in session) hud.onEditorDragEnd()
}

private inline fun clampGroupDelta(
    delta: Float,
    huds: List<Hud>,
    screen: Float,
    pos: (Hud) -> Float,
    size: (Hud) -> Float,
): Float {
    if (screen <= 0f) return delta
    var lo = Float.NEGATIVE_INFINITY
    var hi = Float.POSITIVE_INFINITY
    for (hud in huds) {
        val p = pos(hud)
        val s = size(hud)
        val keep = minOf(s, MIN_VISIBLE_PX)
        lo = maxOf(lo, minOf(keep - s, 0f) - p)
        hi = minOf(hi, maxOf(screen - keep, 0f) - p)
    }
    if (lo > hi) return delta
    return delta.coerceIn(minOf(lo, 0f), maxOf(hi, 0f))
}

internal fun applyHudTargets(targets: List<Triple<Hud, Float, Float>>) {
    val huds = targets.map { it.first }
    val ordered = targets.sortedBy { (hud) -> huds.count { it !== hud && hud.anchorChainContains(it) } }
    repeat(ordered.size + 1) {
        var moved = false
        for ((hud, tx, ty) in ordered) if (hud.x != tx || hud.y != ty) { hud.setAbsolutePosition(tx, ty); moved = true }
        if (!moved) return
    }
}
