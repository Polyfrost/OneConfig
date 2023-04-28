/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.utils.discord

import cc.polyfrost.oneconfig.internal.config.Preferences
import cc.polyfrost.oneconfig.libs.universal.wrappers.UPlayer
import dev.cbyrne.kdiscordipc.KDiscordIPC
import dev.cbyrne.kdiscordipc.core.event.impl.ReadyEvent
import dev.cbyrne.kdiscordipc.data.activity.largeImage
import dev.cbyrne.kdiscordipc.data.activity.smallImage
import kotlinx.coroutines.*
import java.util.*

class DiscordRPC {

    companion object {

        // Constants & IPC
        private val CLIENT_ID = Preferences.applicationId.toLong()
        private val ipc = KDiscordIPC(CLIENT_ID.toString())

        // Attributes
        val details = "Playing Minecraft"
        val largeImage = "oneconfig_light"
        val smallImage = "oneconfig_light"

        /**
         * Starts a coroutine that checks RPC settings every 15 seconds, and connects/disconnects
         * the IPC accordingly. Launched in a separate scope to prevent blocking the main thread.
         */
        fun start() {
            val taskScope = CoroutineScope(Dispatchers.Default)
            taskScope.launch { startRPC() }
        }

//        fun start() {
//            val taskScope = CoroutineScope(Dispatchers.Default)
//            taskScope.launch {
//                while (true) {
//                    if (Preferences.discordRPC && !ipc.connected) {
//                        launch { startRPC() }
//                    } else if (!Preferences.discordRPC && ipc.connected) {
//                        launch { disconnectIPC() }
//                    }
//
//                    delay(15_000)
//                }
//            }
//        }

        fun stop() {
            val taskScope = CoroutineScope(Dispatchers.Default)
            taskScope.launch { disconnectIPC() }
        }

        fun update() {
            val taskScope = CoroutineScope(Dispatchers.Default);
            taskScope.launch { updateRPC() }
        }


        /**
         * Connects to the IPC and sets the activity. This is a suspend function, so
         * it must be called by a coroutine.
         */
        private suspend fun startRPC() {
            ipc.on<ReadyEvent> {
                ipc.activityManager.setActivity(details) {
                    largeImage(largeImage)
                }
            }
            ipc.connect()
        }

        private suspend fun updateRPC() {
            ipc.activityManager.setActivity(details) {
                when(Preferences.RPCStyleValue) {
                    1 -> {
                        smallImage(smallImage)
                    }
                    else -> {
                        largeImage(largeImage)
                    }
                }
            }
        }

        private fun disconnectIPC() {
            ipc.disconnect()
        }
    }
}