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

import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import org.apache.logging.log4j.LogManager
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.render.PolyBitmap
import org.jetbrains.skia.Image

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
public object Notifications {
    private val LOGGER = LogManager.getLogger("OneConfig/Notifications")

    private const val LEGACY_DURATION = 4000f

    private val sentOnce: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * Sends a fully specified notification
     *
     * All parameters except [title] and [message] are optional
     *
     * @return the [Notification] that was pushed so callers can later [dismiss] it
     */
    @JvmStatic
    @JvmOverloads
    public fun send(
        title: String,
        message: String,
        type: NotificationType = NotificationType.INFO,
        icon: PolyBitmap? = null,
        iconName: String? = null,
        durationMillis: Long = Notification.DEFAULT_DURATION_MILLIS,
        persistent: Boolean = false,
        progress: Callable<Float>? = null,
        actions: List<NotificationAction> = emptyList(),
        onClick: Runnable? = null,
        onDismiss: Runnable? = null,
    ): Notification =
        push(Notification(title, message, type, icon, iconName, durationMillis, persistent, progress, actions, onClick, onDismiss))

    @ApiStatus.Experimental
    @JvmStatic
    @JvmOverloads
    public fun sendOnce(
        key: String,
        title: String,
        message: String,
        type: NotificationType = NotificationType.INFO,
        icon: PolyBitmap? = null,
        iconName: String? = null,
        durationMillis: Long = Notification.DEFAULT_DURATION_MILLIS,
        persistent: Boolean = false,
        progress: Callable<Float>? = null,
        actions: List<NotificationAction> = emptyList(),
        onClick: Runnable? = null,
        onDismiss: Runnable? = null,
    ): Notification? {
        if (!sentOnce.add(key)) return null
        return send(title, message, type, icon, iconName, durationMillis, persistent, progress, actions, onClick, onDismiss)
    }

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    @JvmStatic
    @JvmName("send")
    public fun sendLegacy(
        title: String,
        message: String,
        type: NotificationType = NotificationType.INFO,
        icon: PolyBitmap? = null,
        duration: Float = LEGACY_DURATION,
        progress: Callable<Float>? = null,
        onClick: Runnable? = null,
        actions: List<NotificationAction> = emptyList(),
    ): Notification = push(legacyNotification(title, message, type, icon, duration, progress, actions, onClick))

    @Deprecated("Use the overload taking durationMillis: Long and persistent")
    @JvmStatic
    @JvmName("sendWithFloatDuration")
    public fun send(
        title: String,
        message: String,
        type: NotificationType = NotificationType.INFO,
        icon: PolyBitmap? = null,
        duration: Float,
        progress: Callable<Float>? = null,
        onClick: Runnable? = null,
        actions: List<NotificationAction> = emptyList(),
    ): Notification = push(legacyNotification(title, message, type, icon, duration, progress, actions, onClick))

    @Deprecated("Use the overload taking durationMillis: Long and persistent")
    @JvmStatic
    public fun send(title: String, message: String, type: NotificationType, icon: PolyBitmap?, duration: Float): Notification =
        push(legacyNotification(title, message, type, icon, duration, null, emptyList(), null))

    @Deprecated("Use the overload taking durationMillis: Long and persistent")
    @JvmStatic
    public fun send(title: String, message: String, type: NotificationType, icon: PolyBitmap?, duration: Float, progress: Callable<Float>?): Notification =
        push(legacyNotification(title, message, type, icon, duration, progress, emptyList(), null))

    @Deprecated("Use the overload taking durationMillis: Long and persistent")
    @JvmStatic
    public fun send(title: String, message: String, type: NotificationType, icon: PolyBitmap?, duration: Float, progress: Callable<Float>?, onClick: Runnable?): Notification =
        push(legacyNotification(title, message, type, icon, duration, progress, emptyList(), onClick))

    @Deprecated("Skia images are no longer supported", level = DeprecationLevel.HIDDEN)
    @JvmStatic
    @JvmName("send")
    public fun sendSkia(
        title: String,
        message: String,
        type: NotificationType = NotificationType.INFO,
        icon: Image? = null,
        duration: Float = LEGACY_DURATION,
        progress: Callable<Float>? = null,
        onClick: Runnable? = null,
        actions: List<NotificationAction> = emptyList(),
    ): Notification = push(legacyNotification(title, message, type, null, duration, progress, actions, onClick))

