@file:OptIn(InternalComposeUiApi::class, ExperimentalTextApi::class)

package org.polyfrost.compose.backend

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.FontRasterizationSettings
import androidx.compose.ui.text.FontSmoothing
import androidx.compose.ui.text.FontHinting
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlatformParagraph
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextGranularity
import androidx.compose.ui.text.TextInclusionStrategy
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.PlatformTypefacesLoader
import androidx.compose.ui.text.font.createPlatformFontFamilyResolver
import androidx.compose.ui.text.platform.PlatformText
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.PolyRenderer
import java.text.Bidi
import java.text.BreakIterator
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class PolyFontSource(val key: String, private val bytes: () -> ByteArray, override val weight: FontWeight, override val style: FontStyle) : Font {
    internal fun register(): String {
        if (FontManager.fontData(key) == null) FontManager.register(key, bytes())
        return key
    }

    override fun equals(other: Any?) = other is PolyFontSource && other.key == key && other.weight == weight && other.style == style
    override fun hashCode() = (key.hashCode() * 31 + weight.hashCode()) * 31 + style.hashCode()
    override fun toString() = "PolyFontSource($key, $weight, $style)"
}

object PolyText : PlatformText {
    val typefaces = object : PlatformTypefacesLoader {
        override fun loadBlocking(font: Font): Any? = (font as? PolyFontSource)?.register()
        override suspend fun awaitLoad(font: Font): Any? = loadBlocking(font)
        override val cacheKey: Any? = null
        override fun loadPlatformTypes(fontFamily: FontFamily, fontWeight: FontWeight, fontStyle: FontStyle): Any = FontManager.DEFAULT_KEY
        override val fontCollection: Any = Unit
    }

    override fun createParagraph(
        text: String, style: TextStyle, annotations: List<AnnotatedString.Range<out AnnotatedString.Annotation>>,
        placeholders: List<AnnotatedString.Range<Placeholder>>, maxLines: Int, overflow: TextOverflow, constraints: Constraints,
        density: Density, fontFamilyResolver: FontFamily.Resolver,
    ): PlatformParagraph = createParagraph(createParagraphIntrinsics(text, style, annotations, placeholders, density, fontFamilyResolver), maxLines, overflow, constraints)

    override fun createParagraph(paragraphIntrinsics: ParagraphIntrinsics, maxLines: Int, overflow: TextOverflow, constraints: Constraints): PlatformParagraph =
        PolyParagraph(paragraphIntrinsics as PolyIntrinsics, maxLines, overflow, if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() else Float.POSITIVE_INFINITY)

    override fun createParagraphIntrinsics(
        text: String, style: TextStyle, annotations: List<AnnotatedString.Range<out AnnotatedString.Annotation>>,
        placeholders: List<AnnotatedString.Range<Placeholder>>, density: Density, fontFamilyResolver: FontFamily.Resolver,
    ): ParagraphIntrinsics = PolyIntrinsics(text, style, annotations, placeholders.size, density, fontFamilyResolver)

    override fun createFontFamilyResolver(): FontFamily.Resolver = createPlatformFontFamilyResolver(typefaces)
    override fun createFontFamilyResolver(coroutineContext: CoroutineContext): FontFamily.Resolver = createPlatformFontFamilyResolver(typefaces, coroutineContext)

    override fun findPrecedingBreak(text: String, index: Int): Int =
        BreakIterator.getCharacterInstance().also { it.setText(text) }.preceding(index).let { if (it == BreakIterator.DONE) 0 else it }

    override fun findFollowingBreak(text: String, index: Int): Int =
        BreakIterator.getCharacterInstance().also { it.setText(text) }.following(index).let { if (it == BreakIterator.DONE) text.length else it }

    override val defaultFontRasterizationSettings = FontRasterizationSettings(FontSmoothing.AntiAlias, FontHinting.Slight, subpixelPositioning = true, autoHintingForced = false)
}

internal class TextRun(
    val start: Int, val end: Int, val font: PolyFont, val style: SpanStyle,
    val spanColor: Color = Color.Unspecified, val spanDecoration: TextDecoration? = null, val spanShadow: Shadow? = null,
) {
    val metrics get() = font.metrics
}

