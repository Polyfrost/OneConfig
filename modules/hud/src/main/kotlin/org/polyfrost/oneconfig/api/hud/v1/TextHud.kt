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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.PaddingValues
import org.polyfrost.compose.foundation.layout.align
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.wrapContentSize
import org.polyfrost.compose.mc.McText
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.unit.dp
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

    private var displayTextState: MutableState<String> = mutableStateOf("")
    private var displayText: String
        get() = displayTextState.value
        set(value) {
            displayTextState.value = value
        }

    @Composable
    override fun Content() {
        val text = when (caseType) {
            1 -> displayText.uppercase()
            2 -> displayText.lowercase()
            else -> displayText
        }

        val contentAlign = alignment
        val padInsets = PaddingValues(padLeft.dp, padTop.dp, padRight.dp, padBottom.dp)
        // PolyColor.argb cycles on the current time when chroma is on so it keeps animating
        // at render time without needing recomposition
        val fgColor = PolyColor(textColor, textChroma, textChromaSpeed)

        val isStaticValid = staticWidth && staticW > 0f && staticH > 0f

        // when merged HudManager draws this background as part of the fused neighbour shape
        // which hudBackground() accounts for
        val bgModifier = hudBackground()
        val outerModifier =
            if (isStaticValid) bgModifier.size(staticW.dp, staticH.dp).padding(padInsets)
            else bgModifier.wrapContentSize().padding(padInsets)

        Box(modifier = outerModifier) {
            if (font == Font.Poppins) {
                val fontName = getPoppinsFontName()
                val fontSize = 8f * textScale
                val polyFont = PolyFont(fontName, fontSize)
                val metrics = polyFont.metrics
                val lines = text.lines()
                val lineHeight = metrics.spacing
                val blockW = lines.maxOf { polyFont.measureTextWidth(it) }
                val blockH = (lines.size - 1) * lineHeight + (metrics.descent - metrics.ascent)
                val underlinePos = metrics.underlinePosition
                val underlineThick = metrics.underlineThickness
                val shadowCol = if (showShadow) PolyColor(shadowColor, shadowChroma, shadowChromaSpeed) else null

                Canvas(
                    modifier = Modifier.size(blockW.dp, blockH.dp)
                        .let { if (isStaticValid) it.align(contentAlign) else it }) { x, y, _, _ ->
                    lines.forEachIndexed { index, lineText ->
                        if (lineText.isEmpty()) return@forEachIndexed
                        val baseline = y + index * lineHeight - metrics.ascent
                        val lineW = polyFont.measureTextWidth(lineText)
                        if (shadowCol != null) {
                            text(lineText, x + shadowOffsetX, baseline + shadowOffsetY, shadowCol, polyFont)
                            if (textUnderline) {
                                line(
                                    x + shadowOffsetX,
                                    baseline + shadowOffsetY + underlinePos,
                                    x + shadowOffsetX + lineW,
                                    baseline + shadowOffsetY + underlinePos,
                                    shadowCol,
                                    underlineThick
                                )
                            }
                        }
                        text(lineText, x, baseline, fgColor, polyFont)
                        if (textUnderline) {
                            line(
                                x,
                                baseline + underlinePos,
                                x + lineW,
                                baseline + underlinePos,
                                fgColor,
                                underlineThick
                            )
                        }
                    }
                }
            } else {
                val formatted = buildString {
                    if (textBold) append("§l")
                    if (textItalic) append("§o")
                    if (textUnderline) append("§n")
                    append(text)
                    if (textBold || textItalic || textUnderline) append("§r")
                }

                McText(
                    text = formatted,
                    modifier = if (isStaticValid) Modifier.align(contentAlign) else Modifier,
                    color = fgColor,
                    shadow = showShadow,
                    scale = textScale,
                )
            }
        }
    }

    open val concatString: String get() = ""

    open fun concat(prefix: String, value: String?, suffix: String): String {
        val separator = concatString
        var out = prefix
        if (!value.isNullOrEmpty()) out = if (out.isEmpty()) value else out + separator + value
        if (suffix.isNotEmpty()) out = if (out.isEmpty()) suffix else out + separator + suffix
        return out
    }

    /**
     * Wraps the finished line in square brackets when [brackets] is on
     *
     * Empty lines stay empty
     */
    protected fun decorate(text: String): String =
        if (brackets && text.isNotEmpty()) "[$text]" else text

    override fun update(): Boolean {
        displayText = decorate(concat(prefix, getText(), suffix))
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
        it.displayTextState = mutableStateOf("")
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