    @Deprecated("Skia images are no longer supported", level = DeprecationLevel.HIDDEN)
    @JvmStatic
    @JvmName("send")
    public fun sendSkia(title: String, message: String, type: NotificationType, icon: Image?): Notification =
        push(legacyNotification(title, message, type, null, LEGACY_DURATION, null, emptyList(), null))

    @Deprecated("Skia images are no longer supported", level = DeprecationLevel.HIDDEN)
    @JvmStatic
    @JvmName("send")
    public fun sendSkia(title: String, message: String, type: NotificationType, icon: Image?, duration: Float): Notification =
        push(legacyNotification(title, message, type, null, duration, null, emptyList(), null))

    @Deprecated("Skia images are no longer supported", level = DeprecationLevel.HIDDEN)
    @JvmStatic
    @JvmName("send")
    public fun sendSkia(title: String, message: String, type: NotificationType, icon: Image?, duration: Float, progress: Callable<Float>?): Notification =
        push(legacyNotification(title, message, type, null, duration, progress, emptyList(), null))

    @Deprecated("Skia images are no longer supported", level = DeprecationLevel.HIDDEN)
    @JvmStatic
    @JvmName("send")
    public fun sendSkia(title: String, message: String, type: NotificationType, icon: Image?, duration: Float, progress: Callable<Float>?, onClick: Runnable?): Notification =
        push(legacyNotification(title, message, type, null, duration, progress, emptyList(), onClick))

    private fun push(notification: Notification): Notification {
        NotificationsManager.push(notification)
        LOGGER.info("Pushed notification #{} ({}): {}", notification.id, notification.type, notification.title)
        return notification
    }

    @JvmStatic
    @JvmOverloads
    public fun info(title: String, message: String, durationMillis: Long = Notification.DEFAULT_DURATION_MILLIS): Notification =
        push(Notification(title, message, NotificationType.INFO, durationMillis = durationMillis))

    @JvmStatic
    @JvmOverloads
    public fun success(title: String, message: String, durationMillis: Long = Notification.DEFAULT_DURATION_MILLIS): Notification =
        push(Notification(title, message, NotificationType.SUCCESS, durationMillis = durationMillis))

    @JvmStatic
    @JvmOverloads
    public fun error(title: String, message: String, durationMillis: Long = Notification.DEFAULT_DURATION_MILLIS): Notification =
        push(Notification(title, message, NotificationType.ERROR, durationMillis = durationMillis))

    @ApiStatus.Experimental
    @JvmStatic
    @JvmOverloads
    public fun warning(title: String, message: String, durationMillis: Long = Notification.DEFAULT_DURATION_MILLIS): Notification =
        push(Notification(title, message, NotificationType.WARNING, durationMillis = durationMillis))

    @Deprecated("Use the overload taking durationMillis: Long")
    @JvmStatic
    public fun info(title: String, message: String, duration: Float): Notification =
        push(legacyNotification(title, message, NotificationType.INFO, null, duration, null, emptyList(), null))

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    @JvmStatic
    @JvmName("info\$default")
    public fun infoLegacyDefault(title: String, message: String, duration: Float, mask: Int, marker: Any?): Notification =
        push(legacyNotification(title, message, NotificationType.INFO, null, if (mask and 4 != 0) LEGACY_DURATION else duration, null, emptyList(), null))

    @Deprecated("Use the overload taking durationMillis: Long")
    @JvmStatic
    public fun success(title: String, message: String, duration: Float): Notification =
        push(legacyNotification(title, message, NotificationType.SUCCESS, null, duration, null, emptyList(), null))

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    @JvmStatic
    @JvmName("success\$default")
    public fun successLegacyDefault(title: String, message: String, duration: Float, mask: Int, marker: Any?): Notification =
        push(legacyNotification(title, message, NotificationType.SUCCESS, null, if (mask and 4 != 0) LEGACY_DURATION else duration, null, emptyList(), null))

