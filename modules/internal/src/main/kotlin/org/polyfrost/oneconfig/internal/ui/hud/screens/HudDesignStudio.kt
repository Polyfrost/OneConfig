package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.ranges.coerceAtLeast
import kotlin.ranges.coerceAtMost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.animation.core.tween
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.VerticalScrollbar
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.clickable
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.BoxScope
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.absoluteOffset
import org.polyfrost.compose.foundation.layout.align
import org.polyfrost.compose.foundation.layout.fillMaxHeight
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.rememberScrollState
import org.polyfrost.compose.foundation.shape.RoundedCornerShape
import org.polyfrost.compose.foundation.text.BasicTextField
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.foundation.verticalScroll
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.focus.FocusRequester
import org.polyfrost.compose.ui.focus.focusRequester
import org.polyfrost.compose.ui.geometry.Offset
import org.polyfrost.compose.ui.geometry.Rect
import org.polyfrost.compose.ui.geometry.Size
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.graphics.graphicsLayer
import org.polyfrost.compose.ui.input.key.KeyEvent
import org.polyfrost.compose.ui.input.key.onKeyDown
import org.polyfrost.compose.ui.input.pointer.PointerEvent
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.layout.onPlaced
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.text.style.TextAlign
import org.polyfrost.compose.ui.text.style.TextOverflow
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.DpSize
import org.polyfrost.compose.ui.unit.IntOffset
import org.polyfrost.compose.ui.unit.coerceAtLeast
import org.polyfrost.compose.ui.unit.coerceAtMost
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.max
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindUtils
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudAnchor
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.HudResize
import org.polyfrost.oneconfig.api.hud.v1.LegacyHudMarker as LegacyHud
import org.polyfrost.oneconfig.api.notifications.v1.Notification
import org.polyfrost.oneconfig.api.notifications.v1.Notifications
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.keybind.TextInputFocus
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.utils.v1.NetworkUtils
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.canRenderIcon
import org.polyfrost.oneconfig.internal.ui.components.localizedLabel
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.hud.HudCanvasPasteMenu
import org.polyfrost.oneconfig.internal.ui.hud.HudCanvasResetMenu
import org.polyfrost.oneconfig.internal.ui.hud.components.HudPreviewCanvas
import org.polyfrost.oneconfig.internal.ui.hud.components.HudPreviewState
import org.polyfrost.oneconfig.internal.ui.hud.components.rememberHudPreview
import org.polyfrost.oneconfig.internal.ui.hud.configForHud
import org.polyfrost.oneconfig.internal.ui.hud.modNameFor
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.Designer
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.Settings
import org.polyfrost.oneconfig.internal.ui.poly.AnimatedSwap
import org.polyfrost.oneconfig.internal.ui.poly.AnimatedVisibility
import org.polyfrost.oneconfig.internal.ui.poly.WINDOW_HEIGHT
import org.polyfrost.oneconfig.internal.ui.poly.WINDOW_WIDTH
import org.polyfrost.oneconfig.internal.ui.screens.LocalUiClipboard
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus
import org.polyfrost.oneconfig.internal.ui.search.SearchScope
import org.polyfrost.oneconfig.internal.ui.shell.LocalShell
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val LOGGER = LogManager.getLogger("OneConfig/HudDesignStudio")

/** How far the cursor must travel while pressing a HUD before it counts as a drag attempt */
private const val DRAG_SLOP_PX = 3f

private var lockedHudToast: Notification? = null

/**
 * Tells the user why a HUD refused to move
 *
 * Re-uses the toast while it is still on screen so repeated attempts do not stack up identical entries
 */
private fun notifyHudLocked() {
    lockedHudToast?.let { if (NotificationsManager.contains(it)) return }
    lockedHudToast = Notifications.info(
        "HUD element locked",
        "Unlock in settings to change its position.",
    )
}

enum class StudioCategory(val title: String, val icon: String) {
    Settings("Settings", "qol"),
    Designer("Designer", "paintbrush");
}

fun hudEditorFrameKey(): Long {
    var k = HudManager.activeInstances.size.toLong() * 31 + HudManager.guiScreenWidth.toBits() * 17 + HudManager.guiScreenHeight.toBits()
    for (hud in HudManager.activeInstances) {
        k = k * 31 + (hudBounds(hud)?.hashCode() ?: 0)
        k = k * 31 + (if (hud.hidden) 1 else 0) + (if (hud.locked) 2 else 0)
    }
    return k
}

private data class HudBounds(val x: Float, val y: Float, val width: Float, val height: Float)

/** The search field the search keybind focuses */
private enum class StudioSearch {
    Panel,
    Library,
}

private enum class ResizeCorner {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight;

    val isLeft get() = this == TopLeft || this == BottomLeft
    val isTop get() = this == TopLeft || this == TopRight
}

private const val SELECTION_BLUE_ARGB = 0xFF0D99FF.toInt()
private const val HUD_SIZE_BADGE_HEIGHT = 16f
private const val HUD_SIZE_BADGE_GAP = 9f
private val selectionBlue = Color(SELECTION_BLUE_ARGB)

private val idleHudBoxColor = Color.White.copy(0.2f)
private val hoveredHudBoxColor = Color.White.copy(0.5f)
private val hiddenHudBoxColor = Color.White.copy(0.4f)
private val hiddenHudDashEffect = floatArrayOf(4f, 3f)

// locked HUDs get a dashed amber box instead of the plain white one
private val lockedHudBoxColor = Color(0xFFE0A44C)
private val lockedHudDashEffect = floatArrayOf(2f, 4f)

private val hiddenLegacyScrimColor = Color.Black.copy(0.45f)

// backdrop for the per-HUD action bar so the icons stay readable over any HUD content
private val hudActionBarBackground = Color.Black.copy(0.55f)
private val hudActionBarBorder = Color.White.copy(0.12f)
private val hudActionBarDeleteColor = Color(0xFFE5484D)

private const val SNAP_DISTANCE_PX = 6f
private val snapGuideColor = Color(176, 47, 31)

private const val CHROME_SETTINGS_PANEL = "settings-panel"
private const val CHROME_LIBRARY = "library"
private const val CHROME_LIBRARY_ICONS = "library-icons"

private const val PANEL_WIDTH = 500f

private data class SnapGuides(val vertical: Float?, val horizontal: Float?) {
    companion object {
        val NONE = SnapGuides(null, null)
    }
}

private data class AxisSnap(val position: Float, val line: Float?)

/** An unlocked HUD press that becomes a click or drag depending on whether it moves past [DRAG_SLOP_PX] */
private class PendingPress(
    val hud: Hud,
    val origin: Offset,
    val wasSelected: Boolean,
    val narrowsSelection: Boolean,
)

private fun snapAxis(pos: Float, size: Float, lines: List<Float>, threshold: Float): AxisSnap {
    val center = pos + size / 2f
    val end = pos + size
    var bestPos: Float? = null
    var bestLine: Float? = null
    var bestDist = threshold
    for (line in lines) {
        val dLeft = abs(line - pos)
        val dCenter = abs(line - center)
        val dRight = abs(line - end)
        val d = minOf(dLeft, dCenter, dRight)
        if (d >= bestDist) continue
        bestDist = d
        bestLine = line
        bestPos = when (d) {
            dLeft -> line
            dCenter -> line - size / 2f
            else -> line - size
        }
    }
    return if (bestPos == null) AxisSnap(pos, null) else AxisSnap(bestPos, bestLine)
}

private fun verticalSnapLines(dragged: Hud): List<Float> {
    val lines = ArrayList<Float>()
    lines.add(0f)
    lines.add(HudManager.guiScreenWidth / 2f)
    lines.add(HudManager.guiScreenWidth)
    for (hud in HudManager.activeInstances) {
        if (hud === dragged) continue
        val b = hudBounds(hud) ?: continue
        lines.add(b.x)
        lines.add(b.x + b.width / 2f)
        lines.add(b.x + b.width)
    }
    return lines
}

private fun horizontalSnapLines(dragged: Hud): List<Float> {
    val lines = ArrayList<Float>()
    lines.add(0f)
    lines.add(HudManager.guiScreenHeight / 2f)
    lines.add(HudManager.guiScreenHeight)
    for (hud in HudManager.activeInstances) {
        if (hud === dragged) continue
        val b = hudBounds(hud) ?: continue
        lines.add(b.y)
        lines.add(b.y + b.height / 2f)
        lines.add(b.y + b.height)
    }
    return lines
}

private fun overlapArea(a: Rect, b: Rect): Float {
    val w = minOf(a.right, b.right) - maxOf(a.left, b.left)
    if (w <= 0f) return 0f
    val h = minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)
    if (h <= 0f) return 0f
    return w * h
}

/**
 * Places the canvas hint where it covers the least
 *
 * HUDs live anywhere on screen so a hint pinned to one edge sooner or later sits on top of the HUD being
 * worked on
 *
 * Every edge and corner is scored by how much a HUD or panel would cover it and the clearest one wins
 */
private fun pickHintSlot(root: Size, hint: Size, margin: Float, obstacles: List<Rect>): Offset {
    val w = hint.width
    val h = hint.height
    val centerX = (root.width - w) / 2f
    val left = margin
    val right = root.width - w - margin
    val top = margin
    val bottom = root.height - h - margin
    val slots = listOf(
        Offset(centerX, bottom),
        Offset(centerX, top),
        Offset(left, bottom),
        Offset(right, bottom),
        Offset(left, top),
        Offset(right, top),
    )
    var best = slots[0]
    var bestScore = Float.MAX_VALUE
    for ((i, slot) in slots.withIndex()) {
        var score = i.toFloat()
        for (o in obstacles) {
            score += overlapArea(Rect(slot.x, slot.y, slot.x + w, slot.y + h), o)
        }
        if (score < bestScore) {
            bestScore = score
            best = slot
        }
    }
    return best
}

private fun hudBounds(hud: Hud): HudBounds? {
    val scale = hud.effectiveScale
    if (hud is LegacyHud) {
        if (!HudManager.inWorld) return null
        val (minW, minH) = hud.minimumSize()
        val width = minW.takeIf { it > 0f }?.times(scale)
            ?: hud.renderedW.takeIf { it > 0f }
            ?: hud.staticW.takeIf { it > 0f }?.times(scale)
            ?: return null
        val height = minH.takeIf { it > 0f }?.times(scale)
            ?: hud.renderedH.takeIf { it > 0f }
            ?: hud.staticH.takeIf { it > 0f }?.times(scale)
            ?: return null
        return HudBounds(hud.x, hud.y, width, height)
    }
    val width = if (hud.staticWidth) {
        hud.staticW.takeIf { it > 0f }?.times(scale)
    } else {
        hud.renderedW.takeIf { it > 0f } ?: hud.staticW.takeIf { it > 0f }?.times(scale)
    } ?: return null
    val height = if (hud.staticWidth) {
        hud.staticH.takeIf { it > 0f }?.times(scale)
    } else {
        hud.renderedH.takeIf { it > 0f } ?: hud.staticH.takeIf { it > 0f }?.times(scale)
    } ?: return null
    return HudBounds(hud.x, hud.y, width, height)
}

/** The nine points of a HUD box the user can pin another HUD to in reading order */
private val ANCHOR_POINTS = listOf(
    HudAnchor.TopLeft, HudAnchor.Top, HudAnchor.TopRight,
    HudAnchor.Left, HudAnchor.Center, HudAnchor.Right,
    HudAnchor.BottomLeft, HudAnchor.Bottom, HudAnchor.BottomRight,
)

private const val ANCHOR_DOT_RADIUS_PX = 5f
private const val ANCHOR_HIT_RADIUS_PX = 10f
private val anchorLineColor = Color(0xFF0D99FF)
private val anchorLineDashEffect = floatArrayOf(4f, 4f)
private val anchorPickScrimColor = Color.Black.copy(0.25f)

private fun anchorFractions(hud: Hud, point: HudAnchor): Pair<Float, Float> {
    val resolved = if (point == HudAnchor.Auto) hud.effectiveGrowthAnchor else point
    val fx = when (resolved) {
        HudAnchor.TopLeft, HudAnchor.Left, HudAnchor.BottomLeft -> 0f
        HudAnchor.Top, HudAnchor.Center, HudAnchor.Bottom -> 0.5f
        else -> 1f
    }
    val fy = when (resolved) {
        HudAnchor.TopLeft, HudAnchor.Top, HudAnchor.TopRight -> 0f
        HudAnchor.Left, HudAnchor.Center, HudAnchor.Right -> 0.5f
        else -> 1f
    }
    return fx to fy
}

/** Where [point] sits on [hud]'s box in gui coordinates */
private fun anchorPointOf(hud: Hud, point: HudAnchor): Offset? {
    val b = hudBounds(hud) ?: return null
    val (fx, fy) = anchorFractions(hud, point)
    return Offset(b.x + fx * b.width, b.y + fy * b.height)
}

/** Every HUD [sources] may be anchored to excluding themselves and any already hanging off one */
private fun anchorTargetsFor(sources: List<Hud>): List<Hud> =
    HudManager.activeInstances.filter { candidate ->
        sources.none { it === candidate || candidate.anchorChainContains(it) }
    }

/** The point on [source]'s own box under the cursor while picking which point gets anchored */
private fun hitTestSelfAnchorPoint(
    source: Hud,
    screenX: Float,
    screenY: Float,
    mcToScreen: Float,
): HudAnchor? {
    var best: HudAnchor? = null
    var bestDist = ANCHOR_HIT_RADIUS_PX
    for (point in ANCHOR_POINTS) {
        val p = anchorPointOf(source, point) ?: continue
        val d = hypot(p.x * mcToScreen - screenX, p.y * mcToScreen - screenY)
        if (d >= bestDist) continue
        bestDist = d
        best = point
    }
    return best
}

private fun hitTestAnchorPoint(
    sources: List<Hud>,
    screenX: Float,
    screenY: Float,
    mcToScreen: Float,
): Pair<Hud, HudAnchor>? {
    var best: Pair<Hud, HudAnchor>? = null
    var bestDist = ANCHOR_HIT_RADIUS_PX
    for (target in anchorTargetsFor(sources)) {
        for (point in ANCHOR_POINTS) {
            val p = anchorPointOf(target, point) ?: continue
            val d = hypot(p.x * mcToScreen - screenX, p.y * mcToScreen - screenY)
            if (d >= bestDist) continue
            bestDist = d
            best = target to point
        }
    }
    return best
}

private fun orderedInstances(): List<Hud> = HudManager.zOrderedInstances { hud ->
    hudBounds(hud)?.let { floatArrayOf(it.x, it.y, it.width, it.height) }
}

private fun hitTestHud(hud: Hud, mcX: Float, mcY: Float): Boolean {
    val bounds = hudBounds(hud) ?: return false
    return mcX >= bounds.x && mcX <= bounds.x + bounds.width && mcY >= bounds.y && mcY <= bounds.y + bounds.height
}

/**
 * Huds under the cursor with the topmost last
 *
 * Locked huds are only considered when nothing unlocked is under the cursor
 */
private fun hudStackAt(mcX: Float, mcY: Float): List<Hud> {
    val stack = orderedInstances().filter { hitTestHud(it, mcX, mcY) }
    return stack.filter { !it.locked }.ifEmpty { stack }
}

private fun topHudAt(mcX: Float, mcY: Float): Hud? = hudStackAt(mcX, mcY).lastOrNull()

