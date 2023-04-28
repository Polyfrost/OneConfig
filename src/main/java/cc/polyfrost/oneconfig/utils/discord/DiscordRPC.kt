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
import cc.polyfrost.oneconfig.utils.dsl.schedule
import dev.cbyrne.kdiscordipc.KDiscordIPC
import dev.cbyrne.kdiscordipc.core.event.impl.ReadyEvent
import dev.cbyrne.kdiscordipc.data.activity.button
import dev.cbyrne.kdiscordipc.data.activity.largeImage
import dev.cbyrne.kdiscordipc.data.activity.smallImage
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer

class DiscordRPC {

    // Discord application client ID
    val CLIENT_ID = Preferences.applicationId.toLong()
    lateinit var ipc: KDiscordIPC

    // Attributes
    var running = false;
    var details = "Playing Minecraft"
    var largeImage = "oneconfig_light"
    var smallImage = "oneconfig_light"

    // Add compatability with Java
    fun start() {
        val timer = Timer()
        val task = object : TimerTask() {
            override fun run() = runBlocking {
                if (Preferences.discordRPC) {
                    // RPC Enabled
                    if (!running) startRPC() // If switch toggled but RPC isn't running, start
                    updateRPC()
                } else {
                    if (running) disconnectIPC() // If switch disabled & RPC running, disconnect
                }
            }
        }
        timer.scheduleAtFixedRate(task, 0, 15000)
    }

    private suspend fun startRPC() {
        if (Preferences.discordRPC) {
            ipc = KDiscordIPC(CLIENT_ID.toString())

            ipc.on<ReadyEvent> {
                updateRPC()
            }

            ipc.connect()
        }
    }

    fun disconnectIPC() {
        ipc.disconnect()
    }

    private suspend fun updateRPC() {
        // If RPC is running
        ipc.activityManager.setActivity(details) {
            when (Preferences.rpcInfoAmount) {
                0 -> {
                    largeImage(largeImage)
                }

                1 -> {
                    smallImage(smallImage)
                }
            }
        }
    }
}