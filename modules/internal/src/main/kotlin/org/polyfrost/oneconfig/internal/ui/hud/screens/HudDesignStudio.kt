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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.HudResize
import org.polyfrost.oneconfig.api.hud.v1.LegacyHudMarker as LegacyHud
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
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
import org.apache.logging.log4j.LogManager
import org.jetbrains.skia.Paint
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

private val LOGGER = LogManager.getLogger("OneConfig/HudDesignStudio")

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
    val iconSize = 24.dp
    val settingsX = (layout.settingsX / densityFloat).coerceAtLeast(0f)
    val visibilityX = (layout.visibilityX / densityFloat).coerceAtLeast(0f)
    val iconY = (layout.y / densityFloat).coerceAtLeast(0f)
    val isHidden = hud.hidden
    Box(
        modifier = Modifier
            .padding(start = settingsX.dp, top = iconY.dp)
            .graphicsLayer { alpha = chromeAlpha }
            .safePointerEvent(PointerEventType.Move, PointerEventPass.Final) { event ->
                if (event.changes.any { it.pressed }) return@safePointerEvent
                event.changes.forEach { if (!it.isConsumed) it.consume() }
            }
            .safePointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
                if (event.changes.none { it.isConsumed }) {
                    event.changes.forEach { it.consume() }
                    UiSounds.play(UiSoundEvent.HUD_SELECT)
                    onSettings()
                }
            },
    ) {
        IconButton(
            "settings",
            modifier = Modifier.size(iconSize),
            foreground = Color.White.copy(0.7f),
            hoveredForeground = Color.White,
        ) { onSettings() }
    }
    Box(
        modifier = Modifier
            .padding(start = visibilityX.dp, top = iconY.dp)
            .graphicsLayer { alpha = chromeAlpha }
            .safePointerEvent(PointerEventType.Move, PointerEventPass.Final) { event ->
                if (event.changes.any { it.pressed }) return@safePointerEvent
                event.changes.forEach { if (!it.isConsumed) it.consume() }
            }
            .safePointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
                if (event.changes.none { it.isConsumed }) {
                    event.changes.forEach { it.consume() }
                    UiSounds.play(UiSoundEvent.CLICK)
                    Snapshot.withMutableSnapshot { hud.hidden = !hud.hidden }
                }
            },
    ) {
        IconButton(
            if (isHidden) "eye-off" else "eye",
            modifier = Modifier.size(iconSize),
            foreground = Color.White.copy(0.7f),
            hoveredForeground = Color.White,
        ) {
            Snapshot.withMutableSnapshot { hud.hidden = !hud.hidden }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HudDesignStudio(onReturnToOneConfig: (() -> Unit)? = null) {
    HudEditorViewport.observe()
    var activeCategory by remember { mutableStateOf(StudioCategory.Settings) }
    var selectedHud by remember { mutableStateOf<Hud?>(null) }
    var panelHud by remember { mutableStateOf<Hud?>(null) }
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
    var libraryVisible by remember { mutableStateOf(true) }
    var searchText by remember { mutableStateOf("") }
    var pendingLibraryScroll by remember { mutableStateOf<String?>(null) }
    val libraryListState = rememberLazyListState()
    val chromeRects = remember { mutableStateMapOf<String, Rect>() }
    var panelOffset by remember { mutableStateOf(Offset.Zero) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var hudContextMenuTarget by remember { mutableStateOf<Hud?>(null) }
    var hudContextMenuOffset by remember { mutableStateOf(IntOffset.Zero) }
    val keyFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        HudManager.pendingSelection?.let { pending ->
            HudManager.pendingSelection = null
            if (pending in HudManager.activeInstances) {
                Snapshot.withMutableSnapshot {
                    selectedHud = pending
                    panelHud = pending
                }
            }
        }
    }

    LaunchedEffect(selectedHud) {
        selectedHud?.let { repairHudStaticSize(it) }
        if (panelHud !== selectedHud) panelHud = null
    }

    val providers = remember { HudManager.providers().toList() }
    val modIds = remember(providers) { providers.mapNotNull { it.configId }.distinct() }
    val modNames = remember(modIds) { modIds.associateWith { modNameFor(it) ?: it } }
    val librarySections = providers
        .filter { hud ->
            (searchText.isEmpty() || hud.title?.contains(searchText, ignoreCase = true) == true) &&
                (hud.multipleInstancesAllowed() || HudManager.getHudsOfType(hud::class.java).isEmpty())
        }
        .groupBy { it.configId }
        .map { (modId, huds) -> HudLibrarySection(modId, modId?.let { modNames[it] } ?: "Other", huds) }
    val librarySectionIds = librarySections.map { it.modId }
    val activeLibraryMod = librarySections.getOrNull(libraryListState.firstVisibleItemIndex)?.modId

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
    val libraryChromeVisible = modIds.isNotEmpty() && selectedHud == null && !isDragging

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

    // Unified pointer modifier: drag any HUD, click to select, hover to show action bar
    val pointerModifier = Modifier
        .safePointerEvent(PointerEventType.Press) { event ->
            if (event.changes.any { it.isConsumed }) return@safePointerEvent
            val pos = event.changes.firstOrNull()?.position ?: return@safePointerEvent
            if (inChrome(pos.x, pos.y)) return@safePointerEvent
            if (event.buttons.isSecondaryPressed) {
                // right click never cycles: keep the current selection if it is under the cursor
                val hit = selectedHud?.takeIf { hitTestHud(it, pos.x, pos.y) }
                    ?: topHudAt(pos.x, pos.y)
                if (hit != null) {
                    event.changes.forEach { it.consume() }
                    Snapshot.withMutableSnapshot {
                        hudContextMenuTarget = hit
                        hudContextMenuOffset = IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
                        selectedHud = hit
                        libraryVisible = false
                    }
                }
                return@safePointerEvent
            }
            val mcToScreen = Platform.screen().mcToScreenScale()
            val selected = selectedHud
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
                    }
                    return@safePointerEvent
                }
            }
            val actionBarCandidates = LinkedHashSet<Hud>()
            (selectedHud ?: hoveredHud)?.let { actionBarCandidates.add(it) }
            // locked huds always show an action bar; it must not swallow clicks aimed at an unlocked hud
            if (hudStackAt(pos.x, pos.y).none { !it.locked }) {
                for (h in HudManager.activeInstances) if (h.locked) actionBarCandidates.add(h)
            }
            if (actionBarCandidates.any { hitTestHudActionBar(it, pos.x, pos.y, mcToScreen, actionIconPx, actionBarGapPx) }) {
                return@safePointerEvent
            }
            val s = Platform.screen().screenToMcScale()
            val hit = pickHudAt(pos.x, pos.y, selectedHud)
            if (hit != null && hit.locked) {
                event.changes.forEach { it.consume() }
                if (hit !== selectedHud) UiSounds.play(UiSoundEvent.HUD_SELECT)
                Snapshot.withMutableSnapshot {
                    selectedHud = hit
                    libraryVisible = false
                }
                return@safePointerEvent
            }
            if (hit != null) UiSounds.play(UiSoundEvent.HUD_DRAG_START)
            if (hit != null) event.changes.forEach { it.consume() }
            hit?.onEditorDragStart()
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
        .safePointerEvent(PointerEventType.Move) { event ->
            val pos = event.changes.firstOrNull()?.position ?: return@safePointerEvent
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
                        snapGuides = SnapGuides.NONE
                    }
                    dropped?.onEditorDragEnd()
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
                if (inChrome(pos.x, pos.y)) {
                    hoveredHud = null
                    return@safePointerEvent
                }
                val hit = topHudAt(pos.x, pos.y)
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
        .safePointerEvent(PointerEventType.Release) { event ->
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
                return@safePointerEvent
            }
            val wasDragging = isDragging
            val wasDraggedHud = draggedHud
            Snapshot.withMutableSnapshot {
                isDragging = false
                draggedHud = null
                snapGuides = SnapGuides.NONE
            }
            if (wasDragging) wasDraggedHud?.onEditorDragEnd()
            if (!wasDragging || wasDraggedHud == null) {
                val pos = event.changes.firstOrNull()?.position ?: return@safePointerEvent
                if (inChrome(pos.x, pos.y)) return@safePointerEvent
                val mcToScreen = Platform.screen().mcToScreenScale()
                val actionBarCandidates = LinkedHashSet<Hud>()
                (selectedHud ?: hoveredHud)?.let { actionBarCandidates.add(it) }
                // locked huds always show an action bar; it must not swallow clicks aimed at an unlocked hud
                if (hudStackAt(pos.x, pos.y).none { !it.locked }) {
                    for (h in HudManager.activeInstances) if (h.locked) actionBarCandidates.add(h)
                }
                if (actionBarCandidates.any { hitTestHudActionBar(it, pos.x, pos.y, mcToScreen, actionIconPx, actionBarGapPx) }) {
                    return@safePointerEvent
                }
                val hit = pickHudAt(pos.x, pos.y, selectedHud)
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
            .onSizeChanged { rootSize = it }
            .focusRequester(keyFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
                val hud = selectedHud ?: return@onKeyEvent false
                if (hud.locked) return@onKeyEvent false
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
            AnimatedVisibility(
                visible = selectedHud == null && !isDragging,
                modifier = Modifier.align(Alignment.CenterStart),
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = slideOutHorizontally(targetOffsetX = { -it }),
            ) {
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
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
                        if (hud.locked) continue
                        val bounds = hudBounds(hud) ?: continue
                        val resizable = hud.resizeAxes != HudResize.None
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
        val actionBarHuds = LinkedHashSet<Hud>()
        actionBarTarget?.let { actionBarHuds.add(it) }
        for (h in HudManager.activeInstances) if (h.locked) actionBarHuds.add(h)
        if (actionBarHuds.isNotEmpty()) {
            val mcToScreen = Platform.screen().mcToScreenScale()
            actionBarHuds.forEach { h ->
                key(h) {
                    HudActionBar(
                        hud = h,
                        mcToScreen = mcToScreen,
                        densityFloat = densityFloat,
                        actionIconPx = actionIconPx,
                        actionBarGapPx = actionBarGapPx,
                        chromeAlpha = chromeAlpha,
                    ) {
                        Snapshot.withMutableSnapshot {
                            selectedHud = h
                            panelHud = h
                        }
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
                            selectedHud = null
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
                                val instance = hud.make()
                                HudManager.markProviderKnown(instance)
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
                    activeModId = activeLibraryMod,
                    libraryVisible = libraryVisible,
                ) { modId ->
                    Snapshot.withMutableSnapshot {
                        if (activeLibraryMod == modId && libraryVisible) {
                            libraryVisible = false
                        } else {
                            if (librarySectionIds.none { it == modId }) searchText = ""
                            libraryVisible = true
                            pendingLibraryScroll = modId
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
    HudEditorViewport.observe()
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
                    ?: return@safePointerEvent
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
                    val hit = if (overShell(pos.x, pos.y)) null
                        else orderedInstances().lastOrNull { !it.locked && hitTestHud(it, pos.x, pos.y) }
                    if (hit !== hoveredHud) hoveredHud = hit
                }
            }
            .safePointerEvent(PointerEventType.Release) {
                if (isDragging) UiSounds.play(UiSoundEvent.HUD_DRAG_END)
                endDrag()
            }
            .drawWithContent {
                drawContent()
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
                    if (hud.locked) continue
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
                        .safePointerEvent(PointerEventType.Move, PointerEventPass.Final) { event ->
                            if (event.changes.any { it.pressed }) return@safePointerEvent
                            event.changes.forEach { if (!it.isConsumed) it.consume() }
                        }
                        .safePointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
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
                        .safePointerEvent(PointerEventType.Move, PointerEventPass.Final) { event ->
                            if (event.changes.any { it.pressed }) return@safePointerEvent
                            event.changes.forEach { if (!it.isConsumed) it.consume() }
                        }
                        .safePointerEvent(PointerEventType.Press, PointerEventPass.Final) { event ->
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

private fun modNameFor(configId: String): String? {
    val modId = configId.removeSuffix(".json").substringBefore('/')
    ModInfo.loadedMods.firstOrNull { it.id == modId }?.name?.let { return it }
    val config = ConfigRegistry.findById(configId)
        ?: ConfigRegistry.findById("$configId.json")
        ?: ConfigRegistry.configs.firstOrNull { it.id.removeSuffix(".json") == modId }
    return config?.title?.toString()
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
            val name = hud.title ?: return@let null
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
                val maxCardWidth = maxWidth - 8.dp
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(end = 8.dp),
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
                                    HudPreviewCard(hud, maxCardWidth, onDragStart)
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
            .padding(end = 16.dp, top = 16.dp, bottom = 16.dp)
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
private fun HudPreviewCard(hud: Hud, maxCardWidth: Dp, onDragStart: (Hud, Float, Float, Float, Float) -> Unit) {
    // Legacy HUDs have no Compose content tree, so render a sized, titled placeholder tile
    // instead of an empty (zero-size, invisible) preview. The placed instance still renders
    // for real through LegacyHudRenderer once dragged onto the canvas.
    if (hud is LegacyHud) {
        LegacyHudPreviewCard(hud, maxCardWidth, onDragStart)
    } else {
        ComposeHudPreviewCard(hud, maxCardWidth, onDragStart)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LegacyHudPreviewCard(hud: Hud, maxCardWidth: Dp, onDragStart: (Hud, Float, Float, Float, Float) -> Unit) {
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
private fun ComposeHudPreviewCard(hud: Hud, maxCardWidth: Dp, onDragStart: (Hud, Float, Float, Float, Float) -> Unit) {
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
