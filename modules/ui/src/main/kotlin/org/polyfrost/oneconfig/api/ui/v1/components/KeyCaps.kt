@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeStyle

private val KEY_HEIGHT = 23.dp
private val KEY_SKIRT = 1.dp
private val KEY_MAX_RADIUS = 6.dp
private val ARROW_KEY = 20.dp
private val ARROW_ICON = 11.dp
private val ARROW_GAP = 2.dp
private val MOUSE_WIDTH = 15.dp
private val MOUSE_HEIGHT = 22.dp
private val DRAG_TRAIL = 8.dp

@ExperimentalOneConfigUiApi
public enum class ArrowDirection(internal val degrees: Float) { Left(180f), Up(270f), Down(90f), Right(0f) }

@ExperimentalOneConfigUiApi
public enum class MouseGesture { Click, Drag, Hover }

private class KeyCapColors(val face: Color, val rim: Color)

@Composable
private fun keyCapColors(tint: Color? = null): KeyCapColors {
    val theme = OneConfigTheme.current
    val face = lerp(theme.componentBackground, theme.textColor, 0.06f).copy(alpha = 1f)
    val rim = lerp(theme.componentBackground, theme.textColor, 0.2f).copy(alpha = 1f)
    if (tint == null) return KeyCapColors(face, rim)
    return KeyCapColors(lerp(face, tint, 0.16f), lerp(rim, tint, 0.55f))
}

private fun DrawScope.pixelUnit(): Float = 1.dp.toPx().roundToInt().coerceAtLeast(1).toFloat()

private fun DrawScope.drawSmoothKeyCap(colors: KeyCapColors, radius: Float, depth: Float) {
    val unit = 1.dp.toPx()
    val w = size.width
    val h = size.height
    drawRoundRect(colors.rim, Offset.Zero, Size(w, h), CornerRadius(radius))
    drawRoundRect(
        colors.face,
        Offset(unit, unit),
        Size(w - unit * 2, h - unit * 2 - depth),
        CornerRadius((radius - unit).coerceAtLeast(0f)),
    )
}

private fun DrawScope.drawPixelKeyCap(colors: KeyCapColors) {
    val p = pixelUnit()
    val cw = (size.width / p).toInt()
    val ch = (size.height / p).toInt()
    fun cells(x: Int, y: Int, w: Int, h: Int, color: Color) {
        if (w > 0 && h > 0) drawRect(color, Offset(x * p, y * p), Size(w * p, h * p))
    }
    cells(1, 0, cw - 2, ch, colors.rim)
    cells(0, 1, cw, ch - 2, colors.rim)
    cells(1, 1, cw - 2, ch - 3, colors.face)
}

