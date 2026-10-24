package org.polyfrost.oneconfig.internal.ui.poly

import kotlin.math.abs
import kotlin.math.sqrt
import org.polyfrost.oneconfig.internal.ui.screens.*
import org.polyfrost.oneconfig.internal.ui.components.settings.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentIteratorType
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.TextDecoration
import org.polyfrost.compose.animation.AnimatedColor
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.animation.core.spring
import org.polyfrost.compose.animation.core.springStep
import org.polyfrost.compose.animation.core.tween
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.Image
import org.polyfrost.compose.foundation.ScrollState
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.absoluteOffset
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.node.PolyTextSpan
import org.polyfrost.compose.node.TextNode
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.runtime.PolyApplier
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.draw.scale
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.ColorFilter
import org.polyfrost.compose.ui.graphics.ImageBitmap
import org.polyfrost.compose.ui.input.key.KeyEvent
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.notifications.v1.SvgMask
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.snapScaleToPixelGrid
import org.polyfrost.oneconfig.internal.ui.shell.fastOutSlowIn
import org.polyfrost.oneconfig.internal.ui.components.IconButton

internal const val DESIGN_WIDTH_DP = 1391f
internal const val DESIGN_HEIGHT_DP = 700f
internal const val EDGE_MARGIN_FRACTION = 0.9f


const val WINDOW_WIDTH = DESIGN_WIDTH_DP
const val WINDOW_HEIGHT = DESIGN_HEIGHT_DP

const val GUTTER = 25f

fun polyUiScale(windowWidth: Float, windowHeight: Float, density: Float, surfaceRatio: Float, shell: Boolean = false): Float {
    var d = density
    var headroom = min(
        windowWidth * EDGE_MARGIN_FRACTION / (DESIGN_WIDTH_DP * d),
        windowHeight * EDGE_MARGIN_FRACTION / (DESIGN_HEIGHT_DP * d),
    )
    if (headroom <= 0f) return d * surfaceRatio
    if (shell) {
        val fit = min(headroom, 1f).coerceAtLeast(0.25f)
        d *= fit
        headroom /= fit
    }
    val grid = snapScaleToPixelGrid(14f * d * surfaceRatio, headroom)
    return d * grid * surfaceRatio
}

@Composable
fun UiText(
    text: String,
    color: PolyColor,
    size: Float,
    font: String,
    modifier: Modifier = Modifier,
    wrap: Boolean = false,
    spans: List<PolyTextSpan>? = null,
    maxLines: Int = Int.MAX_VALUE,
    ellipsis: Boolean = false,
    lineHeight: Float? = null,
    center: Boolean = false,
    baselineShift: Float = 0f,
) = UiText(text, color, null, size, font, modifier, wrap, spans, maxLines, ellipsis, lineHeight, center, baselineShift)

@Composable
fun UiText(
    text: String,
    color: AnimatedColor,
    size: Float,
    font: String,
    modifier: Modifier = Modifier,
    wrap: Boolean = false,
    spans: List<PolyTextSpan>? = null,
    maxLines: Int = Int.MAX_VALUE,
    ellipsis: Boolean = false,
    lineHeight: Float? = null,
    center: Boolean = false,
    baselineShift: Float = 0f,
) = UiText(text, color.target, color, size, font, modifier, wrap, spans, maxLines, ellipsis, lineHeight, center, baselineShift)

@Composable
private fun UiText(
    text: String, color: PolyColor, animated: AnimatedColor?, size: Float, font: String, modifier: Modifier, wrap: Boolean,
    spans: List<PolyTextSpan>?, maxLines: Int, ellipsis: Boolean, lineHeight: Float?, center: Boolean, baselineShift: Float,
) {
    ComposeNode<TextNode, PolyApplier>(
        factory = { TextNode().also { it.leading = true } },
        update = {
            set(text) { this.text = it; remeasure() }
            set(size) { fontSize = it; remeasure() }
            set(color) { this.color = it }
            set(animated) { animatedColor = it }
            set(font) { fontName = it; remeasure() }
            set(wrap) { this.wrap = it; remeasure() }
            set(spans) { this.spans = it; remeasure() }
            set(maxLines) { this.maxLines = it; remeasure() }
            set(ellipsis) { this.ellipsis = it; remeasure() }
            set(lineHeight) { this.lineHeight = it; remeasure() }
            set(center) { this.center = it; remeasure() }
            set(baselineShift) { this.baselineShift = it }
            set(modifier) { modifier.applyTo(style); remeasure() }
        },
    )
}

