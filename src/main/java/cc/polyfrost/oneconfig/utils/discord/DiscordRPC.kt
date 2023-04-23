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
import cc.polyfrost.oneconfig.utils.discord.exception.RPCCreationException
import cc.polyfrost.oneconfig.utils.discord.exception.RPCDownloadFail
import de.jcm.discordgamesdk.Core
import de.jcm.discordgamesdk.CreateParams
import de.jcm.discordgamesdk.activity.Activity
import org.spongepowered.asm.mixin.MixinEnvironment

class DiscordRPC {

    // Constants
    val CLIENT_ID = Preferences.applicationId.toLong()

    // Start DiscordPRC
    @Throws(RPCCreationException::class)
    fun start() {
        // Define library & throw exception on failure
        val library = DiscordNativeLibrary().downloadLibrary() ?: throw RPCDownloadFail("Failed to download Discord SDK.")

        // Initialize core
        Core.init(library)

        try {
            val params = CreateParams()

            // Set client ID
            params.clientID = CLIENT_ID
            params.setFlags(CreateParams.getDefaultFlags())

            // Create core
            try {
                val core = Core(params)

                try {
                    val activity = Activity()
                    updateActivity(core)
                } catch (e: Exception) {
                    throw RPCDownloadFail("Failed to create Discord RPC activity.")
                }
//                Thread {
//                    while (true) {
//                        core.runCallbacks()
//                        Thread.sleep(1000)
//                    }
//                }.start()
            } catch (e: Exception) {
                throw RPCDownloadFail("Failed to create Discord RPC core.")
            }
        } catch (e: Exception) {
            throw RPCDownloadFail("Failed to create Discord RPC parameters.")
        }
    }

    fun updateActivity(core: Core) {
        try {
            val activity = Activity()

            // Set activity details
            activity.details = "Playing Minecraft " + MixinEnvironment.getCurrentEnvironment().version
            activity.assets().smallImage = "oneconfig_light"

            core.activityManager().updateActivity(activity)
        } catch (e: Exception) {
            throw RPCDownloadFail("Failed to update Discord RPC activity.")
        }
    }
}