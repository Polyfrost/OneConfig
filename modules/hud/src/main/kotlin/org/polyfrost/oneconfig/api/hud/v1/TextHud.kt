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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.composables.PolyBox
import org.polyfrost.compose.composables.PolyCanvas
import org.polyfrost.compose.composables.PolyMcText
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.align
import org.polyfrost.compose.composables.background
import org.polyfrost.compose.composables.padding
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.layout.PolyAlign
import org.polyfrost.compose.layout.PolyInsets
import org.polyfrost.compose.layout.PolySize
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyColor
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

    init {
        padLeft = 4f
        padRight = 4f
        padTop = 4f
        padBottom = 4f
    }

    @get:JvmName("getStringBuilder")
    protected val sb = StringBuilder()

    private var displayText by mutableStateOf("")

    @Composable
    override fun Content() {
        val text = when (caseType) {
            1 -> displayText.uppercase()
            2 -> displayText.lowercase()
            else -> displayText
        }

        val contentAlign = alignment
        val padInsets = PolyInsets(padLeft, padTop, padRight, padBottom)
        val fgColor = PolyColor(textColor)

        val outerModifier = if (showBackground) {
            val bgModifier = PolyModifier.background(PolyColor(bgColor), bgRadius)
            if (staticWidth) bgModifier.size(staticW, staticH).padding(padInsets)
            else bgModifier.padding(padInsets)
        } else {
            if (staticWidth) PolyModifier.size(staticW, staticH).padding(padInsets)
            else PolyModifier.padding(padInsets)
        }

        PolyBox(modifier = outerModifier) {
            if (font == Font.Poppins) {
                val fontName = when {
                    textBold && textItalic -> "poppins-bold-italic"
                    textBold -> "poppins-bold"
                    textItalic -> "poppins-italic"
                    else -> "poppins"
                }
                val fontSize = 8f * textScale
                val skiaFont = FontManager.getFont(fontSize, fontName)
                val textWidth = skiaFont.measureTextWidth(text)
                val textHeight = skiaFont.metrics.let { it.descent - it.ascent }

                if (showShadow) {
                    PolyCanvas(modifier = PolyModifier.size(textWidth, textHeight)
                        .let { if (staticWidth) it.align(contentAlign) else it }) { x, y, _, _ ->
                        text(text, x + shadowOffsetX, y - skiaFont.metrics.ascent + shadowOffsetY, PolyColor(shadowColor), skiaFont)
                        text(text, x, y - skiaFont.metrics.ascent, fgColor, skiaFont)
                    }
                } else {
                    PolyCanvas(modifier = PolyModifier.size(textWidth, textHeight)
                        .let { if (staticWidth) it.align(contentAlign) else it }) { x, y, _, _ ->
                        text(text, x, y - skiaFont.metrics.ascent, fgColor, skiaFont)
                    }
                }
            } else {
                val formatted = buildString {
                    if (textBold) append("§l")
                    if (textItalic) append("§o")
                    append(text)
                    if (textBold || textItalic) append("§r")
                }

                PolyMcText(
                    text = formatted,
                    color = textColor,
                    shadow = showShadow,
                    scale = textScale,
                    modifier = if (staticWidth) PolyModifier.align(contentAlign) else PolyModifier,
                )
            }
        }
    }

    override fun update(): Boolean {
        if (prefix.isNotEmpty()) sb.append(prefix).append(' ')
        getText()?.let { sb.append(it) }
        if (suffix.isNotEmpty()) sb.append(' ').append(suffix)
        displayText = sb.toString()
        sb.clear()
        return true
    }

    override fun minimumSize() = 120f to 32f

    override fun setup() {
        super.setup()
        if (isReal) {
            updateWhenChanged("prefix")
            updateWhenChanged("suffix")
        }
        update()
    }

    protected abstract fun getText(): String?

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
    open class DateTime(
        header: String,
        @TextAnnotation(title = "Time template") var template: String,
        suffix: String = "",
    ) : TextHud("date_time_hud.yml", "Date/Time Hud", Category.INFO, header, suffix) {

        private var _formatter: DateTimeFormatter? = null

        private val formatter: DateTimeFormatter
            get() = _formatter ?: DateTimeFormatter.ofPattern(template).also { _formatter = it }

        override fun updateFrequency(): Long = when {
            'S' in template -> 100_000_000L
            's' in template -> 1_000_000_000L
            'm' in template -> 60_000_000_000L
            else -> 300_000_000_000L
        }

        override fun setup() {
            super.setup()
            if (isReal) addCallback("template") { _formatter = null; updateAndRecalculate() }
        }

        override fun getText(): String = LocalDateTime.now().format(formatter)

        override fun clone(): Hud = (super.clone() as DateTime).also { it._formatter = null }
    }
}
