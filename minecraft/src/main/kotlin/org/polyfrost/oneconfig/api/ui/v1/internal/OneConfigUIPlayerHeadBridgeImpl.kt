@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import org.apache.logging.log4j.LogManager
import org.polyfrost.compose.backend.asComposeImageBitmap
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
//? if > 1.8.9 {
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.DefaultPlayerSkin
//?} else {
/*import net.ornithemc.osl.core.api.util.NamespacedIdentifiers
import net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager
*///?}

class OneConfigUIPlayerHeadBridgeImpl : OneConfigUIPlayerHeadBridge {
    private class Entry {
        var shown by mutableStateOf<ImageBitmap?>(null)

        var settled = false
    }

    private val entries = object : LinkedHashMap<UUID, Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<UUID, Entry>) = size > MAX_ENTRIES
    }

    private val defaults = HashMap<UUID, ImageBitmap?>()

    private val fetching = HashMap<UUID, Int>()

    private val loaders: ScheduledExecutorService = Executors.newScheduledThreadPool(2) { task ->
        Thread(task, "OneConfig-PlayerHeads").apply { isDaemon = true }
    }

    override fun face(id: UUID): ImageBitmap? {
        var fetch = false
        val entry = synchronized(entries) {
            val entry = entries.getOrPut(id) { Entry() }
            if (!entry.settled && id !in fetching) {
                fetching[id] = 0
                fetch = true
            }
            entry
        }
        if (fetch) loaders.execute { fetch(id) }
        return entry.shown
    }

    override fun defaultFace(id: UUID): ImageBitmap? = synchronized(defaults) {
        defaults.getOrPut(id) { runCatching { loadDefaultFace(id) }.getOrNull() }
    }

    private fun fetch(id: UUID) {
        val outcome = runCatching { loadFace(id) }
        outcome.exceptionOrNull()?.let { LOGGER.debug("Failed to load the head for {}; will retry", id, it) }
        val face = outcome.getOrNull()
        val missing = outcome.isSuccess && face == null
        val fallback = if (face == null) defaultFace(id) else null

        val retryIn = synchronized(entries) {
            val entry = entries[id] ?: run {
                fetching.remove(id)
                return@synchronized null
            }
            if (face != null || missing) {
                entry.shown = face ?: fallback
                entry.settled = true
                fetching.remove(id)
                return@synchronized null
            }
            if (entry.shown == null) entry.shown = fallback
            val attempts = (fetching[id] ?: 0) + 1
            fetching[id] = attempts
            if (attempts < MAX_ATTEMPTS) {
                (RETRY_BASE_MS shl (attempts - 1).coerceIn(0, MAX_DOUBLINGS)).coerceAtMost(RETRY_MAX_MS) + (0..RETRY_JITTER_MS).random()
            } else {
                entry.settled = true
                fetching.remove(id)
                null
            }
        }
        if (retryIn != null) loaders.schedule({ fetch(id) }, retryIn, TimeUnit.MILLISECONDS)
    }

    private fun loadFace(id: UUID): ImageBitmap? {
        val url = skinUrl(id) ?: return null
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        val skin = try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.inputStream.use { ImageIO.read(it) }
        } finally {
            connection.disconnect()
        } ?: return null
        return buildFace(skin)
    }

    private fun skinUrl(id: UUID): String? {
        val body = httpGet("https://sessionserver.mojang.com/session/minecraft/profile/${id.toString().replace("-", "")}") ?: return null
        val encoded = TEXTURES_VALUE.find(body)?.groupValues?.get(1) ?: return null
        val textures = String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8)
        return SKIN_URL.find(textures)?.groupValues?.get(1)
    }

    private fun httpGet(url: String): String? {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            when (val code = connection.responseCode) {
                200 -> connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                204, 404 -> null
                else -> throw IOException("GET $url returned $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun loadDefaultFace(id: UUID): ImageBitmap? {
        //? if > 1.8.9 {
        val skin = DefaultPlayerSkin.get(id)
        //? if >= 1.21.10 {
        val location = skin.body().texturePath()
        //?} else {
        /*val location = skin.texture()
        *///?}
        val image = Minecraft.getInstance().resourceManager.getResource(location).orElse(null)
            ?.open()?.use { ImageIO.read(it) } ?: return null
        //?} else {
        /*val path = if ((id.hashCode() and 1) == 1) "textures/entity/alex.png" else "textures/entity/steve.png"
        val image = ResourceManager.client().getResource(NamespacedIdentifiers.from("minecraft", path)).orElse(null)
            ?.open()?.use { ImageIO.read(it) } ?: return null
        *///?}
        return buildFace(image)
    }

    private fun buildFace(skin: BufferedImage): ImageBitmap {
        val legacy = skin.height <= 32
        val showHat = !legacy || !isOpaque(skin, 32, 0, 64, 32)
        val out = ByteArray(FACE_SIZE * FACE_SIZE * 4)
        for (y in 0 until FACE_SIZE) {
            val oy = y * 8 / FACE_SIZE
            for (x in 0 until FACE_SIZE) {
                val ox = x * 8 / FACE_SIZE
                val base = skin.getRGB(8 + ox, 8 + oy)
                val hat = if (showHat) runCatching { skin.getRGB(40 + ox, 8 + oy) }.getOrDefault(0) else 0
                val argb = if ((hat ushr 24) != 0) hat else base
                val i = (y * FACE_SIZE + x) * 4
                out[i] = (argb ushr 16).toByte()
                out[i + 1] = (argb ushr 8).toByte()
                out[i + 2] = argb.toByte()
                out[i + 3] = 0xFF.toByte()
            }
        }
        return PolyBitmap(FACE_SIZE, FACE_SIZE, out).asComposeImageBitmap()
    }

    private fun isOpaque(skin: BufferedImage, x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        for (x in x0 until minOf(x1, skin.width)) {
            for (y in y0 until minOf(y1, skin.height)) {
                if (skin.getRGB(x, y) ushr 24 < 128) return false
            }
        }
        return true
    }

    private companion object {
        private val LOGGER = LogManager.getLogger("OneConfig/PlayerHeads")
        private const val MAX_ENTRIES = 256
        private const val FACE_SIZE = 64
        private const val TIMEOUT_MS = 8000
        private const val MAX_ATTEMPTS = 6
        private const val MAX_DOUBLINGS = 5
        private const val RETRY_BASE_MS = 30_000L
        private const val RETRY_MAX_MS = 10L * 60 * 1000
        private const val RETRY_JITTER_MS = 5_000L
        private val TEXTURES_VALUE = Regex("\"name\"\\s*:\\s*\"textures\"\\s*,\\s*\"value\"\\s*:\\s*\"([^\"]+)\"")
        private val SKIN_URL = Regex("\"SKIN\"\\s*:\\s*\\{\\s*\"url\"\\s*:\\s*\"([^\"]+)\"")
    }
}
