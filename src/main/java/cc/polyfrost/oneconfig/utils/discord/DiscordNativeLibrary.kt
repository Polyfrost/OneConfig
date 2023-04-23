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

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


class DiscordNativeLibrary {

    fun downloadLibrary(): File? {
        println("Downloading Discord RPC library...")
        val name = "discord_game_sdk"
        var suffix = ""

        val osName = System.getProperty("os.name").lowercase()
        var osArch = System.getProperty("os.arch").lowercase()

        // Determine OS suffix
        when {
            osName.contains("windows") -> {
                suffix = ".dll"
            }

            osName.contains("mac os") -> {
                suffix = ".dylib"
            }

            osName.contains("linux") -> {
                suffix = ".so"
            }

            else -> {
                throw UnsupportedOperationException("RPC - Unsupported OS: $osName")
            }
        }

        if (osArch.contains("amd64")) {
            osArch = "x86_64"
        }

        // Define library path inside ZIP
        val zipPath = "lib/$osArch/$name$suffix"

        // Load library from ZIP

        val url = URL("https://dl-game-sdk.discordapp.net/2.5.6/discord_game_sdk.zip")
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.setRequestProperty("User-Agent", "discord-game-sdk4j (https://github.com/JnCrMx/discord-game-sdk4j)")
        val zin = ZipInputStream(connection.inputStream)

        // Search for library inside ZIP

        var entry: ZipEntry? = null
        while (zin.nextEntry.also { entry = it } != null) {
            if (entry!!.name.equals(zipPath)) {
                // Create a new temporary directory
                // We need to do this, because we may not change the filename on Windows
                val tempDir = File(System.getProperty("java.io.tmpdir"), "java-" + name + System.nanoTime())
                if (!tempDir.mkdir()) throw IOException("Cannot create temporary directory")
                tempDir.deleteOnExit()

                // Create a temporary file inside our directory (with a "normal" name)
                val temp = File(tempDir, name + suffix)
                temp.deleteOnExit()

                // Copy the file in the ZIP to our temporary file
                Files.copy(zin, temp.toPath())

                // We are done, so close the input stream
                zin.close()

                // Return our temporary file
                return temp
            }
            // next entry
            zin.closeEntry()
        }
        zin.close()
        return null
    }
}