private const val LEGACY_CHAR = '§'

private val LEGACY_COLORS = mapOf(
    '0' to 0x000000, '1' to 0x0000AA, '2' to 0x00AA00, '3' to 0x00AAAA,
    '4' to 0xAA0000, '5' to 0xAA00AA, '6' to 0xFFAA00, '7' to 0xAAAAAA,
    '8' to 0x555555, '9' to 0x5555FF, 'a' to 0x55FF55, 'b' to 0x55FFFF,
    'c' to 0xFF5555, 'd' to 0xFF55FF, 'e' to 0xFFFF55, 'f' to 0xFFFFFF,
)

fun richText(text: Any, regular: String, bold: String, italic: String): Pair<String, List<PolyTextSpan>?> {
    return when (val comp: Any = Platform.compatibility().wrapPlatformComponent(text)) {
        is String -> comp to if (comp.indexOf(LEGACY_CHAR) >= 0) legacySpans(comp, regular, bold, italic) else null
        is Component -> componentSpans(comp, regular, bold, italic)
        is ComponentLike -> componentSpans(comp.asComponent(), regular, bold, italic)
        else -> comp.toString() to null
    }
}

private fun legacySpans(
    text: String, regular: String, boldFont: String, italicFont: String,
    baseColor: PolyColor? = null, baseUnderline: Boolean = false, baseStrike: Boolean = false,
): List<PolyTextSpan> {
    val out = ArrayList<PolyTextSpan>()
    var color: PolyColor? = baseColor
    var bold = false
    var italic = false
    var strike = baseStrike
    var underline = baseUnderline
    val pending = StringBuilder()
    fun flush() {
        if (pending.isEmpty()) return
        out += PolyTextSpan(pending.toString(), color, if (bold) boldFont else if (italic) italicFont else regular, underline, strike)
        pending.setLength(0)
    }
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == LEGACY_CHAR && i + 1 < text.length) {
            val code = text[i + 1].lowercaseChar()
            val rgb = LEGACY_COLORS[code]
            when {
                rgb != null -> { flush(); color = PolyColor(0xFF000000.toInt() or rgb); bold = false; italic = false; strike = false; underline = false }
                code == 'l' -> { flush(); bold = true }
                code == 'o' -> { flush(); italic = true }
                code == 'm' -> { flush(); strike = true }
                code == 'n' -> { flush(); underline = true }
                code == 'r' -> { flush(); color = baseColor; bold = false; italic = false; strike = baseStrike; underline = baseUnderline }
                code == 'k' -> flush()
                else -> pending.append(c).append(text[i + 1])
            }
            i += 2
        } else {
            pending.append(c)
            i++
        }
    }
    flush()
    return out
}

private fun componentSpans(comp: Component, regular: String, bold: String, italic: String): Pair<String, List<PolyTextSpan>?> {
    val spans = comp.iterable(ComponentIteratorType.DEPTH_FIRST).flatMap {
        val font = when {
            it.hasDecoration(TextDecoration.BOLD) -> bold
            it.hasDecoration(TextDecoration.ITALIC) -> italic
            else -> regular
        }
        val text = Platform.compatibility().resolveComponent(it.children(emptyList()))
        val color = it.color()?.let { c -> PolyColor(0xFF000000.toInt() or c.value()) }
        val underline = it.hasDecoration(TextDecoration.UNDERLINED)
        val strike = it.hasDecoration(TextDecoration.STRIKETHROUGH)
        if (text.indexOf(LEGACY_CHAR) >= 0) legacySpans(text, font, bold, italic, color, underline, strike)
        else listOf(PolyTextSpan(text, color, font, underline, strike))
    }.filter { it.text.isNotEmpty() }
    return spans.joinToString("") { it.text } to spans
}

private val rotatedCache = ConcurrentHashMap<String, Optional>()

private class Optional(val bitmap: ImageBitmap?)

fun svgBitmap(path: String, pixelSize: Int, tint: Int?, rotation: Int = 0): ImageBitmap? {
    val base = SvgMask.get(path, pixelSize, tint) ?: return null
    val deg = ((rotation % 360) + 360) % 360
    if (deg == 0) return base
    return rotatedCache.getOrPut("$path@$pixelSize#$tint/$deg") { Optional(rotate(base, deg.toFloat())) }.bitmap
}