internal class PolyIntrinsics(
    val text: String, val style: TextStyle, annotations: List<AnnotatedString.Range<out AnnotatedString.Annotation>>,
    val placeholderCount: Int, val density: Density, resolver: FontFamily.Resolver,
) : ParagraphIntrinsics {
    val runs: List<TextRun>
    val advances = FloatArray(text.length)
    val lineHeightPx: Float
    val align: TextAlign = style.textAlign

    init {
        val base = style.toSpanStyle()
        val spans = annotations.mapNotNull { r -> (r.item as? SpanStyle)?.let { AnnotatedString.Range(it, r.start.coerceIn(0, text.length), r.end.coerceIn(0, text.length)) } }
        val cuts = sortedSetOf(0, text.length)
        for (s in spans) { cuts += s.start; cuts += s.end }
        val bounds = cuts.toList()
        val fontCache = HashMap<Pair<String, Float>, PolyFont>()
        runs = ArrayList()
        for (i in 0 until bounds.size - 1) {
            val a = bounds[i]
            val b = bounds[i + 1]
            if (a >= b) continue
            var s = base
            var spanColor = Color.Unspecified
            var spanDecoration: TextDecoration? = null
            var spanShadow: Shadow? = null
            for (sp in spans) if (sp.start <= a && sp.end >= b) {
                s = s.merge(sp.item)
                if (sp.item.color.isSpecified) spanColor = sp.item.color
                sp.item.textDecoration?.let { spanDecoration = it }
                sp.item.shadow?.let { spanShadow = it }
            }
            val name = resolver.resolve(s.fontFamily, s.fontWeight ?: FontWeight.Normal, s.fontStyle ?: FontStyle.Normal, s.fontSynthesis ?: FontSynthesis.All).value as? String
                ?: FontManager.DEFAULT_KEY
            val size = px(s.fontSize, 14f)
            runs += TextRun(a, b, fontCache.getOrPut(name to size) { PolyFont(name, size) }, s, spanColor, spanDecoration, spanShadow)
        }
        if (runs.isEmpty()) {
            val name = resolver.resolve(base.fontFamily, base.fontWeight ?: FontWeight.Normal, base.fontStyle ?: FontStyle.Normal).value as? String ?: FontManager.DEFAULT_KEY
            runs += TextRun(0, 0, PolyFont(name, px(base.fontSize, 14f)), base)
        }
        for (run in runs) {
            val letter = px(run.style.letterSpacing, 0f)
            var i = run.start
            while (i < run.end) {
                val cp = text.codePointAt(i)
                val n = Character.charCount(cp)
                advances[i] = if (cp == '\n'.code) 0f else Advances.of(run.font, cp) + letter
                i += n
            }
        }
        val natural = runs.maxOf { it.metrics.spacing }
        lineHeightPx = if (style.lineHeight.isSpecified) px(style.lineHeight, natural) else round(natural)
    }

    fun px(unit: TextUnit, fallback: Float): Float = if (!unit.isSpecified) fallback else with(density) {
        if (unit.isEm) unit.value * px(style.fontSize, 14f) else unit.toPx()
    }

    fun runAt(offset: Int): TextRun = runs.firstOrNull { offset < it.end } ?: runs.last()

    fun width(start: Int, end: Int): Float { var w = 0f; for (i in start until end) w += advances[i]; return w }

    override val maxIntrinsicWidth: Float by lazy {
        var best = 0f; var cur = 0f
        for (i in text.indices) { if (text[i] == '\n') { best = max(best, cur); cur = 0f } else cur += advances[i] }
        max(best, cur)
    }

    override val minIntrinsicWidth: Float by lazy {
        val it = BreakIterator.getLineInstance().also { b -> b.setText(text) }
        var best = 0f
        var start = it.first()
        var end = it.next()
        while (end != BreakIterator.DONE) {
            var e = end
            while (e > start && text[e - 1].isWhitespace()) e--
            best = max(best, width(start, e))
            start = end
            end = it.next()
        }
        best
    }

    override val hasStaleResolvedFonts get() = false
}

private object Advances {
    private val cache = ConcurrentHashMap<PolyFont, ConcurrentHashMap<Int, Float>>()