    @Deprecated("Use the overload taking durationMillis: Long")
    @JvmStatic
    public fun error(title: String, message: String, duration: Float): Notification =
        push(legacyNotification(title, message, NotificationType.ERROR, null, duration, null, emptyList(), null))

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    @JvmStatic
    @JvmName("error\$default")
    public fun errorLegacyDefault(title: String, message: String, duration: Float, mask: Int, marker: Any?): Notification =
        push(legacyNotification(title, message, NotificationType.ERROR, null, if (mask and 4 != 0) LEGACY_DURATION else duration, null, emptyList(), null))

    /**
     * Sends a [NotificationType.PROGRESS] notification driven by [progress]
     * which is a supplier returning `0..1`
     *
     * The toast stays up until the supplier reports `>= 1`
     */
    @JvmStatic
    public fun progress(title: String, message: String, progress: Callable<Float>): Notification =
        push(Notification(title, message, NotificationType.PROGRESS, progress = progress))

    @ApiStatus.Experimental
    @JvmStatic
    public fun progress(title: String, message: String): Notification =
        push(Notification(title, message, NotificationType.PROGRESS).also { it.startProgress() })

    @JvmStatic
    public fun dismiss(notification: Notification): Unit = NotificationsManager.dismiss(notification)

    @JvmStatic
    public fun clearAll(): Unit = NotificationsManager.clearAll()

    @JvmStatic
    @JvmOverloads
    public fun icon(resourcePath: String, size: Int = 64): PolyBitmap? = SvgMask.get(resourcePath, size)

    @Deprecated("Skia images are no longer supported", level = DeprecationLevel.HIDDEN)
    @JvmStatic
    @JvmOverloads
    @JvmName("icon")
    public fun skiaIcon(resourcePath: String, size: Int = 64): Image? = null

    @JvmStatic
    public fun builder(title: String, message: String): Builder = Builder(title, message)

    public class Builder internal constructor(private val title: String, private val message: String) {
        private var type: NotificationType = NotificationType.INFO
        private var icon: PolyBitmap? = null
        private var iconName: String? = null
        private var durationMillis: Long = Notification.DEFAULT_DURATION_MILLIS
        private var persistent: Boolean = false
        private var progress: Callable<Float>? = null
        private var onClick: Runnable? = null
        private var onDismiss: Runnable? = null
        private val actions = ArrayList<NotificationAction>()

        public fun type(type: NotificationType): Builder = apply { this.type = type }
        public fun icon(icon: PolyBitmap?): Builder = apply { this.icon = icon }

        @ApiStatus.Experimental
        public fun icon(name: String): Builder = apply { this.iconName = name }

        @Deprecated("Skia images are no longer supported", level = DeprecationLevel.HIDDEN)
        @JvmName("icon")
        public fun skiaIcon(icon: Image?): Builder = apply { this.icon = null }

        @ApiStatus.Experimental
        public fun duration(durationMillis: Long): Builder = apply { this.durationMillis = durationMillis }

        @Deprecated("Use duration(durationMillis: Long) and persistent()")
        public fun duration(duration: Float): Builder = apply {
            this.durationMillis = if (duration > 0f) duration.toLong() else Notification.DEFAULT_DURATION_MILLIS
            this.persistent = duration <= 0f
        }

        /**
         * Makes the notification persistent so it stays until dismissed or progress completes
         */
        public fun persistent(): Builder = apply { this.persistent = true }

        @ApiStatus.Experimental
        public fun persistent(persistent: Boolean): Builder = apply { this.persistent = persistent }

        public fun progress(progress: Callable<Float>?): Builder = apply {
            this.progress = progress
            if (progress != null) this.type = NotificationType.PROGRESS
        }

        public fun onClick(onClick: Runnable?): Builder = apply { this.onClick = onClick }

        @ApiStatus.Experimental
        public fun onDismiss(onDismiss: Runnable?): Builder = apply { this.onDismiss = onDismiss }

        public fun action(action: NotificationAction): Builder = apply { this.actions.add(action) }
        public fun action(label: String, onClick: Runnable): Builder = apply { this.actions.add(NotificationAction(label, onClick = onClick)) }

        /**
         * Builds and pushes the notification
         */
        public fun send(): Notification =
            push(Notification(title, message, type, icon, iconName, durationMillis, persistent, progress, actions.toList(), onClick, onDismiss))
    }
}
