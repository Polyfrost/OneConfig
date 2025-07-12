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

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.component.impl.Text
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.milliseconds
import org.polyfrost.polyui.unit.minutes
import org.polyfrost.polyui.unit.seconds
import org.polyfrost.polyui.utils.dont
import org.polyfrost.polyui.utils.translated
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.polyfrost.oneconfig.api.config.v1.annotations.Text as TextAnnotation

/**
 * Basic HUD element which displays text.
 * @see TextHud.DateTime
 */
abstract class TextHud(
    id: String,
    title: String,
    category: Category,
    @TextAnnotation(title = "Text Prefix")
    var prefix: String,
    @TextAnnotation(title = "Text Suffix")
    var suffix: String = ""
) : Hud<Text>(id, title, category) {
    /**
     * [StringBuilder] instance that is used for constructing the HUD text.
     *
     * **Hot-Tip**: you can use this to construct the text directly for better performance, and also from other places.
     * For example, if you operate by updating your text in an [eventHandler][org.polyfrost.oneconfig.api.event.v1.eventHandler], you can do the following:
     * ```kotlin
     * eventHandler { event: Event ->
     *    // your code here
     *    sb.append(event.data)
     *    updateAndRecalculate()
     * }
     * ```
     */
    @get:JvmName("getStringBuilder")
    protected val sb = StringBuilder()
    override fun create() = Text("".translated().dont(), fontSize = 16f)

    override fun update(): Boolean {
        if (prefix.isNotEmpty()) {
            if (sb.isEmpty()) sb.append(prefix).append(' ')
            else sb.insert(0, prefix).insert(prefix.length, ' ')
        }
        val t = getText()
        if (t != null) sb.append(t)
        if (suffix.isNotEmpty()) sb.append(' ').append(suffix)
        get().text = sb.toString()
        sb.clear()
        return true
    }

    override fun minimumSize() = Vec2(120f, 32f)

    override fun initialize() {
        super.initialize()
        if (isReal) {
            updateWhenChanged("prefix")
            updateWhenChanged("suffix")
        }
        update()
    }

    /**
     * get the text to be shown on this HUD.
     * **do not call this method yourself.**
     *
     * **hot tip:** use [sb] directly for better performance.
     */
    protected abstract fun getText(): String?


    /**
     * [TextHud] which displays a simple text element which is configured by the user.
     *
     * **This is an example implementation bundled with OneConfig. You should not use it yourself in code.**
     */
    @ApiStatus.Internal
    class Simple(prefix: String, @TextAnnotation(title = "Text") var it: String, suffix: String) : TextHud("text_hud.yml", "Text Hud", Category.INFO, prefix, suffix) {

        override fun getText() = it

        override fun initialize() {
            super.initialize()
            if (isReal) updateWhenChanged("it")
        }
    }

    /**
     * [TextHud] which displays the date/time information.
     * @param template the template to use for the time. See [DateTimeFormatter] for an explanation of the different keywords.
     *
     * **This is an example implementation bundled with OneConfig. You should not use it yourself in code.**
     */
    @ApiStatus.Internal
    class DateTime(
        header: String,
        @TextAnnotation(title = "Time template") var template: String,
        suffix: String = ""
    ) : TextHud("date_time_hud.yml", "Date/Time Hud", Category.INFO, header, suffix) {

        override fun updateFrequency(): Long {
            return if ('S' in template) 100.milliseconds
            else if ('s' in template) 1.seconds
            else if ('m' in template) 1.minutes
            else 5.minutes
        }

        override fun initialize() {
            super.initialize()
            if (isReal) {
                addCallback("template") {
                    _formatter = null
                    updateAndRecalculate()
                }
            }
        }

        private var _formatter: DateTimeFormatter? = null

        private val formatter: DateTimeFormatter
            get() {
                val formatter = _formatter
                if (formatter != null) return formatter
                val forMatHer = DateTimeFormatter.ofPattern(this.template)
                this._formatter = forMatHer
                return forMatHer
            }

        override fun getText(): String = LocalDateTime.now().format(formatter)

        override fun clone() = super.clone().also { _formatter = null }
    }
}
