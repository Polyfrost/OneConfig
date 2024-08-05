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

package org.polyfrost.oneconfig.api.ui.v1

import org.polyfrost.polyui.animate.Animation
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.notify.Notifications
import org.polyfrost.polyui.unit.seconds


object NotificationsManager {
    private val it = Notifications(UIManager.INSTANCE.defaultInstance, max = 5)

    fun enqueueCustom(vararg components: Component, progressFunc: Animation) {
        it.enqueue(*components, progressFunc = progressFunc)
    }

    @JvmOverloads
    fun enqueue(type: Notifications.Type, title: String = type.title, description: String, durationNanos: Long = 5.seconds) {
        it.enqueueStandard(type, title, description, durationNanos)
    }
}
