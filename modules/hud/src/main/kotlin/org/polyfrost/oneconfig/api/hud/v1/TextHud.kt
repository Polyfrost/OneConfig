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
import androidx.compose.ui.util.fastFilterNotNull
import androidx.compose.ui.util.fastJoinToString
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.composables.PolyBox
import org.polyfrost.compose.composables.PolyCanvas
import org.polyfrost.compose.composables.PolyMcText
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.align
import org.polyfrost.compose.composables.padding
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.layout.PolyInsets
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
                val fontName = getPoppinsFontName()
                val fontSize = 8f * textScale
                val skiaFont = FontManager.getFont(fontSize, fontName)
                val textScale = skiaFont.measureText(text)

                if (showShadow) {
                    val shadowCol = PolyColor(shadowColor, shadowChroma, shadowChromaSpeed)
                    PolyCanvas(
                        modifier = PolyModifier.size(textScale.width, textScale.height)
                            .let { if (isStaticValid) it.align(contentAlign) else it }) { x, y, _, _ ->
                        val baseline = y - skiaFont.metrics.ascent
                        text(text, x + shadowOffsetX, baseline + shadowOffsetY, shadowCol, skiaFont)
                        if (textUnderline) {
                            val underlinePos = skiaFont.metrics.underlinePosition ?: (fontSize * 0.08f)
                            val underlineThick = skiaFont.metrics.underlineThickness ?: (fontSize * 0.06f)
                            line(
                                x + shadowOffsetX,
                                baseline + shadowOffsetY + underlinePos,
                                x + shadowOffsetX + textScale.width,
                                baseline + shadowOffsetY + underlinePos,
                                shadowCol,
                                underlineThick
                            )
                        }
                        text(text, x, baseline, fgColor, skiaFont)
                        if (textUnderline) {
                            val underlinePos = skiaFont.metrics.underlinePosition ?: (fontSize * 0.08f)
                            val underlineThick = skiaFont.metrics.underlineThickness ?: (fontSize * 0.06f)
                            line(
                                x,
                                baseline + underlinePos,
                                x + textScale.width,
                                baseline + underlinePos,
                                fgColor,
                                underlineThick
                            )
                        }
                    }
                } else {
                    PolyCanvas(
                        modifier = PolyModifier.size(textScale.width, textScale.height)
                            .let { if (isStaticValid) it.align(contentAlign) else it }) { x, y, _, _ ->
                        val baseline = y - skiaFont.metrics.ascent
                        text(text, x, baseline, fgColor, skiaFont)
                        if (textUnderline) {
                            val underlinePos = skiaFont.metrics.underlinePosition ?: (fontSize * 0.08f)
                            val underlineThick = skiaFont.metrics.underlineThickness ?: (fontSize * 0.06f)
                            line(
                                x,
                                baseline + underlinePos,
                                x + textScale.width,
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

                PolyMcText(
                    text = formatted,
                    color = fgColor,
                    shadow = showShadow,
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

        override fun updateFrequency(): Long = when {
            'S' in template -> 100_000_000L
            's' in template -> 1_000_000_000L
            'm' in template -> 60_000_000_000L
            else -> 300_000_000_000L
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
