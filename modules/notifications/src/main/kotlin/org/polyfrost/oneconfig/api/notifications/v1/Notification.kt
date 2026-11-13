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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyBitmap
import org.jetbrains.skia.Image

/**
 * The visual category of a [Notification]
 */
public enum class NotificationType(public val iconName: String) {
    INFO("info"),
    SUCCESS("check-circle"),
    ERROR("alert-circle"),
    PROGRESS("refresh"),

    @ApiStatus.Experimental
    WARNING("alert-circle");

    public val accent: PolyColor get() = NotificationTheme.accentFor(this)
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
public class NotificationAction @JvmOverloads constructor(
    public val label: String,
    public val primary: Boolean = false,
    @get:ApiStatus.Experimental
    public val dismissOnClick: Boolean = true,
    @get:ApiStatus.Experimental
    public val enabled: Boolean = true,
    public val onClick: Runnable = Runnable {},
) {
    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(label: String, primary: Boolean = false, onClick: Runnable = Runnable {}) : this(label, primary, true, true, onClick)
}

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
public class Notification @JvmOverloads constructor(
    title: String,
    message: String,
    type: NotificationType = NotificationType.INFO,
    public val icon: PolyBitmap? = null,
    @get:ApiStatus.Experimental
    public val iconName: String? = null,
    @get:ApiStatus.Experimental
    public val durationMillis: Long = DEFAULT_DURATION_MILLIS,
    public val persistent: Boolean = false,
    public val progress: Callable<Float>? = null,
    public val actions: List<NotificationAction> = emptyList(),
    public val onClick: Runnable? = null,
    @get:ApiStatus.Experimental
    public val onDismiss: Runnable? = null,
) : NotificationHandle {
    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(
        title: String,
        message: String,
        type: NotificationType = NotificationType.INFO,
        icon: PolyBitmap? = null,
        duration: Float = LEGACY_DURATION,
        progress: Callable<Float>? = null,
        actions: List<NotificationAction> = emptyList(),
        onClick: Runnable? = null,
    ) : this(title, message, type, icon, null, legacyMillis(duration), legacyPersistent(duration, progress), progress, actions, onClick, null)

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(title: String, message: String, type: NotificationType, icon: PolyBitmap?, duration: Float) :
        this(title, message, type, icon, null, legacyMillis(duration), legacyPersistent(duration, null), null, emptyList(), null, null)

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(title: String, message: String, type: NotificationType, icon: PolyBitmap?, duration: Float, progress: Callable<Float>?) :
        this(title, message, type, icon, null, legacyMillis(duration), legacyPersistent(duration, progress), progress, emptyList(), null, null)

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(title: String, message: String, type: NotificationType, icon: PolyBitmap?, duration: Float, progress: Callable<Float>?, actions: List<NotificationAction>) :
        this(title, message, type, icon, null, legacyMillis(duration), legacyPersistent(duration, progress), progress, actions, null, null)

    @Deprecated("Skia images are no longer supported", level = DeprecationLevel.HIDDEN)
    public constructor(
        title: String,
        message: String,
        type: NotificationType = NotificationType.INFO,
        icon: Image? = null,
        duration: Float = LEGACY_DURATION,
        progress: Callable<Float>? = null,
        actions: List<NotificationAction> = emptyList(),
        onClick: Runnable? = null,
    ) : this(title, message, type, null, null, legacyMillis(duration), legacyPersistent(duration, progress), progress, actions, onClick, null)

    @Deprecated("Skia images are no longer supported", level = DeprecationLevel.HIDDEN)
    public constructor(title: String, message: String, type: NotificationType, icon: Image?) :
        this(title, message, type, null as PolyBitmap?)

    @Deprecated("Skia images are no longer supported", level = DeprecationLevel.HIDDEN)
    public constructor(title: String, message: String, type: NotificationType, icon: Image?, duration: Float) :
        this(title, message, type, null, null, legacyMillis(duration), legacyPersistent(duration, null), null, emptyList(), null, null)

    @Deprecated("Skia images are no longer supported", level = DeprecationLevel.HIDDEN)
    public constructor(title: String, message: String, type: NotificationType, icon: Image?, duration: Float, progress: Callable<Float>?) :
        this(title, message, type, null, null, legacyMillis(duration), legacyPersistent(duration, progress), progress, emptyList(), null, null)

    @Deprecated("Skia images are no longer supported", level = DeprecationLevel.HIDDEN)
    public constructor(title: String, message: String, type: NotificationType, icon: Image?, duration: Float, progress: Callable<Float>?, actions: List<NotificationAction>) :
        this(title, message, type, null, null, legacyMillis(duration), legacyPersistent(duration, progress), progress, actions, null, null)

    @Deprecated("Skia images are no longer supported", level = DeprecationLevel.HIDDEN)
    @get:JvmName("getIcon")
    public val skiaIcon: Image? get() = null

    public var title: String by mutableStateOf(title)
        private set

    public var message: String by mutableStateOf(message)
        private set

    public var type: NotificationType by mutableStateOf(type)
        private set

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public val duration: Float get() = if (persistent) -1f else durationMillis.toFloat()

    public val id: Long = COUNTER.incrementAndGet()

    public val createdAt: Long = System.currentTimeMillis()

    internal var hovered by mutableStateOf(false)

    internal var dismissRequested by mutableStateOf(false)

    internal var revision by mutableIntStateOf(0)

    private var progressValue by mutableStateOf<Float?>(null)

    private var progressEnded by mutableStateOf(false)

    private val dismissFired = AtomicBoolean(false)

    /**
     * Whether this notification has been seen in the notifications center
     */
    public var read: Boolean by mutableStateOf(false)
        internal set

    internal val tracksProgress: Boolean get() = !progressEnded && (progressValue != null || progress != null)

    public fun progressOrNull(): Float? {
        if (progressEnded) return null
        progressValue?.let { return it.coerceIn(0f, 1f) }
        val p = progress ?: return null
        return runCatching { p.call() }.getOrNull()?.coerceIn(0f, 1f)
    }

    @ApiStatus.Experimental
    override fun update(title: String?, message: String?, type: NotificationType?, progress: Float?) {
        NotificationsManager.mutate {
            if (title != null) this.title = title
            if (message != null) this.message = message
            if (type != null) {
                if (progress == null && type != NotificationType.PROGRESS && type != this.type) progressEnded = true
                this.type = type
            }
            if (progress != null) {
                progressValue = progress.coerceIn(0f, 1f)
                progressEnded = false
            }
            if (title != null || message != null || type != null) read = false
            revision++
        }
    }

    @ApiStatus.Experimental
    override fun dismiss() {
        NotificationsManager.mutate { dismissRequested = true }
    }

    internal fun startProgress() {
        progressValue = 0f
    }

    internal fun fireDismissed(): Runnable? = if (dismissFired.compareAndSet(false, true)) onDismiss else null

    public companion object {
        @Deprecated("Use DEFAULT_DURATION_MILLIS", ReplaceWith("Notification.DEFAULT_DURATION_MILLIS"))
        public const val DEFAULT_DURATION: Float = 4000f

        @ApiStatus.Experimental
        public const val DEFAULT_DURATION_MILLIS: Long = 4000L

        private const val LEGACY_DURATION = 4000f

        private val COUNTER = AtomicLong()

        private fun legacyMillis(duration: Float): Long = if (duration > 0f) duration.toLong() else DEFAULT_DURATION_MILLIS

        private fun legacyPersistent(duration: Float, progress: Callable<Float>?): Boolean = duration <= 0f && progress == null
    }
}

internal fun legacyNotification(
    title: String,
    message: String,
    type: NotificationType,
    icon: PolyBitmap?,
    duration: Float,
    progress: Callable<Float>?,
    actions: List<NotificationAction>,
    onClick: Runnable?,
): Notification = Notification(
    title, message, type, icon, null,
    if (duration > 0f) duration.toLong() else Notification.DEFAULT_DURATION_MILLIS,
    duration <= 0f && progress == null,
    progress, actions, onClick, null,
)
