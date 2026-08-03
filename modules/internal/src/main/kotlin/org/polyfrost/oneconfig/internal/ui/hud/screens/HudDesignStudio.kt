package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.apache.logging.log4j.LogManager
import org.jetbrains.skia.Paint
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudAnchor
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.HudResize
import org.polyfrost.oneconfig.api.notifications.v1.Notification
import org.polyfrost.oneconfig.api.notifications.v1.Notifications
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeybindUtils
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.*
import org.polyfrost.oneconfig.internal.ui.components.layout.FlexibleLayout
import org.polyfrost.oneconfig.internal.ui.hud.HudCanvasPasteMenu
import org.polyfrost.oneconfig.internal.ui.hud.HudCanvasResetMenu
import org.polyfrost.oneconfig.internal.ui.hud.LegacyHudOverlayBridge
import org.polyfrost.oneconfig.internal.ui.hud.repairHudStaticSize
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.Designer
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.Settings
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.ranges.coerceAtLeast
import kotlin.ranges.coerceAtMost
import org.polyfrost.oneconfig.api.hud.v1.LegacyHudMarker as LegacyHud

private val LOGGER = LogManager.getLogger("OneConfig/HudDesignStudio")

/** How far the cursor must travel while pressing a locked HUD before it counts as a move attempt. */
private const val LOCKED_DRAG_SLOP_PX = 3f

private var lockedHudToast: Notification? = null

/**
 * Tells the user why a HUD refused to move. Re-uses the toast while it is still on screen so
 * repeated attempts don't stack the notification centre full of identical entries.
 */
private fun notifyHudLocked() {
    lockedHudToast?.let { if (NotificationsManager.contains(it)) return }
    lockedHudToast = Notifications.info(
        "HUD element locked",
        "Unlock in settings to change its position.",
    )
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.safePointerEvent(
    eventType: PointerEventType,
    pass: PointerEventPass = PointerEventPass.Main,
    onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit,
): Modifier = onPointerEvent(eventType, pass) { event ->
    try {
        onEvent(event)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        LOGGER.error("Error while handling $eventType in the HUD Design Studio", e)
    }
}

enum class StudioCategory(val title: String, val icon: String) {
    Settings("Settings", "qol"),
    Designer("Designer", "paintbrush");
}

object HudEditorViewport {
    var viewportWidth by mutableStateOf(0)
        private set
    var viewportHeight by mutableStateOf(0)
        private set

    fun update(width: Int, height: Int) {
        if (width == viewportWidth && height == viewportHeight) return
        Snapshot.withMutableSnapshot {
            viewportWidth = width
            viewportHeight = height
        }
    }

    fun observe() {
        @Suppress("UNUSED_EXPRESSION") viewportWidth
        @Suppress("UNUSED_EXPRESSION") viewportHeight
    }
}

private data class HudBounds(val x: Float, val y: Float, val width: Float, val height: Float)

private val hiddenHudPaint = Paint().apply { setAlphaf(0.35f) }

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
private val marqueeFillColor = Color(SELECTION_BLUE_ARGB).copy(alpha = 0.15f)

private val idleHudBoxColor = Color.White.copy(0.2f)
private val hoveredHudBoxColor = Color.White.copy(0.5f)
private val hiddenHudBoxColor = Color.White.copy(0.4f)
private val hiddenHudDashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f), 0f)

// Locked HUDs get a dashed amber box instead of the plain white one, so "why won't this move?"
// is answered before the user tries to drag it.
private val lockedHudBoxColor = Color(0xFFE0A44C)
private val lockedHudDashEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 4f), 0f)

private val hiddenLegacyScrimColor = Color.Black.copy(0.45f)

// Backdrop for the per-HUD action bar, so the icons stay readable over any HUD content.
private val hudActionBarBackground = Color.Black.copy(0.55f)
private val hudActionBarBorder = Color.White.copy(0.12f)

private const val SNAP_DISTANCE_PX = 6f
private val snapGuideColor = Color(176, 47, 31)

private const val CHROME_SETTINGS_PANEL = "settings-panel"
private const val CHROME_LIBRARY = "library"
private const val SIDE_CHROME_DIM_ALPHA = 0.45f

private data class SnapGuides(val vertical: Float?, val horizontal: Float?) {
    companion object {
        val NONE = SnapGuides(null, null)
    }
}

private data class AxisSnap(val position: Float, val line: Float?)

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

/** The nine points of a HUD box the user can pin another HUD to, in reading order. */
private val ANCHOR_POINTS = listOf(
    HudAnchor.TopLeft, HudAnchor.Top, HudAnchor.TopRight,
    HudAnchor.Left, HudAnchor.Center, HudAnchor.Right,
    HudAnchor.BottomLeft, HudAnchor.Bottom, HudAnchor.BottomRight,
)

private const val ANCHOR_DOT_RADIUS_PX = 5f
private const val ANCHOR_HIT_RADIUS_PX = 10f
private val anchorLineColor = Color(0xFF0D99FF)
private val anchorLineDashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
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

/** Where [point] sits on [hud]'s box, in gui coordinates. */
private fun anchorPointOf(hud: Hud, point: HudAnchor): Offset? {
    val b = hudBounds(hud) ?: return null
    val (fx, fy) = anchorFractions(hud, point)
    return Offset(b.x + fx * b.width, b.y + fy * b.height)
}

/** Every HUD [sources] may be anchored to: not one of them, and not already hanging off one. */
private fun anchorTargetsFor(sources: List<Hud>): List<Hud> =
    HudManager.activeInstances.filter { candidate ->
        sources.none { it === candidate || candidate.anchorChainContains(it) }
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

private fun drawHudContents(sk: org.jetbrains.skia.Canvas, mcToScreen: Float) {
    for (hud in HudManager.activeInstances) {
        if (hud is LegacyHud) continue
        val root = hud.runtimeOrNull?.root ?: continue
        val bounds = hudBounds(hud) ?: continue
        val contentScale = hud.effectiveScale * mcToScreen
        sk.save()
        sk.translate(bounds.x * mcToScreen, bounds.y * mcToScreen)
        if (contentScale != 1f) sk.scale(contentScale, contentScale)
        if (hud.hidden) sk.saveLayer(null, hiddenHudPaint)
        try { root.render(RenderContext(sk)) } catch (_: Throwable) {}
        if (hud.hidden) sk.restore()
        sk.restore()
    }
    LegacyHudOverlayBridge.painter?.invoke(sk)
}

private fun orderedInstances(): List<Hud> = HudManager.zOrderedInstances { hud ->
    hudBounds(hud)?.let { floatArrayOf(it.x, it.y, it.width, it.height) }
}

private fun hitTestHud(hud: Hud, screenX: Float, screenY: Float): Boolean {
    val s = Platform.screen().screenToMcScale()
    val mcX = screenX * s
    val mcY = screenY * s
    val bounds = hudBounds(hud) ?: return false
    return mcX >= bounds.x && mcX <= bounds.x + bounds.width && mcY >= bounds.y && mcY <= bounds.y + bounds.height
}

/**
 * Huds under the cursor, topmost last. Locked huds never sit in front of unlocked ones: they are
 * only considered when nothing unlocked is under the cursor.
 */
private fun hudStackAt(screenX: Float, screenY: Float): List<Hud> {
    val stack = orderedInstances().filter { hitTestHud(it, screenX, screenY) }
    return stack.filter { !it.locked }.ifEmpty { stack }
}

private fun topHudAt(screenX: Float, screenY: Float): Hud? = hudStackAt(screenX, screenY).lastOrNull()

private fun pickHudAt(screenX: Float, screenY: Float, current: Hud?): Hud? {
    val stack = hudStackAt(screenX, screenY)
    if (stack.isEmpty()) return null
    val index = stack.indexOfFirst { it === current }
    if (index < 0) return stack.last()
    return stack[(index - 1 + stack.size) % stack.size]
}

/** Screen-space rectangle of the whole action bar, backdrop included. */
private data class HudActionBarLayout(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    fun contains(px: Float, py: Float) =
        px >= x && px <= x + width && py >= y && py <= y + height
}

private const val HUD_ACTION_BAR_ICONS = 3

/** Inset between the backdrop edge and the icons. */
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

    // The bar lives outside the HUD box: small HUDs used to swallow both icons (or overlap them
    // on top of each other) when the buttons were laid out inside the bounds.
    val screenW = HudEditorViewport.viewportWidth.toFloat()
        .takeIf { it > 0f } ?: (HudManager.guiScreenWidth * mcToScreen)
    val screenH = HudEditorViewport.viewportHeight.toFloat()
        .takeIf { it > 0f } ?: (HudManager.guiScreenHeight * mcToScreen)

    val padPx = actionBarPadding(gapPx)
    val barWidth = iconPx * HUD_ACTION_BAR_ICONS + gapPx * (HUD_ACTION_BAR_ICONS - 1) + padPx * 2f
    val barHeight = iconPx + padPx * 2f
    val maxLeft = (screenW - barWidth).coerceAtLeast(0f)
    val left = (sx + (sw - barWidth) / 2f).coerceIn(0f, maxLeft)

    val below = sy + sh + gapPx
    // keep clear of the size badge that is drawn above the selection box
    val above = sy - gapPx - barHeight - (HUD_SIZE_BADGE_GAP + HUD_SIZE_BADGE_HEIGHT)
    val y = when {
        below + barHeight <= screenH -> below
        above >= 0f -> above
        // no room either side: sit on the bottom edge of the HUD, clamped to the screen
        else -> (sy + sh - barHeight).coerceIn(0f, (screenH - barHeight).coerceAtLeast(0f))
    }

    return HudActionBarLayout(left, y, barWidth, barHeight)
}

private fun hitTestHudActionBar(
    hud: Hud,
    screenX: Float,
    screenY: Float,
    mcToScreen: Float,
    iconPx: Float,
    gapPx: Float,
): Boolean = hudActionBarLayout(hud, mcToScreen, iconPx, gapPx)?.contains(screenX, screenY) == true

/**
 * The bar plus the thin corridor between it and the HUD box. At small GUI scales HUDs sit packed
 * together, so the gap the cursor has to cross to reach the icons is usually owned by a neighbouring
 * HUD: without claiming it the hover flipped mid-travel and the bar disappeared before it could be
 * clicked.
 */
private fun hitTestHudActionBarZone(
    hud: Hud,
    screenX: Float,
    screenY: Float,
    mcToScreen: Float,
    iconPx: Float,
    gapPx: Float,
): Boolean {
    val bar = hudActionBarLayout(hud, mcToScreen, iconPx, gapPx) ?: return false
    if (bar.contains(screenX, screenY)) return true
    val bounds = hudBounds(hud) ?: return false
    val hudLeft = bounds.x * mcToScreen
    val hudTop = bounds.y * mcToScreen
    val hudRight = hudLeft + bounds.width * mcToScreen
    val hudBottom = hudTop + bounds.height * mcToScreen

    val top: Float
    val bottom: Float
    when {
        bar.y >= hudBottom -> { top = hudBottom; bottom = bar.y }
        bar.y + bar.height <= hudTop -> { top = bar.y + bar.height; bottom = hudTop }
        // bar overlaps the box (no room either side): there is no corridor to claim
        else -> return false
    }
    if (screenY < top || screenY > bottom) return false
    return screenX >= minOf(hudLeft, bar.x) && screenX <= maxOf(hudRight, bar.x + bar.width)
}