    fun of(font: PolyFont, cp: Int): Float =
        cache.getOrPut(font) { ConcurrentHashMap() }.getOrPut(cp) { font.measureTextWidth(String(Character.toChars(cp))) }
}

internal class Seg(val str: String, val x: Float, val w: Float, val run: TextRun)

internal class Line(
    val start: Int, val end: Int, val visibleEnd: Int, val top: Float, val bottom: Float, val baseline: Float, val left: Float, val width: Float, val ellipsis: Boolean,
    val rtl: Boolean = false, val runs: IntArray? = null, val runX: FloatArray? = null, val segs: Array<Seg>? = null,
)

internal class PolyParagraph(private val intr: PolyIntrinsics, maxLines: Int, overflow: TextOverflow, private val maxWidth: Float) : PlatformParagraph {
    private val text = intr.text
    private val lines: List<Line>
    private val levels: ByteArray?
    override val didExceedMaxLines: Boolean
    override val width: Float
    override val height: Float

    init {
        val breaks = layout()
        val limit = if (maxLines <= 0) Int.MAX_VALUE else maxLines
        didExceedMaxLines = breaks.size > limit
        val kept = breaks.take(limit)
        val lh = intr.lineHeightPx
        val ascent = -intr.runs.minOf { it.metrics.ascent }
        val content = ascent + intr.runs.maxOf { it.metrics.descent }
        val boxW = if (maxWidth.isFinite()) maxWidth else kept.maxOfOrNull { intr.width(it.first, it.third) } ?: 0f
        val flags = when (intr.style.textDirection) {
            TextDirection.Ltr -> Bidi.DIRECTION_LEFT_TO_RIGHT
            TextDirection.Rtl -> Bidi.DIRECTION_RIGHT_TO_LEFT
            TextDirection.ContentOrRtl -> Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT
            else -> Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT
        }
        val forcedRtl = flags == Bidi.DIRECTION_RIGHT_TO_LEFT || flags == Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT
        val lv = if (!forcedRtl && text.none { it >= '֐' }) null else ByteArray(text.length)
        var para: Bidi? = null
        var paraStart = 0
        var paraEnd = 0
        lines = kept.mapIndexed { i, (s, e, ve) ->
            val ellipsize = didExceedMaxLines && i == kept.lastIndex && overflow == TextOverflow.Ellipsis
            val vEnd = if (ellipsize) ellipsisEnd(s, ve) else ve
            val ew = if (ellipsize) ellipsisWidth(vEnd) else 0f
            val w = intr.width(s, vEnd) + ew
            val top = i * lh
            if (lv != null && (para == null || s > paraEnd)) {
                paraStart = text.lastIndexOf('\n', s - 1) + 1
                paraEnd = text.indexOf('\n', s).let { if (it < 0) text.length else it }
                para = Bidi(text.substring(paraStart, paraEnd), flags).also { lv.fill(it.baseLevel.toByte(), paraStart, min(paraEnd + 1, text.length)) }
            }
            val b = para
            val rtl = b != null && !b.baseIsLeftToRight()
            val left = when (intr.align) {
                TextAlign.Center -> (boxW - w) / 2f
                TextAlign.Right -> boxW - w
                TextAlign.End -> if (rtl) 0f else boxW - w
                TextAlign.Left -> 0f
                else -> if (rtl) boxW - w else 0f
            }
            val baseline = if (intr.style.lineHeight.isSpecified) top + (lh - content) / 2f + ascent else top + lh - (content - ascent)
            val line = Line(s, e, vEnd, top, top + lh, baseline, max(0f, left), w, ellipsize, rtl)
            if (b == null || lv == null) line else bidiLine(line, b, paraStart, if (ellipsize) vEnd else min(e, paraEnd), ew, lv)
        }
        levels = lv
        width = if (maxWidth.isFinite()) maxWidth else lines.maxOfOrNull { it.width } ?: 0f
        height = lines.lastOrNull()?.bottom ?: lh
    }

