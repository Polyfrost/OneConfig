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

import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.unit.milliseconds
import org.polyfrost.polyui.unit.minutes
import org.polyfrost.polyui.unit.seconds
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Basic HUD element which displays text.
 * @see TextHud.Supplier
 * @see TextHud.DateTime
 */
abstract class TextHud(id: String, name: String, category: Category, var prefix: String, var suffix: String = "", private val frequency: Long) : Hud<Text>(id, name, category) {
    override fun create() = Text("$prefix${getText()}$suffix", fontSize = 16f, font = PolyUI.defaultFonts.medium)

    override fun updateFrequency() = frequency

    override fun update(): Boolean {
        get().text = "$prefix${getText()}$suffix"
        return true
    }

    protected abstract fun getText(): String

    /**
     * [TextHud] which uses the given [supplier] to get the text.
     */
    class Supplier(id: String, name: String, category: Category, prefix: String, suffix: String = "", frequency: Long, private val supplier: () -> String) : TextHud(id, name, category, prefix, suffix, frequency) {
        constructor(id: String, name: String, category: Category, prefix: String, suffix: String = "", frequency: Long, supplier: java.util.function.Supplier<String>) : this(id, name, category, prefix, suffix, frequency, supplier::get)

        override fun getText() = supplier()
    }

    class Field(id: String, name: String, category: Category, prefix: String, text: String, suffix: String = "") : TextHud(id, name, category, prefix, suffix, 0L) {
        var theText = text
            set(value) {
                field = value
                update()
            }

        override fun getText() = theText
    }

    /**
     * [TextHud] which displays the date/time information.
     * @param template the template to use for the time. See [DateTimeFormatter] for an explanation of the different keywords.
     */
    class DateTime(header: String, template: String, suffix: String = "") : TextHud(
        "date_time_hud",
        "Date/Time Hud",
        Category.INFO,
        header,
        suffix,
        frequency =
        if (template.contains('S')) 100.milliseconds
        else if (template.contains('s')) 1.seconds
        else if (template.contains('m')) 1.minutes
        else 5.minutes, // asm: updating every 5 minutes is reasonable
    ) {
        var string = template
            set(value) {
                field = value
                template = DateTimeFormatter.ofPattern(value)
            }

        private var template = DateTimeFormatter.ofPattern(template)

        override fun getText() = LocalDateTime.now().format(template)
    }
}