/**
 * The HUD box unioned with its action bar. The bar sits outside the box with a gap in between, so
 * hover has to survive the whole span: without this the bar vanished the moment the cursor left the
 * HUD on its way to the icons.
 */
private fun hitTestHudWithActionBar(
    hud: Hud,
    screenX: Float,
    screenY: Float,
    mcToScreen: Float,
    iconPx: Float,
    gapPx: Float,
): Boolean {
    val bounds = hudBounds(hud) ?: return false
    var left = bounds.x * mcToScreen
    var top = bounds.y * mcToScreen
    var right = left + bounds.width * mcToScreen
    var bottom = top + bounds.height * mcToScreen
    hudActionBarLayout(hud, mcToScreen, iconPx, gapPx)?.let { bar ->
        left = minOf(left, bar.x)
        top = minOf(top, bar.y)
        right = maxOf(right, bar.x + bar.width)
        bottom = maxOf(bottom, bar.y + bar.height)
    }
    return screenX >= left && screenX <= right && screenY >= top && screenY <= bottom
}

private fun hitTestResizeHandle(hud: Hud, screenX: Float, screenY: Float, mcToScreen: Float): ResizeCorner? {
    if (hud.resizeAxes == HudResize.None) return null
    val bounds = hudBounds(hud) ?: return null
    val sx = bounds.x * mcToScreen
    val sy = bounds.y * mcToScreen
    val sw = bounds.width * mcToScreen
    val sh = bounds.height * mcToScreen
    val hitSize = 14f
    fun contains(cx: Float, cy: Float): Boolean =
        screenX >= cx - hitSize / 2 && screenX <= cx + hitSize / 2 &&
            screenY >= cy - hitSize / 2 && screenY <= cy + hitSize / 2

    return when {
        contains(sx, sy) -> ResizeCorner.TopLeft
        contains(sx + sw, sy) -> ResizeCorner.TopRight
        contains(sx, sy + sh) -> ResizeCorner.BottomLeft
        contains(sx + sw, sy + sh) -> ResizeCorner.BottomRight
        else -> null
    }
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
        val factor = newScale / baseScale
        val newWidth = startBounds.width * factor
        val newHeight = startBounds.height * factor
        val newX = if (corner.isLeft) anchorX - newWidth else anchorX
        val newY = if (corner.isTop) anchorY - newHeight else anchorY
        hud.customScale = newScale
        hud.setAbsolutePosition(newX, newY)
        return
    }

    if (freeResize) {
        val effectiveScale = hud.effectiveScale.coerceAtLeast(0.001f)
        val (minStaticW, minStaticH) = hud.minimumSize()
        val newWidth = targetWidth.coerceAtLeast(minStaticW * effectiveScale)
        val newHeight = targetHeight.coerceAtLeast(minStaticH * effectiveScale)
        val newX = if (corner.isLeft) anchorX - newWidth else anchorX
        val newY = if (corner.isTop) anchorY - newHeight else anchorY

        hud.staticWidth = true
        hud.staticW = newWidth / effectiveScale
        hud.staticH = newHeight / effectiveScale
        hud.setAbsolutePosition(newX, newY)
        return
    }

    // Scale proportionally by projecting the mouse onto the diagonal from the anchor to the
    // dragged corner. Using the per-axis max-deviation factor amplified small moves on the
    // short axis into huge uniform jumps and made the corner drift away from the cursor.
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
    val newWidth = startBounds.width * factor
    val newHeight = startBounds.height * factor
    val newX = if (corner.isLeft) anchorX - newWidth else anchorX
    val newY = if (corner.isTop) anchorY - newHeight else anchorY

    hud.textScale = newTextScale
    if (hud.staticWidth && startStaticW > 0f && startStaticH > 0f) {
        hud.staticW = startStaticW * factor
        hud.staticH = startStaticH * factor
    }
    hud.setAbsolutePosition(newX, newY)
}

private fun DrawScope.drawSelectedHudBounds(
    bounds: HudBounds,
    mcToScreen: Float,
    showHandles: Boolean,
    showBadge: Boolean = true,
) {
    val sx = bounds.x * mcToScreen
    val sy = bounds.y * mcToScreen
    val sw = bounds.width * mcToScreen
    val sh = bounds.height * mcToScreen

    drawRect(
        color = selectionBlue,
        topLeft = Offset(sx, sy),
        size = Size(sw, sh),
        style = Stroke(width = 1f)
    )

    if (showHandles) {
        val handleSize = 7f
        listOf(
            Offset(sx, sy),
            Offset(sx + sw, sy),
            Offset(sx, sy + sh),
            Offset(sx + sw, sy + sh),
        ).forEach { corner ->
            val topLeft = Offset(corner.x - handleSize / 2, corner.y - handleSize / 2)
            drawRect(color = Color.White, topLeft = topLeft, size = Size(handleSize, handleSize))
            drawRect(color = selectionBlue, topLeft = topLeft, size = Size(handleSize, handleSize), style = Stroke(width = 1f))
        }
    }

    if (showBadge) {
        val badgeTopY = (sy - HUD_SIZE_BADGE_GAP - HUD_SIZE_BADGE_HEIGHT).coerceAtLeast(0f)
        drawHudSizeBadge("${bounds.width.roundToInt()} x ${bounds.height.roundToInt()}", sx + sw / 2, badgeTopY)
    }
}

