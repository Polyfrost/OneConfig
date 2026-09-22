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

import org.apache.logging.log4j.LogManager
import org.jetbrains.skia.Image
import java.util.concurrent.Callable

/**
 * Utility for sending notifications
 *
 * Kotlin
 * ```
 * Notifications.send("Saved", "Your config was saved")
 * Notifications.success("Downloaded!", "All-of-Fabric-6")
 * Notifications.progress("Downloading", "All-of-Fabric-6") { downloadFraction() }
 * ```
 *
 * Java
 * ```
 * Notifications.INSTANCE.send("Saved", "Your config was saved");
 * Notifications.builder("8 mods have updates", "DamageTint, Hytils, ...")
 *     .type(NotificationType.PROGRESS)
 *     .action(new NotificationAction("Update all", true, this::updateAll))
 *     .action(new NotificationAction("See mods", false, this::openMods))
 *     .send();
 * ```
 */
object Notifications {
    private val LOGGER = LogManager.getLogger("OneConfig/Notifications")

    /**
     * Sends a fully specified notification
     *
     * All parameters except [title] and [message] are optional
     *
     * @return the [Notification] that was pushed so callers can later [dismiss] it
     */
    @JvmStatic
    @JvmOverloads
    fun send(
        title: String,
        message: String,
        type: NotificationType = NotificationType.INFO,
        icon: Image? = null,
        duration: Float = Notification.DEFAULT_DURATION,
        progress: Callable<Float>? = null,
        onClick: Runnable? = null,
        actions: List<NotificationAction> = emptyList(),
    ): Notification {
        val notification = Notification(title, message, type, icon, duration, progress, actions, onClick)
        NotificationsManager.push(notification)
        LOGGER.info("Pushed notification #{} ({}): {}", notification.id, type, title)
        return notification
    }

    @JvmStatic
    @JvmOverloads
    fun info(title: String, message: String, duration: Float = Notification.DEFAULT_DURATION): Notification =
        send(title, message, NotificationType.INFO, duration = duration)

    @JvmStatic
    @JvmOverloads
    fun success(title: String, message: String, duration: Float = Notification.DEFAULT_DURATION): Notification =
        send(title, message, NotificationType.SUCCESS, duration = duration)

    @JvmStatic
    @JvmOverloads
    fun error(title: String, message: String, duration: Float = Notification.DEFAULT_DURATION): Notification =
        send(title, message, NotificationType.ERROR, duration = duration)

    /**
     * Sends a [NotificationType.PROGRESS] notification driven by [progress]
     * which is a supplier returning `0..1`
     *
     * The toast stays up until the supplier reports `>= 1`
     */
    @JvmStatic
    fun progress(title: String, message: String, progress: Callable<Float>): Notification =
        send(title, message, NotificationType.PROGRESS, duration = -1f, progress = progress)

    @JvmStatic
    fun dismiss(notification: Notification) = NotificationsManager.dismiss(notification)

    @JvmStatic
    fun clearAll() = NotificationsManager.clearAll()

    @JvmStatic
    @JvmOverloads
    fun icon(resourcePath: String, size: Int = 64): Image? = SvgRasterizer.getByPath(resourcePath, size)

    @JvmStatic
    fun builder(title: String, message: String): Builder = Builder(title, message)

    class Builder internal constructor(private val title: String, private val message: String) {
        private var type: NotificationType = NotificationType.INFO
        private var icon: Image? = null
        private var duration: Float = Notification.DEFAULT_DURATION
        private var progress: Callable<Float>? = null
        private var onClick: Runnable? = null
        private val actions = ArrayList<NotificationAction>()

        fun type(type: NotificationType) = apply { this.type = type }
        fun icon(icon: Image?) = apply { this.icon = icon }
        fun duration(duration: Float) = apply { this.duration = duration }

        /**
         * Makes the notification persistent so it stays until dismissed or progress completes
         */
        fun persistent() = apply { this.duration = -1f }

        fun progress(progress: Callable<Float>?) = apply {
            this.progress = progress
            if (progress != null) this.type = NotificationType.PROGRESS
        }

        fun onClick(onClick: Runnable?) = apply { this.onClick = onClick }
        fun action(action: NotificationAction) = apply { this.actions.add(action) }
        fun action(label: String, onClick: Runnable) = apply { this.actions.add(NotificationAction(label, onClick = onClick)) }

        /**
         * Builds and pushes the notification
         */
        fun send(): Notification = send(title, message, type, icon, duration, progress, onClick, actions.toList())
    }
}