private fun pickHudAt(mcX: Float, mcY: Float, current: Hud?): Hud? {
    val stack = hudStackAt(mcX, mcY)
    if (stack.isEmpty()) return null
    val index = stack.indexOfFirst { it === current }
    if (index < 0) return stack.last()
    return stack[(index - 1 + stack.size) % stack.size]
}

/** Screen-space rectangle of the whole action bar including the backdrop */
private data class HudActionBarLayout(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

private const val HUD_ACTION_BAR_ICONS = 3

private fun hudActionBarIcons(hud: Hud) =
    if (hud.canDelete()) HUD_ACTION_BAR_ICONS + 1 else HUD_ACTION_BAR_ICONS

/** Inset between the backdrop edge and the icons */
private fun actionBarPadding(gapPx: Float) = gapPx / 2f

private fun hudActionBarLayout(
    hud: Hud,
    mcToScreen: Float,
    iconPx: Float,
    gapPx: Float,
): HudActionBarLayout? {
    val bounds = hudBounds(hud) ?: return null
    val sx = bounds.x * mcToScreen
    val sy = bounds.y * mcToScreen
    val sw = bounds.width * mcToScreen
    val sh = bounds.height * mcToScreen

    // the bar lives outside the HUD box because small HUDs swallow or overlap the icons when the
    // buttons are laid out inside the bounds
    val screenW = HudManager.guiScreenWidth * mcToScreen
    val screenH = HudManager.guiScreenHeight * mcToScreen

    val padPx = actionBarPadding(gapPx)
    val icons = hudActionBarIcons(hud)
    val barWidth = iconPx * icons + gapPx * (icons - 1) + padPx * 2f
    val barHeight = iconPx + padPx * 2f
    val maxLeft = (screenW - barWidth).coerceAtLeast(0f)
    val left = (sx + (sw - barWidth) / 2f).coerceIn(0f, maxLeft)

    val below = sy + sh + gapPx
    // keep clear of the size badge that is drawn above the selection box
    val above = sy - gapPx - barHeight - (HUD_SIZE_BADGE_GAP + HUD_SIZE_BADGE_HEIGHT)
    val y = when {
        below + barHeight <= screenH -> below
        above >= 0f -> above
        // no room either side so sit on the bottom edge of the HUD clamped to the screen
        else -> (sy + sh - barHeight).coerceIn(0f, (screenH - barHeight).coerceAtLeast(0f))
    }

    return HudActionBarLayout(left, y, barWidth, barHeight)
}

private fun resizeHud(
    hud: Hud,
    corner: ResizeCorner,
    startBounds: HudBounds,
    startTextScale: Float,
    startScale: Float,
    startStaticW: Float,
    startStaticH: Float,
    mouseX: Float,
    mouseY: Float,
    freeResize: Boolean,
) {
    val anchorX = if (corner.isLeft) startBounds.x + startBounds.width else startBounds.x
    val anchorY = if (corner.isTop) startBounds.y + startBounds.height else startBounds.y
    val targetWidth = (if (corner.isLeft) anchorX - mouseX else mouseX - anchorX).coerceAtLeast(1f)
    val targetHeight = (if (corner.isTop) anchorY - mouseY else mouseY - anchorY).coerceAtLeast(1f)

    if (hud.resizeAxes == HudResize.Width) {
        hud.applyEditorWidth(targetWidth)
        return
    }

    if (hud is LegacyHud) {
        val cornerVecX = if (corner.isLeft) -startBounds.width else startBounds.width
        val cornerVecY = if (corner.isTop) -startBounds.height else startBounds.height
        val mouseVecX = mouseX - anchorX
        val mouseVecY = mouseY - anchorY
        val diagLenSq = cornerVecX * cornerVecX + cornerVecY * cornerVecY
        val rawFactor = if (diagLenSq > 0f) {
            (mouseVecX * cornerVecX + mouseVecY * cornerVecY) / diagLenSq
        } else 1f
        val baseScale = startScale.coerceAtLeast(0.001f)
        val newScale = (baseScale * rawFactor).coerceIn(0.25f, 4f)
        hud.customScale = newScale
        return
    }

    if (freeResize) {
        val effectiveScale = hud.effectiveScale.coerceAtLeast(0.001f)
        val (minStaticW, minStaticH) = hud.minimumSize()
        val newWidth = targetWidth.coerceAtLeast(minStaticW * effectiveScale)
        val newHeight = targetHeight.coerceAtLeast(minStaticH * effectiveScale)

        hud.staticWidth = true
        hud.staticW = newWidth / effectiveScale
        hud.staticH = newHeight / effectiveScale
        return
    }

    // scale proportionally by projecting the mouse onto the diagonal from the anchor to the dragged
    // corner because a per-axis max-deviation factor turns small moves on the short axis into huge jumps
    val cornerVecX = if (corner.isLeft) -startBounds.width else startBounds.width
    val cornerVecY = if (corner.isTop) -startBounds.height else startBounds.height
    val mouseVecX = mouseX - anchorX
    val mouseVecY = mouseY - anchorY
    val diagLenSq = cornerVecX * cornerVecX + cornerVecY * cornerVecY
    val rawFactor = if (diagLenSq > 0f) {
        (mouseVecX * cornerVecX + mouseVecY * cornerVecY) / diagLenSq
    } else 1f
    val minTextScale = 6f / 14f
    val maxTextScale = 64f / 14f
    val newTextScale = (startTextScale * rawFactor).coerceIn(minTextScale, maxTextScale)
    val factor = if (startTextScale > 0f) newTextScale / startTextScale else 1f

    hud.textScale = newTextScale
    if (hud.staticWidth && startStaticW > 0f && startStaticH > 0f) {
        hud.staticW = startStaticW * factor
        hud.staticH = startStaticH * factor
    }
}

private fun RenderContext.drawSelectedHudBounds(
    bounds: HudBounds,
    mcToScreen: Float,
    showHandles: Boolean,
    showBadge: Boolean = true,
    font: PolyFont,
) {
    val sx = bounds.x * mcToScreen
    val sy = bounds.y * mcToScreen
    val sw = bounds.width * mcToScreen
    val sh = bounds.height * mcToScreen

    rectStroke(sx, sy, sw, sh, selectionBlue, 1f)

    if (showHandles) {
        val handleSize = 7f
        listOf(
            Offset(sx, sy),
            Offset(sx + sw, sy),
            Offset(sx, sy + sh),
            Offset(sx + sw, sy + sh),
        ).forEach { corner ->
            val topLeft = Offset(corner.x - handleSize / 2, corner.y - handleSize / 2)
            rect(topLeft.x, topLeft.y, handleSize, handleSize, Color.White)
            rectStroke(topLeft.x, topLeft.y, handleSize, handleSize, selectionBlue, 1f)
        }
    }

    if (showBadge) {
        val badgeTopY = (sy - HUD_SIZE_BADGE_GAP - HUD_SIZE_BADGE_HEIGHT).coerceAtLeast(0f)
        drawHudSizeBadge("${bounds.width.roundToInt()} x ${bounds.height.roundToInt()}", sx + sw / 2, badgeTopY, font)
    }
}

private fun RenderContext.drawHudSizeBadge(label: String, centerX: Float, topY: Float, font: PolyFont) {
    val metrics = font.metrics
    val textWidth = font.measureTextWidth(label)
    val textHeight = metrics.descent - metrics.ascent
    val horizontalPadding = 5f
    val badgeWidth = textWidth + horizontalPadding * 2
    val badgeHeight = HUD_SIZE_BADGE_HEIGHT
    val badgeX = centerX - badgeWidth / 2

    rect(badgeX, topY, badgeWidth, badgeHeight, selectionBlue, 3f)
    val textX = badgeX + horizontalPadding
    val textY = topY + (badgeHeight - textHeight) / 2f - metrics.ascent
    text(label, textX, textY, Color.White, font)
}

internal fun RenderContext.dashedLine(x1: Float, y1: Float, x2: Float, y2: Float, color: PolyColor, stroke: Float, dash: FloatArray) {
    val len = hypot(x2 - x1, y2 - y1)
    if (len == 0f) return
    var t = 0f
    while (t < len) {
        val e = min(t + dash[0], len)
        line(x1 + (x2 - x1) * t / len, y1 + (y2 - y1) * t / len, x1 + (x2 - x1) * e / len, y1 + (y2 - y1) * e / len, color, stroke)
        t += dash[0] + dash[1]
    }
}

internal fun RenderContext.dashedRect(x: Float, y: Float, w: Float, h: Float, color: PolyColor, dash: FloatArray) {
    dashedLine(x, y, x + w, y, color, 1f, dash)
    dashedLine(x + w, y, x + w, y + h, color, 1f, dash)
    dashedLine(x + w, y + h, x, y + h, color, 1f, dash)
    dashedLine(x, y + h, x, y, color, 1f, dash)
}

private fun RenderContext.drawAnchorDot(center: Offset, hot: Boolean, accent: Color) {
    val radius = if (hot) ANCHOR_DOT_RADIUS_PX + 2f else ANCHOR_DOT_RADIUS_PX
    rect(center.x - radius, center.y - radius, radius * 2, radius * 2, Color.White, radius)
    val inner = radius - 2f
    rect(center.x - inner, center.y - inner, inner * 2, inner * 2, if (hot) accent else anchorLineColor, inner)
}

@Composable
private fun HudIconButton(
    icon: String,
    modifier: Modifier = Modifier.size(24.dp),
    foreground: Color = LocalTheme.current.textColor.copy(0.7f),
    hoveredForeground: Color = LocalTheme.current.textColor,
    onClick: () -> Unit,
) {
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val color = animateColorAsState(if (isHovered) hoveredForeground else foreground)
    Icon(
        icon,
        color = color,
        modifier = modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource, indication = null, onClick = onClick),
    )
}

@Composable
private fun HudActionButton(
    icon: String,
    size: Dp,
    foreground: Color,
    hoveredForeground: Color,
    sound: UiSoundEvent?,
    onClick: () -> Unit,
) {
    HudIconButton(
        icon,
        modifier = Modifier.size(size),
        foreground = foreground,
        hoveredForeground = hoveredForeground,
    ) {
        sound?.let { UiSounds.play(it) }
        onClick()
    }
}