private fun rotate(src: ImageBitmap, degrees: Float): ImageBitmap {
    val w = src.width
    val h = src.height
    val out = ByteArray(w * h * 4)
    val rad = Math.toRadians(degrees.toDouble())
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()
    val cx = w / 2f
    val cy = h / 2f
    val px = src.pixels
    fun at(x: Int, y: Int, ch: Int): Float {
        if (x < 0 || y < 0 || x >= w || y >= h) return 0f
        val o = (y * w + x) * 4
        val a = (px[o + 3].toInt() and 0xFF) / 255f
        return if (ch == 3) a else (px[o + ch].toInt() and 0xFF) * a
    }
    for (y in 0 until h) for (x in 0 until w) {
        val dx = x + 0.5f - cx
        val dy = y + 0.5f - cy
        val sx = c * dx + s * dy + cx - 0.5f
        val sy = -s * dx + c * dy + cy - 0.5f
        val x0 = floor(sx).toInt()
        val y0 = floor(sy).toInt()
        val fx = sx - x0
        val fy = sy - y0
        val o = (y * w + x) * 4
        val vals = FloatArray(4) { ch ->
            (at(x0, y0, ch) * (1 - fx) + at(x0 + 1, y0, ch) * fx) * (1 - fy) + (at(x0, y0 + 1, ch) * (1 - fx) + at(x0 + 1, y0 + 1, ch) * fx) * fy
        }
        val a = vals[3]
        out[o + 3] = (a * 255f).roundToInt().coerceIn(0, 255).toByte()
        for (ch in 0..2) out[o + ch] = if (a > 0f) (vals[ch] / a).roundToInt().coerceIn(0, 255).toByte() else 0
    }
    return ImageBitmap(w, h, out)
}

@Composable
fun SvgImage(
    path: String,
    width: Float,
    height: Float,
    pxPerDp: Float,
    modifier: Modifier = Modifier,
    tint: Int? = null,
    recolor: ((ImageBitmap) -> ImageBitmap)? = null,
    rotation: Int = 0,
    animatedTint: AnimatedColor? = null,
) {
    val bitmap = remember(path, pxPerDp, tint, recolor, rotation) {
        val px = (max(width, height) * pxPerDp).roundToInt()
        svgBitmap(path, px, tint, rotation)?.let { recolor?.invoke(it) ?: it }
    }
    Box(modifier.size(width.dp, height.dp), contentAlignment = Alignment.Center) {
        if (bitmap == null) return@Box
        val fit = min(width / bitmap.width, height / bitmap.height)
        Image(bitmap, null, Modifier.size((bitmap.width * fit).dp, (bitmap.height * fit).dp), animatedTint?.let { ColorFilter.tint(it) })
    }
}

private const val LUM_R = 0.2126f
private const val LUM_G = 0.7152f
private const val LUM_B = 0.0722f

