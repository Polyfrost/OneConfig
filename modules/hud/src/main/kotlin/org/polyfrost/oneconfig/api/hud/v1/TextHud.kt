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

import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.input.Translator
import org.polyfrost.polyui.unit.milliseconds
import org.polyfrost.polyui.unit.minutes
import org.polyfrost.polyui.unit.seconds
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.polyfrost.oneconfig.api.config.v1.annotations.Text as TextAnnotation

/**
 * Basic HUD element which displays text.
 * @see TextHud.DateTime
 */
abstract class TextHud(
    @TextAnnotation(title = "Text Prefix")
    var prefix: String,
    @TextAnnotation(title = "Text Suffix")
    var suffix: String = ""
) : Hud<Text>() {
    override fun create() = Text(Translator.Text.Simple(""), fontSize = 16f)

    override fun update(): Boolean {
        get().text = "$prefix${getText()}$suffix"
        return true
    }

    override fun initialize() = update()

    /**
     * get the text to be shown on this HUD.
     * **do not call this method yourself.**
     */
    protected abstract fun getText(): String

    /**
     * [TextHud] which displays the date/time information.
     * @param template the template to use for the time. See [DateTimeFormatter] for an explanation of the different keywords.
     */
    class DateTime(
        header: String,
        @TextAnnotation(title = "The template to use for the time.")
        var template: String,
        suffix: String = ""
    ) : TextHud(header, suffix) {

        override fun id() = "date_time_hud.yml"

        override fun title() = "Date/Time Hud"

        override fun category() = Category.INFO

        override fun updateFrequency(): Long {
            return if (template.contains('S')) 100.milliseconds
            else if (template.contains('s')) 1.seconds
            else if (template.contains('m')) 1.minutes
            else 5.minutes
        }

        private var _formatter: DateTimeFormatter? = null

        private fun formatter(): DateTimeFormatter {
            val formatter = _formatter
            if (formatter != null) return formatter
            val forMatHer = DateTimeFormatter.ofPattern(this.template)
            this._formatter = forMatHer
            return forMatHer
        }

        override fun getText(): String = LocalDateTime.now().format(formatter())
    }
}
