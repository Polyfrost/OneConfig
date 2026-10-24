package org.polyfrost.compose.node

import org.polyfrost.compose.layout.PolyLayoutEngine
import org.polyfrost.compose.layout.PolySize
import kotlin.math.ceil
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.RenderContext

class PolyTextSpan(
    val text: String,
    val color: PolyColor? = null,
    val font: String? = null,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val background: PolyColor? = null,
    val size: Float? = null,
)

class TextNode : PolyNode() {
    var text: String = ""
    var color: PolyColor = PolyColor.WHITE

    var fontSize: Float = 12f
    var shadow: Boolean = false
    var shadowColor: PolyColor = PolyColor(0x80000000.toInt())
    var shadowOffset: Float = 1f

    var fontName: String? = null

    var leading: Boolean = false

    var spans: List<PolyTextSpan>? = null

    var wrap: Boolean = false
    var maxLines: Int = Int.MAX_VALUE

    var ellipsis: Boolean = false

    var lineHeight: Float? = null

    var center: Boolean = false

    var baselineShift: Float = 0f

    private class Run(val text: String, val x: Float, val span: PolyTextSpan?)
    private class Line(val runs: List<Run>, val width: Float)

    private var lines: List<Line>? = null
    private var linesFor = Float.NaN

    private fun font() = fontName?.let { PolyFont(it, fontSize) } ?: PolyFont.default(fontSize)
    private fun font(span: PolyTextSpan?) = if (span?.font == null && span?.size == null) font()
        else (span.font ?: fontName)?.let { PolyFont(it, span.size ?: fontSize) } ?: PolyFont.default(span.size ?: fontSize)

    private val flowing get() = wrap || ellipsis || spans != null || center

    fun remeasure() {
        lines = null
        if (flowing) return
        val font = font()
        style.width = PolySize.Px(font.measureTextWidth(text))
        style.height = PolySize.Px(lineHeight(font))
    }

    private fun naturalHeight(font: PolyFont) = font.metrics.let { it.descent - it.ascent + if (leading) it.leading else 0f }
    private fun lineHeight(font: PolyFont) = lineHeight ?: naturalHeight(font)

    private fun baseline(font: PolyFont, i: Int): Float {
        val m = font.metrics
        val lh = lineHeight(font)
        return i * lh + (lh - naturalHeight(font)) / 2f - m.ascent - baselineShift * fontSize + if (leading) m.leading / 2f else 0f
    }

    override fun measureContent(maxWidth: Float, maxHeight: Float) {
        if (!flowing) return
        val ls = lines?.takeIf { linesFor == maxWidth } ?: layoutLines(maxWidth).also { lines = it; linesFor = maxWidth }
        contentWidth = snap(ls.maxOfOrNull { it.width } ?: 0f)
        contentHeight = snap(lineHeight(font()) * ls.size.coerceAtLeast(1))
    }

    private fun snap(v: Float): Float {
        val px = PolyLayoutEngine.pixelSnap
        return if (px > 0f) ceil(v * px - 1e-3f) / px else v
    }

    private fun layoutLines(maxWidth: Float): List<Line> {
        val pieces = spans ?: listOf(PolyTextSpan(text))
        val limit = if (wrap) maxWidth else Float.POSITIVE_INFINITY
        val out = ArrayList<Line>()
        var runs = ArrayList<Run>()
        var x = 0f

        fun newLine() {
            out.add(Line(runs, x))
            runs = ArrayList()
            x = 0f
        }

        for (span in pieces) {
            val f = font(span)
            for ((li, para) in span.text.split('\n').withIndex()) {
                if (li > 0) newLine()
                for (word in Regex("\\S*\\s*").findAll(para).map { it.value }.filter { it.isNotEmpty() }) {
                    var piece = word
                    while (piece.isNotEmpty()) {
                        if (x + f.measureTextWidth(piece.trimEnd()) <= limit) {
                            runs.add(Run(piece, x, span))
                            x += f.measureTextWidth(piece)
                            break
                        }
                        if (x > 0f) { newLine(); continue }
                        var n = 1
                        while (n < piece.length && f.measureTextWidth(piece.substring(0, n + 1)) <= limit) n++
                        runs.add(Run(piece.substring(0, n), x, span))
                        x += f.measureTextWidth(piece.substring(0, n))
                        piece = piece.substring(n)
                        newLine()
                    }
                }
            }
        }
        if (runs.isNotEmpty() || out.isEmpty()) out.add(Line(runs, x))
        val trimmed = out.map { l -> Line(l.runs, l.runs.lastOrNull()?.let { it.x + font(it.span).measureTextWidth(it.text.trimEnd()) } ?: 0f) }

        val cut = trimmed.size > maxLines
        val kept = if (cut) trimmed.subList(0, maxLines) else trimmed
        if (!ellipsis || (!cut && kept.last().width <= maxWidth)) return kept
        return kept.dropLast(1) + ellipsize(kept.last(), maxWidth)
    }

    private fun ellipsize(line: Line, maxWidth: Float): Line {
        val runs = line.runs.toMutableList()
        while (runs.isNotEmpty()) {
            val last = runs.last()
            val f = font(last.span)
            var t = last.text.trimEnd()
            while (t.isNotEmpty() && last.x + f.measureTextWidth("$t…") > maxWidth) t = t.dropLast(1)
            if (t.isNotEmpty() || runs.size == 1) {
                runs[runs.size - 1] = Run("$t…", last.x, last.span)
                return Line(runs, last.x + f.measureTextWidth("$t…"))
            }
            runs.removeAt(runs.size - 1)
        }
        return line
    }


    override fun render(ctx: RenderContext) {
        val font = font()
        val m = font.metrics
        val ls = lines
        if (!flowing || ls == null) {
            val baseline = y + baseline(font, 0)
            if (shadow) ctx.text(text, x + shadowOffset, baseline + shadowOffset, shadowColor, font)
            ctx.text(text, x, baseline, color, font)
            return
        }
        val lh = lineHeight(font)
        for ((i, line) in ls.withIndex()) {
            val baseline = y + baseline(font, i)
            val lx = if (center) (width - line.width) / 2f else 0f
            for (run in line.runs) {
                val f = font(run.span)
                val c = run.span?.color ?: color
                val rx = x + lx + run.x
                run.span?.background?.let { bg ->
                    val bw = f.measureTextWidth(run.text.trimEnd())
                    if (bw > 0f) ctx.rect(rx - 2f, y + i * lh, bw + 4f, lh, bg, lh / 4f)
                }
                if (shadow) ctx.text(run.text, rx + shadowOffset, baseline + shadowOffset, shadowColor, f)
                ctx.text(run.text, rx, baseline, c, f)
                val span = run.span ?: continue
                if (!span.underline && !span.strikethrough) continue
                val w = f.measureTextWidth(run.text.trimEnd())
                val fm = f.metrics
                if (span.underline) ctx.rect(rx, baseline + fm.underlinePosition, w, fm.underlineThickness, c)
                if (span.strikethrough) ctx.rect(rx, baseline + fm.strikeoutPosition, w, fm.strikeoutThickness, c)
            }
        }
    }
}
