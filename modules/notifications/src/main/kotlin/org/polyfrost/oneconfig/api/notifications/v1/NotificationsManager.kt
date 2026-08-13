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
import org.jetbrains.annotations.ApiStatus

/**
 * Holds the set of currently-live notifications
 */
object NotificationsManager {
    private const val MAX_HISTORY = 50

    private val _active = mutableStateListOf<Notification>()
    private val _history = mutableStateListOf<Notification>()

    @JvmStatic
    fun ensureInitialized() {
        if (_active.size < 0 || _history.size < 0) error("unreachable")
    }

    private fun mutate(block: () -> Unit) {
        if (Snapshot.current.readOnly) block() else Snapshot.withMutableSnapshot(block)
    }

    /**
     * The notifications currently on screen with the oldest first
     */
    val active: List<Notification> get() = _active

    /**
     * Every notification that has been pushed up to [MAX_HISTORY] with the newest first
     */
    val history: List<Notification> get() = _history

    val unreadCount: Int get() = _history.count { !it.read }

    /**
     * Adds [notification] to the live set so it begins rendering on the next frame
     *
     * Also records it in the [history] so it remains visible in the notifications center
     * after the toast dismisses
     */
    fun push(notification: Notification) {
        mutate {
            _active.add(notification)
            _history.add(0, notification)
            while (_history.size > MAX_HISTORY) _history.removeAt(_history.lastIndex)
        }
    }

    fun markAllRead() {
        mutate {
            for (notification in _history) notification.read = true
        }
    }

    fun removeFromHistory(notification: Notification) {
        mutate {
            _history.remove(notification)
        }
    }

    /**
     * Fully retires [notification] by dropping it from history and dismissing it
     */
    fun remove(notification: Notification) {
        mutate {
            notification.dismissRequested = true
            _history.remove(notification)
        }
    }

    fun clearHistory() {
        mutate {
            _history.clear()
        }
    }

    /**
     * Removes [notification] immediately and skips any exit animation
     */
    fun dismiss(notification: Notification) {
        mutate {
            _active.remove(notification)
        }
    }

    fun dismiss(id: Long) {
        mutate {
            _active.removeAll { it.id == id }
        }
    }

    fun clearAll() {
        mutate {
            _active.clear()
        }
    }

    val count: Int get() = _active.size

    @ApiStatus.Internal
    fun contains(notification: Notification): Boolean = _active.contains(notification)
}