    private fun bidiLine(l: Line, para: Bidi, paraStart: Int, limit: Int, ellipsisW: Float, levels: ByteArray): Line {
        val s = l.start
        val line = if (limit > s) para.createLineBidi(s - paraStart, limit - paraStart) else null
        if (!l.rtl && (line == null || line.isLeftToRight)) return l
        if (line == null) return Line(s, l.end, l.visibleEnd, l.top, l.bottom, l.baseline, l.left, l.width, l.ellipsis, true, IntArray(0), FloatArray(0), emptyArray())
        val n = line.runCount
        val order = Array<Any>(n) { it }
        Bidi.reorderVisually(ByteArray(n) { line.getRunLevel(it).toByte() }, 0, order, 0, n)
        val runs = IntArray(n * 3)
        val runX = FloatArray(n)
        val segs = ArrayList<Seg>()
        var x = if (l.rtl) l.left + ellipsisW - intr.width(l.visibleEnd, limit) else l.left
        for (v in 0 until n) {
            val k = order[v] as Int
            val rs = s + line.getRunStart(k)
            val re = s + line.getRunLimit(k)
            val level = line.getRunLevel(k)
            levels.fill(level.toByte(), rs, re)
            runs[v * 3] = rs
            runs[v * 3 + 1] = re
            runs[v * 3 + 2] = level
            runX[v] = x
            for (run in intr.runs) {
                val a = max(run.start, rs)
                val e = min(min(run.end, re), l.visibleEnd)
                if (a >= e) continue
                segs += if (level and 1 == 0) Seg(text.substring(a, e), x + intr.width(rs, a), intr.width(a, e), run)
                else Seg(StringBuilder(e - a).append(text, a, e).reverse().toString(), x + intr.width(e, re), intr.width(a, e), run)
            }
            x += intr.width(rs, re)
        }
        return Line(s, l.end, l.visibleEnd, l.top, l.bottom, l.baseline, l.left, l.width, l.ellipsis, l.rtl, runs, runX, segs.toTypedArray())
    }

    private fun layout(): List<Triple<Int, Int, Int>> {
        val out = ArrayList<Triple<Int, Int, Int>>()
        val it = BreakIterator.getLineInstance().also { b -> b.setText(text) }
        var lineStart = 0
        var lineW = 0f
        var pos = it.first()
        var next = it.next()
        fun trimmedEnd(s: Int, e: Int): Int { var v = e; while (v > s && text[v - 1].isWhitespace()) v--; return v }
        while (next != BreakIterator.DONE) {
            val segW = intr.width(pos, trimmedEnd(pos, next))
            val segFull = intr.width(pos, next)
            if (lineW + segW > maxWidth + 0.01f && pos > lineStart) {
                out += Triple(lineStart, pos, trimmedEnd(lineStart, pos))
                lineStart = pos
                lineW = 0f
            }
            if (segW > maxWidth && pos == lineStart) {
                var e = pos
                var w = 0f
                while (e < next) {
                    if (w + intr.advances[e] > maxWidth && e > lineStart) {
                        out += Triple(lineStart, e, e)
                        lineStart = e
                        w = 0f
                    }
                    w += intr.advances[e]
                    e++
                }
                lineW = w
            } else lineW += segFull
            if (next > 0 && text[next - 1] == '\n') {
                out += Triple(lineStart, next, next - 1)
                lineStart = next
                lineW = 0f
            }
            pos = next
            next = it.next()
        }
        if (lineStart < text.length || out.isEmpty() || text.endsWith('\n')) out += Triple(lineStart, text.length, trimmedEnd(lineStart, text.length))
        return out
    }

    private fun ellipsisWidth(at: Int) = intr.runAt(max(0, at - 1)).font.measureTextWidth(ELLIPSIS)

    private fun ellipsisEnd(start: Int, end: Int): Int {
        if (!maxWidth.isFinite()) return end
        var e = end
        while (e > start && intr.width(start, e) + ellipsisWidth(e) > maxWidth) e--
        return e
    }

    private fun editEnd(l: Line) = when {
        l.ellipsis -> l.visibleEnd
        l.end > l.start && text[l.end - 1] == '\n' -> l.end - 1
        else -> l.end
    }

    private fun lineFor(offset: Int): Int {
        for (i in lines.indices) if (offset < lines[i].end) return i
        return lines.lastIndex.coerceAtLeast(0)
    }

