/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.ui.util.fastFilterNotNull
import androidx.compose.ui.util.fastJoinToString
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.composables.PolyBox
import org.polyfrost.compose.composables.PolyMcText
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.align
import org.polyfrost.compose.composables.padding
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.layout.PolyAlign
import org.polyfrost.compose.layout.PolyInsets
import org.polyfrost.compose.layout.PolySize
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.runtime.PolyApplier
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.config.v1.annotations.Text as TextAnnotation

abstract class TextHud(
    id: String,
    title: String,
    category: Category,
    @TextAnnotation(title = "Text Prefix") var prefix: String,
    @TextAnnotation(title = "Text Suffix") var suffix: String = "",
) : Hud(id, title, category) {

    companion object {
        private const val UNMEASURED = -1f
    }

    @Switch(title = "Brackets")
    var brackets: Boolean = false

    init {
        padLeft = 4f
        padRight = 4f
        padTop = 4f
        padBottom = 4f
        staticWidth = false
        staticW = UNMEASURED
        staticH = UNMEASURED
    }

    @Volatile private var displayText: String = ""

    private fun casedText(case: Int): String = when (case) {
        1 -> displayText.uppercase()
        2 -> displayText.lowercase()
        else -> displayText
    }

    @Composable
    override fun Content() {
        val case = caseType

        val contentAlign = alignment
        val padInsets = PolyInsets(padLeft, padTop, padRight, padBottom)
        // PolyColor.argb cycles on the current time when chroma is on so it keeps animating
        // at render time without needing recomposition
        val fgColor = PolyColor(textColor, textChroma, textChromaSpeed)

        val isStaticValid = staticWidth && staticW > 0f && staticH > 0f

        // when merged HudManager draws this background as part of the fused neighbour shape
        // which hudBackground() accounts for
        val bgModifier = hudBackground()
        val outerModifier =
            if (isStaticValid) bgModifier.size(staticW, staticH).padding(padInsets)
            else bgModifier.padding(padInsets)

        PolyBox(modifier = outerModifier) {
            if (font == Font.Poppins) {
                val spec = PoppinsTextSpec(
                    PolyFont(getPoppinsFontName(), 8f * textScale),
                    fgColor,
                    if (showShadow) PolyColor(shadowColor, shadowChroma, shadowChromaSpeed) else null,
                    fullShadow,
                    textUnderline,
                    shadowOffsetX,
                    shadowOffsetY,
                    textScale,
                )
                val provider = { casedText(case) }
                ComposeNode<PoppinsTextNode, PolyApplier>(
                    factory = { PoppinsTextNode() },
                    update = {
                        set(provider) { text = it }
                        set(spec) { this.spec = it }
                        set(if (isStaticValid) contentAlign else PolyAlign.TopLeft) { style.align = it }
                    },
                )
            } else {
                val head = buildString {
                    if (textBold) append("§l")
                    if (textItalic) append("§o")
                    if (textUnderline) append("§n")
                }
                val tail = if (textBold || textItalic || textUnderline) "§r" else ""
                val provider = { head + casedText(case) + tail }

                PolyMcText(
                    text = provider,
                    color = fgColor,
                    shadow = showShadow && !fullShadow,
                    outlineColor = if (showShadow && fullShadow) PolyColor(shadowColor, shadowChroma, shadowChromaSpeed) else null,
                    scale = textScale,
                    modifier = if (isStaticValid) PolyModifier.align(contentAlign) else PolyModifier,
                )
            }
        }
    }

    open val concatString: String get() = ""

    open fun concat(prefix: String, value: String?, suffix: String): String {
        return arrayListOf(
            prefix.takeIf { it.isNotEmpty() },
            value?.takeIf { it.isNotEmpty() },
            suffix.takeIf { it.isNotEmpty() }
        ).fastFilterNotNull().fastJoinToString(concatString)
    }

    /**
     * Wraps the finished line in square brackets when [brackets] is on
     *
     * Empty lines stay empty
     */
    protected fun decorate(text: String): String =
        if (brackets && text.isNotEmpty()) "[$text]" else text

    @Transient private var lastValue: String? = null
    @Transient private var lastPrefix: String? = null
    @Transient private var lastSuffix: String? = null
    @Transient private var lastBrackets = false

    override fun update(): Boolean {
        val value = getText()
        val p = prefix
        val sfx = suffix
        if (lastPrefix === p && lastSuffix === sfx && lastBrackets == brackets && value == lastValue && plainConcat.get(javaClass)) return true
        lastValue = value
        lastPrefix = p
        lastSuffix = sfx
        lastBrackets = brackets
        val text = decorate(concat(p, value, sfx))
        if (text != displayText) {
            displayText = text
            contentChanged()
        }
        return true
    }

    override fun minimumSize(): Pair<Float, Float> =
        (padLeft + padRight + 2f) to (padTop + padBottom + 2f)

    override fun setup() {
        super.setup()
        if (isReal) {
            updateWhenChanged("prefix")
            updateWhenChanged("suffix")
            updateWhenChanged("brackets")
        }
        update()
        reseedStaticSizeIfNeeded()
        captureStaticSizeDefaults()
    }

    protected abstract fun getText(): String?

    override fun clone(): Hud = (super.clone() as TextHud).also {
        it.displayText = ""
        it.lastPrefix = null
    }

    @ApiStatus.Internal
    class Simple(
        prefix: String,
        @TextAnnotation(title = "Text") var it: String,
        suffix: String,
    ) : TextHud("text_hud.yml", "Text Hud", Category.INFO, prefix, suffix) {
        override fun getText() = it
        override fun setup() {
            super.setup()
            if (isReal) updateWhenChanged("it")
        }
    }

    @ApiStatus.Internal
    open class DateTime @JvmOverloads constructor(
        header: String,
        @TextAnnotation(title = "Time template") var template: String,
        suffix: String = "",
        title: String = "Date/Time Hud",
    ) : TextHud("date_time_hud.yml", title, Category.INFO, header, suffix) {

        private var _formatter: DateTimeFormatter? = null
        private var _formatterTemplate: String? = null

        private val formatter: DateTimeFormatter?
            get() {
                if (_formatterTemplate == template) return _formatter

                return try {
                    DateTimeFormatter.ofPattern(template).also {
                        _formatter = it
                        _formatterTemplate = template
                    }
                } catch (_: IllegalArgumentException) {
                    _formatter = null
                    _formatterTemplate = template
                    null
                }
            }

        private var frequencyTemplate: String? = null
        private var frequency = 0L

        override fun updateFrequency(): Long {
            val t = template
            if (t !== frequencyTemplate) {
                frequencyTemplate = t
                frequency = when {
                    'S' in t -> 100_000_000L
                    's' in t -> 1_000_000_000L
                    'm' in t -> 60_000_000_000L
                    else -> 300_000_000_000L
                }
            }
            return frequency
        }

        override fun setup() {
            super.setup()
            if (isReal) addCallback("template") {
                _formatter = null
                _formatterTemplate = null
                updateAndRecalculate()
            }
        }

        override fun getText(): String? = formatter?.let { LocalDateTime.now().format(it) }

        override fun clone(): Hud = (super.clone() as DateTime).also {
            it._formatter = null
            it._formatterTemplate = null
        }
    }
}

