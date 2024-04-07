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

package org.polyfrost.oneconfig.ui

import org.polyfrost.oneconfig.api.events.EventManager
import org.polyfrost.oneconfig.api.events.event.RenderEvent
import org.polyfrost.oneconfig.api.events.event.Stage
import org.polyfrost.polyui.PolyUI
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.property.Settings
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.unit.seconds
import java.util.function.Function

object Notifications {
    private val queue = ArrayDeque<Drawable>()

    private val settings = Settings().apply {
        cleanupAfterInit = false
        framebuffersEnabled = false
        renderPausingEnabled = false
        debug = false
    }
    private val polyUI = PolyUI(renderer = LwjglManager.INSTANCE.renderer, settings = settings)

    init {
        EventManager.register(RenderEvent::class) {
            if (it.stage == Stage.START || queue.isEmpty()) return@register
            polyUI.render()
        }
    }

    @JvmOverloads
    fun send(title: String, message: String, icon: PolyImage? = null, durationNanos: Long = 4.seconds, progressFunction: Function<Long, Float>? = null, action: Runnable? = null) {
        queue.addLast(create(title, message, icon, progressFunction, action))
    }
}