    override val minIntrinsicWidth get() = intr.minIntrinsicWidth
    override val maxIntrinsicWidth get() = intr.maxIntrinsicWidth
    override val firstBaseline get() = lines.firstOrNull()?.baseline ?: 0f
    override val lastBaseline get() = lines.lastOrNull()?.baseline ?: 0f
    override val lineCount get() = lines.size
    override val placeholderRects: List<Rect?> get() = List(intr.placeholderCount) { null }

    private val ascent get() = -intr.runs.minOf { it.metrics.ascent }
    private val descent get() = intr.runs.maxOf { it.metrics.descent }

    override fun getLineAscent(lineIndex: Int) = -ascent
    override fun getLineDescent(lineIndex: Int) = descent
    override fun getLineLeft(lineIndex: Int) = lines[lineIndex].left
    override fun getLineRight(lineIndex: Int) = lines[lineIndex].left + lines[lineIndex].width
    override fun getLineTop(lineIndex: Int) = lines[lineIndex].top
    override fun getLineBaseline(lineIndex: Int) = lines[lineIndex].baseline
    override fun getLineBottom(lineIndex: Int) = lines[lineIndex].bottom
    override fun getLineHeight(lineIndex: Int) = lines[lineIndex].bottom - lines[lineIndex].top
    override fun getLineWidth(lineIndex: Int) = lines[lineIndex].width
    override fun getLineStart(lineIndex: Int) = lines[lineIndex].start
    override fun getLineEnd(lineIndex: Int, visibleEnd: Boolean) = if (visibleEnd) lines[lineIndex].visibleEnd else lines[lineIndex].end
    override fun isLineEllipsized(lineIndex: Int) = lines[lineIndex].ellipsis
    override fun getLineForOffset(offset: Int) = lineFor(offset)

    override fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float {
        if (lines.isEmpty()) return 0f
        val l = lines[lineFor(offset)]
        if (l.runs != null) return bidiX(l, offset.coerceIn(l.start, editEnd(l)), usePrimaryDirection)
        return l.left + intr.width(l.start, offset.coerceIn(l.start, editEnd(l)))
    }

    private fun runIndex(runs: IntArray, i: Int): Int {
        var k = 0
        while (k < runs.size) {
            if (i >= runs[k] && i < runs[k + 1]) return k
            k += 3
        }
        return -1
    }

    private fun edge(l: Line, k: Int, offset: Int): Float {
        val runs = l.runs!!
        val x = l.runX!![k / 3]
        return if (runs[k + 2] and 1 == 0) x + intr.width(runs[k], offset) else x + intr.width(offset, runs[k + 1])
    }

    private fun bidiX(l: Line, offset: Int, primary: Boolean): Float {
        val runs = l.runs!!
        val runX = l.runX!!
        if (runs.isEmpty()) return if (l.rtl) l.left + l.width else l.left
        val visLeft = runX[0]
        val visRight = runX[runX.size - 1] + intr.width(runs[runs.size - 3], runs[runs.size - 2])
        val paraLevel = if (l.rtl) 1 else 0
        val before = if (offset > l.start) runIndex(runs, offset - 1) else -1
        val at = runIndex(runs, offset)
        val levelBefore = if (before < 0) paraLevel else runs[before + 2]
        val levelAt = if (at < 0) paraLevel else runs[at + 2]
        val trailing = (levelBefore < levelAt) == primary
        return when {
            trailing && before >= 0 -> edge(l, before, offset)
            !trailing && at >= 0 -> edge(l, at, offset)
            trailing -> if (l.rtl) visRight else visLeft
            else -> if (l.rtl) visLeft else visRight
        }
    }

    override fun getCursorRect(offset: Int): Rect {
        val x = getHorizontalPosition(offset, true)
        val l = lines.getOrNull(lineFor(offset)) ?: return Rect(x, 0f, x, height)
        return Rect(x, l.top, x, l.bottom)
    }

    override fun getParagraphDirection(offset: Int) =
        if (lines.isNotEmpty() && lines[lineFor(offset)].rtl) ResolvedTextDirection.Rtl else ResolvedTextDirection.Ltr

