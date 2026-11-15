@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.polyfrost.compose.composables.NodeBox
import org.polyfrost.compose.composables.padding
import org.polyfrost.compose.layout.PolyAlign
import org.polyfrost.compose.layout.PolyInsets
import org.polyfrost.compose.layout.PolySize
import org.polyfrost.compose.mc.McFontQueue
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.runtime.PolyApplier
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import kotlin.math.roundToInt

@ExperimentalOneConfigUiApi
public data class TextRun(
    public val text: String,
    public val color: PolyColor? = null,
    public val bold: Boolean = false,
    public val italic: Boolean = false,
    public val underline: Boolean = false,
)

@ExperimentalOneConfigUiApi
public fun List<TextRun>.plainText(): String = joinToString("") { it.text }

@ExperimentalOneConfigUiApi
public fun Component.toTextRuns(): List<TextRun> {
    val out = ArrayList<TextRun>()
    appendRuns(this, Style.empty(), out)
    return out
}

@ExperimentalOneConfigUiApi
@Composable
public fun Hud.HudText(lines: List<List<TextRun>>, alignColumns: Boolean = false) {
    HudRunText({ lines }, alignColumns)
}

@ExperimentalOneConfigUiApi
@Composable
public fun Hud.HudCellText(lines: List<List<List<TextRun>>>, alignColumns: Boolean = false) {
    HudRunText({ emptyList() }, alignColumns, { lines })
}

@Composable
internal fun Hud.HudRunText(
    lines: () -> List<List<TextRun>>,
    alignColumns: Boolean,
    cells: (() -> List<List<List<TextRun>>>)? = null,
) {
    val contentAlign = alignment
    val padInsets = PolyInsets(padLeft, padTop, padRight, padBottom)
    val aligned = hasSizeLimits
    val outerModifier = hudBackground().padding(padInsets)

    val spec = RunTextSpec(
        poppinsName = if (font == Font.Poppins) getPoppinsFontName() else null,
        bold = textBold,
        italic = textItalic,
        underline = textUnderline,
        scale = textScale,
        color = PolyColor(textColor, textChroma, textChromaSpeed),
        shadow = if (showShadow) PolyColor(shadowColor, shadowChroma, shadowChromaSpeed) else null,
        fullShadow = fullShadow,
        shadowX = shadowOffsetX,
        shadowY = shadowOffsetY,
        case = caseType,
        alignColumns = alignColumns,
        lineAlign = if (aligned) horizontalFactor(contentAlign) else 0f,
    )

    NodeBox(modifier = outerModifier) {
        ComposeNode<RunTextNode, PolyApplier>(
            factory = { RunTextNode() },
            update = {
                set(lines) { this.lines = it }
                set(cells) { this.cells = it }
                set(spec) { this.spec = it }
                set(if (aligned) contentAlign else PolyAlign.TopLeft) { style.align = it }
            },
        )
    }
}

private const val LINE_GAP = 2f
private const val POPPINS_SIZE = 8f
private const val LEGACY_CHAR = '§'

private val LEGACY_COLORS = intArrayOf(
    0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
    0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF,
)

private fun horizontalFactor(align: PolyAlign): Float = when (align) {
    PolyAlign.Top, PolyAlign.Center, PolyAlign.Bottom -> 0.5f
    PolyAlign.TopRight, PolyAlign.Right, PolyAlign.BottomRight -> 1f
    else -> 0f
}

private fun applyCase(text: String, case: Int): String = when (case) {
    1 -> text.uppercase()
    2 -> text.lowercase()
    else -> text
}

private fun poppinsVariant(base: String, bold: Boolean, italic: Boolean): String {
    val weight = if (bold) "poppins-bold" else base.removeSuffix("-italic")
    return if (italic) "$weight-italic" else weight
}

