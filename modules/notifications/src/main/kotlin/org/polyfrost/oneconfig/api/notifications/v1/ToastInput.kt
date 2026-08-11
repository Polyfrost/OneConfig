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

package org.polyfrost.oneconfig.api.notifications.v1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.MouseInputEvent
import java.util.concurrent.atomic.AtomicBoolean

internal object ToastInput {
    @Volatile var mouseX: Float = -1f
        private set

    @Volatile var mouseY: Float = -1f
        private set

    var hoveredAction by mutableStateOf<NotificationAction?>(null)

    @Volatile
    var hoverTarget: ToastHit? = null

    private val installed = AtomicBoolean(false)

    fun clearHover() {
        hoverTarget = null
        hoveredAction = null
    }

    fun install() {
        if (!installed.compareAndSet(false, true)) return
        EventManager.register(MouseInputEvent.Moved::class.java) { e ->
            mouseX = e.x
            mouseY = e.y
        }
        EventManager.register(MouseInputEvent::class.java) { e ->
            // button 0 is GLFW left and state 1 is GLFW_PRESS
            if (e.button == 0 && e.state == 1) NotificationsRenderer.dispatchClick(hoverTarget)
        }
    }
}