@Composable
private fun HudActionBar(
    hud: Hud,
    mcToScreen: Float,
    actionIconPx: Float,
    actionBarGapPx: Float,
    chromeAlpha: Float,
    onHover: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onSettings: () -> Unit,
) {
    val bounds = hudBounds(hud) ?: return
    if (bounds.width <= 0f || bounds.height <= 0f) return
    val layout = hudActionBarLayout(hud, mcToScreen, actionIconPx, actionBarGapPx) ?: return
    val shape = LocalTheme.current.buttonShape
    val iconSize = actionIconPx.dp
    val barPadding = actionBarPadding(actionBarGapPx).dp
    val isHidden = hud.hidden
    val isLocked = hud.locked

    val hudLeft = bounds.x * mcToScreen
    val hudTop = bounds.y * mcToScreen
    val hudRight = hudLeft + bounds.width * mcToScreen
    val hudBottom = hudTop + bounds.height * mcToScreen
    val top: Float
    val bottom: Float
    when {
        layout.y >= hudBottom -> { top = hudBottom; bottom = layout.y + layout.height }
        layout.y + layout.height <= hudTop -> { top = layout.y; bottom = hudTop }
        else -> { top = layout.y; bottom = layout.y + layout.height }
    }
    val left = minOf(hudLeft, layout.x)
    val right = maxOf(hudRight, layout.x + layout.width)
    Box(Modifier.absoluteOffset(left.dp, top.dp).size((right - left).dp, (bottom - top).dp).hoverable(onHover)) {
        Row(
            modifier = Modifier
                .padding(start = (layout.x - left).dp, top = (layout.y - top).dp)
                .graphicsLayer(alpha = chromeAlpha)
                .background(hudActionBarBackground, shape)
                .border(1.dp, hudActionBarBorder, shape)
                .padding(barPadding)
                .onPointerEvent { it.type == PointerEventType.Press },
            horizontalArrangement = Arrangement.spacedBy(actionBarGapPx.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HudActionButton(
                "settings",
                iconSize,
                foreground = Color.White.copy(0.7f),
                hoveredForeground = Color.White,
                sound = UiSoundEvent.HUD_SELECT,
                onClick = onSettings,
            )
            HudActionButton(
                if (isHidden) "eye-off" else "eye",
                iconSize,
                foreground = Color.White.copy(0.7f),
                hoveredForeground = Color.White,
                sound = UiSoundEvent.CLICK,
            ) {
                Snapshot.withMutableSnapshot { hud.hidden = !hud.hidden }
            }
            if (isLocked) {
                HudActionButton("lock", iconSize, foreground = Accent, hoveredForeground = Accent, sound = UiSoundEvent.CLICK) {
                    Snapshot.withMutableSnapshot { hud.locked = false }
                }
            } else {
                HudActionButton("unlock", iconSize, foreground = Color.White.copy(0.7f), hoveredForeground = Color.White, sound = UiSoundEvent.CLICK) {
                    Snapshot.withMutableSnapshot { hud.locked = true }
                }
            }
            if (hud.canDelete()) {
                HudActionButton(
                    "trash",
                    iconSize,
                    foreground = hudActionBarDeleteColor.copy(alpha = 0.7f),
                    hoveredForeground = hudActionBarDeleteColor,
                    sound = null,
                    onClick = onDelete,
                )
            }
        }
    }
}

private fun pickTourHud(): Hud? {
    val visible = HudManager.activeInstances.filter { !it.hidden && hudBounds(it) != null }
    val mergeable = visible.filter(::canTourMerge)
    return mergeable.firstOrNull { hud -> mergeable.any { canTourMergeInto(hud, it) } }
        ?: visible.minByOrNull { if (it is LegacyHud) 1 else 0 }
}

private fun hasTourMergePair(): Boolean {
    val mergeable = HudManager.activeInstances.filter(::canTourMerge)
    return mergeable.any { a -> mergeable.any { b -> canTourMergeInto(a, b) } }
}

private fun canTourMergeInto(mover: Hud, neighbour: Hud): Boolean =
    neighbour !== mover && HudManager.backgroundStylesMatch(mover, neighbour) &&
        HudManager.mergeGroupOf(neighbour).none { it === mover }

private fun canTourMerge(hud: Hud): Boolean =
    !hud.locked && !hud.hidden && HudManager.isMergeCandidate(hud)

private fun tourMergeHint(mcToScreen: Float): Pair<Rect, Rect>? =
    tourMergeHint(mcToScreen, stacked = true) ?: tourMergeHint(mcToScreen, stacked = false)

private fun tourMergeHint(mcToScreen: Float, stacked: Boolean): Pair<Rect, Rect>? {
    val candidates = HudManager.activeInstances.filter(::canTourMerge)
    val screenW = HudManager.guiScreenWidth
    val screenH = HudManager.guiScreenHeight
    var best: Pair<Rect, Rect>? = null
    var bestDistance = Float.MAX_VALUE
    for (mover in candidates) {
        val a = hudBounds(mover) ?: continue
        for (neighbour in candidates) {
            if (!canTourMergeInto(mover, neighbour)) continue
            val b = hudBounds(neighbour) ?: continue
            val slots = if (stacked) {
                listOf(Offset(b.x, b.y + b.height), Offset(b.x, b.y - a.height))
            } else {
                listOf(Offset(b.x + b.width, b.y), Offset(b.x - a.width, b.y))
            }
            for (slot in slots) {
                if (slot.x < 0f || slot.y < 0f || slot.x + a.width > screenW || slot.y + a.height > screenH) continue
                val distance = hypot(slot.x - a.x, slot.y - a.y)
                if (distance >= bestDistance) continue
                bestDistance = distance
                best = Rect(a.x, a.y, a.x + a.width, a.y + a.height).scaled(mcToScreen) to
                    Rect(slot.x, slot.y, slot.x + a.width, slot.y + a.height).scaled(mcToScreen)
            }
        }
    }
    return best
}

private fun Rect.scaled(factor: Float) = Rect(left * factor, top * factor, right * factor, bottom * factor)

private fun Rect.including(other: Rect) =
    Rect(minOf(left, other.left), minOf(top, other.top), maxOf(right, other.right), maxOf(bottom, other.bottom))

private fun tourMenuRect(hud: Rect, menu: Size, root: Size, gap: Float): Rect {
    val rightX = hud.right + gap
    val x = if (rightX + menu.width <= root.width - gap) rightX else hud.left - gap - menu.width
    val y = hud.top.coerceIn(gap, (root.height - menu.height - gap).coerceAtLeast(gap))
    return Rect(Offset(x.coerceAtLeast(gap), y), menu)
}

private fun placeHudCentered(provider: Hud): Hud? = try {
    val instance = if (provider.isReal) provider else provider.make()
    HudManager.markProviderKnown(instance)
    val screenW = HudManager.guiScreenWidth
    val screenH = HudManager.guiScreenHeight
    val scale = instance.effectiveScale
    val (minW, minH) = instance.minimumSize()
    val w = if (instance.staticWidth) {
        instance.staticW.takeIf { it > 0f }?.times(scale)
    } else {
        instance.renderedW.takeIf { it > 0f } ?: minW.takeIf { it > 0f }?.times(scale) ?: instance.staticW.takeIf { it > 0f }?.times(scale)
    } ?: 60f
    val h = if (instance.staticWidth) {
        instance.staticH.takeIf { it > 0f }?.times(scale)
    } else {
        instance.renderedH.takeIf { it > 0f } ?: minH.takeIf { it > 0f }?.times(scale) ?: instance.staticH.takeIf { it > 0f }?.times(scale)
    } ?: 20f
    instance.setAbsolutePosition((screenW - w) / 2f, (screenH - h) / 2f)
    if (instance !in HudManager.activeInstances) {
        HudManager.activeInstances.add(instance)
        HudManager.instancesRevision.intValue++
        instance.setup()
        instance.captureStaticSizeDefaults()
        instance.capturePositionDefaults()
    }
    instance
} catch (e: Throwable) {
    LOGGER.error("Failed to place HUD ${provider.title}", e)
    null
}

@Composable
private fun previewPxPerDp(): Float = LocalDensity.current.density / Platform.screen().surfaceRatio()

private class HudMenuAt(val x: Float, val y: Float, val hud: Hud?)

@Composable
fun HudDesignStudio(frame: Int, visible: Boolean = true) {
    @Suppress("UNUSED_EXPRESSION") frame
    var activeCategory by remember { mutableStateOf(StudioCategory.Settings) }
    var selectedHuds by remember { mutableStateOf<Set<Hud>>(emptySet()) }
    var panelOpen by remember { mutableStateOf(false) }
    var hoveredHud by remember { mutableStateOf<Hud?>(null) }
    var actionBarHud by remember { mutableStateOf<Hud?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var draggedHud by remember { mutableStateOf<Hud?>(null) }
    var dragStarts by remember { mutableStateOf<Map<Hud, Pair<Float, Float>>>(emptyMap()) }
    // set while the current drag grabbed a whole fused shape rather than the selection itself
    var dragTookWholeMerge by remember { mutableStateOf(false) }
    var pendingPress by remember { mutableStateOf<PendingPress?>(null) }
    var snapGuides by remember { mutableStateOf(SnapGuides.NONE) }
    var isResizing by remember { mutableStateOf(false) }
    var libraryVisible by remember { mutableStateOf(true) }
    var searchFocusTarget by remember { mutableStateOf<StudioSearch?>(null) }
    var pendingLibraryScroll by remember { mutableStateOf<String?>(null) }
    // the mod last picked from the icon column kept separate from the scroll-derived section because the
    // list clamps at its end so the final sections can never become the first visible item
    var libraryModIntent by remember { mutableStateOf<String?>(null) }
    val chromeRects = remember { HashMap<String, Rect>() }
    var panelOffset by remember { mutableStateOf(Offset.Zero) }
    var rootSize by remember { mutableStateOf(Size.Zero) }
    var menu by remember { mutableStateOf<HudMenuAt?>(null) }
    // a locked HUD currently being pressed so a far enough move can explain why it is not following
    var lockedPressHud by remember { mutableStateOf<Hud?>(null) }
    val history = remember { HudHistory() }
    var hudClipboard by remember { mutableStateOf<List<Hud>>(emptyList()) }
    var marqueeStart by remember { mutableStateOf<Offset?>(null) }
    var marqueeCurrent by remember { mutableStateOf<Offset?>(null) }
    var marqueeBase by remember { mutableStateOf<Set<Hud>>(emptySet()) }
    // HUDs waiting for the user to pick an anchor point and non-empty while the picker overlay is up
    var anchorPickSources by remember { mutableStateOf<List<Hud>>(emptyList()) }
    var hoveredAnchor by remember { mutableStateOf<Pair<Hud, HudAnchor>?>(null) }
    // null while the first click is still choosing which point of the HUD being anchored is pinned
    var anchorPickSelf by remember { mutableStateOf<HudAnchor?>(null) }
    val keyFocusRequester = remember { FocusRequester() }
    var tourStep by remember { mutableStateOf<HudTourStep?>(null) }
    var tourHud by remember { mutableStateOf<Hud?>(null) }
    var tourTaskDone by remember { mutableStateOf(false) }
    var tourPreviousCategory by remember { mutableStateOf(StudioCategory.Settings) }
    var tourPreviousSelection by remember { mutableStateOf<Set<Hud>>(emptySet()) }
    var tourPreviousPanelOpen by remember { mutableStateOf(false) }
    var tourDeferred by remember { mutableStateOf(false) }
    val tourBlocksEditor = tourStep?.interactive == false
    val tourMergePairExists by remember {
        derivedStateOf { HudManager.instancesRevision.intValue.let { hasTourMergePair() } }
    }
    val screen = Platform.screen()
    val mcToScreen = screen.viewportWidth().toFloat() / screen.guiWidth() / LocalDensity.current.density
    val actionIconPx = 24f
    val actionBarGapPx = 8f

    LaunchedEffect(Unit) {
        while (true) {
            history.poll(busy = draggedHud != null || isResizing || pendingPress != null || marqueeStart != null)
            delay(250)
        }
    }

    // the active HUD of a selection driving the settings panel resize handles action bar and keybinds
    fun primaryHud(): Hud? = selectedHuds.lastOrNull()

    fun closeLibrary() {
        libraryVisible = false
        pendingLibraryScroll = null
        libraryModIntent = null
    }

    val panelHud: Hud? = if (panelOpen) primaryHud()?.takeIf { it in HudManager.activeInstances } else null

    fun clearTransientEditorState() {
        menu = null
        anchorPickSources = emptyList()
        anchorPickSelf = null
        hoveredAnchor = null
        hoveredHud = null
        actionBarHud = null
    }

    fun startTour() {
        Snapshot.withMutableSnapshot {
            clearTransientEditorState()
            if (tourStep == null) {
                tourPreviousCategory = activeCategory
                tourPreviousSelection = selectedHuds
                tourPreviousPanelOpen = panelOpen
            }
            tourHud = null
            tourTaskDone = false
            tourStep = HudTourStep.entries.first()
        }
        keyFocusRequester.requestFocus()
    }

    fun endTour() {
        if (tourStep == null) return
        OneConfigConfig.markHudEditorTourSeen()
        Snapshot.withMutableSnapshot {
            clearTransientEditorState()
            tourStep = null
            tourHud = null
            val restored = tourPreviousSelection.filterTo(LinkedHashSet()) { it in HudManager.activeInstances }
            selectedHuds = restored
            panelOpen = tourPreviousPanelOpen && restored.isNotEmpty()
            activeCategory = tourPreviousCategory
            tourPreviousSelection = emptySet()
        }
    }

    LaunchedEffect(visible) {
        if (!visible && tourStep != null) endTour()
    }

    fun moveTour(delta: Int) {
        val step = tourStep ?: return
        if (delta > 0 && step.interactive && !tourTaskDone) return
        val next = step.offset(delta)
        if (next != null) tourStep = next else if (delta > 0) endTour()
    }

    LaunchedEffect(tourStep) {
        val step = tourStep ?: return@LaunchedEffect
        val hud = tourHud?.takeIf { it in HudManager.activeInstances && !it.hidden } ?: pickTourHud()
        Snapshot.withMutableSnapshot {
            clearTransientEditorState()
            tourHud = hud
            if (step.focus == HudTourFocus.Panel && hud != null) {
                selectedHuds = setOf(hud)
                panelOpen = true
                activeCategory = if (hud is LegacyHud) StudioCategory.Settings else step.category
            } else {
                selectedHuds = emptySet()
                panelOpen = false
            }
            if (step.focus == HudTourFocus.Library || step.interactive) libraryVisible = true
        }
        if (step.interactive && !tourTaskDone) {
            var stillMerged = HudManager.activeInstances.filter { it.bgMerged }
            snapshotFlow {
                val huds = HudManager.instancesRevision.intValue.let { HudManager.activeInstances.toList() }
                (draggedHud != null) to huds.filter { it.bgMerged }
            }.first { (dragging, merged) ->
                stillMerged = stillMerged.filter { hud -> merged.any { it === hud } }
                !dragging && merged.any { hud -> stillMerged.none { it === hud } }
            }
            tourTaskDone = true
        }
    }

    val deleteHuds: (Collection<Hud>) -> Unit = { huds ->
        val removed = huds.filter { it.canDelete() && it in HudManager.activeInstances }
        if (removed.isNotEmpty()) {
            UiSounds.play(UiSoundEvent.CLICK)
            hoveredHud = null
            actionBarHud = null
            removed.forEach { hud -> HudManager.removeHud(hud, delete = true) }
            selectedHuds = selectedHuds - removed.toSet()
        }
    }

    fun paste(huds: List<Hud>, at: Offset? = null) {
        val pasted = duplicateHudGroup(huds, at)
        if (pasted.isNotEmpty()) selectedHuds = pasted.toSet()
        UiSounds.play(UiSoundEvent.CLICK)
    }

    fun undoRedo(undo: Boolean) {
        if (draggedHud != null || isResizing || pendingPress != null) return
        val revived = if (undo) history.undo() else history.redo()
        if (revived != null) {
            val live = HudManager.activeInstances
            selectedHuds = selectedHuds.filterTo(LinkedHashSet()) { it in live } + revived
            if (hoveredHud !in live) hoveredHud = null
            UiSounds.play(UiSoundEvent.CLICK)
        }
    }

    fun startAnchorPick(sources: List<Hud>) {
        anchorPickSources = sources
        anchorPickSelf = if (sources.size == 1) null else HudAnchor.Auto
        hoveredAnchor = null
        hoveredHud = null
        libraryVisible = false
    }

    fun endAnchorPick() {
        anchorPickSources = emptyList()
        anchorPickSelf = null
        hoveredAnchor = null
    }

    fun openSettings(hud: Hud) {
        selectedHuds = setOf(hud)
        panelOpen = true
    }

    fun dragTo(screenX: Float, screenY: Float, alt: Boolean) {
        val hit = draggedHud ?: return
        val start = dragStarts[hit] ?: return
        val rawX = screenX / mcToScreen - dragOffsetX
        val rawY = screenY / mcToScreen - dragOffsetY
        val bounds = hudBounds(hit)
        val snapXLine: Float?
        val snapYLine: Float?
        val targetX: Float
        val targetY: Float
        if (bounds != null && !alt) {
            val threshold = SNAP_DISTANCE_PX / mcToScreen
            val snapX = snapAxis(rawX, bounds.width, verticalSnapLines(hit), threshold)
            val snapY = snapAxis(rawY, bounds.height, horizontalSnapLines(hit), threshold)
            targetX = snapX.position
            targetY = snapY.position
            snapXLine = snapX.line
            snapYLine = snapY.line
        } else {
            targetX = rawX
            targetY = rawY
            snapXLine = null
            snapYLine = null
        }
        val dx = targetX - start.first
        val dy = targetY - start.second
        for ((member, ms) in dragStarts) member.setAbsolutePosition(ms.first + dx, ms.second + dy)
        snapGuides = SnapGuides(snapXLine, snapYLine)
    }

    fun endDrag() {
        val dropped = draggedHud ?: return
        val group = dragStarts.keys
        UiSounds.play(UiSoundEvent.HUD_DRAG_END)
        HudManager.setMergeExclusions(emptySet())
        selectedHuds = if (dragTookWholeMerge) setOf(dropped) else LinkedHashSet(group)
        libraryVisible = false
        draggedHud = null
        dragStarts = emptyMap()
        dragTookWholeMerge = false
        snapGuides = SnapGuides.NONE
        dropped.onEditorDragEnd()
    }

    LaunchedEffect(Unit) {
        val pendingAdd = HudManager.pendingAdd
        val pending = HudManager.pendingSelection
        HudManager.pendingAdd = null
        HudManager.pendingSelection = null
        tourDeferred = pending != null || pendingAdd != null
        if (tourDeferred && tourStep != null) endTour()
        if (pendingAdd != null) {
            while (HudManager.guiScreenWidth <= 0f) delay(16)
            val instance = placeHudCentered(pendingAdd)
            if (instance != null) {
                Snapshot.withMutableSnapshot {
                    selectedHuds = setOf(instance)
                    panelOpen = true
                    libraryVisible = false
                }
            }
        } else if (pending != null) {
            if (pending in HudManager.activeInstances) {
                Snapshot.withMutableSnapshot {
                    selectedHuds = setOf(pending)
                    panelOpen = true
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (OneConfigConfig.hudEditorTourSeen || tourDeferred) return@LaunchedEffect
        while (HudManager.guiScreenWidth <= 0f) delay(16)
        while (true) {
            snapshotFlow {
                tourStep == null && draggedHud == null && !isResizing && pendingPress == null &&
                    marqueeStart == null && !panelOpen && tourMergePairExists
            }.first { it }
            if (!TextInputFocus.isTyping) break
            delay(250)
        }
        if (!OneConfigConfig.hudEditorTourSeen) startTour()
    }

    val selection = if (tourBlocksEditor) emptyList() else selectedHuds.toList()
    SideEffect { HudDesignSession.activeSelection = selection }
    DisposableEffect(Unit) {
        onDispose {
            HudDesignSession.activeSelection = emptyList()
            if (tourStep != null) OneConfigConfig.markHudEditorTourSeen()
        }
    }

    val providers = remember { HudManager.providers().toList() }

    LaunchedEffect(Unit) {
        HudDesignSession.clearCommands()
        for (command in HudDesignSession.commands) {
            if (tourStep?.interactive == false) continue
            Snapshot.withMutableSnapshot {
                when (command) {
                    is StudioCommand.Select -> {
                        val huds = command.huds
                        if (huds.isNotEmpty() && huds.all { it in HudManager.activeInstances }) {
                            selectedHuds = huds.toSet()
                        }
                    }

                    StudioCommand.OpenSettings -> {
                        if (selectedHuds.isNotEmpty()) {
                            panelOpen = true
                            activeCategory = StudioCategory.Settings
                        }
                    }

                    StudioCommand.Copy -> {
                        hudClipboard = selectedHuds.toList()
                        UiSounds.play(UiSoundEvent.CLICK)
                    }

                    StudioCommand.Cut -> {
                        hudClipboard = selectedHuds.toList()
                        deleteHuds(hudClipboard)
                    }

                    StudioCommand.Paste -> {
                        if (hudClipboard.isNotEmpty()) {
                            paste(hudClipboard, HudDesignSession.pointer()?.let { Offset(it.first / mcToScreen, it.second / mcToScreen) })
                        }
                    }

                    StudioCommand.Delete -> deleteHuds(selectedHuds)

                    StudioCommand.SelectAll -> {
                        selectedHuds = HudManager.activeInstances.filterTo(LinkedHashSet()) { !it.locked }
                    }

                    StudioCommand.Lock -> {
                        if (selectedHuds.isNotEmpty()) {
                            val lock = selectedHuds.any { !it.locked }
                            selectedHuds.forEach { it.locked = lock }
                        }
                    }

                    StudioCommand.Search -> {
                        if (draggedHud == null && !isResizing && marqueeStart == null && pendingPress == null) {
                            if (selectedHuds.isNotEmpty()) {
                                panelOpen = true
                                searchFocusTarget = StudioSearch.Panel
                            } else if (providers.any { it.addableToLibrary && it.configId != null }) {
                                libraryVisible = true
                                searchFocusTarget = StudioSearch.Library
                            }
                        }
                    }

                    StudioCommand.Undo -> undoRedo(true)
                    StudioCommand.Redo -> undoRedo(false)
                }
            }
        }
    }

    @Suppress("UNUSED_EXPRESSION") HudManager.instancesRevision.intValue
    val addable = providers.filter { it.addableToLibrary }
    val modIds = addable.mapNotNull { it.configId }.distinct()
    val libraryChromeVisible = selectedHuds.isEmpty() && draggedHud == null && marqueeStart == null

    fun Modifier.chromeRegion(key: String, active: Boolean) = onPlaced {
        if (active) chromeRects[key] = Rect(it.x, it.y, it.x + it.width, it.y + it.height)
    }

    /** Recomputes the marquee selection from [marqueeStart]..[marqueeCurrent] */
    fun updateMarqueeSelection() {
        val start = marqueeStart ?: return
        val cur = marqueeCurrent ?: return
        val left = minOf(start.x, cur.x) / mcToScreen
        val top = minOf(start.y, cur.y) / mcToScreen
        val right = maxOf(start.x, cur.x) / mcToScreen
        val bottom = maxOf(start.y, cur.y) / mcToScreen
        val intersecting = HudManager.activeInstances.filter { hud ->
            if (hud.locked) return@filter false
            val b = hudBounds(hud) ?: return@filter false
            b.x < right && b.x + b.width > left && b.y < bottom && b.y + b.height > top
        }
        selectedHuds = marqueeBase + intersecting
    }

    val pointerModifier = Modifier
        .onPointerEvent { event ->
            val mcX = event.x / mcToScreen
            val mcY = event.y / mcToScreen
            when (event.type) {
                PointerEventType.Press -> {
                    if (event.button == 1) {
                        val hit = primaryHud()?.takeIf { hitTestHud(it, mcX, mcY) }
                            ?: topHudAt(mcX, mcY)
                        if (hit != null) {
                            if (hit !in selectedHuds) {
                                UiSounds.play(UiSoundEvent.HUD_SELECT)
                                selectedHuds = setOf(hit)
                            }
                            libraryVisible = false
                        }
                        menu = HudMenuAt(event.x, event.y, hit)
                        return@onPointerEvent true
                    }
                    if (event.button != 0) return@onPointerEvent false
                    pendingPress = null
                    val pos = Offset(event.x, event.y)
                    val actionPressed = KeybindUtils.isActionModifierPressed(event.modifiers)
                    val shift = event.modifiers and KeyEvent.SHIFT != 0
                    val hit = pickHudAt(mcX, mcY, primaryHud())
                    if (hit != null && hit.locked) {
                        if (hit !in selectedHuds) UiSounds.play(UiSoundEvent.HUD_SELECT)
                        selectedHuds = if (actionPressed) selectedHuds + hit else setOf(hit)
                        libraryVisible = false
                        lockedPressHud = hit
                        pendingPress = PendingPress(hit, pos, wasSelected = true, narrowsSelection = false)
                    } else if (hit != null) {
                        val wasSelected = hit in selectedHuds
                        val newSelection = when {
                            actionPressed -> if (wasSelected) selectedHuds - hit else selectedHuds + hit
                            shift -> if (wasSelected) selectedHuds else selectedHuds + hit
                            else -> if (wasSelected) selectedHuds else setOf(hit)
                        }
                        selectedHuds = newSelection
                        libraryVisible = false
                        pendingPress = PendingPress(
                            hud = hit,
                            origin = pos,
                            wasSelected = wasSelected,
                            narrowsSelection = wasSelected && !actionPressed && !shift && newSelection.size > 1,
                        )
                        dragOffsetX = mcX - hit.x
                        dragOffsetY = mcY - hit.y
                        hoveredHud = hit
                    } else {
                        marqueeBase = if (actionPressed || shift) selectedHuds else emptySet()
                        selectedHuds = marqueeBase
                        marqueeStart = pos
                        marqueeCurrent = pos
                    }
                    true
                }

                PointerEventType.Move -> {
                    val press = pendingPress
                    if (draggedHud != null) {
                        dragTo(event.x, event.y, event.modifiers and KeyEvent.ALT != 0)
                    } else if (press != null) {
                        if (hypot(event.x - press.origin.x, event.y - press.origin.y) <= DRAG_SLOP_PX) return@onPointerEvent true
                        if (press.hud.locked || press.hud !in selectedHuds) {
                            lockedPressHud = null
                            return@onPointerEvent true
                        }
                        UiSounds.play(UiSoundEvent.HUD_DRAG_START)
                        val movable = selectedHuds.filterTo(LinkedHashSet()) { !it.locked }
                        val detach = press.wasSelected
                        val dragSet = if (detach) {
                            movable
                        } else {
                            movable + HudManager.mergeGroupOf(press.hud).filter { !it.locked }
                        }
                        HudManager.setMergeExclusions(if (detach) dragSet else emptySet())
                        press.hud.onEditorDragStart()
                        dragTookWholeMerge = !detach && dragSet.size > movable.size
                        draggedHud = press.hud
                        dragStarts = dragSet.associateWith { it.x to it.y }
                        dragTo(event.x, event.y, event.modifiers and KeyEvent.ALT != 0)
                    } else {
                        if (marqueeStart == null) return@onPointerEvent false
                        marqueeCurrent = Offset(event.x, event.y)
                        updateMarqueeSelection()
                    }
                    true
                }

                PointerEventType.Release -> {
                    lockedPressHud = null
                    val click = pendingPress
                    pendingPress = null
                    when {
                        marqueeStart != null -> {
                            marqueeStart = null
                            marqueeCurrent = null
                        }

                        draggedHud != null -> endDrag()

                        click != null && !click.hud.locked -> {
                            val narrowsSelection = click.narrowsSelection && click.hud in selectedHuds
                            if (!click.wasSelected || narrowsSelection) UiSounds.play(UiSoundEvent.HUD_SELECT)
                            if (narrowsSelection) selectedHuds = setOf(click.hud)
                        }
                    }
                    true
                }

                else -> false
            }
        }

    LaunchedEffect(selectedHuds) {
        if (selectedHuds.isNotEmpty()) keyFocusRequester.requestFocus()
    }

    val chromeAlpha by animateFloatAsState(
        targetValue = if (draggedHud != null) OneConfigConfig.hudDragUiOpacity.coerceIn(0f, 1f) else 1f,
        animationSpec = tween(150),
    )

    val marqueeAnimEnabled = OneConfigConfig.marqueeSelectionAnim
    val marqueeAnimDuration = OneConfigConfig.marqueeSelectionAnimDuration.toInt().coerceAtLeast(1)
    val marqueeAlpha by animateFloatAsState(
        targetValue = if (marqueeStart != null) 1f else 0f,
        animationSpec = tween(if (marqueeAnimEnabled) marqueeAnimDuration else 1),
    )

    val lastMarquee = remember { arrayOfNulls<Offset>(2) }
    if (marqueeStart != null && marqueeCurrent != null) {
        lastMarquee[0] = marqueeStart
        lastMarquee[1] = marqueeCurrent
    }

    val theme = LocalTheme.current
    val accent = Accent
    val selectionCornerRadius = theme.buttonShape.radius

    val panelWidthPx = PANEL_WIDTH
    val panelDockedLeft = remember { booleanArrayOf(false) }
    if (panelHud == null) chromeRects.remove(CHROME_SETTINGS_PANEL)
    if (panelHud != null && panelOffset == Offset.Zero && rootSize.width > 0f) {
        val rootW = rootSize.width
        val rootH = rootSize.height
        val leftSlot = Rect(0f, 0f, panelWidthPx, rootH)
        val rightSlot = Rect(rootW - panelWidthPx, 0f, rootW, rootH)
        var leftScore = 0f
        var rightScore = 0f
        for (hud in HudManager.activeInstances) {
            val b = hudBounds(hud) ?: continue
            val box = Rect(
                b.x * mcToScreen,
                b.y * mcToScreen,
                (b.x + b.width) * mcToScreen,
                (b.y + b.height) * mcToScreen,
            )
            val weight = if (hud in selectedHuds) 8f else 1f
            leftScore += overlapArea(box, leftSlot) * weight
            rightScore += overlapArea(box, rightSlot) * weight
        }
        panelDockedLeft[0] = leftScore < rightScore
    }
    val panelDockX by animateFloatAsState(
        targetValue = if (panelDockedLeft[0]) -(rootSize.width - panelWidthPx) else 0f,
        animationSpec = tween(220),
    )

    fun movePanel(delta: Offset) {
        val x = (panelOffset.x + delta.x).coerceIn(-(rootSize.width - panelWidthPx) - panelDockX - 16f, 16f - panelDockX)
        val y = (panelOffset.y + delta.y).coerceIn(-16f, 16f)
        panelOffset = Offset(x, y)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPlaced { rootSize = Size(it.width, it.height) }
            .focusRequester(keyFocusRequester)
            .onKeyDown { key, modifiers ->
                if (tourBlocksEditor) {
                    when (key) {
                        KeyEvent.KEY_RIGHT, KeyEvent.KEY_ENTER -> moveTour(1)
                        KeyEvent.KEY_LEFT -> moveTour(-1)
                    }
                    return@onKeyDown key != KeyEvent.KEY_ESCAPE
                }
                if (key == KeyEvent.KEY_ESCAPE && anchorPickSources.isNotEmpty()) {
                    if (anchorPickSelf != null && anchorPickSources.size == 1) {
                        anchorPickSelf = null
                        hoveredAnchor = null
                    } else {
                        endAnchorPick()
                    }
                    return@onKeyDown true
                }
                if (key == KeyEvent.KEY_ESCAPE && (selectedHuds.isNotEmpty() || panelOpen)) {
                    selectedHuds = emptySet()
                    panelOpen = false
                    panelOffset = Offset.Zero
                    return@onKeyDown true
                }
                val live = selectedHuds.filter { it in HudManager.activeInstances }
                if (live.isEmpty()) return@onKeyDown false
                if (key == KeyEvent.KEY_BACKSPACE) {
                    deleteHuds(live)
                    return@onKeyDown true
                }
                val step = if (modifiers and KeyEvent.SHIFT != 0) 10f else 1f
                val (dx, dy) = when (key) {
                    KeyEvent.KEY_LEFT -> -step to 0f
                    KeyEvent.KEY_RIGHT -> step to 0f
                    KeyEvent.KEY_UP -> 0f to -step
                    KeyEvent.KEY_DOWN -> 0f to step
                    else -> return@onKeyDown false
                }
                if (live.any { it.locked }) {
                    notifyHudLocked()
                } else {
                    for (hud in live) {
                        hud.setAbsolutePosition(hud.x + dx, hud.y + dy)
                    }
                }
                true
            }
            .then(pointerModifier)
    ) {
        val dragged = draggedHud
        val starts = dragStarts
        val selected = selectedHuds
        val primary = primaryHud()
        val hovered = hoveredHud
        val singleSelection = selected.size == 1
        val guides = snapGuides
        val font = PolyFont(theme.typography.family.resolve(FontWeight.Normal), 10f)
        val renderStart = lastMarquee[0]
        val renderCurrent = lastMarquee[1]
        val hoverLink = hovered?.takeIf { anchorPickSources.isEmpty() && dragged == null }?.let { hud ->
            val parent = hud.effectiveAnchorParent ?: return@let null
            val from = anchorPointOf(hud, hud.effectiveSelfAnchorPoint) ?: return@let null
            val to = anchorPointOf(parent, hud.effectiveAnchorPoint) ?: return@let null
            from * mcToScreen to to * mcToScreen
        }
        Canvas(Modifier.fillMaxSize()) { _, _, width, height ->
            if (dragged != null) {
                guides.vertical?.let { lineX ->
                    val sx = lineX * mcToScreen
                    line(sx, 0f, sx, height, snapGuideColor, 1f)
                }
                guides.horizontal?.let { lineY ->
                    val sy = lineY * mcToScreen
                    line(0f, sy, width, sy, snapGuideColor, 1f)
                }
            }
            for (hud in HudManager.activeInstances) {
                val bounds = hudBounds(hud) ?: continue
                val resizable = hud.resizeAxes != HudResize.None
                val sx = bounds.x * mcToScreen
                val sy = bounds.y * mcToScreen
                val sw = bounds.width * mcToScreen
                val sh = bounds.height * mcToScreen
                val isSelected = hud in selected
                val isPrimary = hud === primary
                val isHovered = hud === hovered
                val isBeingDragged = dragged != null && (hud === dragged || hud in starts)

                if (hud.locked) {
                    if (isSelected || isHovered) {
                        dashedRect(sx, sy, sw, sh, lockedHudBoxColor, lockedHudDashEffect)
                    }
                    continue
                }

                if (isBeingDragged) {
                    rect(sx, sy, sw, sh, selectionBlue.copy(alpha = 0.10f))
                    drawSelectedHudBounds(bounds, mcToScreen, resizable && singleSelection, showBadge = singleSelection, font = font)
                } else if (isSelected) {
                    drawSelectedHudBounds(
                        bounds,
                        mcToScreen,
                        showHandles = isPrimary && singleSelection,
                        showBadge = singleSelection,
                        font = font,
                    )
                } else if (hud.hidden) {
                    if (hud is LegacyHud) {
                        rect(sx, sy, sw, sh, hiddenLegacyScrimColor)
                    }
                    dashedRect(sx, sy, sw, sh, if (isHovered) hoveredHudBoxColor else hiddenHudBoxColor, hiddenHudDashEffect)
                } else {
                    rectStroke(sx, sy, sw, sh, if (isHovered) hoveredHudBoxColor else idleHudBoxColor, 1f)
                }
            }
            hoverLink?.let { (from, to) ->
                dashedLine(from.x, from.y, to.x, to.y, anchorLineColor, 1.5f, anchorLineDashEffect)
                rect(to.x - ANCHOR_DOT_RADIUS_PX, to.y - ANCHOR_DOT_RADIUS_PX, ANCHOR_DOT_RADIUS_PX * 2, ANCHOR_DOT_RADIUS_PX * 2, anchorLineColor, ANCHOR_DOT_RADIUS_PX)
            }

            if (marqueeAlpha > 0f && renderStart != null && renderCurrent != null) {
                val left = minOf(renderStart.x, renderCurrent.x)
                val top = minOf(renderStart.y, renderCurrent.y)
                val w = abs(renderCurrent.x - renderStart.x)
                val h = abs(renderCurrent.y - renderStart.y)
                if (w > 0f && h > 0f) {
                    rect(left, top, w, h, selectionBlue.copy(alpha = 0.15f * marqueeAlpha), selectionCornerRadius)
                    rectStroke(left, top, w, h, selectionBlue.copy(alpha = marqueeAlpha), 1f, selectionCornerRadius)
                }
            }
        }

        for (hud in orderedInstances().sortedBy { !it.locked }) {
            val bounds = hudBounds(hud) ?: continue
            key(hud) {
                Box(
                    Modifier
                        .absoluteOffset((bounds.x * mcToScreen).dp, (bounds.y * mcToScreen).dp)
                        .size((bounds.width * mcToScreen).dp, (bounds.height * mcToScreen).dp)
                        .pointerHoverIcon(if (hud.locked) PointerIcon.Default else PointerIcon.Hand)
                        .hoverable { over ->
                            if (over) {
                                if (draggedHud == null) hoveredHud = hud
                            } else if (hoveredHud === hud && draggedHud == null) {
                                hoveredHud = null
                            }
                        }
                )
            }
        }

        val resizedHud = primary?.takeIf {
            singleSelection && it in HudManager.activeInstances && !it.locked && it.resizeAxes != HudResize.None && dragged == null
        }
        val resizeStartBounds = resizedHud?.let(::hudBounds)
        if (resizedHud != null && resizeStartBounds != null) key(resizedHud) {
            for (corner in ResizeCorner.entries) key(corner) {
                ResizeHandle(resizedHud, corner, resizeStartBounds, mcToScreen) { resizing ->
                    isResizing = resizing
                    if (resizing) {
                        hoveredHud = resizedHud
                        libraryVisible = false
                    } else {
                        selectedHuds = setOf(resizedHud)
                    }
                }
            }
        }

        if (anchorPickSources.isNotEmpty()) {
            AnchorPicker(
                sources = anchorPickSources,
                self = anchorPickSelf,
                hoveredAnchor = hoveredAnchor,
                mcToScreen = mcToScreen,
                accent = accent,
                onHover = { hoveredAnchor = it },
                onSelf = {
                    anchorPickSelf = it
                    hoveredAnchor = null
                    UiSounds.play(UiSoundEvent.CLICK)
                },
                onPick = { picked ->
                    val self = anchorPickSelf ?: HudAnchor.Auto
                    when (picked) {
                        null -> anchorPickSources.forEach { it.clearAnchor() }
                        else -> anchorPickSources.forEach { it.anchorTo(picked.first, picked.second, self) }
                    }
                    UiSounds.play(UiSoundEvent.CLICK)
                    endAnchorPick()
                },
                onCancel = ::endAnchorPick,
            )
        }

        val actionBarTarget = (dragged ?: hovered ?: actionBarHud)?.takeIf {
            marqueeStart == null && anchorPickSources.isEmpty() && !tourBlocksEditor && it in HudManager.activeInstances
        }
        if (actionBarTarget != null) {
            key(actionBarTarget) {
                HudActionBar(
                    hud = actionBarTarget,
                    mcToScreen = mcToScreen,
                    actionIconPx = actionIconPx,
                    actionBarGapPx = actionBarGapPx,
                    chromeAlpha = if (dragged != null) chromeAlpha else 1f,
                    onHover = { over ->
                        if (over) actionBarHud = actionBarTarget else if (actionBarHud === actionBarTarget) actionBarHud = null
                    },
                    onDelete = { deleteHuds(listOf(actionBarTarget)) },
                ) {
                    openSettings(actionBarTarget)
                }
            }
        }

        // one line of guidance for the gestures the canvas cannot show on its own
        val hintText = when {
            anchorPickSources.isNotEmpty() && anchorPickSelf == null ->
                "Click the point on this HUD that gets anchored  ·  Esc to cancel"
            anchorPickSources.isNotEmpty() ->
                "Click the point to anchor it to  ·  Right click to remove the anchor  ·  Esc to go back"
            dragged != null -> null
            lockedPressHud != null || hoveredHud?.locked == true ->
                "This HUD is locked  ·  Use the lock button above it to move or resize it"
            hoveredHud?.let { it.bgMerged && !it.locked } == true ->
                "Drag to move the merged HUDs together  ·  Click it first, then drag, to pull it out"
            else -> null
        }
        val lastHint = remember { arrayOf("") }
        if (hintText != null) lastHint[0] = hintText
        var hintSize by remember { mutableStateOf(Size.Zero) }
        val hintSlot = remember { arrayOfNulls<Offset>(1) }
        val hintAlpha by animateFloatAsState(if (hintText != null) 1f else 0f, tween(150))
        val hintMargin = 16f
        // the action bar floats above the hovered HUD so keep that strip clear as well
        val actionBarStrip = 40f
        if (hintText != null && rootSize.width > 0f && hintSize.width > 0f) {
            val obstacles = ArrayList<Rect>(HudManager.activeInstances.size + chromeRects.size)
            for (hud in HudManager.activeInstances) {
                val b = hudBounds(hud) ?: continue
                val top = b.y * mcToScreen - if (hud === hoveredHud) actionBarStrip else 0f
                obstacles.add(
                    Rect(b.x * mcToScreen, top, (b.x + b.width) * mcToScreen, (b.y + b.height) * mcToScreen)
                )
            }
            obstacles.addAll(chromeRects.values)
            hintSlot[0] = pickHintSlot(rootSize, hintSize, hintMargin, obstacles)
        }
        val hintPlaced = hintSlot[0] != null
        val hintSpec = tween(if (hintPlaced) 160 else 1)
        val hintX by animateFloatAsState(hintSlot[0]?.x ?: 0f, hintSpec)
        val hintY by animateFloatAsState(hintSlot[0]?.y ?: 0f, hintSpec)
        if (hintAlpha > 0f) {
            Box(
                modifier = Modifier
                    .absoluteOffset(hintX.dp, hintY.dp)
                    .alpha(hintAlpha * (if (hintPlaced) 1f else 0f))
                    .onPlaced { hintSize = Size(it.width, it.height) }
                    .background(theme.popupBackground, theme.backgroundShape)
                    .border(1.dp, theme.borderColor, theme.backgroundShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(lastHint[0], color = theme.textColorSecondary, fontSize = 12.sp)
            }
        }

        val panelContentHud = remember { arrayOfNulls<Hud>(1) }
        if (panelHud != null) panelContentHud[0] = panelHud
        AnimatedVisibility(panelHud != null) { t ->
            panelContentHud[0]?.let { shownHud ->
                key(shownHud) {
                    DesignStudioPanel(
                        modifier = Modifier
                            .absoluteOffset(
                                (rootSize.width - panelWidthPx + panelDockX + panelOffset.x + (1f - t) * panelWidthPx).dp,
                                panelOffset.y.dp,
                            )
                            .alpha(chromeAlpha * t)
                            .chromeRegion(CHROME_SETTINGS_PANEL, panelHud != null),
                        selectedHud = shownHud,
                        activeCategory = activeCategory,
                        onCategoryChange = { activeCategory = it },
                        searchFocusRequested = searchFocusTarget == StudioSearch.Panel,
                        onSearchFocused = { searchFocusTarget = null },
                        onBack = {
                            panelOpen = false
                            panelOffset = Offset.Zero
                            selectedHuds = emptySet()
                        },
                        onDragPanel = { movePanel(it) },
                    )
                }
            }
        }

        if (!libraryChromeVisible) {
            chromeRects.remove(CHROME_LIBRARY)
            chromeRects.remove(CHROME_LIBRARY_ICONS)
        }
        if (!libraryVisible) chromeRects.remove(CHROME_LIBRARY)
        AnimatedVisibility(libraryChromeVisible) { rowT ->
            val rowWidth = remember { FloatArray(1) }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .graphicsLayer(translationX = (1f - rowT) * rowWidth[0])
                    .onPlaced { rowWidth[0] = it.width },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AnimatedVisibility(libraryVisible) { libT ->
                    HudLibraryPanel(
                        modifier = Modifier
                            .alpha(libT)
                            .graphicsLayer(translationX = (1f - libT) * 401f)
                            .chromeRegion(CHROME_LIBRARY, libraryChromeVisible && libraryVisible),
                        searchFocusRequested = searchFocusTarget == StudioSearch.Library,
                        onSearchFocused = { searchFocusTarget = null },
                        onClose = { closeLibrary() },
                        providers = providers,
                        suggestEvergreenHud = modIds.isEmpty(),
                        activeModId = libraryModIntent,
                        pendingScroll = pendingLibraryScroll,
                        onScrollComplete = { pendingLibraryScroll = null },
                        onDragStart = { hud, sx, sy, hudLocalOffX, hudLocalOffY ->
                            try {
                                // a single-instance provider is its own instance so a real but inactive one
                                // is re-used rather than made a second time
                                val instance = if (hud.isReal) hud else hud.make()
                                HudManager.markProviderKnown(instance)
                                val effScale = instance.effectiveScale
                                val offX = hudLocalOffX * effScale
                                val offY = hudLocalOffY * effScale
                                val initX = sx / mcToScreen - offX
                                val initY = sy / mcToScreen - offY
                                instance.setAbsolutePosition(initX, initY)
                                if (instance !in HudManager.activeInstances) {
                                    HudManager.activeInstances.add(instance)
                                    HudManager.instancesRevision.intValue++
                                    instance.setup()
                                    instance.captureStaticSizeDefaults()
                                    instance.capturePositionDefaults()
                                }
                                UiSounds.play(UiSoundEvent.HUD_DRAG_START)
                                instance.onEditorDragStart()
                                dragOffsetX = offX
                                dragOffsetY = offY
                                draggedHud = instance
                                dragStarts = mapOf(instance to (initX to initY))
                                selectedHuds = setOf(instance)
                                hoveredHud = instance
                                libraryVisible = false
                            } catch (e: Throwable) {
                                LOGGER.error("Failed to place HUD ${hud.title}", e)
                            }
                        },
                        onDragMove = { event -> dragTo(event.x, event.y, event.modifiers and KeyEvent.ALT != 0) },
                        onDragEnd = {
                            pendingPress = null
                            endDrag()
                        },
                        onCardClick = { hud ->
                            val instance = placeHudCentered(hud)
                            if (instance != null) {
                                UiSounds.play(UiSoundEvent.HUD_SELECT)
                                selectedHuds = setOf(instance)
                                panelOpen = true
                                libraryVisible = false
                            }
                        },
                    )
                }
                if (modIds.isNotEmpty()) Column(
                    modifier = Modifier.padding(end = 16.dp, top = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModIconColumn(
                        modifier = Modifier.chromeRegion(CHROME_LIBRARY_ICONS, libraryChromeVisible),
                        modIds = modIds,
                        activeModId = libraryModIntent,
                        libraryVisible = libraryVisible,
                    ) { modId ->
                        libraryVisible = true
                        libraryModIntent = modId
                        pendingLibraryScroll = modId
                    }
                }
            }
        }

        val density = LocalDensity.current
        val contextMenuHud = menu?.hud?.takeIf { it in HudManager.activeInstances }
        val contextMenuTargets = contextMenuHud?.let { hud ->
            if (hud in selectedHuds) selectedHuds.filter { it in HudManager.activeInstances } else listOf(hud)
        } ?: emptyList()
        val menuOffset = menu?.let { at -> with(density) { IntOffset(at.x.dp.roundToPx(), at.y.dp.roundToPx()) } } ?: IntOffset.Zero
        val menuPosition = menu?.let { at -> Offset(at.x / mcToScreen, at.y / mcToScreen) }

        HudCanvasResetMenu(
            hud = contextMenuHud,
            expanded = contextMenuHud != null,
            offset = menuOffset,
            onDismiss = { menu = null },
            targets = contextMenuTargets,
            settingsEnabled = contextMenuTargets.size <= 1,
            onSettings = { hud -> openSettings(hud) },
            onCopy = { _ ->
                hudClipboard = contextMenuTargets
                UiSounds.play(UiSoundEvent.CLICK)
            },
            onCut = { _ ->
                hudClipboard = contextMenuTargets
                deleteHuds(contextMenuTargets)
            },
            onPaste = { _ -> paste(hudClipboard, menuPosition) },
            pasteEnabled = hudClipboard.any(::canDuplicateHud),
            onDuplicate = { _ -> paste(contextMenuTargets) },
            duplicateEnabled = contextMenuTargets.any(::canDuplicateHud),
            onAnchor = { _ -> startAnchorPick(contextMenuTargets) },
            anchorEnabled = anchorTargetsFor(contextMenuTargets).isNotEmpty(),
            onDelete = { _ -> deleteHuds(contextMenuTargets) },
            onUndo = { undoRedo(true) },
            undoEnabled = history.canUndo,
            onRedo = { undoRedo(false) },
            redoEnabled = history.canRedo,
        )

        HudCanvasPasteMenu(
            expanded = menu != null && contextMenuHud == null,
            offset = menuOffset,
            pasteEnabled = hudClipboard.any(::canDuplicateHud),
            onDismiss = { menu = null },
            onPaste = { paste(hudClipboard, menuPosition) },
            onSelectAll = {
                selectedHuds = HudManager.activeInstances.filterTo(LinkedHashSet()) { !it.locked }
                UiSounds.play(UiSoundEvent.CLICK)
            },
            onUndo = { undoRedo(true) },
            undoEnabled = history.canUndo,
            onRedo = { undoRedo(false) },
            redoEnabled = history.canRedo,
            onTour = { startTour() },
        )

        val lastTourStep = remember { arrayOf(HudTourStep.entries.first()) }
        tourStep?.let { lastTourStep[0] = it }
        AnimatedVisibility(tourStep != null) { tourT ->
            val shownStep = lastTourStep[0]
            val tourHudRect = tourHud?.let { hudBounds(it) }?.let { b ->
                Rect(b.x, b.y, b.x + b.width, b.y + b.height).scaled(mcToScreen)
            }
            var tourMenuSize by remember { mutableStateOf(Size.Zero) }
            val tourMenuRect = if (shownStep.focus == HudTourFocus.AnchorMenu && tourHudRect != null) {
                tourMenuRect(tourHudRect, tourMenuSize, rootSize, 8f)
            } else {
                null
            }
            val focusRect = when (shownStep.focus) {
                HudTourFocus.None -> null
                HudTourFocus.Library -> chromeRects[CHROME_LIBRARY]
                HudTourFocus.Panel -> chromeRects[CHROME_SETTINGS_PANEL]
                HudTourFocus.Hud -> tourHudRect
                HudTourFocus.AnchorMenu -> tourHudRect?.let { hudRect ->
                    tourMenuRect?.takeIf { tourMenuSize != Size.Zero }?.let { hudRect.including(it) } ?: hudRect
                }
            }
            val previewHud = tourHud
            if (tourStep?.focus == HudTourFocus.AnchorMenu && previewHud != null && tourMenuRect != null) {
                HudCanvasResetMenu(
                    hud = previewHud,
                    expanded = true,
                    offset = with(density) { IntOffset(tourMenuRect.left.dp.roundToPx(), tourMenuRect.top.dp.roundToPx()) },
                    onDismiss = {},
                    targets = listOf(previewHud),
                    preview = true,
                    onMeasured = { tourMenuSize = Size(it.width / density.density, it.height / density.density) },
                )
            }
            val tourTask = if (!shownStep.interactive) {
                null
            } else when {
                tourTaskDone -> tourString("${shownStep.id}.task_done")
                !tourMergePairExists -> tourString("${shownStep.id}.task_add")
                else -> tourString("${shownStep.id}.task")
            }
            val mergeHint = if (tourStep?.interactive == true && !tourTaskDone && draggedHud == null) {
                tourMergeHint(mcToScreen)
            } else {
                null
            }
            val libraryRect = listOfNotNull(chromeRects[CHROME_LIBRARY], chromeRects[CHROME_LIBRARY_ICONS])
                .reduceOrNull { a, b -> a.including(b) }
            HudTourOverlay(
                step = shownStep,
                focus = focusRect,
                task = tourTask,
                taskDone = tourTaskDone,
                dragHint = mergeHint,
                avoid = libraryRect,
                blocksEditor = tourBlocksEditor,
                modifier = Modifier.alpha(tourT),
                onBack = { moveTour(-1) },
                onNext = { moveTour(1) },
                onSkip = { endTour() },
            )
        }
    }
}

internal fun canDuplicateHud(hud: Hud?): Boolean =
    hud != null && hud.isReal && (HudManager.getProvider(hud::class.java)?.multipleInstancesAllowed() == true)

internal fun duplicateHud(
    copied: Hud?,
    anchor: Hud? = null,
    offset: Float = 32f,
    mcPosition: Offset? = null,
): Hud? {
    if (copied == null) return null
    val provider = HudManager.getProvider(copied::class.java) ?: return null
    if (!provider.multipleInstancesAllowed()) return null
    val fresh = try {
        provider.make()
    } catch (e: Exception) {
        LOGGER.warn("Failed to duplicate HUD {}", copied.title, e)
        return null
    }
    Snapshot.withMutableSnapshot {
        fresh.tree.overwrite(detachedValues(copied.tree), true, false, null)
        fresh.hidden = copied.hidden
        fresh.locked = copied.locked
        fresh.staticWidth = copied.staticWidth
        fresh.staticW = copied.staticW
        fresh.staticH = copied.staticH
        fresh.customScale = copied.customScale
        if (mcPosition != null) {
            val sw = HudManager.guiScreenWidth
            val sh = HudManager.guiScreenHeight
            val w = copied.scaledWidth
            val h = copied.scaledHeight
            fresh.renderedW = w
            fresh.renderedH = h
            fresh.setAbsolutePosition(
                (mcPosition.x - w / 2f).coerceIn(0f, maxOf(0f, sw - w)),
                (mcPosition.y - h / 2f).coerceIn(0f, maxOf(0f, sh - h)),
            )
        } else {
            val source = anchor ?: copied
            fresh.setAbsolutePosition(source.x + offset, source.y + offset)
        }
        HudManager.activeInstances.add(fresh)
        HudManager.instancesRevision.intValue++
    }
    HudManager.markProviderKnown(fresh)
    fresh.setup()
    fresh.captureStaticSizeDefaults()
    fresh.capturePositionDefaults()
    HudManager.invalidate()
    return fresh
}

@Suppress("UNCHECKED_CAST")
private fun detachedValues(tree: Tree): Tree {
    val out = Tree(tree.id, null, null, null)
    for ((key, node) in tree.map) {
        when (node) {
            is Tree -> out.put(detachedValues(node))
            is Property<*> -> {
                if (node.type == Void::class.java) continue
                val value = node.get() ?: continue
                out.put(Properties.simple(key, null, null, Config.copyDefault(node.type, value), node.type as Class<Any?>))
            }
        }
    }
    return out
}

internal fun duplicateHudGroup(
    clipboard: List<Hud>,
    mcPosition: Offset? = null,
): List<Hud> {
    if (clipboard.isEmpty()) return emptyList()
    val viable = clipboard.filter(::canDuplicateHud)
    if (viable.isEmpty()) return emptyList()
    val sw = HudManager.guiScreenWidth
    val sh = HudManager.guiScreenHeight
    val groupBounds = viable.mapNotNull { hud -> hudBounds(hud) }
    if (groupBounds.isEmpty()) return emptyList()
    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    for (b in groupBounds) {
        if (b.x < minX) minX = b.x
        if (b.y < minY) minY = b.y
        if (b.x + b.width > maxX) maxX = b.x + b.width
        if (b.y + b.height > maxY) maxY = b.y + b.height
    }
    val groupW = maxX - minX
    val groupH = maxY - minY
    val target = if (mcPosition != null) {
        Offset(
            (mcPosition.x - groupW / 2f).coerceIn(0f, maxOf(0f, sw - groupW)),
            (mcPosition.y - groupH / 2f).coerceIn(0f, maxOf(0f, sh - groupH)),
        )
    } else {
        Offset(minX + 32f, minY + 32f)
    }
    val pasted = mutableListOf<Hud>()
    for (hud in viable) {
        if (!canDuplicateHud(hud)) continue
        val bounds = hudBounds(hud) ?: continue
        val dx = bounds.x - minX
        val dy = bounds.y - minY
        val fresh = duplicateHud(hud, mcPosition = Offset.Zero) ?: continue
        val w = bounds.width
        val h = bounds.height
        fresh.renderedW = w
        fresh.renderedH = h
        val nx = (target.x + dx).coerceIn(0f, maxOf(0f, sw - w))
        val ny = (target.y + dy).coerceIn(0f, maxOf(0f, sh - h))
        Snapshot.withMutableSnapshot {
            fresh.setAbsolutePosition(nx, ny)
        }
        pasted.add(fresh)
    }
    return pasted
}

internal enum class HudAlign(val icon: String, val rotation: Int, val label: String) {
    Left("align-left", 0, "Align left"), CenterX("align-center", 0, "Align centre"), Right("align-right", 0, "Align right"),
    Top("align-left", 90, "Align top"), CenterY("align-center", 90, "Align middle"), Bottom("align-right", 90, "Align bottom"),
}

internal fun alignHuds(huds: List<Hud>, align: HudAlign) {
    val boxes = huds.mapNotNull { hud -> hudBounds(hud)?.let { hud to it } }
    if (boxes.isEmpty()) return
    if (boxes.any { it.first.locked }) notifyHudLocked()
    val single = boxes.size == 1
    val x0 = if (single) 0f else boxes.minOf { it.second.x }
    val y0 = if (single) 0f else boxes.minOf { it.second.y }
    val x1 = if (single) HudManager.guiScreenWidth else boxes.maxOf { it.second.x + it.second.width }
    val y1 = if (single) HudManager.guiScreenHeight else boxes.maxOf { it.second.y + it.second.height }
    val targets = boxes.filter { !it.first.locked }.map { (hud, b) ->
        val dx = when (align) {
            HudAlign.Left -> x0 - b.x
            HudAlign.CenterX -> (x0 + x1 - b.width) / 2f - b.x
            HudAlign.Right -> x1 - b.width - b.x
            else -> 0f
        }
        val dy = when (align) {
            HudAlign.Top -> y0 - b.y
            HudAlign.CenterY -> (y0 + y1 - b.height) / 2f - b.y
            HudAlign.Bottom -> y1 - b.height - b.y
            else -> 0f
        }
        Triple(hud, hud.x + dx, hud.y + dy)
    }
    repeat(3) {
        var moved = false
        for ((hud, tx, ty) in targets) if (hud.x != tx || hud.y != ty) { hud.setAbsolutePosition(tx, ty); moved = true }
        if (!moved) return
    }
}

@Composable
fun HudDragLayer(frame: Int, modifier: Modifier = Modifier) {
    @Suppress("UNUSED_EXPRESSION") frame
    val shellLayout = LocalShell.current.layout
    var hoveredHud by remember { mutableStateOf<Hud?>(null) }
    var actionBarHud by remember { mutableStateOf<Hud?>(null) }
    var draggedHud by remember { mutableStateOf<Hud?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var snapGuides by remember { mutableStateOf(SnapGuides.NONE) }
    var lockedPressOrigin by remember { mutableStateOf<Offset?>(null) }
    var rootSize by remember { mutableStateOf(Size.Zero) }
    val screen = Platform.screen()
    val mcToScreen = screen.viewportWidth().toFloat() / screen.guiWidth() / LocalDensity.current.density
    val actionIconPx = 24f
    val actionBarGapPx = 8f

    fun endDrag() {
        val dropped = draggedHud ?: return
        draggedHud = null
        snapGuides = SnapGuides.NONE
        shellLayout.markHudDragging(false)
        dropped.onEditorDragEnd()
    }
    DisposableEffect(Unit) { onDispose { endDrag() } }

    fun overShell(px: Float, py: Float) =
        abs(px - rootSize.width / 2f) <= WINDOW_WIDTH / 2f && abs(py - rootSize.height / 2f) <= WINDOW_HEIGHT / 2f

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPlaced { rootSize = Size(it.width, it.height) }
            .onPointerEvent { event ->
                val mcX = event.x / mcToScreen
                val mcY = event.y / mcToScreen
                when (event.type) {
                    PointerEventType.Press -> {
                        if (event.button != 0 || overShell(event.x, event.y)) return@onPointerEvent false
                        val hit = topHudAt(mcX, mcY) ?: return@onPointerEvent false
                        if (hit.locked) {
                            lockedPressOrigin = Offset(event.x, event.y)
                            return@onPointerEvent true
                        }
                        UiSounds.play(UiSoundEvent.HUD_DRAG_START)
                        hit.onEditorDragStart()
                        dragOffsetX = mcX - hit.x
                        dragOffsetY = mcY - hit.y
                        draggedHud = hit
                        hoveredHud = hit
                        shellLayout.markHudDragging(true)
                        true
                    }

                    PointerEventType.Move -> {
                        lockedPressOrigin?.let { origin ->
                            if (hypot(event.x - origin.x, event.y - origin.y) > DRAG_SLOP_PX) {
                                lockedPressOrigin = null
                                notifyHudLocked()
                            }
                        }
                        val hit = draggedHud ?: return@onPointerEvent true
                        val rawX = mcX - dragOffsetX
                        val rawY = mcY - dragOffsetY
                        val bounds = hudBounds(hit)
                        if (bounds != null && event.modifiers and KeyEvent.ALT == 0) {
                            val threshold = SNAP_DISTANCE_PX / mcToScreen
                            val snapX = snapAxis(rawX, bounds.width, verticalSnapLines(hit), threshold)
                            val snapY = snapAxis(rawY, bounds.height, horizontalSnapLines(hit), threshold)
                            hit.setAbsolutePosition(snapX.position, snapY.position)
                            snapGuides = SnapGuides(snapX.line, snapY.line)
                        } else {
                            hit.setAbsolutePosition(rawX, rawY)
                            snapGuides = SnapGuides.NONE
                        }
                        true
                    }

                    PointerEventType.Release -> {
                        lockedPressOrigin = null
                        if (draggedHud != null) UiSounds.play(UiSoundEvent.HUD_DRAG_END)
                        endDrag()
                        true
                    }

                    else -> false
                }
            }
    ) {
        val dragged = draggedHud
        val hovered = hoveredHud
        val guides = snapGuides
        val font = PolyFont(LocalTheme.current.typography.family.resolve(FontWeight.Normal), 10f)
        Canvas(Modifier.fillMaxSize()) { _, _, width, height ->
            if (dragged != null) {
                guides.vertical?.let { lineX ->
                    val sx = lineX * mcToScreen
                    line(sx, 0f, sx, height, snapGuideColor, 1f)
                }
                guides.horizontal?.let { lineY ->
                    val sy = lineY * mcToScreen
                    line(0f, sy, width, sy, snapGuideColor, 1f)
                }
            }
            for (hud in HudManager.activeInstances) {
                val bounds = hudBounds(hud) ?: continue
                val sx = bounds.x * mcToScreen
                val sy = bounds.y * mcToScreen
                val sw = bounds.width * mcToScreen
                val sh = bounds.height * mcToScreen
                val isBeingDragged = hud === dragged
                val isHovered = hud === hovered
                if (hud.locked) {
                    if (isHovered) {
                        dashedRect(sx, sy, sw, sh, lockedHudBoxColor, lockedHudDashEffect)
                    }
                    continue
                }
                if (isBeingDragged) {
                    rect(sx, sy, sw, sh, selectionBlue.copy(alpha = 0.10f))
                    drawSelectedHudBounds(bounds, mcToScreen, showHandles = false, font = font)
                } else if (hud.hidden) {
                    if (hud is LegacyHud) {
                        rect(sx, sy, sw, sh, hiddenLegacyScrimColor)
                    }
                    dashedRect(sx, sy, sw, sh, if (isHovered) hoveredHudBoxColor else hiddenHudBoxColor, hiddenHudDashEffect)
                } else {
                    rectStroke(sx, sy, sw, sh, if (isHovered) hoveredHudBoxColor else idleHudBoxColor, 1f)
                }
            }
        }
        for (hud in orderedInstances().sortedBy { !it.locked }) {
            val bounds = hudBounds(hud) ?: continue
            key(hud) {
                Box(
                    Modifier
                        .absoluteOffset((bounds.x * mcToScreen).dp, (bounds.y * mcToScreen).dp)
                        .size((bounds.width * mcToScreen).dp, (bounds.height * mcToScreen).dp)
                        .pointerHoverIcon(if (hud.locked) PointerIcon.Default else PointerIcon.Hand)
                        .hoverable { over ->
                            if (over) {
                                if (draggedHud == null) hoveredHud = hud
                            } else if (hoveredHud === hud && draggedHud == null) {
                                hoveredHud = null
                            }
                        }
                )
            }
        }
        val actionBarTarget = (hovered ?: actionBarHud)?.takeIf { dragged == null && it in HudManager.activeInstances }
        if (actionBarTarget != null) {
            key(actionBarTarget) {
                HudActionBar(
                    hud = actionBarTarget,
                    mcToScreen = mcToScreen,
                    actionIconPx = actionIconPx,
                    actionBarGapPx = actionBarGapPx,
                    chromeAlpha = 1f,
                    onHover = { over ->
                        if (over) actionBarHud = actionBarTarget else if (actionBarHud === actionBarTarget) actionBarHud = null
                    },
                    onDelete = {
                        if (actionBarTarget.canDelete()) {
                            UiSounds.play(UiSoundEvent.CLICK)
                            hoveredHud = null
                            actionBarHud = null
                            HudManager.removeHud(actionBarTarget, delete = true)
                        }
                    },
                ) {
                    HudManager.pendingSelection = actionBarTarget
                    HudManager.openEditor()
                }
            }
        }
    }
}

@Composable
private fun ResizeHandle(hud: Hud, corner: ResizeCorner, bounds: HudBounds, mcToScreen: Float, onResizing: (Boolean) -> Unit) {
    val resizeStartBounds = remember { arrayOfNulls<HudBounds>(1) }
    val resizeStart = remember { FloatArray(4) }
    val hitSize = 14f
    val cx = (if (corner.isLeft) bounds.x else bounds.x + bounds.width) * mcToScreen
    val cy = (if (corner.isTop) bounds.y else bounds.y + bounds.height) * mcToScreen
    Box(Modifier.absoluteOffset((cx - hitSize / 2).dp, (cy - hitSize / 2).dp).size(hitSize.dp).onPointerEvent { event ->
        when (event.type) {
            PointerEventType.Press -> {
                if (event.button != 0) return@onPointerEvent false
                resizeStartBounds[0] = hudBounds(hud) ?: return@onPointerEvent false
                resizeStart[0] = hud.textScale
                resizeStart[1] = hud.customScale
                resizeStart[2] = hud.staticW
                resizeStart[3] = hud.staticH
                hud.pinCorner(
                    when (corner) {
                        ResizeCorner.TopLeft -> HudAnchor.BottomRight
                        ResizeCorner.TopRight -> HudAnchor.BottomLeft
                        ResizeCorner.BottomLeft -> HudAnchor.TopRight
                        ResizeCorner.BottomRight -> HudAnchor.TopLeft
                    }
                )
                UiSounds.play(UiSoundEvent.HUD_RESIZE_START)
                onResizing(true)
                true
            }

            PointerEventType.Move -> {
                val start = resizeStartBounds[0] ?: return@onPointerEvent false
                resizeHud(
                    hud = hud,
                    corner = corner,
                    startBounds = start,
                    startTextScale = resizeStart[0],
                    startScale = resizeStart[1],
                    startStaticW = resizeStart[2],
                    startStaticH = resizeStart[3],
                    mouseX = event.x / mcToScreen,
                    mouseY = event.y / mcToScreen,
                    freeResize = event.modifiers and KeyEvent.SHIFT != 0,
                )
                true
            }

            PointerEventType.Release -> {
                if (resizeStartBounds[0] == null) return@onPointerEvent false
                resizeStartBounds[0] = null
                hud.unpinCorner()
                UiSounds.play(UiSoundEvent.HUD_RESIZE_END)
                onResizing(false)
                true
            }

            else -> false
        }
    })
}

@Composable
private fun AnchorPicker(
    sources: List<Hud>,
    self: HudAnchor?,
    hoveredAnchor: Pair<Hud, HudAnchor>?,
    mcToScreen: Float,
    accent: Color,
    onHover: (Pair<Hud, HudAnchor>?) -> Unit,
    onSelf: (HudAnchor) -> Unit,
    onPick: (Pair<Hud, HudAnchor>?) -> Unit,
    onCancel: () -> Unit,
) {
    val source = sources[0]
    val targets = if (self == null) emptyList() else anchorTargetsFor(sources)
    fun hit(x: Float, y: Float) = if (self == null) {
        hitTestSelfAnchorPoint(source, x, y, mcToScreen)?.let { source to it }
    } else {
        hitTestAnchorPoint(sources, x, y, mcToScreen)
    }
    Canvas(Modifier.fillMaxSize().onPointerEvent { event ->
        when (event.type) {
            PointerEventType.Press -> when {
                event.button == 1 -> onPick(null)
                event.button != 0 -> {}
                self == null -> hit(event.x, event.y)?.let { onSelf(it.second) } ?: onCancel()
                else -> hit(event.x, event.y)?.let(onPick) ?: onCancel()
            }

            PointerEventType.Move -> hit(event.x, event.y).let { if (it != hoveredAnchor) onHover(it) }
            else -> {}
        }
        true
    }) { x, y, w, h ->
        rect(x, y, w, h, anchorPickScrimColor)
        if (self == null || sources.size == 1) {
            val b = hudBounds(source)
            if (b != null) {
                rectStroke(b.x * mcToScreen, b.y * mcToScreen, b.width * mcToScreen, b.height * mcToScreen, accent.copy(alpha = 0.5f), 1f)
            }
            for (point in ANCHOR_POINTS) {
                if (self != null && point != self) continue
                val p = anchorPointOf(source, point) ?: continue
                val hot = self == point ||
                    (hoveredAnchor?.first === source && hoveredAnchor.second == point)
                drawAnchorDot(p * mcToScreen, hot, accent)
            }
        }
        for (target in targets) {
            val b = hudBounds(target) ?: continue
            rectStroke(b.x * mcToScreen, b.y * mcToScreen, b.width * mcToScreen, b.height * mcToScreen, anchorLineColor.copy(alpha = 0.5f), 1f)
            for (point in ANCHOR_POINTS) {
                val p = anchorPointOf(target, point) ?: continue
                val hot = hoveredAnchor?.first === target && hoveredAnchor.second == point
                drawAnchorDot(p * mcToScreen, hot, accent)
            }
        }
    }
}

@Composable
private fun DesignStudioPanel(
    modifier: Modifier = Modifier,
    selectedHud: Hud,
    activeCategory: StudioCategory,
    onCategoryChange: (StudioCategory) -> Unit,
    searchFocusRequested: Boolean,
    onSearchFocused: () -> Unit,
    onBack: () -> Unit,
    onDragPanel: (Offset) -> Unit,
) {
    val theme = LocalTheme.current
    val isLegacy = selectedHud is LegacyHud
    val categories = if (isLegacy) listOf(StudioCategory.Settings) else StudioCategory.entries
    val category = if (activeCategory in categories) activeCategory else StudioCategory.Settings
    val search = remember { TextFieldState() }
    val query = search.text.trim()
    val subtitle = remember(selectedHud) {
        selectedHud.let { hud ->
            val name = localizedLabel(hud.title) ?: return@let null
            val modName = hud.configId?.let(::modNameFor)?.takeIf { it != name }
            if (modName != null) "$name / $modName" else name
        }
    }
    val dragOrigin = remember { FloatArray(2) }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(PANEL_WIDTH.dp)
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.popupBackground, theme.backgroundShape)
                .border(1.dp, theme.borderColor, theme.backgroundShape)
                .padding(16.dp)
                .onPointerEvent { true },
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HudIconButton("close") { onBack() }
                    SearchBar(search, "Search...", searchFocusRequested, onSearchFocused)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPointerEvent { event ->
                            when (event.type) {
                                PointerEventType.Press -> {
                                    dragOrigin[0] = event.x
                                    dragOrigin[1] = event.y
                                    event.button == 0
                                }

                                PointerEventType.Move -> {
                                    onDragPanel(Offset(event.x - dragOrigin[0], event.y - dragOrigin[1]))
                                    dragOrigin[0] = event.x
                                    dragOrigin[1] = event.y
                                    true
                                }

                                else -> false
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        "dots-grid",
                        color = theme.textColor.copy(0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("HUD Design Studio", color = theme.textColor, fontSize = 24.sp)
                        subtitle?.let {
                            Text(it, color = theme.textColor.copy(0.5f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (categories.size > 1 && query.isEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach {
                            Chip(it.title, it == category, it.icon) { onCategoryChange(it) }
                        }
                    }
                }
            }
            AnimatedSwap(category, { it.ordinal }, 250, Modifier.fillMaxWidth().weight(1f).clip()) { shown, t, side ->
                val panelScrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(t)
                        .graphicsLayer(translationX = side * (1f - t) * (PANEL_WIDTH - 64f) / 3f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .verticalScroll(panelScrollState)
                            .padding(end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        categories.forEach {
                            if (query.isNotEmpty() || it == shown) when (it) {
                                StudioCategory.Designer -> Designer(selectedHud, query)
                                StudioCategory.Settings -> Settings(selectedHud, query, onDeleted = onBack)
                            }
                        }
                    }
                    VerticalScrollbar(
                        panelScrollState,
                        color = theme.textColorSecondary.copy(0.40f),
                        hoverColor = theme.textColorSecondary.copy(0.70f),
                        modifier = Modifier.fillMaxHeight().align(Alignment.TopEnd),
                        radius = theme.scrollBarShape.radius.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberHudSearchResults(providers: List<Hud>, query: String): List<Pair<String?, List<Hud>>>? {
    var results by remember { mutableStateOf<List<Pair<String?, List<Hud>>>?>(null) }
    LaunchedEffect(providers, query) {
        results = if (query.isBlank()) null
        else withContext(Dispatchers.Default) {
            val known = providers.toHashSet()
            SearchCorpus.searchGrouped(query, setOf(SearchScope.Huds)) { (it.payload as? Hud)?.configId }
                .map { (modId, documents) ->
                    modId to documents.mapNotNull { document -> (document.payload as? Hud)?.takeIf { it in known } }
                }
                .filter { (_, huds) -> huds.isNotEmpty() }
        }
    }
    return results.takeUnless { query.isBlank() }
}

/** One mod's worth of addable HUDs as shown in the continuous library list */
private val Hud.addableToLibrary: Boolean
    get() = multipleInstancesAllowed() || !HudManager.hasHudOfType(this::class.java)

private val HudLibraryGutter = 18.dp

private class HudLibrarySection(val modId: String?, val title: String, val huds: List<Hud>)

/** Map key for a section since the catch-all section has no mod id */
private fun librarySectionKey(modId: String?): String = modId ?: ""

private fun libraryIconFor(modId: String?): String =
    modId?.let { id -> (HudManager.iconFor(id) ?: configForHud(id)?.icon)?.takeIf(::canRenderIcon) } ?: "qol"

private val LIBRARY_HEADER_HEIGHT = 16.dp
private val LIBRARY_SPACING = 10.dp

private data class HudLibraryRow(val huds: List<Hud>, val height: Float, val widths: List<Float>)

private data class HudPreviewGeometry(val size: DpSize, val scale: Float, val cardSize: DpSize)

private data class HudLibraryLayout(
    val rows: List<List<HudLibraryRow>>,
    val sectionOffsets: Map<String, Float>,
    val previews: Map<Hud, HudPreviewState>,
    val geometry: Map<Hud, HudPreviewGeometry>,
)

@Composable
private fun HudLibraryHeader(section: HudLibrarySection, active: Boolean) {
    val theme = LocalTheme.current
    val textColor = animateColorAsState(
        if (active) theme.textColor else theme.textColorSecondary,
    )
    Row(
        modifier = Modifier.fillMaxWidth().height(LIBRARY_HEADER_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(libraryIconFor(section.modId), modifier = Modifier.size(16.dp), color = theme.textColorSecondary)
        Text(
            section.title.uppercase(),
            color = textColor,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val ROW_HEIGHT_TOLERANCE = 0.6f
private const val MAX_CARD_STRETCH = 1.35f

private fun packHudLibraryRows(huds: List<Hud>, sizes: Map<Hud, Size>, availableWidth: Float, spacing: Float): List<HudLibraryRow> {
    val rows = ArrayList<MutableList<Hud>>()
    val rowWidths = ArrayList<Float>()
    val rowHeights = ArrayList<Float>()
    // Tallest first, each HUD takes the first row with room, so small ones fill gaps left in earlier rows.
    // A row only takes HUDs close to its height to keep cards from stretching much vertically.
    for (hud in huds.filter { it in sizes }.sortedByDescending { sizes.getValue(it).height }) {
        val size = sizes.getValue(hud)
        val index = rows.indices.firstOrNull {
            rowWidths[it] + spacing + size.width <= availableWidth && size.height >= rowHeights[it] * ROW_HEIGHT_TOLERANCE
        }
        if (index == null) {
            rows.add(mutableListOf(hud))
            rowWidths.add(size.width)
            rowHeights.add(size.height)
        } else {
            rows[index].add(hud)
            rowWidths[index] += spacing + size.width
        }
    }
    return rows.mapIndexed { i, row ->
        val natural = row.map { sizes.getValue(it).width }
        val stretch = ((availableWidth - spacing * (row.size - 1)) / natural.sum()).coerceIn(1f, MAX_CARD_STRETCH)
        HudLibraryRow(row, rowHeights[i], natural.map { it * stretch })
    }
}

@Composable
private fun rememberHudLibraryLayout(sections: List<HudLibrarySection>, maxCardWidth: Dp): HudLibraryLayout {
    val density = previewPxPerDp()
    val previews = HashMap<Hud, HudPreviewState>()
    for (section in sections) {
        for (hud in section.huds) {
            if (hud !is LegacyHud) previews[hud] = key(hud) { rememberHudPreview(hud) }
        }
    }
    val labelFont = PolyFont(LocalTheme.current.typography.family.resolve(FontWeight.Normal), HUD_LABEL_FONT_SIZE.value)
    val sizes = HashMap<Hud, Size>()
    val geometry = HashMap<Hud, HudPreviewGeometry>()
    for (section in sections) {
        for (hud in section.huds) {
            val (naturalW, naturalH) = if (hud is LegacyHud) {
                val (minW, minH) = hud.minimumSize()
                (if (minW > 0f) minW else LEGACY_PREVIEW_FALLBACK_SIZE) to
                        (if (minH > 0f) minH else LEGACY_PREVIEW_FALLBACK_SIZE)
            } else previews.getValue(hud).let { it.naturalWidth to it.naturalHeight }
            if (naturalW > 0f && naturalH > 0f) {
                val scale = previewScaleFor(naturalW, naturalH, maxCardWidth, density)
                val size = previewCardSize(naturalW, naturalH, scale, maxCardWidth, density, hud is LegacyHud)
                val cardSize = if (hud is LegacyHud) size else {
                    val labelWidth = labelFont.measureTextWidth(hudLabel(hud)).dp
                    DpSize(
                        max(size.width, labelWidth + PREVIEW_CARD_PADDING * 2).coerceAtMost(maxCardWidth),
                        size.height + HUD_LABEL_HEIGHT,
                    )
                }
                geometry[hud] = HudPreviewGeometry(size, scale, cardSize)
                sizes[hud] = Size(cardSize.width.value, cardSize.height.value)
            }
        }
    }
    val rows = sections.map { packHudLibraryRows(it.huds, sizes, maxCardWidth.value, LIBRARY_SPACING.value) }
    val offsets = LinkedHashMap<String, Float>()
    var y = 0f
    for ((sectionIndex, section) in sections.withIndex()) {
        offsets[librarySectionKey(section.modId)] = y
        y += (LIBRARY_HEADER_HEIGHT + LIBRARY_SPACING).value
        val sectionRows = rows[sectionIndex]
        for ((rowIndex, row) in sectionRows.withIndex()) {
            val padding = if (rowIndex == sectionRows.lastIndex && sectionIndex < sections.lastIndex) 6.dp else 0.dp
            y += row.height + padding.value + LIBRARY_SPACING.value
        }
    }
    return HudLibraryLayout(rows, offsets, previews, geometry)
}

@Composable
private fun HudLibraryPanel(
    modifier: Modifier = Modifier,
    searchFocusRequested: Boolean,
    onSearchFocused: () -> Unit,
    onClose: () -> Unit,
    providers: List<Hud>,
    suggestEvergreenHud: Boolean,
    activeModId: String?,
    pendingScroll: String?,
    onScrollComplete: () -> Unit,
    onDragStart: (Hud, Float, Float, Float, Float) -> Unit = { _, _, _, _, _ -> },
    onDragMove: (PointerEvent) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onCardClick: (Hud) -> Unit = {},
) {
    val theme = LocalTheme.current
    val search = remember { TextFieldState() }
    val searchText = search.text.trim()
    val scrollState = rememberScrollState()
    val searchHits = rememberHudSearchResults(providers, searchText)
    val groupedHuds = (searchHits ?: providers.filter { it.addableToLibrary }.groupBy { it.configId }.toList())
        .map { (modId, huds) -> modId to huds.filter { it.addableToLibrary } }
    val modIds = groupedHuds.mapNotNull { it.first }
    val modNames = remember(modIds) { modIds.associateWith { modNameFor(it) ?: it } }
    val sections = remember(groupedHuds, modNames) {
        groupedHuds.mapNotNull { (modId, huds) ->
            huds.takeIf { it.isNotEmpty() }
                ?.let { HudLibrarySection(modId, modId?.let { id -> modNames[id] } ?: "Other", it) }
        }
    }
    val maxCardWidth = 401.dp - 16.dp - (if (suggestEvergreenHud) 16.dp else 0.dp) - HudLibraryGutter * 2
    val (rows, sectionOffsets, previews, geometry) = rememberHudLibraryLayout(sections, maxCardWidth)
    LaunchedEffect(pendingScroll, sectionOffsets.keys.toList()) {
        val target = pendingScroll ?: return@LaunchedEffect
        val offset = sectionOffsets[librarySectionKey(target)]
        if (offset == null) {
            if (searchText.isNotEmpty()) search.replaceText("") else onScrollComplete()
            return@LaunchedEffect
        }
        scrollState.animateBy(offset - scrollState.value)
        onScrollComplete()
    }
    val scrolledLibraryMod = if (scrollState.maxValue <= 0f) {
        sections.firstOrNull()?.modId
    } else {
        val activationLine = scrollState.value + scrollState.viewport * 0.55f
        sections.lastOrNull { section ->
            sectionOffsets.getValue(librarySectionKey(section.modId)) <= activationLine
        }?.modId ?: sections.firstOrNull()?.modId
    }
    val activeLibraryMod = activeModId?.takeIf { intent -> sections.any { it.modId == intent } }
        ?: scrolledLibraryMod

    Box(
        modifier = modifier
            .size(401.dp, 481.dp)
            // the mod icon column normally supplies the gap to the screen edge
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = if (suggestEvergreenHud) 16.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.popupBackground, theme.backgroundShape)
                .border(1.dp, theme.borderColor, theme.backgroundShape)
                .padding(vertical = HudLibraryGutter)
                .onPointerEvent { true },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = HudLibraryGutter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("HUDs", color = theme.textColor, fontSize = 18.sp)
                if (!suggestEvergreenHud) {
                    SearchBar(search, "Search HUDs...", searchFocusRequested, onSearchFocused)
                    HudIconButton("close", modifier = Modifier.size(18.dp), onClick = onClose)
                }
            }
            if (suggestEvergreenHud) {
                EvergreenHudSuggestion(Modifier.fillMaxWidth().weight(1f).padding(horizontal = HudLibraryGutter))
                return@Column
            }
            Text(
                "Click to add  ·  Drag to place it yourself",
                modifier = Modifier.padding(horizontal = HudLibraryGutter),
                color = theme.textColorSecondary,
                fontSize = 11.sp,
            )
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = HudLibraryGutter),
                    verticalArrangement = Arrangement.spacedBy(LIBRARY_SPACING),
                ) {
                    if (sections.isEmpty()) {
                        Text(
                            "No HUDs found",
                            color = theme.textColorSecondary,
                            fontSize = 14.sp,
                        )
                    }
                    sections.forEachIndexed { sectionIndex, section ->
                        key(section.modId) {
                            HudLibraryHeader(section, active = section.modId == activeLibraryMod)
                            val sectionRows = rows[sectionIndex]
                            sectionRows.forEachIndexed { rowIndex, row ->
                                val bottomPadding = if (rowIndex == sectionRows.lastIndex && sectionIndex < sections.lastIndex) 6.dp else 0.dp
                                Row(
                                    modifier = Modifier
                                        .height(row.height.dp + bottomPadding)
                                        .padding(bottom = bottomPadding),
                                    horizontalArrangement = Arrangement.spacedBy(LIBRARY_SPACING),
                                ) {
                                    row.huds.forEachIndexed { i, hud ->
                                        key(hud) {
                                            val hudGeometry = geometry.getValue(hud)
                                            val cardModifier = Modifier.width(row.widths[i].dp).fillMaxHeight()
                                            if (hud is LegacyHud) {
                                                LegacyHudPreviewCard(hud, hudGeometry, onDragStart, onDragMove, onDragEnd, onCardClick, cardModifier)
                                            } else {
                                                ComposeHudPreviewCard(hud, previews.getValue(hud), hudGeometry, onDragStart, onDragMove, onDragEnd, onCardClick, cardModifier)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    scrollState,
                    color = theme.textColorSecondary.copy(0.40f),
                    hoverColor = theme.textColorSecondary.copy(0.70f),
                    modifier = Modifier.padding(end = (HudLibraryGutter - 8.dp) / 2).fillMaxHeight().align(Alignment.TopEnd),
                    radius = theme.scrollBarShape.radius.dp,
                )
            }
        }
    }
}

@Composable
private fun EvergreenHudSuggestion(modifier: Modifier) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val isHovered by source.collectIsHoveredAsState()
    val background = animateColorAsState(if (isHovered) Accent.lerp(Color.White, 0.12f) else Accent)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No HUDs installed", color = theme.textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(
            "Install EvergreenHUD for FPS, CPS, coordinates, ping, and many more HUDs.",
            color = theme.textColorSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            softWrap = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        )
        Box(
            modifier = Modifier
                .background(background, theme.buttonShape)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(source, indication = null) { NetworkUtils.browseLink("https://modrinth.com/mod/evergreenhud") },
        ) {
            Text("Get EvergreenHUD", color = Color.White.copy(0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ModIconColumn(
    modifier: Modifier = Modifier,
    modIds: List<String>,
    activeModId: String?,
    libraryVisible: Boolean,
    onModClick: (String) -> Unit,
) {
    val theme = LocalTheme.current
    Column(
        modifier = modifier
            .background(theme.popupBackground, theme.backgroundShape)
            .border(1.dp, theme.borderColor, theme.backgroundShape)
            .padding(12.dp)
            .onPointerEvent { true },
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        modIds.forEach { modId ->
            key(modId) {
                ModFilterIcon(
                    iconName = libraryIconFor(modId),
                    selected = activeModId == modId && libraryVisible,
                ) { onModClick(modId) }
            }
        }
    }
}

private val hudCardOverlayScrim = Color.Black.copy(0.4f)

@Composable
private fun BoxScope.HudCardAddOverlay(visible: Boolean) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(150))
    if (alpha > 0f) {
        Box(
            modifier = Modifier.fillMaxSize().alpha(alpha).background(hudCardOverlayScrim),
            contentAlignment = Alignment.Center,
        ) {
            Icon("plus", modifier = Modifier.size(20.dp), color = Color.White)
        }
    }
}

private const val PREVIEW_SCALE = 2f
private val PREVIEW_CARD_PADDING = 12.dp
private val HUD_LABEL_HEIGHT = 20.dp
private val HUD_LABEL_FONT_SIZE = 11.sp

private fun hudLabel(hud: Hud): String = localizedLabel(hud.title) ?: hud.title
private val PREVIEW_MAX_CARD_HEIGHT = 160.dp
private const val LEGACY_PREVIEW_FALLBACK_SIZE = 48f

private fun previewScaleFor(naturalW: Float, naturalH: Float, maxCardWidth: Dp, density: Float): Float {
    if (naturalW <= 0f || naturalH <= 0f) return PREVIEW_SCALE
    val availableW = (maxCardWidth - PREVIEW_CARD_PADDING * 2).value
    val availableH = (PREVIEW_MAX_CARD_HEIGHT - PREVIEW_CARD_PADDING * 2).value
    return minOf(PREVIEW_SCALE, availableW * density / naturalW, availableH * density / naturalH)
        .coerceAtLeast(0.05f)
}

private fun previewCardSize(naturalW: Float, naturalH: Float, scale: Float, maxCardWidth: Dp, density: Float, legacy: Boolean): DpSize {
    val width = (naturalW * scale / density).dp + PREVIEW_CARD_PADDING * 2
    val height = (naturalH * scale / density).dp + PREVIEW_CARD_PADDING * 2
    return DpSize(
        (if (legacy) width.coerceAtLeast(72.dp) else width).coerceAtMost(maxCardWidth),
        if (legacy) height.coerceAtLeast(72.dp) else height,
    )
}

@Composable
private fun LegacyHudPreviewCard(
    hud: Hud,
    geometry: HudPreviewGeometry,
    onDragStart: (Hud, Float, Float, Float, Float) -> Unit,
    onDragMove: (PointerEvent) -> Unit,
    onDragEnd: () -> Unit,
    onCardClick: (Hud) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = previewPxPerDp()
    val theme = LocalTheme.current

    var isHovered by remember { mutableStateOf(false) }
    val backgroundColor = animateColorAsState(
        if (isHovered) Accent else theme.popupBackground,
    )
    val pressPos = remember { arrayOfNulls<Offset>(1) }
    var dragStarted by remember { mutableStateOf(false) }
    val cardBounds = remember { arrayOf(Rect.Zero) }

    val cardPadding = PREVIEW_CARD_PADDING
    val (size, previewScale) = geometry
    Box(
        modifier = modifier
            .background(backgroundColor, theme.buttonShape)
            .border(1.dp, theme.borderColor, theme.buttonShape)
            .clip(theme.buttonShape)
            .pointerHoverIcon(PointerIcon.Hand)
            .onPlaced { cardBounds[0] = Rect(it.x, it.y, it.x + it.width, it.y + it.height) }
            .hoverable { isHovered = it }
            .onPointerEvent { event ->
                when (event.type) {
                    PointerEventType.Press -> {
                        if (event.button != 0) return@onPointerEvent false
                        pressPos[0] = Offset(event.x, event.y)
                        dragStarted = false
                        true
                    }

                    PointerEventType.Move -> {
                        val start = pressPos[0] ?: return@onPointerEvent false
                        if (dragStarted) {
                            onDragMove(event)
                        } else if (hypot(event.x - start.x, event.y - start.y) > 8f) {
                            dragStarted = true
                            isHovered = false
                            val card = cardBounds[0]
                            val insetX = (card.width - size.width.value) / 2f + cardPadding.value
                            val insetY = (card.height - size.height.value) / 2f + cardPadding.value
                            val hudLocalX = ((start.x - card.left - insetX) * density / previewScale).coerceAtLeast(0f)
                            val hudLocalY = ((start.y - card.top - insetY) * density / previewScale).coerceAtLeast(0f)
                            onDragStart(hud, event.x, event.y, hudLocalX, hudLocalY)
                        }
                        true
                    }

                    PointerEventType.Release -> {
                        if (dragStarted) onDragEnd() else if (pressPos[0] != null && event.inside) onCardClick(hud)
                        pressPos[0] = null
                        dragStarted = false
                        true
                    }

                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            localizedLabel(hud.title) ?: "Legacy HUD",
            color = theme.textColor.copy(0.85f),
            fontSize = 12.sp,
            modifier = Modifier.padding(cardPadding),
        )
        HudCardAddOverlay(isHovered)
    }
}

@Composable
private fun ComposeHudPreviewCard(
    hud: Hud,
    preview: HudPreviewState,
    geometry: HudPreviewGeometry,
    onDragStart: (Hud, Float, Float, Float, Float) -> Unit,
    onDragMove: (PointerEvent) -> Unit,
    onDragEnd: () -> Unit,
    onCardClick: (Hud) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = previewPxPerDp()
    val theme = LocalTheme.current

    var isHovered by remember { mutableStateOf(false) }
    val backgroundColor = animateColorAsState(
        if (isHovered) Accent else theme.popupBackground,
    )

    val pressPos = remember { arrayOfNulls<Offset>(1) }
    var dragStarted by remember { mutableStateOf(false) }
    val cardBounds = remember { arrayOf(Rect.Zero) }

    val cardPadding = PREVIEW_CARD_PADDING
    val (size, previewScale) = geometry
    Box(
        modifier = modifier
            .background(backgroundColor, theme.buttonShape)
            .border(1.dp, theme.borderColor, theme.buttonShape)
            .clip(theme.buttonShape)
            .pointerHoverIcon(PointerIcon.Hand)
            .onPlaced { cardBounds[0] = Rect(it.x, it.y, it.x + it.width, it.y + it.height) }
            .hoverable { isHovered = it }
            .onPointerEvent { event ->
                when (event.type) {
                    PointerEventType.Press -> {
                        if (event.button != 0) return@onPointerEvent false
                        pressPos[0] = Offset(event.x, event.y)
                        dragStarted = false
                        true
                    }

                    PointerEventType.Move -> {
                        val start = pressPos[0] ?: return@onPointerEvent false
                        if (dragStarted) {
                            onDragMove(event)
                        } else if (hypot(event.x - start.x, event.y - start.y) > 8f) {
                            dragStarted = true
                            isHovered = false
                            val card = cardBounds[0]
                            val insetX = (card.width - size.width.value) / 2f + cardPadding.value
                            val insetY = (card.height - (size.height + HUD_LABEL_HEIGHT).value) / 2f + cardPadding.value
                            val hudLocalX = ((start.x - card.left - insetX) * density / previewScale).coerceAtLeast(0f)
                            val hudLocalY = ((start.y - card.top - insetY) * density / previewScale).coerceAtLeast(0f)
                            onDragStart(hud, event.x, event.y, hudLocalX, hudLocalY)
                        }
                        true
                    }

                    PointerEventType.Release -> {
                        if (dragStarted) onDragEnd() else if (pressPos[0] != null && event.inside) onCardClick(hud)
                        pressPos[0] = null
                        dragStarted = false
                        true
                    }

                    else -> false
                }
            },
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                HudPreviewCanvas(
                    preview,
                    previewScale,
                    hud.alwaysRedraw,
                    modifier = Modifier.size(size.width - cardPadding * 2, size.height - cardPadding * 2),
                )
            }
            Text(
                hudLabel(hud),
                modifier = Modifier.height(HUD_LABEL_HEIGHT).padding(horizontal = cardPadding),
                color = theme.textColorSecondary,
                fontSize = HUD_LABEL_FONT_SIZE,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HudCardAddOverlay(isHovered)
    }
}

@Composable
private fun ModFilterIcon(iconName: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val theme = LocalTheme.current
    val iconAlpha by animateFloatAsState(
        if (selected) 1f else if (isHovered) 0.8f else 0.7f,
        tween(150),
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .pointerHoverIcon(PointerIcon.Hand)
            .onClick(interactionSource) { onClick() },
    ) {
        Icon(
            iconName,
            modifier = Modifier.size(32.dp).align(Alignment.Center),
            color = theme.textColor.copy(alpha = iconAlpha),
        )
        if (selected) {
            Box(Modifier.absoluteOffset((-8).dp, 6.dp).size(4.dp, 24.dp).background(Accent, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
private fun SearchBar(
    state: TextFieldState,
    placeholder: String,
    focusRequested: Boolean = false,
    onFocused: () -> Unit = {},
) {
    LaunchedEffect(focusRequested) {
        if (!focusRequested) return@LaunchedEffect
        state.requestFocus(selectAll = true)
        onFocused()
    }
    val isFocused = state.focused
    val theme = LocalTheme.current
    val clipboard = LocalUiClipboard.current
    val borderColor = animateColorAsState(
        if (isFocused) theme.textColor.copy(.20f) else theme.textColor.copy(.10f)
    )
    val iconColor = animateColorAsState(
        if (isFocused) theme.textColor.copy(0.70f) else theme.textColor.copy(0.50f)
    ).value

    Row(
        modifier = Modifier.width(181.dp).height(32.dp)
            .border(1.dp, borderColor, theme.buttonShape)
            .background(theme.componentBackground, theme.buttonShape)
            .padding(start = 8.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon("search", color = iconColor, modifier = Modifier.size(12.dp))
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            BasicTextField(
                state,
                PolyFont(theme.typography.family.resolve(FontWeight.Normal), 12f),
                color = iconColor,
                caretColor = iconColor,
                selectionColor = Accent.copy(alpha = 0.4f),
                readClipboard = clipboard.read,
                writeClipboard = clipboard.write,
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart),
                placeholder = if (isFocused) "" else placeholder,
                placeholderColor = iconColor,
            )
        }
        if (state.text.isNotEmpty()) {
            HudIconButton("close", modifier = Modifier.size(16.dp)) {
                state.replaceText("")
                state.moveCaret(0)
            }
        }
    }
}