private fun appendRuns(component: Component, inherited: Style, out: MutableList<TextRun>) {
    val style = component.style().merge(inherited, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
    if (component is TextComponent) {
        if (component.content().isNotEmpty()) appendLegacy(component.content(), style, out)
    } else if (component is TranslatableComponent && component.arguments().isNotEmpty()) {
        appendTranslated(component, style, out)
    } else {
        val text = runCatching { Platform.compatibility().resolveComponent(component.children(emptyList())) }.getOrNull().orEmpty()
        if (text.isNotEmpty()) appendLegacy(text, style, out)
    }
    for (child in component.children()) appendRuns(child, style, out)
}

private const val ARG_OPEN = ''
private const val ARG_CLOSE = ''

private fun appendTranslated(component: TranslatableComponent, style: Style, out: MutableList<TextRun>) {
    val args = component.arguments()
    val markers = args.indices.map { Component.text("$ARG_OPEN$it$ARG_CLOSE") }
    val template = runCatching {
        Platform.compatibility().resolveComponent(component.arguments(markers).children(emptyList()))
    }.getOrNull().orEmpty()
    var pos = 0
    while (pos < template.length) {
        val open = template.indexOf(ARG_OPEN, pos)
        val close = if (open < 0) -1 else template.indexOf(ARG_CLOSE, open)
        val index = if (close < 0) null else template.substring(open + 1, close).toIntOrNull()?.takeIf { it in args.indices }
        if (index == null) {
            if (pos < template.length) appendLegacy(template.substring(pos), style, out)
            return
        }
        if (open > pos) appendLegacy(template.substring(pos, open), style, out)
        val value = args[index].value()
        appendRuns(if (value is Component) value else Component.text(value.toString()), style, out)
        pos = close + 1
    }
}

private fun appendLegacy(text: String, style: Style, out: MutableList<TextRun>) {
    val baseColor = style.color()?.let { PolyColor(0xFF000000.toInt() or it.value()) }
    val baseBold = style.hasDecoration(TextDecoration.BOLD)
    val baseItalic = style.hasDecoration(TextDecoration.ITALIC)
    val baseUnderline = style.hasDecoration(TextDecoration.UNDERLINED)
    if (LEGACY_CHAR !in text) {
        out.add(TextRun(text, baseColor, baseBold, baseItalic, baseUnderline))
        return
    }

    var color = baseColor
    var bold = baseBold
    var italic = baseItalic
    var underline = baseUnderline
    val pending = StringBuilder()
    fun flush() {
        if (pending.isEmpty()) return
        out.add(TextRun(pending.toString(), color, bold, italic, underline))
        pending.setLength(0)
    }

    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c != LEGACY_CHAR || i + 1 >= text.length) {
            pending.append(c)
            i++
            continue
        }
        val code = text[i + 1].lowercaseChar()
        val colorIndex = Character.digit(code, 16)
        flush()
        when {
            colorIndex >= 0 -> {
                color = PolyColor(0xFF000000.toInt() or LEGACY_COLORS[colorIndex])
                bold = false; italic = false; underline = false
            }
            code == 'l' -> bold = true
            code == 'o' -> italic = true
            code == 'n' -> underline = true
            code == 'r' -> {
                color = baseColor; bold = baseBold; italic = baseItalic; underline = baseUnderline
            }
        }
        i += 2
    }
    flush()
}

private data class RunTextSpec(
    val poppinsName: String?,
    val bold: Boolean,
    val italic: Boolean,
    val underline: Boolean,
    val scale: Float,
    val color: PolyColor,
    val shadow: PolyColor?,
    val fullShadow: Boolean,
    val shadowX: Float,
    val shadowY: Float,
    val case: Int,
    val alignColumns: Boolean,
    val lineAlign: Float,
)

private class LaidRun(val text: String, val color: PolyColor?, val font: PolyFont?, val underline: Boolean, val width: Float) {
    var offset = 0f
}

private class LaidLine(val runs: List<LaidRun>, val width: Float)

private class RunTextNode : PolyNode() {
    var lines: () -> List<List<TextRun>> = { emptyList() }
    var cells: (() -> List<List<List<TextRun>>>)? = null
    var spec: RunTextSpec? = null
    private var measuredLines: List<List<TextRun>>? = null
    private var measuredCells: List<List<List<TextRun>>>? = null
    private var measuredSpec: RunTextSpec? = null
    private var measuredFonts = -1
    private var laid: List<LaidLine> = emptyList()
    private var lineHeight = 0f
    private var ascent = 0f
    private var chroma = false

    override fun measureContent() {
        val s = spec ?: return
        val cellSource = cells?.invoke()
        val source = if (cellSource == null) lines() else null
        if (source == measuredLines && cellSource == measuredCells && s == measuredSpec && measuredFonts == FontManager.revision) return
        measuredLines = source
        measuredCells = cellSource
        measuredSpec = s
        measuredFonts = FontManager.revision

        val base = s.poppinsName?.let { PolyFont(it, POPPINS_SIZE * s.scale) }
        val measured = cellSource?.map { line -> line.map { cell -> cell.map { layoutRun(s, base, it) } } }
            ?: source.orEmpty().map { line -> line.map { listOf(layoutRun(s, base, it)) } }
        val columns = if (s.alignColumns) columnOffsets(measured) else null
        laid = measured.map { line ->
            var cursor = 0f
            var right = 0f
            for ((i, cell) in line.withIndex()) {
                cursor = columns?.get(i) ?: cursor
                for (run in cell) {
                    run.offset = cursor
                    cursor += run.width
                }
                right = maxOf(right, cursor)
            }
            LaidLine(line.flatten(), right)
        }
        chroma = s.color.chroma || s.shadow?.chroma == true || measured.any { line -> line.any { cell -> cell.any { it.color?.chroma == true } } }

        if (base != null) {
            val metrics = base.metrics
            ascent = metrics.ascent
            lineHeight = metrics.descent - metrics.ascent
        } else {
            ascent = 0f
            lineHeight = McFontQueue.measureBlockHeight(1, s.scale)
        }
        val count = laid.size
        style.width = PolySize.Px(laid.maxOfOrNull { it.width } ?: 0f)
        style.height = PolySize.Px(if (count == 0) 0f else count * lineHeight + (count - 1) * LINE_GAP * s.scale)
    }

