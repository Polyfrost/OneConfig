package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHudMarker as LegacyHud
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.layout.FlexibleLayout
import org.polyfrost.oneconfig.internal.ui.hud.HudCanvasResetMenu
import org.polyfrost.oneconfig.internal.ui.hud.LegacyHudOverlayBridge
import org.polyfrost.oneconfig.internal.ui.hud.repairHudStaticSize
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.Designer
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.Settings
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.Theme
import org.jetbrains.skia.Paint
import kotlin.math.roundToInt

enum class StudioCategory(val title: String, val icon: String) {
    Settings("Settings", "qol"),
    Designer("Designer", "paintbrush");
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

private val idleHudBoxColor = Color.White.copy(0.2f)
private val hoveredHudBoxColor = Color.White.copy(0.5f)
private val hiddenHudBoxColor = Color.White.copy(0.4f)
private val hiddenHudDashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f), 0f)

private val hiddenLegacyScrimColor = Color.Black.copy(0.45f)

private const val SNAP_DISTANCE_PX = 6f
private val snapGuideColor = Color(176, 47, 31)

private const val CHROME_SETTINGS_PANEL = "settings-panel"
private const val CHROME_LIBRARY = "library"

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
        val dLeft = kotlin.math.abs(line - pos)
        val dCenter = kotlin.math.abs(line - center)
        val dRight = kotlin.math.abs(line - end)
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

private data class HudActionBarLayout(val settingsX: Float, val visibilityX: Float, val y: Float)

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
    val pad = 4f
    return HudActionBarLayout(sx + pad, sx + sw - iconPx - pad, sy + pad)
}

private fun hitTestActionButton(
    screenX: Float,
    screenY: Float,
    iconX: Float,
    iconY: Float,
    iconPx: Float,
): Boolean =
    screenX >= iconX && screenX <= iconX + iconPx && screenY >= iconY && screenY <= iconY + iconPx

private fun hitTestHudActionBar(
    hud: Hud,
    screenX: Float,
    screenY: Float,
    mcToScreen: Float,
    iconPx: Float,
    gapPx: Float,
): Boolean {
    val layout = hudActionBarLayout(hud, mcToScreen, iconPx, gapPx) ?: return false
    return hitTestActionButton(screenX, screenY, layout.settingsX, layout.y, iconPx) ||
        hitTestActionButton(screenX, screenY, layout.visibilityX, layout.y, iconPx)
}

private fun hitTestHudWithActionBar(
    hud: Hud,
    screenX: Float,
    screenY: Float,
    mcToScreen: Float,
    iconPx: Float,
    gapPx: Float,
): Boolean {
    val bounds = hudBounds(hud) ?: return false
    val sx = bounds.x * mcToScreen
    val sy = bounds.y * mcToScreen
    val sw = bounds.width * mcToScreen
    val sh = bounds.height * mcToScreen
    return screenX >= sx && screenX <= sx + sw && screenY >= sy && screenY <= sy + sh
}