    override fun getBidiRunDirection(offset: Int): ResolvedTextDirection {
        val lv = levels ?: return ResolvedTextDirection.Ltr
        if (lv.isEmpty()) return getParagraphDirection(offset)
        return if (lv[offset.coerceIn(0, lv.size - 1)].toInt() and 1 == 0) ResolvedTextDirection.Ltr else ResolvedTextDirection.Rtl
    }

    override fun getLineForVerticalPosition(vertical: Float): Int {
        for (i in lines.indices) if (vertical < lines[i].bottom) return i
        return lines.lastIndex.coerceAtLeast(0)
    }

    override fun getOffsetForPosition(position: Offset): Int {
        if (lines.isEmpty()) return 0
        val l = lines[getLineForVerticalPosition(position.y)]
        var x = l.left
        val end = editEnd(l)
        if (l.runs != null) {
            val hi = if (l === lines.last() || end < l.end) end else max(l.start, end - 1)
            var best = l.start
            var bestD = Float.POSITIVE_INFINITY
            for (o in l.start..hi) {
                if (o > l.start && o < text.length && Character.isLowSurrogate(text[o]) && Character.isHighSurrogate(text[o - 1])) continue
                val d = abs(bidiX(l, o, true) - position.x)
                if (d < bestD) { bestD = d; best = o }
            }
            return best
        }
        for (i in l.start until end) {
            val a = intr.advances[i]
            if (a == 0f) continue
            if (position.x < x + a / 2f) return i
            x += a
        }
        return if (l === lines.last() || end < l.end) end else max(l.start, end - 1)
    }

    override fun getRangeForRect(rect: Rect, granularity: TextGranularity, inclusionStrategy: TextInclusionStrategy): TextRange {
        val a = getOffsetForPosition(rect.topLeft)
        val b = getOffsetForPosition(rect.bottomRight)
        return if (a < b) TextRange(a, b) else TextRange.Zero
    }

    override fun getBoundingBox(offset: Int): Rect {
        val l = lines.getOrNull(lineFor(offset)) ?: return Rect.Zero
        val runs = l.runs
        val k = if (runs != null) runIndex(runs, offset) else -1
        if (k >= 0) {
            val a = edge(l, k, offset)
            val b = edge(l, k, offset + 1)
            return Rect(min(a, b), l.top, max(a, b), l.bottom)
        }
        val x = getHorizontalPosition(offset, true)
        return Rect(x, l.top, x + (intr.advances.getOrNull(offset) ?: 0f), l.bottom)
    }

    override fun fillBoundingBoxes(range: TextRange, array: FloatArray, arrayStart: Int) {
        var k = arrayStart
        for (i in range.min until range.max) {
            val b = getBoundingBox(i)
            array[k++] = b.left; array[k++] = b.top; array[k++] = b.right; array[k++] = b.bottom
        }
    }

    override fun getWordBoundary(offset: Int): TextRange {
        val it = BreakIterator.getWordInstance().also { b -> b.setText(text) }
        val o = offset.coerceIn(0, text.length)
        val end = if (it.isBoundary(o) && o < text.length) it.following(o) else it.following(o.coerceAtMost(max(0, text.length - 1)))
        val start = it.preceding(if (end == BreakIterator.DONE) text.length else end)
        return TextRange(if (start == BreakIterator.DONE) 0 else start, if (end == BreakIterator.DONE) text.length else end)
    }

    override fun getPathForRange(start: Int, end: Int): Path {
        val p = PolyComposePath()
        for (l in lines) {
            val runs = l.runs
            if (runs != null) {
                var k = 0
                while (k < runs.size) {
                    val a = max(start, runs[k])
                    val b = min(end, runs[k + 1])
                    if (a < b) {
                        val x0 = edge(l, k, a)
                        val x1 = edge(l, k, b)
                        p.addRect(Rect(min(x0, x1), l.top, max(x0, x1), l.bottom))
                    }
                    k += 3
                }
                continue
            }
            val s = max(start, l.start)
            val e = min(end, l.visibleEnd)
            if (s >= e && !(start < l.end && end > l.visibleEnd && s <= l.visibleEnd)) continue
            // measured from this line's start because a wrapped line's end offset resolves to the next line
            val x0 = l.left + intr.width(l.start, s)
            val x1 = if (end > l.visibleEnd && l.end > l.visibleEnd) l.left + intr.width(l.start, l.visibleEnd) + intr.advances.getOrElse(l.visibleEnd) { 0f } else l.left + intr.width(l.start, e)
            p.addRect(Rect(x0, l.top, max(x1, x0), l.bottom))
        }
        return p
    }