@Composable
private fun KeyCapFrame(modifier: Modifier, width: Dp?, height: Dp, tint: Color? = null, content: @Composable () -> Unit) {
    val theme = OneConfigTheme.current
    val pixel = theme.style == ThemeStyle.Pixel
    val colors = keyCapColors(tint)
    val density = LocalDensity.current
    val radius = with(density) {
        val shape = theme.buttonShape as? CornerBasedShape
        (shape?.topStart?.toPx(Size(100f, height.toPx()), density) ?: 0f).coerceAtMost(KEY_MAX_RADIUS.toPx())
    }
    val depthPx = with(density) { KEY_SKIRT.toPx() }
    val sizing = if (width != null) Modifier.size(width, height) else Modifier.height(height).widthIn(min = height)
    Box(
        modifier
            .then(sizing)
            .drawBehind { if (pixel) drawPixelKeyCap(colors) else drawSmoothKeyCap(colors, radius, depthPx) }
            .padding(bottom = KEY_SKIRT),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
@ExperimentalOneConfigUiApi
public fun KeyCap(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp? = null,
    tint: Color? = null,
    content: @Composable () -> Unit,
) {
    KeyCapFrame(modifier, width, height ?: KEY_HEIGHT, tint, content)
}

@Composable
@ExperimentalOneConfigUiApi
public fun KeyCap(
    label: String,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp? = null,
    tint: Color? = null,
) {
    val theme = OneConfigTheme.current
    KeyCapFrame(modifier, width, height ?: KEY_HEIGHT, tint) {
        Text(
            label,
            Modifier.padding(horizontal = if (width != null) 3.dp else 8.dp),
            color = theme.textColor,
            style = theme.typography.captionMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
@ExperimentalOneConfigUiApi
public fun ArrowKey(
    direction: ArrowDirection,
    modifier: Modifier = Modifier,
    size: Dp? = null,
    tint: Color? = null,
) {
    val theme = OneConfigTheme.current
    KeyCapFrame(modifier, size ?: ARROW_KEY, size ?: ARROW_KEY, tint) {
        Icon("right-arrow", null, Modifier.size(ARROW_ICON).rotate(direction.degrees), tint = theme.textColor)
    }
}

@Composable
@ExperimentalOneConfigUiApi
public fun ArrowKeys(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ARROW_GAP)) {
        ArrowKey(ArrowDirection.Up)
        Row(horizontalArrangement = Arrangement.spacedBy(ARROW_GAP)) {
            ArrowKey(ArrowDirection.Left)
            ArrowKey(ArrowDirection.Down)
            ArrowKey(ArrowDirection.Right)
        }
    }
}

private fun DrawScope.drawSmoothMouse(colors: KeyCapColors, accent: Color, pressed: Boolean, left: Float) {
    val unit = 1.dp.toPx()
    val w = MOUSE_WIDTH.toPx()
    val h = size.height
    val depth = KEY_SKIRT.toPx()
    val radius = CornerRadius(w / 2f, w * 0.58f)
    fun shape(x: Float, y: Float, sw: Float, sh: Float, r: CornerRadius) = Path().apply {
        addRoundRect(RoundRect(x, y, x + sw, y + sh, r))
    }
    drawPath(shape(left, 0f, w, h, radius), colors.rim)
    val faceX = left + unit
    val faceW = w - unit * 2
    val faceH = h - unit * 2 - depth
    val face = shape(faceX, unit, faceW, faceH, CornerRadius(radius.x - unit, radius.y - unit))
    drawPath(face, colors.face)
    val split = unit + faceH * 0.44f
    val mid = left + w / 2f
    if (pressed) {
        clipPath(face) { drawRect(accent, Offset(faceX, unit), Size(mid - faceX, split - unit)) }
    }
    drawLine(colors.rim, Offset(faceX, split), Offset(faceX + faceW, split), unit)
    drawLine(colors.rim, Offset(mid, unit), Offset(mid, split), unit)
    val wheelW = 3.dp.toPx()
    val wheelH = 5.dp.toPx()
    drawRoundRect(colors.rim, Offset(mid - wheelW / 2f, unit * 3), Size(wheelW, wheelH), CornerRadius(wheelW / 2f))
}

private fun DrawScope.drawPixelMouse(colors: KeyCapColors, accent: Color, pressed: Boolean, left: Float) {
    val p = pixelUnit()
    val ox = (left / p).roundToInt()
    val cw = (MOUSE_WIDTH.toPx() / p).toInt()
    val ch = (size.height / p).toInt()
    fun cells(x: Int, y: Int, w: Int, h: Int, color: Color) {
        if (w > 0 && h > 0) drawRect(color, Offset((ox + x) * p, y * p), Size(w * p, h * p))
    }
    cells(3, 0, cw - 6, ch, colors.rim)
    cells(1, 1, cw - 2, ch - 2, colors.rim)
    cells(0, 3, cw, ch - 6, colors.rim)
    cells(3, 1, cw - 6, ch - 3, colors.face)
    cells(2, 2, cw - 4, ch - 5, colors.face)
    cells(1, 3, cw - 2, ch - 7, colors.face)
    val mid = cw / 2
    val split = (ch - 3) * 9 / 20 + 1
    if (pressed) {
        cells(3, 1, mid - 3, 1, accent)
        cells(2, 2, mid - 2, 1, accent)
        cells(1, 3, mid - 1, split - 3, accent)
    }
    cells(1, split, cw - 2, 1, colors.rim)
    cells(mid, 1, 1, split - 1, colors.rim)
    cells(mid - 1, 3, 3, 5, colors.rim)
}

private fun DrawScope.drawDragTrail(color: Color, pixel: Boolean) {
    val unit = if (pixel) pixelUnit() else 1.dp.toPx()
    val cy = size.height * 0.42f
    val end = DRAG_TRAIL.toPx() - unit * 2
    floatArrayOf(4f, 2f, 4f).forEachIndexed { i, len ->
        val y = cy + (i - 1) * unit * 3
        if (pixel) {
            val cells = len.roundToInt()
            drawRect(color, Offset(((end / unit).toInt() - cells) * unit, (y / unit).roundToInt() * unit), Size(cells * unit, unit))
        } else {
            drawLine(color, Offset(end - len * unit, y), Offset(end, y), unit * 1.2f, StrokeCap.Round)
        }
    }
}

@Composable
@ExperimentalOneConfigUiApi
public fun MouseGlyph(gesture: MouseGesture, modifier: Modifier = Modifier) {
    val theme = OneConfigTheme.current
    val pixel = theme.style == ThemeStyle.Pixel
    val colors = keyCapColors()
    val accent = OneConfigTheme.accent
    val pressed = gesture != MouseGesture.Hover
    val trail = gesture == MouseGesture.Drag
    val trailColor = theme.textColorSecondary.copy(alpha = 0.7f)
    Canvas(modifier.size(width = MOUSE_WIDTH + if (trail) DRAG_TRAIL else 0.dp, height = MOUSE_HEIGHT)) {
        if (trail) drawDragTrail(trailColor, pixel)
        val left = if (trail) DRAG_TRAIL.toPx() else 0f
        if (pixel) drawPixelMouse(colors, accent, pressed, left) else drawSmoothMouse(colors, accent, pressed, left)
    }
}