private fun hitTestResizeHandle(hud: Hud, screenX: Float, screenY: Float, mcToScreen: Float): ResizeCorner? {
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

private fun DrawScope.drawSelectedHudBounds(bounds: HudBounds, mcToScreen: Float, showHandles: Boolean) {
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

    val badgeTopY = (sy - HUD_SIZE_BADGE_GAP - HUD_SIZE_BADGE_HEIGHT).coerceAtLeast(0f)
    drawHudSizeBadge("${bounds.width.roundToInt()} x ${bounds.height.roundToInt()}", sx + sw / 2, badgeTopY)
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
        val paint = org.jetbrains.skia.Paint().apply { color = Color.White.toArgb() }
        val textX = badgeX + horizontalPadding
        val textY = topY + (badgeHeight - textHeight) / 2f - metrics.ascent
        canvas.skiaCanvas.drawString(label, textX, textY, font, paint)
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HudDesignStudio(onReturnToOneConfig: (() -> Unit)? = null) {
    var activeCategory by remember { mutableStateOf(StudioCategory.Settings) }
    var selectedHud by remember { mutableStateOf<Hud?>(null) }
    var hoveredHud by remember { mutableStateOf<Hud?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var draggedHud by remember { mutableStateOf<Hud?>(null) }
    var snapGuides by remember { mutableStateOf(SnapGuides.NONE) }
    var isResizing by remember { mutableStateOf(false) }
    var resizedHud by remember { mutableStateOf<Hud?>(null) }
    var resizeCorner by remember { mutableStateOf<ResizeCorner?>(null) }
    var resizeStartBounds by remember { mutableStateOf<HudBounds?>(null) }
    var resizeStartTextScale by remember { mutableStateOf(1f) }
    var resizeStartScale by remember { mutableStateOf(1f) }
    var resizeStartStaticW by remember { mutableStateOf(0f) }
    var resizeStartStaticH by remember { mutableStateOf(0f) }
    var libraryVisible by remember { mutableStateOf(false) }
    var filterModId by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    val chromeRects = remember { mutableStateMapOf<String, Rect>() }
    var hudContextMenuTarget by remember { mutableStateOf<Hud?>(null) }
    var hudContextMenuOffset by remember { mutableStateOf(IntOffset.Zero) }
    val keyFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        HudManager.pendingSelection?.let { pending ->
            HudManager.pendingSelection = null
            if (pending in HudManager.activeInstances) selectedHud = pending
        }
    }

    LaunchedEffect(selectedHud) {
        selectedHud?.let { repairHudStaticSize(it) }
    }

    val providers = remember { HudManager.providers().toList() }
    val modIds = remember(providers) { providers.mapNotNull { it.configId }.distinct() }
    val filteredProviders = providers.filter { hud ->
        (filterModId == null || hud.configId == filterModId) &&
            (searchText.isEmpty() || hud.title?.contains(searchText, ignoreCase = true) == true) &&
            (hud.multipleInstancesAllowed() || HudManager.getHudsOfType(hud::class.java).isEmpty())
    }
    val densityObj = LocalDensity.current
    val densityFloat = densityObj.density
    val actionIconPx = with(densityObj) { 24.dp.toPx() }
    val actionBarGapPx = with(densityObj) { 8.dp.toPx() }
    val libraryChromeVisible = modIds.isNotEmpty() && selectedHud == null

    fun Modifier.chromeRegion(key: String) = onGloballyPositioned { chromeRects[key] = it.boundsInRoot() }

    fun inChrome(px: Float, py: Float): Boolean {
        val point = Offset(px, py)
        return chromeRects.values.any { it.contains(point) }
    }

    // Unified pointer modifier: drag any HUD, click to select, hover to show action bar
    val pointerModifier = Modifier
        .onPointerEvent(PointerEventType.Press) { event ->
            if (event.changes.any { it.isConsumed }) return@onPointerEvent
            val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
            if (inChrome(pos.x, pos.y)) return@onPointerEvent
            if (event.buttons.isSecondaryPressed) {
                val hit = orderedInstances().lastOrNull { hitTestHud(it, pos.x, pos.y) }
                if (hit != null) {
                    event.changes.forEach { it.consume() }
                    Snapshot.withMutableSnapshot {
                        hudContextMenuTarget = hit
                        hudContextMenuOffset = IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
                        selectedHud = hit
                        libraryVisible = false
                    }
                }
                return@onPointerEvent
            }
            val mcToScreen = Platform.screen().mcToScreenScale()
            val selected = selectedHud
            if (selected != null) {
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
                    }
                    return@onPointerEvent
                }
            }
            val currentActionBarTarget = selectedHud ?: hoveredHud
            if (currentActionBarTarget != null) {
                if (hitTestHudActionBar(currentActionBarTarget, pos.x, pos.y, mcToScreen, actionIconPx, actionBarGapPx)) {
                    return@onPointerEvent
                }
            }
            val s = Platform.screen().screenToMcScale()
            val hit = orderedInstances().lastOrNull { hitTestHud(it, pos.x, pos.y) }
            if (hit != null) UiSounds.play(UiSoundEvent.HUD_DRAG_START)
            if (hit != null) event.changes.forEach { it.consume() }
            Snapshot.withMutableSnapshot {
                if (hit != null) {
                    dragOffsetX = pos.x * s - hit.x
                    dragOffsetY = pos.y * s - hit.y
                    isDragging = true
                    draggedHud = hit
                    hoveredHud = hit
                    libraryVisible = false
                } else {
                    selectedHud = null
                }
            }
        }
        .onPointerEvent(PointerEventType.Move) { event ->
            val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
            if (isResizing) {
                if (event.changes.none { it.pressed }) {
                    Snapshot.withMutableSnapshot {
                        isResizing = false
                        resizedHud = null
                        resizeCorner = null
                        resizeStartBounds = null
                    }
                    return@onPointerEvent
                }
                val hit = resizedHud ?: return@onPointerEvent
                val corner = resizeCorner ?: return@onPointerEvent
                val bounds = resizeStartBounds ?: return@onPointerEvent
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
                    Snapshot.withMutableSnapshot {
                        isDragging = false
                        draggedHud = null
                        snapGuides = SnapGuides.NONE
                    }
                    return@onPointerEvent
                }
                val s = Platform.screen().screenToMcScale()
                val hit = draggedHud ?: return@onPointerEvent
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
                if (inChrome(pos.x, pos.y)) {
                    hoveredHud = null
                    return@onPointerEvent
                }
                val hit = orderedInstances().lastOrNull { hitTestHud(it, pos.x, pos.y) }
                val mcToScreen = Platform.screen().mcToScreenScale()
                val overActionBar = (selectedHud ?: hoveredHud)?.let { hh ->
                    hitTestHudActionBar(hh, pos.x, pos.y, mcToScreen, actionIconPx, actionBarGapPx)
                } == true
                val inExpandedZone = hoveredHud?.let { hh ->
                    hitTestHudWithActionBar(hh, pos.x, pos.y, mcToScreen, actionIconPx, actionBarGapPx)
                } == true
                if (hit != null) {
                    hoveredHud = hit
                } else if (!overActionBar && !inExpandedZone) {
                    hoveredHud = null
                }
            }
        }
        .onPointerEvent(PointerEventType.Release) { event ->
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
                    selectedHud = wasResizedHud
                    libraryVisible = false
                }
                return@onPointerEvent
            }
            val wasDragging = isDragging
            val wasDraggedHud = draggedHud
            Snapshot.withMutableSnapshot {
                isDragging = false
                draggedHud = null
                snapGuides = SnapGuides.NONE
            }
            if (!wasDragging || wasDraggedHud == null) {
                val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                if (inChrome(pos.x, pos.y)) return@onPointerEvent
                val currentActionBarTarget = selectedHud ?: hoveredHud
                if (currentActionBarTarget != null) {
                    val mcToScreen = Platform.screen().mcToScreenScale()
                    if (hitTestHudActionBar(currentActionBarTarget, pos.x, pos.y, mcToScreen, actionIconPx, actionBarGapPx)) {
                        return@onPointerEvent
                    }
                }
                val hit = orderedInstances().lastOrNull { hitTestHud(it, pos.x, pos.y) }
                if (hit != null) {
                    if (hit !== selectedHud) UiSounds.play(UiSoundEvent.HUD_SELECT)
                    Snapshot.withMutableSnapshot {
                        selectedHud = hit
                        libraryVisible = false
                    }
                }
            } else {
                UiSounds.play(UiSoundEvent.HUD_DRAG_END)
                Snapshot.withMutableSnapshot {
                    selectedHud = wasDraggedHud
                    libraryVisible = false
                }
            }
        }

    LaunchedEffect(selectedHud) {
        if (selectedHud != null) keyFocusRequester.requestFocus()
    }

    val chromeAlpha by animateFloatAsState(
        targetValue = if (isDragging) OneConfigConfig.hudDragUiOpacity.coerceIn(0f, 1f) else 1f,
        animationSpec = tween(150),
        label = "hudDragChromeAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(keyFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                val hud = selectedHud ?: return@onKeyEvent false
                val step = if (keyEvent.isShiftPressed) 10f else 1f
                val (dx, dy) = when (keyEvent.key) {
                    Key.DirectionLeft -> -step to 0f
                    Key.DirectionRight -> step to 0f
                    Key.DirectionUp -> 0f to -step
                    Key.DirectionDown -> 0f to step
                    else -> return@onKeyEvent false
                }
                Snapshot.withMutableSnapshot {
                    hud.setAbsolutePosition(hud.x + dx, hud.y + dy)
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
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .graphicsLayer { alpha = chromeAlpha }
                    .clip(theme.buttonShape)
                    .background(returnBackground, theme.buttonShape)
                    .hoverable(returnInteraction)
                    .onPointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    val mcToScreen = Platform.screen().mcToScreenScale()
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
                        val resizable = true
                        val sx = bounds.x * mcToScreen
                        val sy = bounds.y * mcToScreen
                        val sw = bounds.width * mcToScreen
                        val sh = bounds.height * mcToScreen
                        val isSelected = hud === selectedHud
                        val isHovered = hud === hoveredHud
                        val isBeingDragged = hud === draggedHud && isDragging

                        if (isBeingDragged) {
                            drawRect(
                                color = selectionBlue.copy(alpha = 0.10f),
                                topLeft = Offset(sx, sy),
                                size = Size(sw, sh),
                            )
                            drawSelectedHudBounds(bounds, mcToScreen, resizable)
                        } else if (isSelected) {
                            drawSelectedHudBounds(bounds, mcToScreen, resizable)
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
                }
        )

        val actionBarTarget = selectedHud ?: hoveredHud
        if (actionBarTarget != null) {
            val mcToScreen = Platform.screen().mcToScreenScale()
            val bounds = hudBounds(actionBarTarget)
            val layout = hudActionBarLayout(actionBarTarget, mcToScreen, actionIconPx, actionBarGapPx)
            if (bounds != null && bounds.width > 0f && bounds.height > 0f && layout != null) {
                val iconSize = 24.dp
                val settingsX = (layout.settingsX / densityFloat).coerceAtLeast(0f)
                val visibilityX = (layout.visibilityX / densityFloat).coerceAtLeast(0f)
                val iconY = (layout.y / densityFloat).coerceAtLeast(0f)
                val isHidden = actionBarTarget.hidden
                Box(
                    modifier = Modifier
                        .padding(start = settingsX.dp, top = iconY.dp)
                        .graphicsLayer { alpha = chromeAlpha }
                        .onPointerEvent(PointerEventType.Move, PointerEventPass.Final) { event ->
                            if (event.changes.any { it.pressed }) return@onPointerEvent
                            event.changes.forEach { if (!it.isConsumed) it.consume() }
                        }
                        .onPointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
                            if (event.changes.none { it.isConsumed }) {
                                event.changes.forEach { it.consume() }
                                UiSounds.play(UiSoundEvent.HUD_SELECT)
                                Snapshot.withMutableSnapshot { selectedHud = actionBarTarget }
                            }
                        },
                ) {
                    IconButton(
                        "settings",
                        modifier = Modifier.size(iconSize),
                        foreground = Color.White.copy(0.7f),
                        hoveredForeground = Color.White,
                    ) {
                        Snapshot.withMutableSnapshot { selectedHud = actionBarTarget }
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(start = visibilityX.dp, top = iconY.dp)
                        .graphicsLayer { alpha = chromeAlpha }
                        .onPointerEvent(PointerEventType.Move, PointerEventPass.Final) { event ->
                            if (event.changes.any { it.pressed }) return@onPointerEvent
                            event.changes.forEach { if (!it.isConsumed) it.consume() }
                        }
                        .onPointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
                            if (event.changes.none { it.isConsumed }) {
                                event.changes.forEach { it.consume() }
                                UiSounds.play(UiSoundEvent.CLICK)
                                Snapshot.withMutableSnapshot {
                                    actionBarTarget.hidden = !actionBarTarget.hidden
                                }
                            }
                        },
                ) {
                    IconButton(
                        if (isHidden) "eye-off" else "eye",
                        modifier = Modifier.size(iconSize),
                        foreground = Color.White.copy(0.7f),
                        hoveredForeground = Color.White,
                    ) {
                        Snapshot.withMutableSnapshot {
                            actionBarTarget.hidden = !actionBarTarget.hidden
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.CenterEnd)
                .graphicsLayer { alpha = chromeAlpha }
        ) {
        AnimatedVisibility(
            visible = selectedHud != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        ) {
            DisposableEffect(Unit) { onDispose { chromeRects.remove(CHROME_SETTINGS_PANEL) } }
            Box(
                modifier = Modifier
                    .chromeRegion(CHROME_SETTINGS_PANEL)
                    .onPointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
                        event.changes.forEach { if (!it.isConsumed) it.consume() }
                    }
                    .onPointerEvent(PointerEventType.Move, PointerEventPass.Final) { event ->
                        if (event.changes.any { it.pressed }) return@onPointerEvent
                        event.changes.forEach { if (!it.isConsumed) it.consume() }
                    }
                    .onPointerEvent(PointerEventType.Release, PointerEventPass.Final) { event ->
                        event.changes.forEach { if (!it.isConsumed) it.consume() }
                    }
            ) {
                DesignStudioPanel(
                    selectedHud = selectedHud,
                    activeCategory = activeCategory,
                    onCategoryChange = { activeCategory = it },
                    onBack = { Snapshot.withMutableSnapshot { selectedHud = null } }
                )
            }
        }
        }

        Box(
            modifier = Modifier.align(Alignment.CenterEnd)
                .graphicsLayer { alpha = chromeAlpha }
        ) {
        if (libraryChromeVisible) {
            DisposableEffect(Unit) { onDispose { chromeRects.remove(CHROME_LIBRARY) } }
            Row(
                modifier = Modifier
                    .chromeRegion(CHROME_LIBRARY)
                    .onPointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
                        event.changes.forEach { if (!it.isConsumed) it.consume() }
                    }
                    .onPointerEvent(PointerEventType.Move, PointerEventPass.Final) { event ->
                        if (event.changes.any { it.pressed }) return@onPointerEvent
                        event.changes.forEach { if (!it.isConsumed) it.consume() }
                    }
                    .onPointerEvent(PointerEventType.Release, PointerEventPass.Final) { event ->
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
                        filteredProviders = filteredProviders,
                        onDragStart = { hud, sx, sy, hudLocalOffX, hudLocalOffY ->
                            try {
                                val instance = hud.make()
                                val s = Platform.screen().screenToMcScale()
                                val effScale = instance.effectiveScale
                                val offX = hudLocalOffX * effScale
                                val offY = hudLocalOffY * effScale
                                instance.setAbsolutePosition(sx * s - offX, sy * s - offY)
                                if (instance !in HudManager.activeInstances) {
                                    HudManager.activeInstances.add(instance)
                                    instance.setup()
                                    instance.captureStaticSizeDefaults()
                                    instance.capturePositionDefaults()
                                }
                                UiSounds.play(UiSoundEvent.HUD_DRAG_START)
                                Snapshot.withMutableSnapshot {
                                    dragOffsetX = offX
                                    dragOffsetY = offY
                                    isDragging = true
                                    draggedHud = instance
                                    libraryVisible = false
                                }
                            } catch (_: Throwable) {}
                        },
                    )
                }
                ModIconColumn(
                    modIds = modIds,
                    filterModId = filterModId,
                    libraryVisible = libraryVisible,
                ) { modId ->
                    Snapshot.withMutableSnapshot {
                        if (filterModId == modId && libraryVisible) {
                            libraryVisible = false
                        } else {
                            filterModId = modId
                            libraryVisible = true
                        }
                    }
                }
            }
        }
        }

        HudCanvasResetMenu(
            hud = hudContextMenuTarget,
            expanded = hudContextMenuTarget != null,
            offset = hudContextMenuOffset,
            onDismiss = { hudContextMenuTarget = null },
            onDelete = { hud ->
                Snapshot.withMutableSnapshot {
                    if (selectedHud === hud) selectedHud = null
                    if (hoveredHud === hud) hoveredHud = null
                    HudManager.removeHud(hud, delete = true)
                }
                UiSounds.play(UiSoundEvent.CLICK)
            },
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HudDragLayer(modifier: Modifier = Modifier) {
    var hoveredHud by remember { mutableStateOf<Hud?>(null) }
    var draggedHud by remember { mutableStateOf<Hud?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var snapGuides by remember { mutableStateOf(SnapGuides.NONE) }

    val densityObj = LocalDensity.current
    val densityFloat = densityObj.density
    val actionIconPx = with(densityObj) { 24.dp.toPx() }
    val actionBarGapPx = with(densityObj) { 8.dp.toPx() }

    fun endDrag() {
        Snapshot.withMutableSnapshot {
            isDragging = false
            draggedHud = null
            snapGuides = SnapGuides.NONE
            ShellState.hudDragging = false
        }
    }

    fun overShell(px: Float, py: Float) = ShellState.shellBounds?.contains(Offset(px, py)) == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPointerEvent(PointerEventType.Press) { event ->
                if (event.changes.any { it.isConsumed }) return@onPointerEvent
                if (event.buttons.isSecondaryPressed) return@onPointerEvent
                val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                if (overShell(pos.x, pos.y)) return@onPointerEvent
                if (!isDragging) {
                    val actionTarget = hoveredHud
                    if (actionTarget != null && hitTestHudActionBar(
                            actionTarget, pos.x, pos.y,
                            Platform.screen().mcToScreenScale(), actionIconPx, actionBarGapPx,
                        )
                    ) return@onPointerEvent
                }
                val s = Platform.screen().screenToMcScale()
                val hit = orderedInstances().lastOrNull { hitTestHud(it, pos.x, pos.y) }
                    ?: return@onPointerEvent
                UiSounds.play(UiSoundEvent.HUD_DRAG_START)
                Snapshot.withMutableSnapshot {
                    dragOffsetX = pos.x * s - hit.x
                    dragOffsetY = pos.y * s - hit.y
                    isDragging = true
                    draggedHud = hit
                    hoveredHud = hit
                    ShellState.hudDragging = true
                }
            }
            .onPointerEvent(PointerEventType.Move) { event ->
                val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                if (isDragging) {
                    if (event.changes.none { it.pressed }) {
                        endDrag()
                        return@onPointerEvent
                    }
                    val s = Platform.screen().screenToMcScale()
                    val hit = draggedHud ?: return@onPointerEvent
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
                    val hit = if (overShell(pos.x, pos.y)) null
                        else orderedInstances().lastOrNull { hitTestHud(it, pos.x, pos.y) }
                    if (hit !== hoveredHud) hoveredHud = hit
                }
            }
            .onPointerEvent(PointerEventType.Release) {
                if (isDragging) UiSounds.play(UiSoundEvent.HUD_DRAG_END)
                endDrag()
            }
            .drawWithContent {
                drawContent()
                val mcToScreen = Platform.screen().mcToScreenScale()
                drawIntoCanvas { canvas ->
                    val sk = canvas.skiaCanvas
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
                val iconSize = 24.dp
                val settingsX = (layout.settingsX / densityFloat).coerceAtLeast(0f)
                val visibilityX = (layout.visibilityX / densityFloat).coerceAtLeast(0f)
                val iconY = (layout.y / densityFloat).coerceAtLeast(0f)
                val isHidden = actionBarTarget.hidden
                Box(
                    modifier = Modifier
                        .padding(start = settingsX.dp, top = iconY.dp)
                        .onPointerEvent(PointerEventType.Move, PointerEventPass.Final) { event ->
                            if (event.changes.any { it.pressed }) return@onPointerEvent
                            event.changes.forEach { if (!it.isConsumed) it.consume() }
                        }
                        .onPointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
                            if (event.changes.none { it.isConsumed }) {
                                event.changes.forEach { it.consume() }
                                UiSounds.play(UiSoundEvent.HUD_SELECT)
                                HudManager.pendingSelection = actionBarTarget
                                HudManager.openEditor()
                            }
                        },
                ) {
                    IconButton(
                        "settings",
                        modifier = Modifier.size(iconSize),
                        foreground = Color.White.copy(0.7f),
                        hoveredForeground = Color.White,
                    ) {
                        HudManager.pendingSelection = actionBarTarget
                        HudManager.openEditor()
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(start = visibilityX.dp, top = iconY.dp)
                        .onPointerEvent(PointerEventType.Move, PointerEventPass.Final) { event ->
                            if (event.changes.any { it.pressed }) return@onPointerEvent
                            event.changes.forEach { if (!it.isConsumed) it.consume() }
                        }
                        .onPointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
                            if (event.changes.none { it.isConsumed }) {
                                event.changes.forEach { it.consume() }
                                UiSounds.play(UiSoundEvent.CLICK)
                                Snapshot.withMutableSnapshot {
                                    actionBarTarget.hidden = !actionBarTarget.hidden
                                }
                            }
                        },
                ) {
                    IconButton(
                        if (isHidden) "eye-off" else "eye",
                        modifier = Modifier.size(iconSize),
                        foreground = Color.White.copy(0.7f),
                        hoveredForeground = Color.White,
                    ) {
                        Snapshot.withMutableSnapshot {
                            actionBarTarget.hidden = !actionBarTarget.hidden
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesignStudioPanel(
    selectedHud: Hud?,
    activeCategory: StudioCategory,
    onCategoryChange: (StudioCategory) -> Unit,
    onBack: () -> Unit,
) {
    val theme = LocalTheme.current
    val isLegacy = selectedHud is LegacyHud
    val categories = if (isLegacy) listOf(StudioCategory.Settings) else StudioCategory.entries
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
                Text("HUD Design Studio", color = theme.textColor, fontSize = 24.sp)
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
                                        .padding(end = 8.dp),
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

@Composable
private fun HudLibraryPanel(
    searchText: String,
    onSearchChange: (String) -> Unit,
    filteredProviders: List<Hud>,
    onDragStart: (Hud, Float, Float, Float, Float) -> Unit = { _, _, _, _, _ -> },
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
        val libraryScrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .verticalScroll(libraryScrollState)
                    .padding(end = 8.dp),
            ) {
                FlexibleLayout(
                    horizontalSpacing = 10.dp,
                    verticalSpacing = 10.dp
                ) {
                    filteredProviders.forEach { hud ->
                        HudPreviewCard(hud, onDragStart)
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(libraryScrollState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun ModIconColumn(
    modIds: List<String>,
    filterModId: String?,
    libraryVisible: Boolean,
    onModClick: (String) -> Unit,
) {
    val theme = LocalTheme.current
    Column(
        modifier = Modifier
            .padding(end = 16.dp, top = 16.dp, bottom = 16.dp)
            .background(theme.popupBackground, theme.backgroundShape)
            .border(1.dp, theme.borderColor, theme.backgroundShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        modIds.forEach { modId ->
            val icon = HudManager.iconFor(modId) ?: ConfigRegistry.findById(modId)?.icon ?: "qol"
            ModFilterIcon(
                iconName = icon,
                selected = filterModId == modId && libraryVisible,
            ) { onModClick(modId) }
        }
    }
}

private const val PREVIEW_SCALE = 2f

@Composable
private fun HudPreviewCard(hud: Hud, onDragStart: (Hud, Float, Float, Float, Float) -> Unit) {
    // Legacy HUDs have no Compose content tree, so render a sized, titled placeholder tile
    // instead of an empty (zero-size, invisible) preview. The placed instance still renders
    // for real through LegacyHudRenderer once dragged onto the canvas.
    if (hud is LegacyHud) {
        LegacyHudPreviewCard(hud, onDragStart)
    } else {
        ComposeHudPreviewCard(hud, onDragStart)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LegacyHudPreviewCard(hud: Hud, onDragStart: (Hud, Float, Float, Float, Float) -> Unit) {
    val (naturalW, naturalH) = hud.minimumSize()
    if (naturalW <= 0f || naturalH <= 0f) return
    val density = LocalDensity.current.density
    val theme = LocalTheme.current

    var isHovered by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        if (isHovered) Accent else theme.popupBackground,
        animationSpec = tween(150)
    )
    var pressPos by remember { mutableStateOf<Offset?>(null) }
    var dragStarted by remember { mutableStateOf(false) }

    val cardPadding = 12.dp
    val minTile = 72.dp
    val w = ((naturalW * PREVIEW_SCALE / density).dp + cardPadding * 2).coerceAtLeast(minTile)
    val h = ((naturalH * PREVIEW_SCALE / density).dp + cardPadding * 2).coerceAtLeast(minTile)
    Box(
        modifier = Modifier
            .size(w, h)
            .background(backgroundColor, theme.buttonShape)
            .border(1.dp, theme.borderColor, theme.buttonShape)
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) {
                isHovered = false
                if (!dragStarted) pressPos = null
            }
            .onPointerEvent(PointerEventType.Press) { event ->
                val pos = event.changes.firstOrNull()?.position
                if (pos != null) {
                    pressPos = pos
                    dragStarted = false
                }
            }
            .onPointerEvent(PointerEventType.Move) { event ->
                val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                val start = pressPos ?: return@onPointerEvent
                if (!dragStarted && event.changes.any { it.pressed }) {
                    val dx = pos.x - start.x
                    val dy = pos.y - start.y
                    val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (dist > 8f) {
                        dragStarted = true
                        val paddingPx = 12f * density
                        val hudLocalX = ((start.x - paddingPx) / PREVIEW_SCALE).coerceAtLeast(0f)
                        val hudLocalY = ((start.y - paddingPx) / PREVIEW_SCALE).coerceAtLeast(0f)
                        onDragStart(hud, pos.x, pos.y, hudLocalX, hudLocalY)
                        pressPos = null
                        isHovered = false
                    }
                }
            }
            .onPointerEvent(PointerEventType.Release) {
                pressPos = null
                dragStarted = false
            }
            .padding(cardPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            hud.title ?: "Legacy HUD",
            color = theme.textColor.copy(0.85f),
            fontSize = 12.sp,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ComposeHudPreviewCard(hud: Hud, onDragStart: (Hud, Float, Float, Float, Float) -> Unit) {
    val previewRuntime = remember(hud) {
        hud.update()
        PolyComposeRuntime().also { rt -> rt.setContent { hud.Content() } }
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
        val cardPadding = 12.dp
        val w = (naturalW * PREVIEW_SCALE / density).dp + cardPadding * 2
        val h = (naturalH * PREVIEW_SCALE / density).dp + cardPadding * 2
        Canvas(
            modifier = Modifier
                .size(w, h)
                .background(backgroundColor, theme.buttonShape)
                .border(1.dp, theme.borderColor, theme.buttonShape)
                .onPointerEvent(PointerEventType.Enter) { isHovered = true }
                .onPointerEvent(PointerEventType.Exit) {
                    isHovered = false
                    if (!dragStarted) pressPos = null
                }
                .onPointerEvent(PointerEventType.Press) { event ->
                    val pos = event.changes.firstOrNull()?.position
                    if (pos != null) {
                        pressPos = pos
                        dragStarted = false
                    }
                }
                .onPointerEvent(PointerEventType.Move) { event ->
                    val pos = event.changes.firstOrNull()?.position ?: return@onPointerEvent
                    val start = pressPos ?: return@onPointerEvent
                    if (!dragStarted && event.changes.any { it.pressed }) {
                        val dx = pos.x - start.x
                        val dy = pos.y - start.y
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                        if (dist > 8f) {
                            dragStarted = true
                            val paddingPx = 12f * density
                            val hudLocalX = ((start.x - paddingPx) / PREVIEW_SCALE).coerceAtLeast(0f)
                            val hudLocalY = ((start.y - paddingPx) / PREVIEW_SCALE).coerceAtLeast(0f)
                            onDragStart(hud, pos.x, pos.y, hudLocalX, hudLocalY)
                            pressPos = null
                            isHovered = false
                        }
                    }
                }
                .onPointerEvent(PointerEventType.Release) {
                    pressPos = null
                    dragStarted = false
                }
                .padding(cardPadding)
        ) {
            drawIntoCanvas { canvas ->
                val root = previewRuntime.root
                canvas.skiaCanvas.save()
                canvas.skiaCanvas.scale(PREVIEW_SCALE, PREVIEW_SCALE)
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
