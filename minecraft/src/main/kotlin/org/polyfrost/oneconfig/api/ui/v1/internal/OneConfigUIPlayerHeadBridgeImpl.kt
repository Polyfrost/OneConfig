@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.internal

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import java.awt.image.BufferedImage
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.Proxy
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
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.ResourceFinishedLoading
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import net.minecraft.client.Minecraft
//? if > 1.8.9 {
import net.minecraft.client.resources.DefaultPlayerSkin
//?} else {
/*import net.ornithemc.osl.core.api.util.NamespacedIdentifiers
import net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager
*///?}

class OneConfigUIPlayerHeadBridgeImpl : OneConfigUIPlayerHeadBridge {
    private class Face(val bitmap: ImageBitmap?, val isDefault: Boolean)

    private val faces = object : LinkedHashMap<UUID, Face>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<UUID, Face>) = size > MAX_ENTRIES
    }

    private val observers = HashMap<UUID, MutableList<WeakReference<MutableState<ImageBitmap?>>>>()

    private val defaults = HashMap<String, ImageBitmap?>()

    private val fetching = HashMap<UUID, Int>()

    private val fetchedDefault = HashSet<UUID>()

    private val loaders: ScheduledExecutorService = Executors.newScheduledThreadPool(2) { task ->
        Thread(task, "OneConfig-PlayerHeads").apply { isDaemon = true }
    }

    init {
        EventManager.register(ResourceFinishedLoading::class.java, Runnable(::clearDefaults))
    }

    override fun face(id: UUID): ImageBitmap? {
        synchronized(faces) { faces[id]?.let { return it.bitmap } }
        ensureFetch(id)
        return null
    }

    override fun observe(id: UUID): State<ImageBitmap?> {
        val state = mutableStateOf<ImageBitmap?>(null)
        synchronized(faces) {
            val known = faces[id]
            if (known != null) {
                state.value = known.bitmap
                return state
            }
            observers.getOrPut(id) { ArrayList() } += WeakReference(state)
        }
        ensureFetch(id)
        return state
    }

    override fun defaultFace(id: UUID): ImageBitmap? {
        val texture = defaultTexture(id) ?: return null
        return synchronized(defaults) {
            defaults.getOrPut(texture.key) { runCatching { texture.load() }.getOrNull() }
        }
    }

    private fun clearDefaults() {
        synchronized(defaults) { defaults.clear() }
        synchronized(faces) { faces.values.removeAll { it.isDefault } }
    }

    private fun ensureFetch(id: UUID) {
        val start = synchronized(faces) {
            if (faces.containsKey(id) || id in fetching) return
            fetching[id] = 0
            true
        }
        if (start) loaders.execute { fetch(id) }
    }

    private fun publish(id: UUID, bitmap: ImageBitmap?, settle: Boolean) {
        val listeners = synchronized(faces) {
            if (settle) {
                faces[id] = Face(bitmap, isDefault = bitmap == null || fetchedDefault.remove(id))
                fetching.remove(id)
                observers.remove(id)
            } else {
                observers[id]?.also { list -> list.removeAll { it.get() == null } }?.toList()
            }
        } ?: return
        for (ref in listeners) {
            val state = ref.get() ?: continue
            if (settle || state.value == null) state.value = bitmap
        }
    }

    private fun fetch(id: UUID) {
        val outcome = runCatching { loadFace(id) }
        outcome.exceptionOrNull()?.let { LOGGER.debug("Failed to load the head for {}; will retry", id, it) }
        val face = outcome.getOrNull()
        val missing = outcome.isSuccess && face == null
        if (face != null) {
            publish(id, face, true)
            return
        }
        val fallback = defaultFace(id)
        if (missing) {
            synchronized(faces) { fetchedDefault += id }
            publish(id, fallback, true)
            return
        }
        val retryIn = synchronized(faces) {
            val attempts = (fetching[id] ?: 0) + 1
            fetching[id] = attempts
            if (attempts < MAX_ATTEMPTS) {
                (RETRY_BASE_MS shl (attempts - 1).coerceIn(0, MAX_DOUBLINGS)).coerceAtMost(RETRY_MAX_MS) + (0..RETRY_JITTER_MS).random()
            } else {
                null
            }
        }
        if (retryIn == null) {
            synchronized(faces) { fetchedDefault += id }
            publish(id, fallback, true)
            return
        }
        publish(id, fallback, false)
        loaders.schedule({ fetch(id) }, retryIn, TimeUnit.MILLISECONDS)
    }

    private fun loadFace(id: UUID): ImageBitmap? {
        val url = skinUrl(id) ?: return null
        val connection = URI(url).toURL().openConnection(proxy()) as HttpURLConnection
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
        val connection = URI(url).toURL().openConnection(proxy()) as HttpURLConnection
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

    private class DefaultTexture(val key: String, val load: () -> ImageBitmap?)

    private fun defaultTexture(id: UUID): DefaultTexture? {
        //? if > 1.8.9 {
        val skin = DefaultPlayerSkin.get(id)
        //? if >= 1.21.10 {
        val location = skin.body().texturePath()
        //?} else {
        /*val location = skin.texture()
        *///?}
        return DefaultTexture(location.toString()) {
            Minecraft.getInstance().resourceManager.getResource(location).orElse(null)
                ?.open()?.use { ImageIO.read(it) }?.let(::buildFace)
        }
        //?} else {
        /*val path = if ((id.hashCode() and 1) == 1) "textures/entity/alex.png" else "textures/entity/steve.png"
        return DefaultTexture(path) {
            ResourceManager.client().getResource(NamespacedIdentifiers.from("minecraft", path)).orElse(null)
                ?.open()?.use { ImageIO.read(it) }?.let(::buildFace)
        }
        *///?}
    }

    private fun proxy(): Proxy = runCatching {
        //? if > 1.8.9 {
        Minecraft.getInstance().proxy
        //?} else {
        /*val minecraft = Minecraft.getInstance()
        minecraft.javaClass.methods.first { it.parameterCount == 0 && it.returnType == Proxy::class.java }.invoke(minecraft) as Proxy
        *///?}
    }.getOrNull() ?: Proxy.NO_PROXY

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
                val argb = blend(base, hat)
                val i = (y * FACE_SIZE + x) * 4
                out[i] = (argb ushr 16).toByte()
                out[i + 1] = (argb ushr 8).toByte()
                out[i + 2] = argb.toByte()
                out[i + 3] = 0xFF.toByte()
            }
        }
        return PolyBitmap(FACE_SIZE, FACE_SIZE, out).asComposeImageBitmap()
    }

    private fun blend(base: Int, top: Int): Int {
        val alpha = top ushr 24
        if (alpha == 0) return base
        if (alpha == 255) return top
        fun channel(shift: Int): Int {
            val b = (base ushr shift) and 0xFF
            val t = (top ushr shift) and 0xFF
            return (t * alpha + b * (255 - alpha)) / 255
        }
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
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