private fun DrawScope.drawHudSizeBadge(label: String, centerX: Float, topY: Float) {
    val font = FontManager.getFont(10f, "poppins")
    val metrics = font.metrics
    val textWidth = font.measureTextWidth(label)
    val textHeight = metrics.descent - metrics.ascent
    val horizontalPadding = 5f
    val badgeWidth = textWidth + horizontalPadding * 2
    val badgeHeight = HUD_SIZE_BADGE_HEIGHT
    val badgeX = centerX - badgeWidth / 2

    drawRoundRect(
        color = selectionBlue,
        topLeft = Offset(badgeX, topY),
        size = Size(badgeWidth, badgeHeight),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawIntoCanvas { canvas ->
        val paint = Paint().apply { color = Color.White.toArgb() }
        val textX = badgeX + horizontalPadding
        val textY = topY + (badgeHeight - textHeight) / 2f - metrics.ascent
        canvas.skiaCanvas.drawString(label, textX, textY, font, paint)
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HudActionButton(
    icon: String,
    size: Dp,
    foreground: Color,
    hoveredForeground: Color,
    sound: UiSoundEvent,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .safePointerEvent(PointerEventType.Move, PointerEventPass.Final) { event ->
                if (event.changes.any { it.pressed }) return@safePointerEvent
                event.changes.forEach { if (!it.isConsumed) it.consume() }
            }
            .safePointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
                if (event.changes.none { it.isConsumed }) {
                    event.changes.forEach { it.consume() }
                    UiSounds.play(sound)
                    onClick()
                }
            },
    ) {
        IconButton(
            icon,
            modifier = Modifier.size(size),
            foreground = foreground,
            hoveredForeground = hoveredForeground,
            onClick = onClick,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HudActionBar(
    hud: Hud,
    mcToScreen: Float,
    densityFloat: Float,
    actionIconPx: Float,
    actionBarGapPx: Float,
    chromeAlpha: Float,
    onSettings: () -> Unit,
) {
    val bounds = hudBounds(hud) ?: return
    if (bounds.width <= 0f || bounds.height <= 0f) return
    val layout = hudActionBarLayout(hud, mcToScreen, actionIconPx, actionBarGapPx) ?: return
    val shape = LocalTheme.current.buttonShape
    val iconSize = (actionIconPx / densityFloat).dp
    val barX = (layout.x / densityFloat).coerceAtLeast(0f)
    val barY = (layout.y / densityFloat).coerceAtLeast(0f)
    val barPadding = (actionBarPadding(actionBarGapPx) / densityFloat).dp
    val isHidden = hud.hidden
    val isLocked = hud.locked
    Row(
        modifier = Modifier
            .padding(start = barX.dp, top = barY.dp)
            .graphicsLayer { alpha = chromeAlpha }
            .background(hudActionBarBackground, shape)
            .border(1.dp, hudActionBarBorder, shape)
            .padding(barPadding),
        horizontalArrangement = Arrangement.spacedBy((actionBarGapPx / densityFloat).dp),
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
        HudActionButton(
            if (isLocked) "lock" else "unlock",
            iconSize,
            foreground = if (isLocked) Accent else Color.White.copy(0.7f),
            hoveredForeground = if (isLocked) Accent else Color.White,
            sound = UiSoundEvent.CLICK,
        ) {
            Snapshot.withMutableSnapshot { hud.locked = !hud.locked }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HudDesignStudio(onReturnToOneConfig: (() -> Unit)? = null) {
    HudEditorViewport.observe()
    var activeCategory by remember { mutableStateOf(StudioCategory.Settings) }
    var selectedHuds by remember { mutableStateOf<Set<Hud>>(emptySet()) }
    var panelHud by remember { mutableStateOf<Hud?>(null) }
    var hoveredHud by remember { mutableStateOf<Hud?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var draggedHud by remember { mutableStateOf<Hud?>(null) }
    var draggedGroup by remember { mutableStateOf<Set<Hud>>(emptySet()) }
    var dragStarts by remember { mutableStateOf<Map<Hud, Pair<Float, Float>>>(emptyMap()) }
    var snapGuides by remember { mutableStateOf(SnapGuides.NONE) }
    var isResizing by remember { mutableStateOf(false) }
    var resizedHud by remember { mutableStateOf<Hud?>(null) }
    var resizeCorner by remember { mutableStateOf<ResizeCorner?>(null) }
    var resizeStartBounds by remember { mutableStateOf<HudBounds?>(null) }
    var resizeStartTextScale by remember { mutableStateOf(1f) }
    var resizeStartScale by remember { mutableStateOf(1f) }
    var resizeStartStaticW by remember { mutableStateOf(0f) }
    var resizeStartStaticH by remember { mutableStateOf(0f) }
    var libraryVisible by remember { mutableStateOf(true) }
    var searchText by remember { mutableStateOf("") }
    var pendingLibraryScroll by remember { mutableStateOf<String?>(null) }
    // The mod the user last picked from the icon column. Kept separate from the scroll-derived
    // section because the list clamps at its end: the final sections can never become the first
    // visible item, and a mod whose HUDs are all placed has no section to scroll to at all.
    var libraryModIntent by remember { mutableStateOf<String?>(null) }
    val libraryListState = rememberLazyListState()
    val chromeRects = remember { mutableStateMapOf<String, Rect>() }
    var panelOffset by remember { mutableStateOf(Offset.Zero) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var hudContextMenuTarget by remember { mutableStateOf<Hud?>(null) }
    var hudContextMenuOffset by remember { mutableStateOf(IntOffset.Zero) }
    // A locked HUD that is currently being pressed: once the cursor moves far enough we explain
    // why it isn't following the mouse instead of silently doing nothing.
    var lockedPressHud by remember { mutableStateOf<Hud?>(null) }
    var lockedPressOrigin by remember { mutableStateOf(Offset.Zero) }
    val keyFocusRequester = remember { FocusRequester() }
    var hudClipboard by remember { mutableStateOf<List<Hud>>(emptyList()) }
    var pasteMenuOffset by remember { mutableStateOf<IntOffset?>(null) }
    var marqueeStart by remember { mutableStateOf<Offset?>(null) }
    var marqueeCurrent by remember { mutableStateOf<Offset?>(null) }
    var marqueeActive by remember { mutableStateOf(false) }
    var marqueeAdditive by remember { mutableStateOf(false) }
    var pressWasSecondary by remember { mutableStateOf(false) }
    val lastPointerPos = remember { FloatArray(2) }
    // HUDs waiting for the user to pick an anchor point. Non-empty while the picker overlay is up.
    var anchorPickSources by remember { mutableStateOf<List<Hud>>(emptyList()) }
    var hoveredAnchor by remember { mutableStateOf<Pair<Hud, HudAnchor>?>(null) }

    // The "active" HUD of a selection, the panel target if it is still selected, otherwise the
    // most recently added member. Drives the settings panel, resize handles, action bar and keybinds.
    fun primaryHud(): Hud? = panelHud?.takeIf { it in selectedHuds } ?: selectedHuds.lastOrNull()

    val deleteHuds: (Collection<Hud>) -> Unit = { huds ->
        val removed = huds.filter { it.deletable() }
        if (removed.isNotEmpty()) {
            Snapshot.withMutableSnapshot {
                val removedSet = removed.toSet()
                selectedHuds = selectedHuds - removedSet
                removed.forEach { hud ->
                    if (panelHud === hud) panelHud = null
                    if (hoveredHud === hud) hoveredHud = null
                    HudManager.removeHud(hud, delete = true)
                    HudDesignSession.forget(hud)
                }
            }
            UiSounds.play(UiSoundEvent.CLICK)
        }
    }

    LaunchedEffect(Unit) {
        val pending = HudManager.pendingSelection
        HudManager.pendingSelection = null
        if (pending != null) {
            if (pending in HudManager.activeInstances) {
                Snapshot.withMutableSnapshot {
                    selectedHuds = setOf(pending)
                    panelHud = pending
                }
            }
        } else {
            val restored = HudDesignSession.restoreSelection()
            if (restored.isNotEmpty()) {
                Snapshot.withMutableSnapshot {
                    selectedHuds = restored.toSet()
                    if (HudDesignSession.restorePanelOpen()) panelHud = restored.first()
                    activeCategory = HudDesignSession.restoreCategory()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            HudDesignSession.save(emptyList(), false, activeCategory)
            HudDesignSession.activeSelection = emptyList()
            HudDesignSession.clearCommands()
        }
    }

    LaunchedEffect(selectedHuds, panelHud) {
        val primary = primaryHud()
        primary?.let { repairHudStaticSize(it) }
        if (panelHud != null && panelHud !in selectedHuds) panelHud = null
        HudDesignSession.activeSelection = selectedHuds.toList()
    }

    LaunchedEffect(Unit) {
        HudDesignSession.clearCommands()
        for (command in HudDesignSession.commands) {
            when (command) {
                is StudioCommand.Select -> {
                    val huds = command.huds
                    if (huds.isNotEmpty() && huds.all { it in HudManager.activeInstances }) {
                        Snapshot.withMutableSnapshot { selectedHuds = huds.toSet() }
                    }
                }

                StudioCommand.OpenSettings -> {
                    val primary = primaryHud()
                    if (primary != null) {
                        Snapshot.withMutableSnapshot {
                            panelHud = primary
                            activeCategory = StudioCategory.Settings
                        }
                    }
                }

                StudioCommand.Copy -> {
                    Snapshot.withMutableSnapshot { hudClipboard = selectedHuds.toList() }
                    UiSounds.play(UiSoundEvent.CLICK)
                }

                StudioCommand.Cut -> {
                    Snapshot.withMutableSnapshot { hudClipboard = selectedHuds.toList() }
                    deleteHuds(selectedHuds)
                }

                StudioCommand.Paste -> {
                    if (hudClipboard.isNotEmpty()) {
                        val s = Platform.screen().screenToMcScale()
                        val pasted = duplicateHudGroup(
                            hudClipboard,
                            Offset(lastPointerPos[0] * s, lastPointerPos[1] * s),
                        )
                        if (pasted.isNotEmpty()) {
                            Snapshot.withMutableSnapshot {
                                selectedHuds = pasted.toSet()
                                pasteMenuOffset = null
                            }
                        }
                        UiSounds.play(UiSoundEvent.CLICK)
                    }
                }

                StudioCommand.Delete -> deleteHuds(selectedHuds)

                StudioCommand.SelectAll -> Snapshot.withMutableSnapshot {
                    selectedHuds = HudManager.activeInstances.filter { !it.locked }.toSet()
                }

                StudioCommand.Lock -> {
                    val targets = selectedHuds.toList()
                    if (targets.isNotEmpty()) {
                        val lock = targets.any { !it.locked }
                        Snapshot.withMutableSnapshot {
                            targets.forEach { it.locked = lock }
                        }
                        OneConfigConfig.INSTANCE.save()
                    }
                }
            }
        }
    }

    val providers = remember { HudManager.providers().toList() }
    val modIds = remember(providers) { providers.mapNotNull { it.configId }.distinct() }
    val modNames = remember(modIds) { modIds.associateWith { modNameFor(it) ?: it } }
    val librarySections = providers
        .filter { hud ->
            (searchText.isEmpty() || localizedLabel(hud.title)?.contains(searchText, ignoreCase = true) == true) &&
                (hud.multipleInstancesAllowed() || HudManager.getHudsOfType(hud::class.java).isEmpty())
        }
        .groupBy { it.configId }
        .map { (modId, huds) -> HudLibrarySection(modId, modId?.let { modNames[it] } ?: "Other", huds) }
    val librarySectionIds = librarySections.map { it.modId }
    val scrolledLibraryMod = librarySections.getOrNull(libraryListState.firstVisibleItemIndex)?.modId
    val activeLibraryMod = libraryModIntent ?: scrolledLibraryMod

    // A manual scroll takes the highlight back off the clicked icon and hands it to the list.
    LaunchedEffect(libraryListState) {
        snapshotFlow { libraryListState.isScrollInProgress }
            .collect { scrolling -> if (scrolling && pendingLibraryScroll == null) libraryModIntent = null }
    }

    LaunchedEffect(pendingLibraryScroll, librarySectionIds) {
        val target = pendingLibraryScroll ?: return@LaunchedEffect
        val index = librarySectionIds.indexOf(target)
        if (index >= 0) {
            libraryListState.animateScrollToItem(index)
            pendingLibraryScroll = null
        } else if (searchText.isEmpty()) {
            pendingLibraryScroll = null
        }
    }

    val densityObj = LocalDensity.current
    val densityFloat = densityObj.density
    val actionIconPx = with(densityObj) { 24.dp.toPx() }
    val actionBarGapPx = with(densityObj) { 8.dp.toPx() }
    val libraryChromeVisible = modIds.isNotEmpty() && selectedHuds.isEmpty() && !isDragging && !marqueeActive

    fun Modifier.chromeRegion(key: String) = onGloballyPositioned { chromeRects[key] = it.boundsInRoot() }

    fun inChrome(px: Float, py: Float): Boolean {
        val point = Offset(px, py)
        return chromeRects.values.any { it.contains(point) }
    }

    fun movePanel(delta: Offset) {
        val rect = chromeRects[CHROME_SETTINGS_PANEL]
        if (rect == null || rootSize.width <= 0 || rootSize.height <= 0) {
            panelOffset += delta
            return
        }
        val dx = delta.x.coerceIn(minOf(-rect.left, 0f), maxOf(rootSize.width - rect.right, 0f))
        val dy = delta.y.coerceIn(minOf(-rect.top, 0f), maxOf(rootSize.height - rect.bottom, 0f))
        panelOffset += Offset(dx, dy)
    }

    /** Recomputes the marquee selection from [marqueeStart]..[marqueeCurrent]. */
    fun updateMarqueeSelection() {
        val start = marqueeStart ?: return
        val cur = marqueeCurrent ?: return
        val s = Platform.screen().screenToMcScale()
        val left = minOf(start.x, cur.x) * s
        val top = minOf(start.y, cur.y) * s
        val right = maxOf(start.x, cur.x) * s
        val bottom = maxOf(start.y, cur.y) * s
        val intersecting = HudManager.activeInstances.filter { hud ->
            if (hud.locked) return@filter false
            val b = hudBounds(hud) ?: return@filter false
            b.x < right && b.x + b.width > left && b.y < bottom && b.y + b.height > top
        }.toSet()
        selectedHuds = if (marqueeAdditive) selectedHuds + intersecting else intersecting
    }

    // Unified pointer modifier: drag any HUD, click to select, hover to show action bar
    val pointerModifier = Modifier
        .safePointerEvent(PointerEventType.Press) { event ->
            if (event.changes.any { it.isConsumed }) return@safePointerEvent
            val pos = event.changes.firstOrNull()?.position ?: return@safePointerEvent
            if (inChrome(pos.x, pos.y)) return@safePointerEvent
            // While picking an anchor the canvas does nothing else: a left click takes the point
            // under the cursor (or cancels if there is none), a right click drops the anchor.
            if (anchorPickSources.isNotEmpty()) {
                val sources = anchorPickSources
                val secondary = event.buttons.isSecondaryPressed
                event.changes.forEach { it.consume() }
                val picked = if (secondary) {
                    null
                } else {
                    hitTestAnchorPoint(sources, pos.x, pos.y, Platform.screen().mcToScreenScale())
                }
                Snapshot.withMutableSnapshot {
                    when {
                        secondary -> sources.forEach { it.clearAnchor() }
                        picked != null -> sources.forEach { it.anchorTo(picked.first, picked.second) }
                    }
                    anchorPickSources = emptyList()
                    hoveredAnchor = null
                    // swallows the release that follows, so it can't fall through to selection
                    pressWasSecondary = true
                }
                if (secondary || picked != null) UiSounds.play(UiSoundEvent.CLICK)
                return@safePointerEvent
            }
            if (event.buttons.isSecondaryPressed) {
                val hit = primaryHud()?.takeIf { hitTestHud(it, pos.x, pos.y) }
                    ?: topHudAt(pos.x, pos.y)
                event.changes.forEach { it.consume() }
                Snapshot.withMutableSnapshot {
                    pressWasSecondary = true
                    if (hit != null) {
                        if (hit !in selectedHuds) {
                            UiSounds.play(UiSoundEvent.HUD_SELECT)
                            selectedHuds = setOf(hit)
                        }
                        hudContextMenuTarget = hit
                        hudContextMenuOffset = IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
                        libraryVisible = false
                        pasteMenuOffset = null
                    } else {
                        hudContextMenuTarget = null
                        pasteMenuOffset = IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
                    }
                }
                return@safePointerEvent
            }
            pressWasSecondary = false
            val mcToScreen = Platform.screen().mcToScreenScale()
            val singleSelection = selectedHuds.size == 1
            val selected = if (singleSelection) primaryHud() else null
            if (selected != null && !selected.locked) {
                val handle = hitTestResizeHandle(selected, pos.x, pos.y, mcToScreen)
                val bounds = hudBounds(selected)
                if (handle != null && bounds != null) {
                    event.changes.forEach { it.consume() }
                    UiSounds.play(UiSoundEvent.HUD_RESIZE_START)
                    Snapshot.withMutableSnapshot {
                        isResizing = true
                        resizedHud = selected
                        resizeCorner = handle
                        resizeStartBounds = bounds
                        resizeStartTextScale = selected.textScale
                        resizeStartScale = selected.customScale
                        resizeStartStaticW = selected.staticW
                        resizeStartStaticH = selected.staticH
                        hoveredHud = selected
                        libraryVisible = false
                        pasteMenuOffset = null
                    }
                    return@safePointerEvent
                }
            }
            // Clicks inside the bar belong to its buttons: never select or drag the HUD behind it.
            if (hoveredHud?.let { hitTestHudActionBar(it, pos.x, pos.y, mcToScreen, actionIconPx, actionBarGapPx) } == true) {
                Snapshot.withMutableSnapshot { pasteMenuOffset = null }
                return@safePointerEvent
            }
            val s = Platform.screen().screenToMcScale()
            val actionPressed = KeybindUtils.isActionModifierPressed(event.keyboardModifiers)
            val shift = event.keyboardModifiers.isShiftPressed
            val hit = pickHudAt(pos.x, pos.y, primaryHud())
            if (hit != null && hit.locked) {
                event.changes.forEach { it.consume() }
                if (hit !in selectedHuds) UiSounds.play(UiSoundEvent.HUD_SELECT)
                Snapshot.withMutableSnapshot {
                    selectedHuds = if (actionPressed) selectedHuds + hit else setOf(hit)
                    libraryVisible = false
                    lockedPressHud = hit
                    lockedPressOrigin = pos
                }
                return@safePointerEvent
            }
            if (hit != null) UiSounds.play(UiSoundEvent.HUD_DRAG_START)
            if (hit != null) event.changes.forEach { it.consume() }
            Snapshot.withMutableSnapshot {
                if (hit != null) {
                    val wasSelected = hit in selectedHuds
                    val newSelection = when {
                        actionPressed -> if (wasSelected) selectedHuds - hit else selectedHuds + hit
                        shift -> if (wasSelected) selectedHuds else selectedHuds + hit
                        else -> if (wasSelected) selectedHuds else setOf(hit)
                    }
                    selectedHuds = newSelection
                    libraryVisible = false
                    if (newSelection.isNotEmpty()) {
                        hit.onEditorDragStart()
                        dragOffsetX = pos.x * s - hit.x
                        dragOffsetY = pos.y * s - hit.y
                        isDragging = true
                        draggedHud = hit
                        draggedGroup = newSelection
                        dragStarts = newSelection.associateWith { it.x to it.y }
                        hoveredHud = hit
                    } else {
                        isDragging = false
                        draggedHud = null
                        draggedGroup = emptySet()
                        dragStarts = emptyMap()
                    }
                } else {
                    selectedHuds = if (actionPressed || shift) selectedHuds else emptySet()
                    marqueeStart = pos
                    marqueeCurrent = pos
                    marqueeActive = true
                    marqueeAdditive = actionPressed || shift
                    pasteMenuOffset = null
                }
            }
        }
        .safePointerEvent(PointerEventType.Move) { event ->
            val pos = event.changes.firstOrNull()?.position ?: return@safePointerEvent
            lastPointerPos[0] = pos.x
            lastPointerPos[1] = pos.y
            if (anchorPickSources.isNotEmpty()) {
                val mcToScreen = Platform.screen().mcToScreenScale()
                val hit = hitTestAnchorPoint(anchorPickSources, pos.x, pos.y, mcToScreen)
                if (hit != hoveredAnchor) hoveredAnchor = hit
                return@safePointerEvent
            }
            if (isResizing) {
                if (event.changes.none { it.pressed }) {
                    Snapshot.withMutableSnapshot {
                        isResizing = false
                        resizedHud = null
                        resizeCorner = null
                        resizeStartBounds = null
                    }
                    return@safePointerEvent
                }
                val hit = resizedHud ?: return@safePointerEvent
                val corner = resizeCorner ?: return@safePointerEvent
                val bounds = resizeStartBounds ?: return@safePointerEvent
                val s = Platform.screen().screenToMcScale()
                Snapshot.withMutableSnapshot {
                    resizeHud(
                        hud = hit,
                        corner = corner,
                        startBounds = bounds,
                        startTextScale = resizeStartTextScale,
                        startScale = resizeStartScale,
                        startStaticW = resizeStartStaticW,
                        startStaticH = resizeStartStaticH,
                        mouseX = pos.x * s,
                        mouseY = pos.y * s,
                        freeResize = event.keyboardModifiers.isShiftPressed,
                    )
                }
            } else if (isDragging) {
                if (event.changes.none { it.pressed }) {
                    val dropped = draggedHud
                    Snapshot.withMutableSnapshot {
                        isDragging = false
                        draggedHud = null
                        draggedGroup = emptySet()
                        dragStarts = emptyMap()
                        snapGuides = SnapGuides.NONE
                    }
                    dropped?.onEditorDragEnd()
                    return@safePointerEvent
                }
                val s = Platform.screen().screenToMcScale()
                val hit = draggedHud ?: return@safePointerEvent
                val starts = dragStarts
                val start = starts[hit] ?: return@safePointerEvent
                val rawX = pos.x * s - dragOffsetX
                val rawY = pos.y * s - dragOffsetY
                val bounds = hudBounds(hit)
                val snapXLine: Float?
                val snapYLine: Float?
                val targetX: Float
                val targetY: Float
                if (bounds != null && !event.keyboardModifiers.isAltPressed) {
                    val threshold = SNAP_DISTANCE_PX * s
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
                Snapshot.withMutableSnapshot {
                    for (member in draggedGroup) {
                        val ms = starts[member] ?: continue
                        member.setAbsolutePosition(ms.first + dx, ms.second + dy)
                    }
                    snapGuides = SnapGuides(snapXLine, snapYLine)
                }
            } else if (marqueeActive) {
                if (event.changes.none { it.pressed }) {
                    Snapshot.withMutableSnapshot {
                        marqueeActive = false
                        marqueeStart = null
                        marqueeCurrent = null
                    }
                    return@safePointerEvent
                }
                Snapshot.withMutableSnapshot {
                    marqueeCurrent = pos
                    updateMarqueeSelection()
                }
            } else {
                if (lockedPressHud != null) {
                    if (event.changes.none { it.pressed }) {
                        lockedPressHud = null
                    } else if ((pos - lockedPressOrigin).getDistance() > LOCKED_DRAG_SLOP_PX) {
                        lockedPressHud = null
                        notifyHudLocked()
                    }
                }
                if (inChrome(pos.x, pos.y)) {
                    hoveredHud = null
                    return@safePointerEvent
                }
                val hit = topHudAt(pos.x, pos.y)
                val mcToScreen = Platform.screen().mcToScreenScale()
                val hovered = hoveredHud
                // Over the bar (or on the way to it) the hover must stick even if it sits on top of
                // another HUD, otherwise the icons swap out from under the cursor.
                val overActionBar = hovered?.let { hh ->
                    hitTestHudActionBarZone(hh, pos.x, pos.y, mcToScreen, actionIconPx, actionBarGapPx)
                } == true
                val inExpandedZone = hovered?.let { hh ->
                    hitTestHudWithActionBar(hh, pos.x, pos.y, mcToScreen, actionIconPx, actionBarGapPx)
                } == true
                hoveredHud = when {
                    overActionBar -> hovered
                    hit != null -> hit
                    inExpandedZone -> hovered
                    else -> null
                }
            }
        }
        .safePointerEvent(PointerEventType.Release) { event ->
            lockedPressHud = null
            if (marqueeActive) {
                Snapshot.withMutableSnapshot {
                    marqueeActive = false
                    marqueeStart = null
                    marqueeCurrent = null
                }
                return@safePointerEvent
            }
            val wasResizing = isResizing
            val wasResizedHud = resizedHud
            Snapshot.withMutableSnapshot {
                isResizing = false
                resizedHud = null
                resizeCorner = null
                resizeStartBounds = null
            }
            if (wasResizing) {
                UiSounds.play(UiSoundEvent.HUD_RESIZE_END)
                Snapshot.withMutableSnapshot {
                    if (wasResizedHud != null) selectedHuds = setOf(wasResizedHud)
                    libraryVisible = false
                }
                return@safePointerEvent
            }
            val wasDragging = isDragging
            val wasDraggedHud = draggedHud
            val wasDraggedGroup = draggedGroup
            Snapshot.withMutableSnapshot {
                isDragging = false
                draggedHud = null
                draggedGroup = emptySet()
                dragStarts = emptyMap()
                snapGuides = SnapGuides.NONE
            }
            if (wasDragging) wasDraggedHud?.onEditorDragEnd()
            if (!wasDragging || wasDraggedHud == null) {
                // skip releases of presses that were already handled
                if (event.changes.any { it.isConsumed } || pressWasSecondary) return@safePointerEvent
                val pos = event.changes.firstOrNull()?.position ?: return@safePointerEvent
                if (inChrome(pos.x, pos.y)) return@safePointerEvent
                val mcToScreen = Platform.screen().mcToScreenScale()
                if (hoveredHud?.let { hitTestHudActionBar(it, pos.x, pos.y, mcToScreen, actionIconPx, actionBarGapPx) } == true) {
                    return@safePointerEvent
                }
                val hit = pickHudAt(pos.x, pos.y, primaryHud())
                if (hit != null) {
                    if (hit !in selectedHuds) UiSounds.play(UiSoundEvent.HUD_SELECT)
                    Snapshot.withMutableSnapshot {
                        selectedHuds = setOf(hit)
                        libraryVisible = false
                    }
                }
            } else {
                UiSounds.play(UiSoundEvent.HUD_DRAG_END)
                Snapshot.withMutableSnapshot {
                    if (wasDraggedGroup.isNotEmpty()) selectedHuds = wasDraggedGroup
                    libraryVisible = false
                }
            }
        }

    LaunchedEffect(selectedHuds) {
        if (selectedHuds.isNotEmpty()) keyFocusRequester.requestFocus()
    }

    val chromeAlpha by animateFloatAsState(
        targetValue = if (isDragging) OneConfigConfig.hudDragUiOpacity.coerceIn(0f, 1f) else 1f,
        animationSpec = tween(150),
        label = "hudDragChromeAlpha"
    )

    val marqueeAnimEnabled = OneConfigConfig.marqueeSelectionAnim
    val marqueeAnimDuration = OneConfigConfig.marqueeSelectionAnimDuration.toInt().coerceAtLeast(1)
    val marqueeAlpha by animateFloatAsState(
        targetValue = if (marqueeActive) 1f else 0f,
        animationSpec = tween(if (marqueeAnimEnabled) marqueeAnimDuration else 0),
        label = "marqueeAlpha"
    )

    var lastMarqueeStart by remember { mutableStateOf<Offset?>(null) }
    var lastMarqueeCurrent by remember { mutableStateOf<Offset?>(null) }

    if (marqueeStart != null && marqueeCurrent != null) {
        lastMarqueeStart = marqueeStart
        lastMarqueeCurrent = marqueeCurrent
    }

    val theme = LocalTheme.current
    val selectionCornerRadius = theme.buttonShape.cornerRadiusOrNull() ?: Radii.SM

    // The library and the return chip stay on screen while a HUD is selected, dimmed so the
    // settings panel reads as the focus. They only disappear once the HUD is actually moving.
    val sideChromeAlpha by animateFloatAsState(
        targetValue = if (selectedHuds.isNotEmpty()) SIDE_CHROME_DIM_ALPHA else 1f,
        animationSpec = tween(150),
        label = "sideChromeAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { rootSize = it }
            .focusRequester(keyFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                if (keyEvent.key == Key.Escape && anchorPickSources.isNotEmpty()) {
                    Snapshot.withMutableSnapshot {
                        anchorPickSources = emptyList()
                        hoveredAnchor = null
                    }
                    return@onKeyEvent true
                }
                if (keyEvent.key == Key.Escape && (selectedHuds.isNotEmpty() || panelHud != null)) {
                    Snapshot.withMutableSnapshot {
                        selectedHuds = emptySet()
                        panelHud = null
                    }
                    return@onKeyEvent true
                }
                if (selectedHuds.isEmpty()) return@onKeyEvent false
                val key = keyEvent.key
                val hud = primaryHud() ?: return@onKeyEvent false
                if (selectedHuds.any { it.locked }) {
                    notifyHudLocked()
                    return@onKeyEvent true
                }
                val step = if (keyEvent.isShiftPressed) 10f else 1f
                val (dx, dy) = when (key) {
                    Key.DirectionLeft -> -step to 0f
                    Key.DirectionRight -> step to 0f
                    Key.DirectionUp -> 0f to -step
                    Key.DirectionDown -> 0f to step
                    else -> return@onKeyEvent false
                }
                if (hud.locked) {
                    notifyHudLocked()
                    return@onKeyEvent true
                }
                Snapshot.withMutableSnapshot {
                    for (hud in selectedHuds) {
                        hud.setAbsolutePosition(hud.x + dx, hud.y + dy)
                    }
                }
                true
            }
            .then(pointerModifier)
    ) {
        if (onReturnToOneConfig != null) {
            val theme = LocalTheme.current
            val returnKeyName = OneConfigConfig.oneConfigKeybind.displayName()
            val returnInteraction = remember { MutableInteractionSource() }
            val returnHovered by returnInteraction.collectIsHoveredAsState()
            val returnBackground by animateColorAsState(
                if (returnHovered) Accent else Color.Black.copy(alpha = 0.55f),
                animationSpec = tween(120),
                label = "returnChipBackground"
            )
            AnimatedVisibility(
                visible = !isDragging,
                modifier = Modifier.align(Alignment.CenterStart),
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = slideOutHorizontally(targetOffsetX = { -it }),
            ) {
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .graphicsLayer { alpha = sideChromeAlpha }
                    .clip(theme.buttonShape)
                    .background(returnBackground, theme.buttonShape)
                    .hoverable(returnInteraction)
                    .safePointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
                        if (event.changes.none { it.isConsumed }) {
                            event.changes.forEach { it.consume() }
                            UiSounds.play(UiSoundEvent.CLICK)
                            onReturnToOneConfig()
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    "left-arrow",
                    color = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Return to\nOneConfig",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "($returnKeyName)",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val mcToScreen = Platform.screen().mcToScreenScale()
                    if (!HudManager.inWorld) {
                        drawIntoCanvas { canvas -> drawHudContents(canvas.skiaCanvas, mcToScreen) }
                    }
                    if (isDragging) {
                        snapGuides.vertical?.let { lineX ->
                            val sx = lineX * mcToScreen
                            drawLine(snapGuideColor, Offset(sx, 0f), Offset(sx, size.height), strokeWidth = 1f)
                        }
                        snapGuides.horizontal?.let { lineY ->
                            val sy = lineY * mcToScreen
                            drawLine(snapGuideColor, Offset(0f, sy), Offset(size.width, sy), strokeWidth = 1f)
                        }
                    }
                    for (hud in HudManager.activeInstances) {
                        val bounds = hudBounds(hud) ?: continue
                        val resizable = hud.resizeAxes != HudResize.None
                        val sx = bounds.x * mcToScreen
                        val sy = bounds.y * mcToScreen
                        val sw = bounds.width * mcToScreen
                        val sh = bounds.height * mcToScreen
                        val isSelected = hud in selectedHuds
                        val isPrimary = hud === primaryHud()
                        val isHovered = hud === hoveredHud
                        val isBeingDragged = hud === draggedHud && isDragging
                        val singleSelection = selectedHuds.size == 1

                        if (hud.locked) {
                            // no selection handles: a locked HUD can be neither moved nor resized
                            if (isSelected || isHovered) {
                                drawRect(
                                    color = lockedHudBoxColor,
                                    topLeft = Offset(sx, sy),
                                    size = Size(sw, sh),
                                    style = Stroke(width = 1f, pathEffect = lockedHudDashEffect)
                                )
                            }
                            continue
                        }

                        if (isBeingDragged) {
                            drawRect(
                                color = selectionBlue.copy(alpha = 0.10f),
                                topLeft = Offset(sx, sy),
                                size = Size(sw, sh),
                            )
                            drawSelectedHudBounds(bounds, mcToScreen, resizable && singleSelection, showBadge = singleSelection)
                        } else if (isSelected) {
                            drawSelectedHudBounds(
                                bounds,
                                mcToScreen,
                                showHandles = isPrimary && singleSelection,
                                showBadge = singleSelection,
                            )
                        } else if (hud.hidden) {
                            if (hud is LegacyHud) {
                                drawRect(
                                    color = hiddenLegacyScrimColor,
                                    topLeft = Offset(sx, sy),
                                    size = Size(sw, sh),
                                )
                            }
                            drawRect(
                                color = if (isHovered) hoveredHudBoxColor else hiddenHudBoxColor,
                                topLeft = Offset(sx, sy),
                                size = Size(sw, sh),
                                style = Stroke(width = 1f, pathEffect = hiddenHudDashEffect)
                            )
                        } else {
                            drawRect(
                                color = if (isHovered) hoveredHudBoxColor else idleHudBoxColor,
                                topLeft = Offset(sx, sy),
                                size = Size(sw, sh),
                                style = Stroke(width = 1f)
                            )
                        }
                    }
                    // Hovering an anchored HUD traces the link back to the point it hangs off.
                    hoveredHud?.let { hovered ->
                        val parent = hovered.anchorParent
                        val from = if (parent == null) null else anchorPointOf(hovered, hovered.effectiveGrowthAnchor)
                        val to = if (parent == null) null else anchorPointOf(parent, hovered.anchorPoint)
                        if (from != null && to != null) {
                            drawLine(
                                color = anchorLineColor,
                                start = from * mcToScreen,
                                end = to * mcToScreen,
                                strokeWidth = 1.5f,
                                pathEffect = anchorLineDashEffect,
                            )
                            drawCircle(anchorLineColor, radius = ANCHOR_DOT_RADIUS_PX, center = to * mcToScreen)
                        }
                    }

                    if (anchorPickSources.isNotEmpty()) {
                        drawRect(color = anchorPickScrimColor, size = size)
                        val hotAnchor = hoveredAnchor
                        for (target in anchorTargetsFor(anchorPickSources)) {
                            val b = hudBounds(target) ?: continue
                            drawRect(
                                color = anchorLineColor.copy(alpha = 0.5f),
                                topLeft = Offset(b.x * mcToScreen, b.y * mcToScreen),
                                size = Size(b.width * mcToScreen, b.height * mcToScreen),
                                style = Stroke(width = 1f),
                            )
                            for (point in ANCHOR_POINTS) {
                                val p = anchorPointOf(target, point) ?: continue
                                val center = p * mcToScreen
                                val hot = hotAnchor?.first === target && hotAnchor.second == point
                                val radius = if (hot) ANCHOR_DOT_RADIUS_PX + 2f else ANCHOR_DOT_RADIUS_PX
                                drawCircle(Color.White, radius = radius, center = center)
                                drawCircle(
                                    color = if (hot) Accent else anchorLineColor,
                                    radius = radius - 2f,
                                    center = center,
                                )
                            }
                        }
                    }

                    val renderStart = if (marqueeActive) marqueeStart else lastMarqueeStart
                    val renderCurrent = if (marqueeActive) marqueeCurrent else lastMarqueeCurrent
                    if (marqueeAlpha > 0f && renderStart != null && renderCurrent != null) {
                        val left = minOf(renderStart.x, renderCurrent.x)
                        val top = minOf(renderStart.y, renderCurrent.y)
                        val w = abs(renderCurrent.x - renderStart.x)
                        val h = abs(renderCurrent.y - renderStart.y)
                        if (w > 0f && h > 0f) {
                            val cornerRadiusPx = selectionCornerRadius.toPx()
                            val cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                            drawRoundRect(
                                color = marqueeFillColor.copy(alpha = marqueeFillColor.alpha * marqueeAlpha),
                                topLeft = Offset(left, top),
                                size = Size(w, h),
                                cornerRadius = cornerRadius,
                            )
                            drawRoundRect(
                                color = selectionBlue.copy(alpha = selectionBlue.alpha * marqueeAlpha),
                                topLeft = Offset(left, top),
                                size = Size(w, h),
                                cornerRadius = cornerRadius,
                                style = Stroke(width = 1.dp.toPx()),
                            )
                        }
                    }
                }
        )

        // Hover-only: the bar would otherwise clutter the canvas for every locked HUD at once, and
        // linger over a selected HUD the user has moved on from.
        val actionBarTarget = hoveredHud
        if (actionBarTarget != null) {
            key(actionBarTarget) {
                HudActionBar(
                    hud = actionBarTarget,
                    mcToScreen = Platform.screen().mcToScreenScale(),
                    densityFloat = densityFloat,
                    actionIconPx = actionIconPx,
                    actionBarGapPx = actionBarGapPx,
                    chromeAlpha = chromeAlpha,
                ) {
                    Snapshot.withMutableSnapshot {
                        selectedHuds = setOf(actionBarTarget)
                        panelHud = actionBarTarget
                    }
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.CenterEnd)
                .offset { IntOffset(panelOffset.x.roundToInt(), panelOffset.y.roundToInt()) }
                .graphicsLayer { alpha = chromeAlpha }
        ) {
        val panelContentHud = remember { mutableStateOf<Hud?>(null) }
        if (panelHud != null) panelContentHud.value = panelHud
        AnimatedVisibility(
            visible = panelHud != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        ) {
            DisposableEffect(Unit) { onDispose { chromeRects.remove(CHROME_SETTINGS_PANEL) } }
            Box(
                modifier = Modifier
                    .chromeRegion(CHROME_SETTINGS_PANEL)
                    .safePointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
                        event.changes.forEach { if (!it.isConsumed) it.consume() }
                    }
                    .safePointerEvent(PointerEventType.Move, PointerEventPass.Final) { event ->
                        if (event.changes.any { it.pressed }) return@safePointerEvent
                        event.changes.forEach { if (!it.isConsumed) it.consume() }
                    }
                    .safePointerEvent(PointerEventType.Release, PointerEventPass.Final) { event ->
                        event.changes.forEach { if (!it.isConsumed) it.consume() }
                    }
            ) {
                DesignStudioPanel(
                    selectedHud = panelContentHud.value,
                    activeCategory = activeCategory,
                    onCategoryChange = { activeCategory = it },
                    onBack = {
                        Snapshot.withMutableSnapshot {
                            panelHud = null
                            selectedHuds = emptySet()
                        }
                    },
                    onDragPanel = { movePanel(it) },
                )
            }
        }
        }

        AnimatedVisibility(
            visible = libraryChromeVisible,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
        ) {
            DisposableEffect(Unit) { onDispose { chromeRects.remove(CHROME_LIBRARY) } }
            Row(
                modifier = Modifier
                    .chromeRegion(CHROME_LIBRARY)
                    .graphicsLayer { alpha = sideChromeAlpha }
                    .safePointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
                        event.changes.forEach { if (!it.isConsumed) it.consume() }
                    }
                    .safePointerEvent(PointerEventType.Move, PointerEventPass.Final) { event ->
                        if (event.changes.any { it.pressed }) return@safePointerEvent
                        event.changes.forEach { if (!it.isConsumed) it.consume() }
                    }
                    .safePointerEvent(PointerEventType.Release, PointerEventPass.Final) { event ->
                        event.changes.forEach { if (!it.isConsumed) it.consume() }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AnimatedVisibility(
                    visible = libraryVisible,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                ) {
                    HudLibraryPanel(
                        searchText = searchText,
                        onSearchChange = { searchText = it },
                        sections = librarySections,
                        listState = libraryListState,
                        onDragStart = { hud, sx, sy, hudLocalOffX, hudLocalOffY ->
                            try {
                                // a single-instance provider *is* its instance, so one that is real
                                // but no longer active is re-used rather than made a second time
                                val instance = if (hud.isReal) hud else hud.make()
                                HudManager.markProviderKnown(instance)
                                val s = Platform.screen().screenToMcScale()
                                val effScale = instance.effectiveScale
                                val offX = hudLocalOffX * effScale
                                val offY = hudLocalOffY * effScale
                                val cursorX = if (lastPointerPos[0] > 0f) lastPointerPos[0] else sx
                                val cursorY = if (lastPointerPos[1] > 0f) lastPointerPos[1] else sy
                                val initX = cursorX * s - offX
                                val initY = cursorY * s - offY
                                instance.setAbsolutePosition(initX, initY)
                                if (instance !in HudManager.activeInstances) {
                                    HudManager.activeInstances.add(instance)
                                    instance.setup()
                                    instance.captureStaticSizeDefaults()
                                    instance.capturePositionDefaults()
                                }
                                UiSounds.play(UiSoundEvent.HUD_DRAG_START)
                                instance.onEditorDragStart()
                                Snapshot.withMutableSnapshot {
                                    dragOffsetX = offX
                                    dragOffsetY = offY
                                    isDragging = true
                                    draggedHud = instance
                                    draggedGroup = setOf(instance)
                                    dragStarts = mapOf(instance to (initX to initY))
                                    selectedHuds = setOf(instance)
                                    hoveredHud = instance
                                    libraryVisible = false
                                }
                            } catch (_: Throwable) {}
                        },
                        onCardClick = { hud ->
                            try {
                                val instance = hud.make()
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
                                val centerX = (screenW - w) / 2f
                                val centerY = (screenH - h) / 2f
                                instance.setAbsolutePosition(centerX, centerY)
                                if (instance !in HudManager.activeInstances) {
                                    HudManager.activeInstances.add(instance)
                                    instance.setup()
                                    instance.captureStaticSizeDefaults()
                                    instance.capturePositionDefaults()
                                }
                                UiSounds.play(UiSoundEvent.HUD_SELECT)
                                Snapshot.withMutableSnapshot {
                                    selectedHuds = setOf(instance)
                                    panelHud = instance
                                    libraryVisible = false
                                }
                            } catch (e: Throwable) {
                                LOGGER.error("Failed to add HUD ${hud.title} from the library", e)
                            }
                        },
                    )
                }
                Column(
                    modifier = Modifier.padding(end = 16.dp, top = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val theme = LocalTheme.current
                    var masterHudEnabled by remember { mutableStateOf(HudManager.masterHudEnabled) }
                    fun toggleMasterHud() {
                        masterHudEnabled = !masterHudEnabled
                        Snapshot.withMutableSnapshot {
                            HudManager.masterHudEnabled = masterHudEnabled
                            OneConfigConfig.masterHudEnabled = masterHudEnabled
                        }
                        OneConfigConfig.INSTANCE.save()
                    }
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(theme.popupBackground, theme.backgroundShape)
                            .border(1.dp, theme.borderColor, theme.backgroundShape)
                            .safePointerEvent(PointerEventType.Press, PointerEventPass.Main) { event ->
                                if (event.changes.none { it.isConsumed }) {
                                    event.changes.forEach { it.consume() }
                                    UiSounds.play(UiSoundEvent.CLICK)
                                    toggleMasterHud()
                                }
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            if (masterHudEnabled) "eye" else "eye-off",
                            modifier = Modifier.size(32.dp),
                            foreground = if (masterHudEnabled) theme.textColor else theme.textColor.copy(alpha = 0.5f),
                            hoveredForeground = theme.textColor
                        ) {
                            toggleMasterHud()
                        }
                    }
                    ModIconColumn(
                        modIds = modIds,
                        activeModId = activeLibraryMod,
                        libraryVisible = libraryVisible,
                    ) { modId ->
                        Snapshot.withMutableSnapshot {
                            if (activeLibraryMod == modId && libraryVisible) {
                                libraryVisible = false
                                libraryModIntent = null
                            } else {
                                if (librarySectionIds.none { it == modId }) searchText = ""
                                libraryVisible = true
                                libraryModIntent = modId
                                pendingLibraryScroll = modId
                            }
                        }
                    }
                }
            }
        }

        val contextMenuTargets = hudContextMenuTarget?.let { hud ->
            if (hud in selectedHuds) selectedHuds.toList() else listOf(hud)
        } ?: emptyList()

        HudCanvasResetMenu(
            hud = hudContextMenuTarget,
            expanded = hudContextMenuTarget != null,
            offset = hudContextMenuOffset,
            onDismiss = { hudContextMenuTarget = null },
            targets = contextMenuTargets,
            settingsEnabled = contextMenuTargets.size <= 1,
            onSettings = { hud ->
                Snapshot.withMutableSnapshot {
                    hudContextMenuTarget = null
                    selectedHuds = setOf(hud)
                    panelHud = hud
                }
            },
            onCopy = { _ ->
                Snapshot.withMutableSnapshot { hudClipboard = contextMenuTargets }
                UiSounds.play(UiSoundEvent.CLICK)
            },
            onCut = { _ ->
                Snapshot.withMutableSnapshot {
                    hudClipboard = contextMenuTargets
                    hudContextMenuTarget = null
                }
                deleteHuds(contextMenuTargets)
            },
            onPaste = { _ ->
                val s = Platform.screen().screenToMcScale()
                val pasted = duplicateHudGroup(
                    hudClipboard,
                    Offset(hudContextMenuOffset.x * s, hudContextMenuOffset.y * s),
                )
                if (pasted.isNotEmpty()) {
                    Snapshot.withMutableSnapshot { selectedHuds = pasted.toSet() }
                }
                UiSounds.play(UiSoundEvent.CLICK)
            },
            pasteEnabled = hudClipboard.any(::canDuplicateHud),
            onDuplicate = { _ ->
                val pasted = duplicateHudGroup(contextMenuTargets)
                if (pasted.isNotEmpty()) {
                    Snapshot.withMutableSnapshot { selectedHuds = pasted.toSet() }
                }
                UiSounds.play(UiSoundEvent.CLICK)
            },
            duplicateEnabled = contextMenuTargets.any(::canDuplicateHud),
            onAnchor = { _ ->
                Snapshot.withMutableSnapshot {
                    anchorPickSources = contextMenuTargets
                    hoveredAnchor = null
                    hoveredHud = null
                    libraryVisible = false
                }
            },
            anchorEnabled = contextMenuTargets.isNotEmpty() &&
                anchorTargetsFor(contextMenuTargets).isNotEmpty(),
            onDelete = { _ -> deleteHuds(contextMenuTargets) },
        )

        HudCanvasPasteMenu(
            expanded = pasteMenuOffset != null,
            offset = pasteMenuOffset ?: IntOffset.Zero,
            pasteEnabled = hudClipboard.any(::canDuplicateHud),
            onDismiss = { pasteMenuOffset = null },
            onPaste = {
                val off = pasteMenuOffset ?: return@HudCanvasPasteMenu
                val s = Platform.screen().screenToMcScale()
                val pasted = duplicateHudGroup(hudClipboard, Offset(off.x * s, off.y * s))
                if (pasted.isNotEmpty()) {
                    Snapshot.withMutableSnapshot { selectedHuds = pasted.toSet() }
                }
                UiSounds.play(UiSoundEvent.CLICK)
            },
            onSelectAll = {
                Snapshot.withMutableSnapshot {
                    selectedHuds = HudManager.activeInstances.filter { !it.locked }.toSet()
                }
                UiSounds.play(UiSoundEvent.CLICK)
            },
        )
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
        fresh.tree.overwrite(copied.tree, true, false, null)
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
    }
    HudManager.markProviderKnown(fresh)
    fresh.setup()
    fresh.captureStaticSizeDefaults()
    fresh.capturePositionDefaults()
    HudManager.invalidate()
    return fresh
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HudDragLayer(modifier: Modifier = Modifier) {
    HudEditorViewport.observe()
    var hoveredHud by remember { mutableStateOf<Hud?>(null) }
    var draggedHud by remember { mutableStateOf<Hud?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var snapGuides by remember { mutableStateOf(SnapGuides.NONE) }
    var lockedPressHud by remember { mutableStateOf<Hud?>(null) }
    var lockedPressOrigin by remember { mutableStateOf(Offset.Zero) }

    val densityObj = LocalDensity.current
    val densityFloat = densityObj.density
    val actionIconPx = with(densityObj) { 24.dp.toPx() }
    val actionBarGapPx = with(densityObj) { 8.dp.toPx() }

    fun endDrag() {
        val dropped = if (isDragging) draggedHud else null
        Snapshot.withMutableSnapshot {
            isDragging = false
            draggedHud = null
            snapGuides = SnapGuides.NONE
            ShellState.hudDragging = false
        }
        dropped?.onEditorDragEnd()
    }

    fun overShell(px: Float, py: Float) = ShellState.shellBounds?.contains(Offset(px, py)) == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .safePointerEvent(PointerEventType.Press) { event ->
                if (event.changes.any { it.isConsumed }) return@safePointerEvent
                if (event.buttons.isSecondaryPressed) return@safePointerEvent
                val pos = event.changes.firstOrNull()?.position ?: return@safePointerEvent
                if (overShell(pos.x, pos.y)) return@safePointerEvent
                if (!isDragging) {
                    val actionTarget = hoveredHud
                    if (actionTarget != null && hitTestHudActionBar(
                            actionTarget, pos.x, pos.y,
                            Platform.screen().mcToScreenScale(), actionIconPx, actionBarGapPx,
                        )
                    ) return@safePointerEvent
                }
                val s = Platform.screen().screenToMcScale()
                val hit = orderedInstances().lastOrNull { !it.locked && hitTestHud(it, pos.x, pos.y) }
                if (hit == null) {
                    orderedInstances().lastOrNull { it.locked && hitTestHud(it, pos.x, pos.y) }?.let {
                        Snapshot.withMutableSnapshot {
                            lockedPressHud = it
                            lockedPressOrigin = pos
                        }
                    }
                    return@safePointerEvent
                }
                UiSounds.play(UiSoundEvent.HUD_DRAG_START)
                hit.onEditorDragStart()
                Snapshot.withMutableSnapshot {
                    dragOffsetX = pos.x * s - hit.x
                    dragOffsetY = pos.y * s - hit.y
                    isDragging = true
                    draggedHud = hit
                    hoveredHud = hit
                    ShellState.hudDragging = true
                }
            }
            .safePointerEvent(PointerEventType.Move) { event ->
                val pos = event.changes.firstOrNull()?.position ?: return@safePointerEvent
                if (isDragging) {
                    if (event.changes.none { it.pressed }) {
                        endDrag()
                        return@safePointerEvent
                    }
                    val s = Platform.screen().screenToMcScale()
                    val hit = draggedHud ?: return@safePointerEvent
                    val rawX = pos.x * s - dragOffsetX
                    val rawY = pos.y * s - dragOffsetY
                    val bounds = hudBounds(hit)
                    if (bounds != null && !event.keyboardModifiers.isAltPressed) {
                        val threshold = SNAP_DISTANCE_PX * s
                        val snapX = snapAxis(rawX, bounds.width, verticalSnapLines(hit), threshold)
                        val snapY = snapAxis(rawY, bounds.height, horizontalSnapLines(hit), threshold)
                        Snapshot.withMutableSnapshot {
                            hit.setAbsolutePosition(snapX.position, snapY.position)
                            snapGuides = SnapGuides(snapX.line, snapY.line)
                        }
                    } else {
                        Snapshot.withMutableSnapshot {
                            hit.setAbsolutePosition(rawX, rawY)
                            snapGuides = SnapGuides.NONE
                        }
                    }
                } else {
                    if (lockedPressHud != null) {
                        if (event.changes.none { it.pressed }) {
                            lockedPressHud = null
                        } else if ((pos - lockedPressOrigin).getDistance() > LOCKED_DRAG_SLOP_PX) {
                            lockedPressHud = null
                            notifyHudLocked()
                        }
                    }
                    // locked HUDs are hoverable so their action bar (and its lock toggle) is
                    // reachable, but they never steal the hover from an unlocked HUD
                    val hit = if (overShell(pos.x, pos.y)) null else topHudAt(pos.x, pos.y)
                    val hovered = hoveredHud
                    val mcToScreen = Platform.screen().mcToScreenScale()
                    val overActionBar = hovered?.let {
                        hitTestHudActionBarZone(it, pos.x, pos.y, mcToScreen, actionIconPx, actionBarGapPx)
                    } == true
                    val inExpandedZone = hovered?.let {
                        hitTestHudWithActionBar(it, pos.x, pos.y, mcToScreen, actionIconPx, actionBarGapPx)
                    } == true
                    val next = when {
                        overShell(pos.x, pos.y) -> null
                        overActionBar -> hovered
                        hit != null -> hit
                        inExpandedZone -> hovered
                        else -> null
                    }
                    if (next !== hoveredHud) hoveredHud = next
                }
            }
            .safePointerEvent(PointerEventType.Release) {
                lockedPressHud = null
                if (isDragging) UiSounds.play(UiSoundEvent.HUD_DRAG_END)
                endDrag()
            }
            .drawWithContent {
                // HUD contents and their boxes go under the children: the action bar is a child of
                // this Box, so drawing it first would put it behind every HUD painted here.
                val mcToScreen = Platform.screen().mcToScreenScale()
                drawIntoCanvas { canvas -> drawHudContents(canvas.skiaCanvas, mcToScreen) }
                if (isDragging) {
                    snapGuides.vertical?.let { lineX ->
                        val sx = lineX * mcToScreen
                        drawLine(snapGuideColor, Offset(sx, 0f), Offset(sx, size.height), strokeWidth = 1f)
                    }
                    snapGuides.horizontal?.let { lineY ->
                        val sy = lineY * mcToScreen
                        drawLine(snapGuideColor, Offset(0f, sy), Offset(size.width, sy), strokeWidth = 1f)
                    }
                }
                for (hud in HudManager.activeInstances) {
                    val bounds = hudBounds(hud) ?: continue
                    val sx = bounds.x * mcToScreen
                    val sy = bounds.y * mcToScreen
                    val sw = bounds.width * mcToScreen
                    val sh = bounds.height * mcToScreen
                    val isBeingDragged = hud === draggedHud && isDragging
                    val isHovered = hud === hoveredHud
                    if (hud.locked) {
                        if (isHovered) {
                            drawRect(
                                color = lockedHudBoxColor,
                                topLeft = Offset(sx, sy),
                                size = Size(sw, sh),
                                style = Stroke(width = 1f, pathEffect = lockedHudDashEffect)
                            )
                        }
                        continue
                    }
                    if (isBeingDragged) {
                        drawRect(
                            color = selectionBlue.copy(alpha = 0.10f),
                            topLeft = Offset(sx, sy),
                            size = Size(sw, sh),
                        )
                        drawSelectedHudBounds(bounds, mcToScreen, showHandles = false)
                    } else if (hud.hidden) {
                        if (hud is LegacyHud) {
                            drawRect(
                                color = hiddenLegacyScrimColor,
                                topLeft = Offset(sx, sy),
                                size = Size(sw, sh),
                            )
                        }
                        drawRect(
                            color = if (isHovered) hoveredHudBoxColor else hiddenHudBoxColor,
                            topLeft = Offset(sx, sy),
                            size = Size(sw, sh),
                            style = Stroke(width = 1f, pathEffect = hiddenHudDashEffect)
                        )
                    } else {
                        drawRect(
                            color = if (isHovered) hoveredHudBoxColor else idleHudBoxColor,
                            topLeft = Offset(sx, sy),
                            size = Size(sw, sh),
                            style = Stroke(width = 1f)
                        )
                    }
                }
                drawContent()
            }
    ) {
        val actionBarTarget = hoveredHud
        if (actionBarTarget != null && !isDragging) {
            val mcToScreen = Platform.screen().mcToScreenScale()
            val bounds = hudBounds(actionBarTarget)
            val layout = hudActionBarLayout(actionBarTarget, mcToScreen, actionIconPx, actionBarGapPx)
            // HudDragLayer is composed outside OneConfigInterface's Theme scope, so provide one for
            // the icons (IconButton reads LocalTheme). Only entered while a HUD is hovered.
            if (bounds != null && bounds.width > 0f && bounds.height > 0f && layout != null) Theme {
                HudActionBar(
                    hud = actionBarTarget,
                    mcToScreen = mcToScreen,
                    densityFloat = densityFloat,
                    actionIconPx = actionIconPx,
                    actionBarGapPx = actionBarGapPx,
                    chromeAlpha = 1f,
                ) {
                    HudManager.pendingSelection = actionBarTarget
                    HudManager.openEditor()
                }
            }
        }
    }
}


private fun modNameFor(configId: String): String? {
    val modId = configId.removeSuffix(".json").substringBefore('/')
    ModInfo.loadedMods.firstOrNull { it.id == modId }?.name?.let { return it }
    val config = ConfigRegistry.findById(configId)
        ?: ConfigRegistry.findById("$configId.json")
        ?: ConfigRegistry.configs.firstOrNull { it.id.removeSuffix(".json") == modId }
    return config?.title?.asRenderText()
}

@Composable
private fun DesignStudioPanel(
    selectedHud: Hud?,
    activeCategory: StudioCategory,
    onCategoryChange: (StudioCategory) -> Unit,
    onBack: () -> Unit,
    onDragPanel: (Offset) -> Unit,
) {
    val theme = LocalTheme.current
    val isLegacy = selectedHud is LegacyHud
    val categories = if (isLegacy) listOf(StudioCategory.Settings) else StudioCategory.entries
    val subtitle = remember(selectedHud) {
        selectedHud?.let { hud ->
            val name = localizedLabel(hud.title) ?: return@let null
            val modName = hud.configId?.let(::modNameFor)
            if (modName != null) "$name / $modName" else name
        }
    }
    LaunchedEffect(isLegacy) {
        if (isLegacy && activeCategory != StudioCategory.Settings) onCategoryChange(StudioCategory.Settings)
    }
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(500.dp)
            .padding(16.dp)
            .background(theme.popupBackground, theme.backgroundShape)
            .border(1.dp, theme.borderColor, theme.backgroundShape),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
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
                    IconButton("close") { onBack() }
                    SearchBar()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDragPanel(dragAmount)
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
                            Text(it, color = theme.textColor.copy(0.5f), fontSize = 14.sp)
                        }
                    }
                }
                if (categories.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach {
                            Chip(it.title, it == activeCategory, it.icon) { onCategoryChange(it) }
                        }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = activeCategory,
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (slideInHorizontally(tween(250)) { direction * it / 3 } + fadeIn(tween(250)))
                            .togetherWith(slideOutHorizontally(tween(250)) { -direction * it / 3 } + fadeOut(tween(250)))
                    },
                    modifier = Modifier.fillMaxSize()
                ) { category ->
                    when (category) {
                        StudioCategory.Designer -> {
                            val panelScrollState = rememberScrollState()
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                        .verticalScroll(panelScrollState)
                                        .padding(end = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Designer(selectedHud)
                                }
                                VerticalScrollbar(
                                    adapter = rememberScrollbarAdapter(panelScrollState),
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                                )
                            }
                        }
                        StudioCategory.Settings -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Settings(selectedHud, onDeleted = onBack)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One mod's worth of addable HUDs, as shown in the continuous library list. */
private class HudLibrarySection(val modId: String?, val title: String, val huds: List<Hud>)

private fun libraryIconFor(modId: String?): String =
    modId?.let { HudManager.iconFor(it) ?: ConfigRegistry.findById(it)?.icon } ?: "qol"

@Composable
private fun HudLibraryPanel(
    searchText: String,
    onSearchChange: (String) -> Unit,
    sections: List<HudLibrarySection>,
    listState: LazyListState,
    onDragStart: (Hud, Float, Float, Float, Float) -> Unit = { _, _, _, _, _ -> },
    onCardClick: (Hud) -> Unit = {},
) {
    val theme = LocalTheme.current
    Column(
        modifier = Modifier
            .size(401.dp, 481.dp)
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
            .background(theme.popupBackground, theme.backgroundShape)
            .border(1.dp, theme.borderColor, theme.backgroundShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("HUDs", color = theme.textColor, fontSize = 18.sp)
            LibrarySearchBar(searchText, onSearchChange)
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val maxCardWidth = maxWidth - 16.dp
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (sections.isEmpty()) {
                        item {
                            Text(
                                "No HUDs found",
                                color = theme.textColorSecondary,
                                fontSize = 14.sp,
                            )
                        }
                    }
                    items(sections, key = { it.modId ?: "" }) { section ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    libraryIconFor(section.modId),
                                    modifier = Modifier.size(16.dp),
                                    color = theme.textColorSecondary,
                                )
                                Text(
                                    section.title.uppercase(),
                                    color = theme.textColorSecondary,
                                    fontSize = 12.sp,
                                )
                            }
                            FlexibleLayout(
                                horizontalSpacing = 10.dp,
                                verticalSpacing = 10.dp
                            ) {
                                section.huds.forEach { hud ->
                                    HudPreviewCard(hud, maxCardWidth, onDragStart, onCardClick)
                                }
                            }
                        }
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun ModIconColumn(
    modIds: List<String>,
    activeModId: String?,
    libraryVisible: Boolean,
    onModClick: (String) -> Unit,
) {
    val theme = LocalTheme.current
    Column(
        modifier = Modifier
            .background(theme.popupBackground, theme.backgroundShape)
            .border(1.dp, theme.borderColor, theme.backgroundShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        modIds.forEach { modId ->
            ModFilterIcon(
                iconName = libraryIconFor(modId),
                selected = activeModId == modId && libraryVisible,
            ) { onModClick(modId) }
        }
    }
}

private const val PREVIEW_SCALE = 2f
private val PREVIEW_CARD_PADDING = 12.dp
private val PREVIEW_MAX_CARD_HEIGHT = 160.dp
private const val LEGACY_PREVIEW_FALLBACK_SIZE = 48f

private fun previewScaleFor(naturalW: Float, naturalH: Float, maxCardWidth: Dp, density: Float): Float {
    if (naturalW <= 0f || naturalH <= 0f) return PREVIEW_SCALE
    val availableW = (maxCardWidth - PREVIEW_CARD_PADDING * 2).value.coerceAtLeast(0f)
    val availableH = (PREVIEW_MAX_CARD_HEIGHT - PREVIEW_CARD_PADDING * 2).value
    return minOf(PREVIEW_SCALE, availableW * density / naturalW, availableH * density / naturalH)
        .coerceAtLeast(0.05f)
}

@Composable
private fun HudPreviewCard(
    hud: Hud,
    maxCardWidth: Dp,
    onDragStart: (Hud, Float, Float, Float, Float) -> Unit,
    onCardClick: (Hud) -> Unit,
) {
    // Legacy HUDs have no Compose content tree, so render a sized, titled placeholder tile
    // instead of an empty (zero-size, invisible) preview. The placed instance still renders
    // for real through LegacyHudRenderer once dragged onto the canvas.
    if (hud is LegacyHud) {
        LegacyHudPreviewCard(hud, maxCardWidth, onDragStart, onCardClick)
    } else {
        ComposeHudPreviewCard(hud, maxCardWidth, onDragStart, onCardClick)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LegacyHudPreviewCard(
    hud: Hud,
    maxCardWidth: Dp,
    onDragStart: (Hud, Float, Float, Float, Float) -> Unit,
    onCardClick: (Hud) -> Unit,
) {
    // Legacy HUDs that report no minimum size (size only known once they render, or never set)
    // would otherwise be dropped from the library entirely, so fall back to a square tile.
    val (minW, minH) = hud.minimumSize()
    val naturalW = if (minW > 0f) minW else LEGACY_PREVIEW_FALLBACK_SIZE
    val naturalH = if (minH > 0f) minH else LEGACY_PREVIEW_FALLBACK_SIZE
    val density = LocalDensity.current.density
    val theme = LocalTheme.current

    var isHovered by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        if (isHovered) Accent else theme.popupBackground,
        animationSpec = tween(150)
    )
    var pressPos by remember { mutableStateOf<Offset?>(null) }
    var dragStarted by remember { mutableStateOf(false) }

    val cardPadding = PREVIEW_CARD_PADDING
    val minTile = 72.dp
    val previewScale = previewScaleFor(naturalW, naturalH, maxCardWidth, density)
    val w = ((naturalW * previewScale / density).dp + cardPadding * 2)
        .coerceAtLeast(minTile)
        .coerceAtMost(maxCardWidth)
    val h = ((naturalH * previewScale / density).dp + cardPadding * 2).coerceAtLeast(minTile)
    Box(
        modifier = Modifier
            .size(w, h)
            .background(backgroundColor, theme.buttonShape)
            .border(1.dp, theme.borderColor, theme.buttonShape)
            .safePointerEvent(PointerEventType.Enter) { isHovered = true }
            .safePointerEvent(PointerEventType.Exit) {
                isHovered = false
                if (!dragStarted) pressPos = null
            }
            .safePointerEvent(PointerEventType.Press) { event ->
                val pos = event.changes.firstOrNull()?.position
                if (pos != null) {
                    pressPos = pos
                    dragStarted = false
                }
            }
            .safePointerEvent(PointerEventType.Move) { event ->
                val pos = event.changes.firstOrNull()?.position ?: return@safePointerEvent
                val start = pressPos ?: return@safePointerEvent
                if (!dragStarted && event.changes.any { it.pressed }) {
                    val dx = pos.x - start.x
                    val dy = pos.y - start.y
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (dist > 8f) {
                        dragStarted = true
                        val paddingPx = cardPadding.value * density
                        val hudLocalX = ((start.x - paddingPx) / previewScale).coerceAtLeast(0f)
                        val hudLocalY = ((start.y - paddingPx) / previewScale).coerceAtLeast(0f)
                        onDragStart(hud, pos.x, pos.y, hudLocalX, hudLocalY)
                        pressPos = null
                        isHovered = false
                    }
                }
            }
            .safePointerEvent(PointerEventType.Release) {
                if (!dragStarted && pressPos != null) {
                    onCardClick(hud)
                }
                pressPos = null
                dragStarted = false
            }
            .padding(cardPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            localizedLabel(hud.title) ?: "Legacy HUD",
            color = theme.textColor.copy(0.85f),
            fontSize = 12.sp,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ComposeHudPreviewCard(
    hud: Hud,
    maxCardWidth: Dp,
    onDragStart: (Hud, Float, Float, Float, Float) -> Unit,
    onCardClick: (Hud) -> Unit,
) {
    val previewRuntime = remember(hud) {
        hud.update()
        PolyComposeRuntime().also { rt -> rt.setContent { hud.Content() } }
    }
    DisposableEffect(previewRuntime) {
        onDispose { previewRuntime.dispose() }
    }
    var naturalW by remember(hud) { mutableStateOf(0f) }
    var naturalH by remember(hud) { mutableStateOf(0f) }
    val density = LocalDensity.current.density
    val theme = LocalTheme.current

    // Hover state tracked via pointer Enter/Exit
    var isHovered by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        if (isHovered) Accent else theme.popupBackground,
        animationSpec = tween(150)
    )

    var pressPos by remember { mutableStateOf<Offset?>(null) }
    var dragStarted by remember { mutableStateOf(false) }

    LaunchedEffect(hud) {
        hud.update()
        if (hud.staticWidth && hud.staticW > 0f && hud.staticH > 0f) {
            previewRuntime.frame(hud.staticW, hud.staticH)
            naturalW = hud.staticW
            naturalH = hud.staticH
        } else {
            previewRuntime.frame(2000f, 2000f)
            naturalW = previewRuntime.root.width
            naturalH = previewRuntime.root.height
        }
    }

    if (naturalW > 0f && naturalH > 0f) {
        val cardPadding = PREVIEW_CARD_PADDING
        val previewScale = previewScaleFor(naturalW, naturalH, maxCardWidth, density)
        val w = ((naturalW * previewScale / density).dp + cardPadding * 2).coerceAtMost(maxCardWidth)
        val h = (naturalH * previewScale / density).dp + cardPadding * 2
        Canvas(
            modifier = Modifier
                .size(w, h)
                .background(backgroundColor, theme.buttonShape)
                .border(1.dp, theme.borderColor, theme.buttonShape)
                .clip(theme.buttonShape)
                .safePointerEvent(PointerEventType.Enter) { isHovered = true }
                .safePointerEvent(PointerEventType.Exit) {
                    isHovered = false
                    if (!dragStarted) pressPos = null
                }
                .safePointerEvent(PointerEventType.Press) { event ->
                    val pos = event.changes.firstOrNull()?.position
                    if (pos != null) {
                        pressPos = pos
                        dragStarted = false
                    }
                }
                .safePointerEvent(PointerEventType.Move) { event ->
                    val pos = event.changes.firstOrNull()?.position ?: return@safePointerEvent
                    val start = pressPos ?: return@safePointerEvent
                    if (!dragStarted && event.changes.any { it.pressed }) {
                        val dx = pos.x - start.x
                        val dy = pos.y - start.y
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                        if (dist > 8f) {
                            dragStarted = true
                            val paddingPx = cardPadding.value * density
                            val hudLocalX = ((start.x - paddingPx) / previewScale).coerceAtLeast(0f)
                            val hudLocalY = ((start.y - paddingPx) / previewScale).coerceAtLeast(0f)
                            onDragStart(hud, pos.x, pos.y, hudLocalX, hudLocalY)
                            pressPos = null
                            isHovered = false
                        }
                    }
                }
                .safePointerEvent(PointerEventType.Release) {
                    if (!dragStarted && pressPos != null) {
                        onCardClick(hud)
                    }
                    pressPos = null
                    dragStarted = false
                }
                .padding(cardPadding)
        ) {
            drawIntoCanvas { canvas ->
                val root = previewRuntime.root
                canvas.skiaCanvas.save()
                canvas.skiaCanvas.clipRect(org.jetbrains.skia.Rect.makeWH(size.width, size.height))
                canvas.skiaCanvas.scale(previewScale, previewScale)
                root.render(RenderContext(canvas.skiaCanvas))
                canvas.skiaCanvas.restore()
            }
        }
    }
}

@Composable
private fun ModFilterIcon(iconName: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val theme = LocalTheme.current
    val iconColor by animateColorAsState(
        if (selected) theme.textColor.copy(1f)
        else if (isHovered) theme.textColor.copy(0.8f)
        else theme.textColor.copy(0.7f)
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .onClick(interactionSource) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(iconName, modifier = Modifier.size(32.dp), color = iconColor)
    }
}

@Composable
fun SearchBar() {
    var searchText by remember { mutableStateOf("") }
    val interactionSource = rememberInteractionSource()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val theme = LocalTheme.current
    val borderColor by animateColorAsState(
        if (isFocused) theme.textColor.copy(.20f) else theme.textColor.copy(.10f)
    )
    val iconColor by animateColorAsState(
        if (isFocused) theme.textColor.copy(0.70f) else theme.textColor.copy(0.50f)
    )

    BasicTextField(
        searchText,
        { searchText = it },
        interactionSource = interactionSource,
        textStyle = TextStyle(
            color = iconColor, fontSize = 12.sp,
        ),
        cursorBrush = SolidColor(iconColor),
    ) { innerTextField ->
        Row(
            modifier = Modifier.size(181.dp, 32.dp)
                .border(1.dp, borderColor, theme.buttonShape)
                .background(theme.componentBackground, theme.buttonShape),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon("search", color = iconColor, modifier = Modifier.padding(start = 8.dp).size(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (!isFocused && searchText.isEmpty())
                    Text("Search ...", color = iconColor, fontSize = 12.sp)
                innerTextField()
            }
            if (searchText.isNotEmpty()) {
                IconButton("close", modifier = Modifier.padding(end = 4.dp).size(16.dp)) {
                    searchText = ""
                }
            }
        }
    }
}

@Composable
private fun LibrarySearchBar(value: String, onValueChange: (String) -> Unit) {
    val interactionSource = rememberInteractionSource()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val theme = LocalTheme.current
    val borderColor by animateColorAsState(
        if (isFocused) theme.textColor.copy(.20f) else theme.textColor.copy(.10f)
    )
    val iconColor by animateColorAsState(
        if (isFocused) theme.textColor.copy(0.70f) else theme.textColor.copy(0.50f)
    )

    BasicTextField(
        value,
        onValueChange,
        interactionSource = interactionSource,
        textStyle = TextStyle(
            color = iconColor, fontSize = 12.sp,
        ),
        cursorBrush = SolidColor(iconColor)
    ) { innerTextField ->
        Row(
            modifier = Modifier.width(181.dp).height(32.dp)
                .border(1.dp, borderColor, theme.buttonShape)
                .background(theme.componentBackground, theme.buttonShape),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon("search", color = iconColor, modifier = Modifier.padding(start = 8.dp).size(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (!isFocused && value.isEmpty())
                    Text("Search HUDs...", color = iconColor, fontSize = 12.sp)
                innerTextField()
            }
            if (value.isNotEmpty()) {
                IconButton("close", modifier = Modifier.padding(end = 4.dp).size(16.dp)) {
                    onValueChange("")
                }
            }
        }
    }
}
