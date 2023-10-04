/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.utils

import org.polyfrost.oneconfig.internal.create
//import org.polyfrost.oneconfig.renderer.LwjglManager
import org.polyfrost.polyui.input.Translator
import org.polyfrost.polyui.input.Translator.Companion.localised
import org.polyfrost.polyui.layout.Layout
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.seconds
import java.util.function.Function

object Notifications {
    private val queue = ArrayDeque<Layout>()

    //    private val renderer= LwjglManager.INSTANCE.getRenderer(UResolution.windowWidth.toFloat(), UResolution.windowHeight.toFloat())!!
//    private val polyUI = PolyUI(renderer = renderer)
    @JvmOverloads
    fun send(title: String, message: String, icon: PolyImage? = null, durationNanos: Long = 4.seconds, progressFunction: Function<Long, Float>? = null, action: Runnable? = null) {
        send(title.localised(), message.localised(), icon, durationNanos, progressFunction, action)
    }

    @JvmOverloads
    fun send(title: Translator.Text, message: Translator.Text, icon: PolyImage? = null, durationNanos: Long = 4.seconds, progressFunction: Function<Long, Float>? = null, action: Runnable? = null) {
        queue.addLast(create(title, message, icon, progressFunction, action))
    }
}