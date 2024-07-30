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
import org.polyfrost.polyui.utils.dont
import org.polyfrost.polyui.utils.translated
import org.polyfrost.oneconfig.api.config.v1.annotations.Text as TextAnnotation

/**
 * Basic HUD element which displays text.
 */
abstract class TextHud(
    @TextAnnotation(title = "Text Prefix")
    var prefix: String,
    @TextAnnotation(title = "Text Suffix")
    var suffix: String = ""
) : Hud<Text>() {
    private val sb = StringBuilder()
    override fun create() = Text("".translated().dont(), fontSize = 16f)

    override fun update(): Boolean {
        val t = getText()
        if (prefix.isNotEmpty()) sb.append(prefix).append(' ')
        sb.append(t)
        if (suffix.isNotEmpty()) sb.append(' ').append(suffix)
        get().text = sb.toString()
        sb.clear()
        return true
    }

    override fun initialize() {
        if(isReal) {
            updateWhenChanged("prefix")
            updateWhenChanged("suffix")
        }
        update()
    }

    /**
     * get the text to be shown on this HUD.
     * **do not call this method yourself.**
     */
    protected abstract fun getText(): String
}