private class PoppinsTextSpec(
    val font: PolyFont,
    val color: PolyColor,
    val shadow: PolyColor?,
    val fullShadow: Boolean,
    val underline: Boolean,
    val shadowX: Float,
    val shadowY: Float,
    val outline: Float,
)

private class PoppinsTextNode : PolyNode() {
    var text: () -> String = { "" }
    var spec: PoppinsTextSpec? = null
    private var lines: List<String> = emptyList()
    private var measuredText: String? = null
    private var measuredSpec: PoppinsTextSpec? = null

    override fun measureContent() {
        val s = spec ?: return
        val t = text()
        if (t == measuredText && s === measuredSpec) return
        measuredText = t
        measuredSpec = s
        lines = t.lines()
        val metrics = s.font.metrics
        style.width = PolySize.Px(lines.maxOf { s.font.measureTextWidth(it) })
        style.height = PolySize.Px((lines.size - 1) * metrics.spacing + (metrics.descent - metrics.ascent))
    }

    override fun render(ctx: RenderContext) {
        val s = spec ?: return
        if (s.color.chroma || s.shadow?.chroma == true) {
            val ls = lines; val px = x; val py = y
            ctx.live { draw(this, s, ls, px, py) }
        } else {
            draw(ctx, s, lines, x, y)
        }
    }

    private fun draw(ctx: RenderContext, s: PoppinsTextSpec, lines: List<String>, x: Float, y: Float) {
        val font = s.font
        val metrics = font.metrics
        val underlinePos = metrics.underlinePosition
        val underlineThick = metrics.underlineThickness
        val shadowCol = s.shadow
        val outline = s.outline
        lines.forEachIndexed { index, lineText ->
            if (lineText.isEmpty()) return@forEachIndexed
            val baseline = y + index * metrics.spacing - metrics.ascent
            val lineW = font.measureTextWidth(lineText)
            if (shadowCol != null && s.fullShadow) {
                ctx.textOutline(lineText, x, baseline, shadowCol, font, outline)
                if (s.underline) {
                    ctx.line(x - outline, baseline + underlinePos, x + lineW + outline, baseline + underlinePos, shadowCol, underlineThick + outline * 2f)
                }
            } else if (shadowCol != null) {
                ctx.text(lineText, x + s.shadowX, baseline + s.shadowY, shadowCol, font)
                if (s.underline) {
                    ctx.line(x + s.shadowX, baseline + s.shadowY + underlinePos, x + s.shadowX + lineW, baseline + s.shadowY + underlinePos, shadowCol, underlineThick)
                }
            }
            ctx.text(lineText, x, baseline, s.color, font)
            if (s.underline) ctx.line(x, baseline + underlinePos, x + lineW, baseline + underlinePos, s.color, underlineThick)
        }
    }
}

private val plainConcat = object : ClassValue<Boolean>() {
    override fun computeValue(type: Class<*>): Boolean = runCatching {
        type.getMethod("concat", String::class.java, String::class.java, String::class.java).declaringClass == TextHud::class.java &&
            type.getMethod("getConcatString").declaringClass == TextHud::class.java
    }.getOrDefault(false)
}