    override fun paint(canvas: Canvas, color: Color, shadow: Shadow?, textDecoration: TextDecoration?) =
        draw(canvas, if (color.isSpecified) color else null, 1f, shadow, textDecoration)

    override fun paint(canvas: Canvas, color: Color, shadow: Shadow?, textDecoration: TextDecoration?, drawStyle: DrawStyle?, blendMode: BlendMode) =
        draw(canvas, if (color.isSpecified) color else null, 1f, shadow, textDecoration)

    override fun paint(canvas: Canvas, brush: Brush, alpha: Float, shadow: Shadow?, textDecoration: TextDecoration?, drawStyle: DrawStyle?, blendMode: BlendMode) =
        draw(canvas, (brush as? SolidColor)?.value, if (alpha.isNaN()) 1f else alpha, shadow, textDecoration)

    private fun draw(canvas: Canvas, color: Color?, alpha: Float, shadow: Shadow?, decoration: TextDecoration?) {
        (canvas as? PolyDrawCanvas)?.polyOp { pc ->
            val r = pc.renderer
            for (l in lines) {
                var x = l.left
                val segs = l.segs
                if (segs != null) {
                    for (sg in segs) {
                        drawRun(r, l, sg.run, sg.str, sg.x, sg.w, color, alpha, shadow, decoration)
                        x = max(x, sg.x + sg.w)
                    }
                    if (l.rtl) x = l.left
                } else for (run in intr.runs) {
                    val s = max(run.start, l.start)
                    val e = min(run.end, l.visibleEnd)
                    if (s >= e) continue
                    val w = intr.width(s, e)
                    drawRun(r, l, run, text.substring(s, e), x, w, color, alpha, shadow, decoration)
                    x += w
                }
                if (l.ellipsis) {
                    val run = intr.runAt(max(0, l.visibleEnd - 1))
                    val base = run.spanColor.takeIf { it.isSpecified } ?: color ?: run.style.color.takeIf { it.isSpecified } ?: Color.Black
                    r.text(ELLIPSIS, x, l.baseline, base.copy(alpha = base.alpha * alpha).toArgb(), run.font)
                }
            }
        }
    }

    private fun drawRun(r: PolyRenderer, l: Line, run: TextRun, str: String, x: Float, w: Float, color: Color?, alpha: Float, shadow: Shadow?, decoration: TextDecoration?) {
        val base = run.spanColor.takeIf { it.isSpecified } ?: color ?: run.style.color.takeIf { it.isSpecified } ?: Color.Black
        val c = base.copy(alpha = base.alpha * alpha * (run.style.alpha.takeUnless { it.isNaN() } ?: 1f)).toArgb()
        if (run.style.background.isSpecified && run.style.background.alpha > 0f) r.rect(x, l.top, w, l.bottom - l.top, run.style.background.toArgb(), 0f)
        val sh = run.spanShadow ?: shadow ?: run.style.shadow
        val letter = intr.px(run.style.letterSpacing, 0f)
        val skew = run.style.textGeometricTransform?.skewX ?: 0f
        if (sh != null && sh != Shadow.None && sh.color.alpha > 0f) r.text(str, x + sh.offset.x, l.baseline + sh.offset.y, sh.color.toArgb(), run.font, letter, skew)
        r.text(str, x, l.baseline, c, run.font, letter, skew)
        val deco = run.spanDecoration ?: decoration ?: run.style.textDecoration
        if (deco != null) {
            val m = run.metrics
            val t = max(1f, m.underlineThickness)
            if (TextDecoration.Underline in deco) r.rect(x, l.baseline + m.underlinePosition, w, t, c, 0f)
            if (TextDecoration.LineThrough in deco) r.rect(x, l.baseline + m.strikeoutPosition, w, max(1f, m.strikeoutThickness), c, 0f)
        }
    }

    private companion object {
        const val ELLIPSIS = "…"
    }
}