fun brandTint(svgPath: String, tint: PolyColor, tintEnd: PolyColor? = null): ((ImageBitmap) -> ImageBitmap)? {
    val svg = SvgMask::class.java.getResourceAsStream(if (svgPath.startsWith("/")) svgPath else "/$svgPath")?.use { String(it.readBytes()) } ?: return null
    val peak = Regex("#([0-9a-fA-F]{6})\\b").findAll(svg).mapNotNull { m ->
        val rgb = m.groupValues[1].toInt(16)
        val r = (rgb shr 16 and 0xFF) / 255f
        val g = (rgb shr 8 and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        if (r == g && g == b) null else LUM_R * r + LUM_G * g + LUM_B * b
    }.maxOrNull() ?: return null
    val end = tintEnd ?: tint
    return { src ->
        val px = src.pixels.copyOf()
        val span = (src.width - 1).coerceAtLeast(1).toFloat()
        for (i in 0 until px.size / 4) {
            val o = i * 4
            if (px[o + 3].toInt() == 0) continue
            val f = (i % src.width) / span
            val tr = (tint.red + (end.red - tint.red) * f) / 255f / peak
            val tg = (tint.green + (end.green - tint.green) * f) / 255f / peak
            val tb = (tint.blue + (end.blue - tint.blue) * f) / 255f / peak
            val lum = LUM_R * (px[o].toInt() and 0xFF) + LUM_G * (px[o + 1].toInt() and 0xFF) + LUM_B * (px[o + 2].toInt() and 0xFF)
            px[o] = (tr * lum).roundToInt().coerceIn(0, 255).toByte()
            px[o + 1] = (tg * lum).roundToInt().coerceIn(0, 255).toByte()
            px[o + 2] = (tb * lum).roundToInt().coerceIn(0, 255).toByte()
        }
        ImageBitmap(src.width, src.height, px)
    }
}

@Composable
fun OpenTransition(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val t by animateFloatAsState(1f, initialValue = 0f)
    Box(modifier.alpha(t).scale(0.92f + 0.08f * t)) { content() }
}

@Composable
fun AnimatedVisibility(visible: Boolean, content: @Composable (Float) -> Unit) {
    val t by animateFloatAsState(if (visible) 1f else 0f, spring(stiffness = 400f), initialValue = if (visible) 1f else 0f)
    if (visible || t > 0f) content(t)
}

@Composable
fun <T> AnimatedSwap(target: T, order: (T) -> Int, durationMs: Int, modifier: Modifier = Modifier, content: @Composable (T, Float, Int) -> Unit) {
    val shown = remember { mutableStateListOf(target) }
    val from = remember { HashMap<T, T>() }
    val last = remember { mutableListOf(target) }
    if (last[0] != target) {
        from[target] = last[0]
        last[0] = target
        if (target !in shown) shown.add(target)
    }
    Box(modifier) {
        for (s in shown.toList()) key(s) {
            val entering = s == target
            val raw by animateFloatAsState(if (entering) 1f else 0f, tween(durationMs), initialValue = if (entering && shown.size > 1) 0f else 1f)
            if (raw == 0f && !entering) SideEffect { shown.remove(s); from.remove(s) }
            val other = if (entering) from[s] else target
            val side = if (other == null) 0 else order(s).compareTo(order(other)).sign
            content(s, if (entering) fastOutSlowIn(raw) else 1f - fastOutSlowIn(1f - raw), side)
        }
    }
}

@Composable
fun FadingEdges(scroll: ScrollState, color: PolyColor, modifier: Modifier = Modifier.fillMaxSize(), size: Float = 32f) {
    val anim = remember { FloatArray(4) }
    val last = remember { LongArray(1) }
    Canvas(modifier, live = true) { x, y, w, h ->
        val now = System.nanoTime()
        val dt = if (last[0] == 0L) -1f else ((now - last[0]) / 1e9f).coerceIn(0f, 0.05f)
        last[0] = now
        for ((i, target) in floatArrayOf(if (scroll.value > 0.5f) 1f else 0f, if (scroll.value < scroll.maxValue - 0.5f) 1f else 0f).withIndex()) {
            if (dt < 0f) { anim[i * 2] = target; continue }
            val v = springStep(anim[i * 2] - target, anim[i * 2 + 1], sqrt(1500f), dt)
            val settled = abs(v[0]) < 0.01f && abs(v[1]) < 0.01f
            anim[i * 2] = if (settled) target else target + v[0]
            anim[i * 2 + 1] = if (settled) 0f else v[1]
        }
        val top = anim[0].coerceIn(0f, 1f)
        val bottom = anim[2].coerceIn(0f, 1f)
        val f = min(size, h / 2f)
        val clear = color.withAlpha(0)
        if (top > 0f) rectGradient(x, y, w, f, Brush.verticalGradient(color.multiplyAlpha(top), clear))
        if (bottom > 0f) rectGradient(x, y + h - f, w, f, Brush.verticalGradient(clear, color.multiplyAlpha(bottom)))
    }
}

fun scrollKeys(state: ScrollState): (Int, Int) -> Boolean = { key, _ ->
    when (key) {
        KeyEvent.KEY_UP -> { state.animateBy(-40f); true }
        KeyEvent.KEY_DOWN -> { state.animateBy(40f); true }
        KeyEvent.KEY_PAGE_UP -> { state.animateBy(-state.viewport); true }
        KeyEvent.KEY_PAGE_DOWN -> { state.animateBy(state.viewport); true }
        KeyEvent.KEY_HOME -> { state.animateBy(-state.value - state.maxValue); true }
        KeyEvent.KEY_END -> { state.animateBy(state.maxValue); true }
        else -> false
    }
}

@Composable
fun PolyWindow(onClose: () -> Unit, content: @Composable () -> Unit) {
    val theme = LocalTheme.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val window = theme.backgroundShape
        Box(Modifier.size(WINDOW_WIDTH.dp, WINDOW_HEIGHT.dp).background(theme.pageBackground, window).border(1.dp, theme.borderColor, window).clip(window)) {
            content()
            IconButton("close", Modifier.absoluteOffset((WINDOW_WIDTH - 19f - 32f).dp, 19.dp).size(32.dp), hoverBorder = true, onClick = onClose)
        }
    }
}
