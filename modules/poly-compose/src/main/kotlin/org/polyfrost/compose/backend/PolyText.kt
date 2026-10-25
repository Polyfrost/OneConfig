package org.polyfrost.compose.backend

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyFont
import java.text.BreakIterator
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlin.math.min

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

internal class TextRun(val start: Int, val end: Int, val font: PolyFont, val style: SpanStyle) {
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
            for (sp in spans) if (sp.start <= a && sp.end >= b) s = s.merge(sp.item)
            val name = resolver.resolve(s.fontFamily, s.fontWeight ?: FontWeight.Normal, s.fontStyle ?: FontStyle.Normal, s.fontSynthesis ?: FontSynthesis.All).value as? String
                ?: FontManager.DEFAULT_KEY
            val size = px(s.fontSize, 14f)
            runs += TextRun(a, b, fontCache.getOrPut(name to size) { PolyFont(name, size) }, s)
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
        lineHeightPx = if (style.lineHeight.isSpecified) px(style.lineHeight, natural) else natural
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

internal class Line(val start: Int, val end: Int, val visibleEnd: Int, val top: Float, val bottom: Float, val baseline: Float, val left: Float, val width: Float, val ellipsis: Boolean)

internal class PolyParagraph(private val intr: PolyIntrinsics, maxLines: Int, overflow: TextOverflow, private val maxWidth: Float) : PlatformParagraph {
    private val text = intr.text
    private val lines: List<Line>
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
        lines = kept.mapIndexed { i, (s, e, ve) ->
            val ellipsize = didExceedMaxLines && i == kept.lastIndex && overflow == TextOverflow.Ellipsis
            val vEnd = if (ellipsize) ellipsisEnd(s, ve) else ve
            val w = intr.width(s, vEnd) + if (ellipsize) ellipsisWidth(vEnd) else 0f
            val top = i * lh
            val left = when (intr.align) {
                TextAlign.Center -> (boxW - w) / 2f
                TextAlign.End, TextAlign.Right -> boxW - w
                else -> 0f
            }
            Line(s, e, vEnd, top, top + lh, top + (lh - content) / 2f + ascent, max(0f, left), w, ellipsize)
        }
        width = if (maxWidth.isFinite()) maxWidth else lines.maxOfOrNull { it.width } ?: 0f
        height = lines.lastOrNull()?.bottom ?: lh
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
        return l.left + intr.width(l.start, offset.coerceIn(l.start, l.visibleEnd))
    }

    override fun getCursorRect(offset: Int): Rect {
        val x = getHorizontalPosition(offset, true)
        val l = lines.getOrNull(lineFor(offset)) ?: return Rect(x, 0f, x, height)
        return Rect(x, l.top, x, l.bottom)
    }

    override fun getParagraphDirection(offset: Int) = ResolvedTextDirection.Ltr
    override fun getBidiRunDirection(offset: Int) = ResolvedTextDirection.Ltr

    override fun getLineForVerticalPosition(vertical: Float): Int {
        for (i in lines.indices) if (vertical < lines[i].bottom) return i
        return lines.lastIndex.coerceAtLeast(0)
    }

    override fun getOffsetForPosition(position: Offset): Int {
        if (lines.isEmpty()) return 0
        val l = lines[getLineForVerticalPosition(position.y)]
        var x = l.left
        for (i in l.start until l.visibleEnd) {
            val a = intr.advances[i]
            if (a == 0f) continue
            if (position.x < x + a / 2f) return i
            x += a
        }
        return l.visibleEnd
    }

    override fun getRangeForRect(rect: Rect, granularity: TextGranularity, inclusionStrategy: TextInclusionStrategy): TextRange {
        val a = getOffsetForPosition(rect.topLeft)
        val b = getOffsetForPosition(rect.bottomRight)
        return if (a < b) TextRange(a, b) else TextRange.Zero
    }

    override fun getBoundingBox(offset: Int): Rect {
        val l = lines.getOrNull(lineFor(offset)) ?: return Rect.Zero
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
            val s = max(start, l.start)
            val e = min(end, l.visibleEnd)
            if (s >= e && !(start < l.end && end > l.visibleEnd && s <= l.visibleEnd)) continue
            val x0 = getHorizontalPosition(s, true)
            val x1 = if (end > l.visibleEnd && l.end > l.visibleEnd) getHorizontalPosition(l.visibleEnd, true) + intr.advances.getOrElse(l.visibleEnd) { 0f } else getHorizontalPosition(e, true)
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
                for (run in intr.runs) {
                    val s = max(run.start, l.start)
                    val e = min(run.end, l.visibleEnd)
                    if (s >= e) continue
                    val str = text.substring(s, e)
                    val base = color ?: run.style.color.takeIf { it.isSpecified } ?: Color.Black
                    val c = base.copy(alpha = base.alpha * alpha * (run.style.alpha.takeUnless { it.isNaN() } ?: 1f)).toArgb()
                    val w = intr.width(s, e)
                    if (run.style.background.isSpecified && run.style.background.alpha > 0f) r.rect(x, l.top, w, l.bottom - l.top, run.style.background.toArgb(), 0f)
                    val sh = shadow ?: run.style.shadow
                    if (sh != null && sh != Shadow.None && sh.color.alpha > 0f) r.text(str, x + sh.offset.x, l.baseline + sh.offset.y, sh.color.toArgb(), run.font)
                    r.text(str, x, l.baseline, c, run.font)
                    val deco = decoration ?: run.style.textDecoration
                    if (deco != null) {
                        val m = run.metrics
                        val t = max(1f, m.underlineThickness)
                        if (TextDecoration.Underline in deco) r.rect(x, l.baseline + m.underlinePosition, w, t, c, 0f)
                        if (TextDecoration.LineThrough in deco) r.rect(x, l.baseline - m.strikeoutPosition, w, max(1f, m.strikeoutThickness), c, 0f)
                    }
                    x += w
                }
                if (l.ellipsis) {
                    val run = intr.runAt(max(0, l.visibleEnd - 1))
                    val base = color ?: run.style.color.takeIf { it.isSpecified } ?: Color.Black
                    r.text(ELLIPSIS, x, l.baseline, base.copy(alpha = base.alpha * alpha).toArgb(), run.font)
                }
            }
        }
    }

    private companion object {
        const val ELLIPSIS = "…"
    }
}
