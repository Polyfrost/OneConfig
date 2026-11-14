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

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import org.apache.logging.log4j.LogManager
import org.jetbrains.annotations.ApiStatus

/**
 * Holds the set of currently-live notifications
 */
public object NotificationsManager {
    private const val MAX_HISTORY = 50

    private val LOGGER = LogManager.getLogger("OneConfig/Notifications")

    private val _active = mutableStateListOf<Notification>()
    private val _history = mutableStateListOf<Notification>()

    @JvmStatic
    public fun ensureInitialized() {
        if (_active.size < 0 || _history.size < 0) error("unreachable")
    }

    internal fun mutate(block: () -> Unit) {
        if (Snapshot.current.readOnly) Snapshot.global { Snapshot.withMutableSnapshot(block) }
        else Snapshot.withMutableSnapshot(block)
    }

    private fun fireDismissed(removed: List<Notification>) {
        for (notification in removed) {
            val callback = notification.fireDismissed() ?: continue
            runCatching { callback.run() }
                .onFailure { LOGGER.error("Notification #${notification.id} onDismiss threw", it) }
        }
    }

    /**
     * The notifications currently on screen with the oldest first
     */
    public val active: List<Notification> get() = _active

    /**
     * Every notification that has been pushed up to [MAX_HISTORY] with the newest first
     */
    public val history: List<Notification> get() = _history

    public val unreadCount: Int get() = _history.count { !it.read }

    /**
     * Adds [notification] to the live set so it begins rendering on the next frame
     *
     * Also records it in the [history] so it remains visible in the notifications center
     * after the toast dismisses
     */
    public fun push(notification: Notification) {
        mutate {
            _active.add(notification)
            _history.add(0, notification)
            while (_history.size > MAX_HISTORY) _history.removeAt(_history.lastIndex)
        }
    }

    public fun markAllRead() {
        mutate {
            for (notification in _history) notification.read = true
        }
    }

    public fun removeFromHistory(notification: Notification) {
        mutate {
            _history.remove(notification)
        }
    }

    /**
     * Fully retires [notification] by dropping it from history and dismissing it
     */
    public fun remove(notification: Notification) {
        mutate {
            notification.dismissRequested = true
            _history.remove(notification)
        }
    }

    public fun clearHistory() {
        mutate {
            _history.clear()
        }
    }

    /**
     * Removes [notification] immediately and skips any exit animation
     */
    public fun dismiss(notification: Notification) {
        var removed = false
        mutate {
            removed = _active.remove(notification)
        }
        if (removed) fireDismissed(listOf(notification))
    }

    public fun dismiss(id: Long) {
        val removed = ArrayList<Notification>()
        mutate {
            removed += _active.filter { it.id == id }
            _active.removeAll(removed)
        }
        fireDismissed(removed)
    }

    public fun clearAll() {
        val removed = ArrayList<Notification>()
        mutate {
            removed += _active
            _active.clear()
        }
        fireDismissed(removed)
    }

    public val count: Int get() = _active.size

    @ApiStatus.Internal
    public fun contains(notification: Notification): Boolean = _active.contains(notification)
}
