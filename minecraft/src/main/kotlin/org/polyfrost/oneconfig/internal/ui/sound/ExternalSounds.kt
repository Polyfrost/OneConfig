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

package org.polyfrost.oneconfig.internal.ui.sound

import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.utils.v1.JsonUtils
import org.polyfrost.oneconfig.utils.v1.Multithreading
import org.polyfrost.oneconfig.utils.v1.NetworkUtils
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Downloads the large UI sound files that are intentionally not shipped inside the jar (issue #627) and
 * exposes them to Minecraft's sound engine via [OneConfigSoundPackSource], a built-in resource pack rooted
 * at [OneConfigSoundPackSource.PACK_ROOT].
 *
 * The manifest (`assets/oneconfig/sounds/external_sounds.json`) lists each file together with a SHA-1 hash;
 * a file is (re)downloaded only when missing or hash-mismatched, so subsequent launches are offline-friendly.
 */
object ExternalSounds {
    private val LOGGER = LogManager.getLogger("OneConfig/Sounds")
    private const val MANIFEST = "/assets/oneconfig/sounds/external_sounds.json"

    /** Where downloaded oggs live inside the pack: `<PACK_ROOT>/assets/oneconfig/sounds/<rel>`. */
    private val assetsRoot: Path get() = OneConfigSoundPackSource.PACK_ROOT.resolve("assets/oneconfig/sounds")

    private val started = AtomicBoolean(false)

    fun ensureDownloaded() {
        if (!started.compareAndSet(false, true)) return
        try {
            writePackMeta()
        } catch (t: Throwable) {
            LOGGER.warn("Failed to write sound pack metadata", t)
        }
        Multithreading.submit {
            try {
                val changed = download()
                if (changed) {
                    val mc = Minecraft.getInstance()
                    mc.execute {
                        try {
                            mc.reloadResourcePacks()
                        } catch (t: Throwable) {
                            LOGGER.warn("Failed to reload resources after downloading sounds", t)
                        }
                    }
                }
            } catch (t: Throwable) {
                LOGGER.warn("Failed to prepare downloaded sounds", t)
            }
        }
    }

    private fun download(): Boolean {
        val manifest = javaClass.getResourceAsStream(MANIFEST)?.use {
            JsonUtils.parse(it.readBytes().toString(Charsets.UTF_8)).asJsonObject
        } ?: run {
            LOGGER.warn("Missing sound manifest $MANIFEST")
            return false
        }

        val baseUrl = manifest["baseUrl"].asString
        val files = manifest.getAsJsonObject("files")
        var changed = false

        for ((rel, value) in files.entrySet()) {
            val entry = value as JsonObject
            val sha1 = entry["sha1"].asString
            val target = assetsRoot.resolve(rel)

            if (Files.exists(target) && sha1Of(target).equals(sha1, ignoreCase = true)) continue

            Files.createDirectories(target.parent)
            val url = baseUrl + rel
            LOGGER.info("Downloading sound {} -> {}", url, target)
            if (!NetworkUtils.downloadFile(url, target, NetworkUtils.DEF_AGENT, 30000, false)) {
                LOGGER.warn("Failed to download sound from {}", url)
                continue
            }
            if (!sha1Of(target).equals(sha1, ignoreCase = true)) {
                LOGGER.warn("Hash mismatch for downloaded sound {}, discarding", rel)
                runCatching { Files.deleteIfExists(target) }
                continue
            }
            changed = true
        }
        return changed
    }

    private fun writePackMeta() {
        val meta = OneConfigSoundPackSource.PACK_ROOT.resolve("pack.mcmeta")
        Files.createDirectories(meta.parent)
        Files.write(meta, PACK_META.toByteArray(Charsets.UTF_8))
    }

    private const val PACK_META =
        """{"pack":{"description":"OneConfig downloaded sounds","pack_format":64,"supported_formats":{"min_inclusive":0,"max_inclusive":2147483647},"min_format":0,"max_format":2147483647}}"""

    private fun sha1Of(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-1")
        Files.newInputStream(path).use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
