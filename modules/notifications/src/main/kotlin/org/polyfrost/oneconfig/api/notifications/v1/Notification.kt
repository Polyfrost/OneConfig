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
import org.jetbrains.skia.Image
import org.polyfrost.compose.render.PolyColor
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicLong

/**
 * The visual category of a [Notification]
 */
enum class NotificationType(val accent: PolyColor, val iconName: String) {
    INFO(PolyColor.hex("#DFEAFF"), "info"),
    SUCCESS(PolyColor.hex("#3CAF77"), "check-circle"),
    ERROR(PolyColor.hex("#F2545A"), "alert-circle"),
    PROGRESS(PolyColor.hex("#2B4BFF"), "refresh"),
}

/**
 * A labeled button rendered underneath a notification body
 *
 * Clicking it runs [onClick]
 *
 * @param label    the text shown on the button
 * @param primary  whether this is the emphasised accent-coloured action as opposed to a subtle one
 * @param onClick  invoked when the button is pressed
 */
class NotificationAction @JvmOverloads constructor(
    val label: String,
    val primary: Boolean = false,
    val onClick: Runnable = Runnable {},
)

/**
 * A single notification
 *
 * Prefer creating these through [Notifications] rather than directly
 *
 * @param title       the bold heading
 * @param message     the supporting body text wrapped to the notification width
 * @param type        the visual [NotificationType] which defaults to [NotificationType.INFO]
 * @param icon        an optional icon drawn to the left of the text and null for none
 * @param duration    how long in milliseconds the toast stays on screen
 *                    Values `<= 0` keep it on screen until it is dismissed manually
 *                    or when a [progress] bar is present until that bar reaches `1`
 * @param progress    an optional supplier returning `0..1`
 *                    When present a progress bar is drawn
 *                    and the toast auto-dismisses once it reports `>= 1`
 * @param actions     optional action buttons drawn below the body
 * @param onClick     optional callback run when the body of the notification is clicked
 */
class Notification @JvmOverloads constructor(
    val title: String,
    val message: String,
    val type: NotificationType = NotificationType.INFO,
    val icon: Image? = null,
    val duration: Float = DEFAULT_DURATION,
    val progress: Callable<Float>? = null,
    val actions: List<NotificationAction> = emptyList(),
    val onClick: Runnable? = null,
) {
    val id: Long = COUNTER.incrementAndGet()

    val createdAt: Long = System.currentTimeMillis()

    internal var hovered by mutableStateOf(false)

    internal var dismissRequested by mutableStateOf(false)

    /**
     * Whether this notification has been seen in the notifications center
     */
    var read by mutableStateOf(false)
        internal set

    val persistent: Boolean get() = duration <= 0f && progress == null

    fun progressOrNull(): Float? {
        val p = progress ?: return null
        return runCatching { p.call() }.getOrNull()?.coerceIn(0f, 1f)
    }

    companion object {
        const val DEFAULT_DURATION = 4000f

        private val COUNTER = AtomicLong()
    }
}