    private fun layoutRun(s: RunTextSpec, base: PolyFont?, run: TextRun): LaidRun {
        val text = applyCase(run.text, s.case)
        val underline = run.underline || s.underline
        if (base != null && s.poppinsName != null) {
            val italic = run.italic || s.italic
            val font = if (run.bold || run.italic) PolyFont(poppinsVariant(s.poppinsName, run.bold, italic), base.size) else base
            return LaidRun(text, run.color, font, underline, font.measureTextWidth(text))
        }
        val formatted = buildString {
            if (run.bold || s.bold) append("§l")
            if (run.italic || s.italic) append("§o")
            if (underline) append("§n")
            append(text)
        }
        return LaidRun(formatted, run.color, null, false, McFontQueue.measureTextWidth(formatted, s.scale))
    }

    private fun columnOffsets(lines: List<List<List<LaidRun>>>): List<Float> {
        val count = lines.maxOfOrNull { it.size } ?: return emptyList()
        val widths = FloatArray(count)
        for (line in lines) for ((i, cell) in line.withIndex()) widths[i] = maxOf(widths[i], cell.fold(0f) { sum, run -> sum + run.width })
        var offset = 0f
        return List(count) { i -> offset.also { offset += widths[i] } }
    }

    override fun render(ctx: RenderContext) {
        val s = spec ?: return
        val ls = laid
        val px = x
        val py = y
        val w = width
        if (chroma) ctx.live { draw(this, s, ls, px, py, w) } else draw(ctx, s, ls, px, py, w)
    }

    private fun draw(ctx: RenderContext, s: RunTextSpec, lines: List<LaidLine>, x: Float, y: Float, width: Float) {
        val pitch = lineHeight + LINE_GAP * s.scale
        for ((i, line) in lines.withIndex()) {
            val lx = x + (width - line.width) * s.lineAlign
            val top = y + i * pitch
            if (s.poppinsName != null) drawPoppinsLine(ctx, s, line, lx, top - ascent) else drawMcLine(ctx, s, line, lx, top)
        }
    }

    private fun drawPoppinsLine(ctx: RenderContext, s: RunTextSpec, line: LaidLine, x: Float, baseline: Float) {
        val shadow = s.shadow
        if (shadow != null) {
            for (run in line.runs) {
                val font = run.font ?: continue
                val rx = x + run.offset
                val position = font.metrics.underlinePosition
                val thickness = font.metrics.underlineThickness
                if (s.fullShadow) {
                    ctx.textOutline(run.text, rx, baseline, shadow, font, s.scale)
                    if (run.underline) ctx.line(rx - s.scale, baseline + position, rx + run.width + s.scale, baseline + position, shadow, thickness + s.scale * 2f)
                } else {
                    val sx = rx + s.shadowX
                    val sy = baseline + s.shadowY
                    ctx.text(run.text, sx, sy, shadow, font)
                    if (run.underline) ctx.line(sx, sy + position, sx + run.width, sy + position, shadow, thickness)
                }
            }
        }
        for (run in line.runs) {
            val font = run.font ?: continue
            val rx = x + run.offset
            val color = run.color ?: s.color
            ctx.text(run.text, rx, baseline, color, font)
            if (run.underline) {
                val position = font.metrics.underlinePosition
                ctx.line(rx, baseline + position, rx + run.width, baseline + position, color, font.metrics.underlineThickness)
            }
        }
    }

    private fun drawMcLine(ctx: RenderContext, s: RunTextSpec, line: LaidLine, x: Float, top: Float) {
        val shadow = s.shadow
        val m = ctx.deviceTransform()
        val start = if (m == null || m[0] == 0f) x else ((x * m[0] + m[2]).roundToInt() - m[2]) / m[0]
        for (run in line.runs) {
            val color = (run.color ?: s.color).argb
            if (shadow != null && s.fullShadow) ctx.mcTextOutline(run.text, start + run.offset, top, color, shadow.argb, s.scale, false, false)
            else ctx.mcText(run.text, start + run.offset, top, color, shadow != null, s.scale, false, false)
        }
    }
}
