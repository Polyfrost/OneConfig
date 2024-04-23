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

package org.polyfrost.oneconfig.ui.v1.notifications

import org.jetbrains.annotations.ApiStatus
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.renderer.data.PolyImage
import org.polyfrost.polyui.utils.image
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object NotificationManager {

}

@ApiStatus.Internal
fun create(title: String, message: String, icon: PolyImage?, progressFunction: java.util.function.Function<Long, Float>?, action: Runnable?): Drawable {
    return Block()
}

class Notification(vararg val extras: Drawable?, val title: String, val description: String, time: Long, val state: Byte = 0, val icon: PolyImage = "cog.svg".image()) {
    val time = System.currentTimeMillis() - time
    val timeString: String
        get() {
            return if (this.time < 15_000L) {
                "Just now"
            } else {
                val ldt = LocalDateTime.ofInstant(Instant.ofEpochSecond(this.time), ZoneId.systemDefault())
                if (ldt.monthValue == 0) {
                    if (ldt.dayOfMonth == 0) {
                        if (ldt.hour == 0) {
                            if (ldt.minute == 0) {
                                "${ldt.second}s ago"
                            } else {
                                "${ldt.minute}m ago"
                            }
                        } else {
                            "${ldt.hour}h ago"
                        }
                    } else {
                        "${ldt.dayOfMonth}d ago"
                    }
                } else {
                    "${ldt.monthValue}mo ago"
                }
            }
        }
}