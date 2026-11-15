@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.internal

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import com.mojang.blaze3d.platform.NativeImage
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
import org.polyfrost.oneconfig.api.ui.v1.components.PlayerSkinLayers
import org.polyfrost.oneconfig.api.ui.v1.components.PlayerSkinLayersResult
import org.polyfrost.oneconfig.internal.ui.PlayerHeadTextureAccess
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
//? if > 1.8.9 {
import net.minecraft.client.resources.DefaultPlayerSkin
//?} else {
/*import net.minecraft.server.packs.resources.ResourceManager
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers
import net.minecraft.client.render.texture.HttpTexture
*///?}
//? if < 1.21.4 {
/*import com.mojang.blaze3d.platform.GlStateManager
import org.lwjgl.opengl.GL11
*///?}

class OneConfigUIPlayerHeadBridgeImpl : OneConfigUIPlayerHeadBridge {
    private class FaceImage(val pixels: PolyBitmap, val image: ImageBitmap)

    private class Face(val face: FaceImage?, val isDefault: Boolean)

    private val faces = object : LinkedHashMap<UUID, Face>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<UUID, Face>) = size > MAX_ENTRIES
    }

    private val observers = HashMap<UUID, MutableList<WeakReference<MutableState<ImageBitmap?>>>>()

    private val defaults = HashMap<String, FaceImage?>()

    private class LoadedFace(val key: String, val face: FaceImage?, val readAt: Long)

    private val loadedFaces = object : LinkedHashMap<UUID, LoadedFace>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<UUID, LoadedFace>) = size > MAX_ENTRIES
    }

    private class LoadedLayers(val key: String, val layers: PlayerSkinLayers?, val readAt: Long)

    private val loadedLayers = object : LinkedHashMap<UUID, LoadedLayers>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<UUID, LoadedLayers>) = size > MAX_ENTRIES
    }

    private val textureLayers = object : LinkedHashMap<String, PlayerSkinLayersResult>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, PlayerSkinLayersResult>) = size > MAX_ENTRIES
    }

    private val unreadableTextures = object : LinkedHashMap<String, Long>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Long>) = size > MAX_ENTRIES
    }

    private val fetching = HashMap<UUID, Int>()

    private val fetchedDefault = HashSet<UUID>()

    private val loaders: ScheduledExecutorService = Executors.newScheduledThreadPool(2) { task ->
        Thread(task, "OneConfig-PlayerHeads").apply { isDaemon = true }
    }

    init {
        EventManager.register(ResourceFinishedLoading::class.java, Runnable(::clearDefaults))
    }

    override fun face(id: UUID): ImageBitmap? = cachedFace(id)?.image

    override fun bitmap(id: UUID): PolyBitmap? = cachedFace(id)?.pixels

    override fun loadedFace(id: UUID): ImageBitmap? = clientFace(id)?.image ?: face(id)

    override fun loadedBitmap(id: UUID): PolyBitmap? = clientFace(id)?.pixels ?: bitmap(id)

    override fun localPlayerId(): UUID? = runCatching { Minecraft.getInstance().user.profileId }.getOrNull()

    override fun localPlayerName(): String? = runCatching { Minecraft.getInstance().user.name }.getOrNull()

    override fun skinTexture(id: UUID): String? = runCatching { clientSkinLocation(Minecraft.getInstance(), id) }.getOrNull()?.toString()

    override fun skinLayers(id: UUID): PlayerSkinLayers? {
        val mc = Minecraft.getInstance()
        if (!mc.isSameThread()) return synchronized(loadedLayers) { loadedLayers[id]?.layers }
        val location = runCatching { clientSkinLocation(mc, id) }.getOrNull() ?: return null
        val key = location.toString()
        val now = System.currentTimeMillis()
        synchronized(loadedLayers) {
            val known = loadedLayers[id]
            if (known != null && known.key == key && (known.layers != null || now - known.readAt < LOADED_RETRY_MS)) return known.layers
        }
        val layers = (readLayers(mc, key, location, rawFace = false) as? PlayerSkinLayersResult.Loaded)?.layers
        synchronized(loadedLayers) { loadedLayers[id] = LoadedLayers(key, layers, now) }
        return layers
    }

    override fun skinLayers(texture: String): PlayerSkinLayersResult {
        val now = System.currentTimeMillis()
        synchronized(textureLayers) {
            textureLayers[texture]?.let { return it }
            unreadableTextures[texture]?.let { if (now - it < LOADED_RETRY_MS) return PlayerSkinLayersResult.Unreadable }
        }
        val mc = Minecraft.getInstance()
        if (!mc.isSameThread()) return PlayerSkinLayersResult.Pending
        val location = parseTexture(texture) ?: return PlayerSkinLayersResult.Unreadable
        val result = readLayers(mc, texture, location, rawFace = true)
        synchronized(textureLayers) {
            when (result) {
                PlayerSkinLayersResult.Pending -> Unit
                PlayerSkinLayersResult.Unreadable -> unreadableTextures[texture] = now
                else -> {
                    textureLayers[texture] = result
                    unreadableTextures.remove(texture)
                }
            }
        }
        return result
    }

    private fun parseTexture(texture: String): Identifier? {
        //? if > 1.8.9 {
        return Identifier.tryParse(texture)
        //?} else
        //return runCatching { Identifier(texture) }.getOrNull()
    }

    private fun readLayers(mc: Minecraft, key: String, location: Identifier, rawFace: Boolean): PlayerSkinLayersResult {
        val layers = try {
            val texture = mc.textureManager.getTexture(location)
            val pixels = PlayerHeadTextureAccess.readPixels(texture)
            if (pixels != null) {
                buildLayers(key, pixels.width, pixels.height, rawFace) { x, y -> argb(pixels, x, y) }
            } else {
                //? if = 1.8.9 {
                /*if (texture == null || texture is HttpTexture) return PlayerSkinLayersResult.Pending
                *///?}
                val gpu = gpuImage(mc, location)
                val read = gpu?.image
                if (read != null) {
                    read.use { image -> buildLayers(key, image.width, image.height, rawFace) { x, y -> argb(image, x, y) } }
                } else {
                    val image = resourceImage(location)
                        ?: return if (gpu != null) PlayerSkinLayersResult.Pending else PlayerSkinLayersResult.Unreadable
                    buildLayers(key, image.width, image.height, rawFace) { x, y -> image.getRGB(x, y) }
                }
            }
        } catch (_: Exception) {
            return PlayerSkinLayersResult.Unreadable
        }
        return layers?.let(PlayerSkinLayersResult::Loaded) ?: PlayerSkinLayersResult.Unreadable
    }

    private class GpuImage(val image: NativeImage?)

    private fun gpuImage(mc: Minecraft, location: Identifier): GpuImage? {
        if (!mc.isSameThread()) return null
        //? if > 1.8.9 && < 1.21.4 {
        /*val texture = mc.textureManager.getTexture(location) ?: return null
        val previous = GlStateManager._getInteger(GL11.GL_TEXTURE_BINDING_2D)
        GlStateManager._bindTexture(texture.id)
        try {
            val width = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH)
            val height = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT)
            if (width <= 0 || height <= 0) return GpuImage(null)
            return GpuImage(downloaded(width, height))
        } finally {
            GlStateManager._bindTexture(previous)
        }
        *///?} elif = 1.8.9 {
        /*val texture = mc.textureManager.getTexture(location)
        if (texture == null || texture is HttpTexture) return GpuImage(null)
        val previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
        GlStateManager.bindTexture(texture.glId)
        try {
            val width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH)
            val height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT)
            if (width <= 0 || height <= 0) return GpuImage(null)
            return GpuImage(downloaded(width, height))
        } finally {
            GlStateManager.bindTexture(previous)
        }
        *///?} else {
        return null
        //?}
    }

    //? if < 1.21.4 {
    /*private fun downloaded(width: Int, height: Int): NativeImage {
        val image = NativeImage(width, height, false)
        try {
            image.downloadTexture(0, false)
        } catch (t: Throwable) {
            image.close()
            throw t
        }
        return image
    }
    *///?}

    private fun gpuFace(mc: Minecraft, location: Identifier): FaceImage? =
        gpuImage(mc, location)?.image?.use { image -> buildFace(image.width, image.height) { x, y -> argb(image, x, y) } }

    private fun clientFace(id: UUID): FaceImage? {
        val mc = Minecraft.getInstance()
        if (!mc.isSameThread()) return synchronized(loadedFaces) { loadedFaces[id]?.face }
        val location = runCatching { clientSkinLocation(mc, id) }.getOrNull() ?: return null
        val key = location.toString()
        val now = System.currentTimeMillis()
        synchronized(loadedFaces) {
            val known = loadedFaces[id]
            if (known != null && known.key == key && (known.face != null || now - known.readAt < LOADED_RETRY_MS)) return known.face
        }
        val face = runCatching { textureFace(mc, location) ?: gpuFace(mc, location) ?: resourceFace(location) }.getOrNull()
        synchronized(loadedFaces) { loadedFaces[id] = LoadedFace(key, face, now) }
        return face
    }

    private fun clientSkinLocation(mc: Minecraft, id: UUID): Identifier? {
        //? if > 1.8.9 {
        val skin = mc.player?.takeIf { it.uuid == id }?.skin ?: mc.connection?.getPlayerInfo(id)?.skin ?: return null
        //? if >= 1.21.10 {
        return skin.body().texturePath()
        //?} else
        //return skin.texture()
        //?} else {
        /*mc.player?.takeIf { it.uuid == id }?.let { return it.skin.texturePath() }
        return mc.networkHandler?.getOnlinePlayer(id)?.skinTexture
        *///?}
    }

    private fun textureFace(mc: Minecraft, location: Identifier): FaceImage? =
        texturePixels(mc, location) { width, height, rgb -> buildFace(width, height, rgb) }

    private fun resourceFace(location: Identifier): FaceImage? = resourceImage(location)?.let(::buildFace)

    private fun <T> texturePixels(mc: Minecraft, location: Identifier, read: (Int, Int, (Int, Int) -> Int) -> T): T? {
        val pixels = PlayerHeadTextureAccess.readPixels(mc.textureManager.getTexture(location)) ?: return null
        return read(pixels.width, pixels.height) { x, y -> argb(pixels, x, y) }
    }

    private fun resourceImage(location: Identifier): BufferedImage? {
        //? if > 1.8.9 {
        val resource = Minecraft.getInstance().resourceManager.getResource(location).orElse(null) ?: return null
        //?} else
        //val resource = ResourceManager.client().getResource(NamespacedIdentifiers.from(location.namespace, location.path)).orElse(null) ?: return null
        return resource.open().use { ImageIO.read(it) }
    }

    private fun argb(image: NativeImage, x: Int, y: Int): Int {
        //? if >= 1.21.4 {
        return image.getPixel(x, y)
        //?} else {
        /*val abgr = image.getPixelRGBA(x, y)
        return (abgr and 0xFF00FF00.toInt()) or ((abgr shr 16) and 0xFF) or ((abgr and 0xFF) shl 16)
        *///?}
    }

    private fun cachedFace(id: UUID): FaceImage? {
        synchronized(faces) { faces[id]?.let { return it.face } }
        ensureFetch(id)
        return null
    }

    override fun observe(id: UUID): State<ImageBitmap?> {
        val state = mutableStateOf<ImageBitmap?>(null)
        val known = synchronized(faces) {
            if (observers.size > MAX_ENTRIES) observers.values.removeAll { list -> list.removeAll { it.get() == null }; list.isEmpty() }
            val list = observers.getOrPut(id) { ArrayList() }
            list.removeAll { it.get() == null }
            list += WeakReference(state)
            faces[id]?.also { state.value = it.face?.image }
        }
        if (known == null) ensureFetch(id)
        return state
    }

    override fun defaultFace(id: UUID): ImageBitmap? = defaultFaceImage(id)?.image

    override fun defaultBitmap(id: UUID): PolyBitmap? = defaultFaceImage(id)?.pixels

    private fun defaultFaceImage(id: UUID): FaceImage? {
        val texture = defaultTexture(id) ?: return null
        return synchronized(defaults) {
            defaults.getOrPut(texture.key) { runCatching { texture.load() }.getOrNull() }
        }
    }

    private fun clearDefaults() {
        synchronized(defaults) { defaults.clear() }
        synchronized(loadedFaces) { loadedFaces.clear() }
        synchronized(loadedLayers) { loadedLayers.clear() }
        synchronized(textureLayers) {
            textureLayers.clear()
            unreadableTextures.clear()
        }
        val shown = synchronized(faces) {
            val ids = faces.filterValues { it.isDefault }.keys.toList()
            ids.forEach { faces.remove(it) }
            observers.keys.retainAll { id -> observers[id]?.any { it.get() != null } == true }
            ids.filter { it in observers }
        }
        if (shown.isEmpty()) return
        loaders.execute {
            for (id in shown) {
                synchronized(faces) { fetchedDefault += id }
                publish(id, defaultFaceImage(id), true)
            }
        }
    }

    private fun ensureFetch(id: UUID) {
        val start = synchronized(faces) {
            if (faces.containsKey(id) || id in fetching) return
            fetching[id] = 0
            true
        }
        if (start) loaders.execute { fetch(id) }
    }

    private fun publish(id: UUID, face: FaceImage?, settle: Boolean) {
        val bitmap = face?.image
        val listeners = synchronized(faces) {
            if (settle) {
                faces[id] = Face(face, isDefault = face == null || fetchedDefault.remove(id))
                fetching.remove(id)
            }
            observers[id]?.also { list -> list.removeAll { it.get() == null } }?.toList()
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
        val fallback = defaultFaceImage(id)
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

    private fun loadFace(id: UUID): FaceImage? {
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

    private class DefaultTexture(val key: String, val load: () -> FaceImage?)

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

    private fun buildFace(skin: BufferedImage): FaceImage = buildFace(skin.width, skin.height) { x, y -> skin.getRGB(x, y) }

    private fun buildFace(width: Int, height: Int, rgb: (Int, Int) -> Int): FaceImage {
        val scale = maxOf(1, width / 64)
        val face = 8 * scale
        val legacy = height <= 32 * scale
        val showHat = !legacy || !isOpaque(width, height, rgb, 32 * scale, 0, 64 * scale, 32 * scale)
        val out = ByteArray(FACE_SIZE * FACE_SIZE * 4)
        for (y in 0 until FACE_SIZE) {
            val oy = y * face / FACE_SIZE
            for (x in 0 until FACE_SIZE) {
                val ox = x * face / FACE_SIZE
                val base = rgb(face + ox, face + oy)
                val hat = if (showHat) runCatching { rgb(5 * face + ox, face + oy) }.getOrDefault(0) else 0
                val argb = blend(base, hat)
                val i = (y * FACE_SIZE + x) * 4
                out[i] = (argb ushr 16).toByte()
                out[i + 1] = (argb ushr 8).toByte()
                out[i + 2] = argb.toByte()
                out[i + 3] = 0xFF.toByte()
            }
        }
        val pixels = PolyBitmap(FACE_SIZE, FACE_SIZE, out)
        return FaceImage(pixels, pixels.asComposeImageBitmap())
    }

    private fun buildLayers(texture: String, width: Int, height: Int, rawFace: Boolean, rgb: (Int, Int) -> Int): PlayerSkinLayers? {
        val scale = width / 64
        if (scale < 1 || height < 16 * scale) return null
        val step = minOf(scale, MAX_LAYER_SCALE)
        val size = 8 * step
        val legacy = height <= 32 * scale
        val showHat = !legacy || !isOpaque(width, height, rgb, 32 * scale, 0, 64 * scale, 32 * scale)
        fun layer(u: Int, opaque: Boolean, visible: Boolean): PolyBitmap {
            val out = ByteArray(size * size * 4)
            if (!visible) return PolyBitmap(size, size, out)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val argb = rgb(u * scale + x * scale / step, 8 * scale + y * scale / step)
                    val i = (y * size + x) * 4
                    out[i] = (argb ushr 16).toByte()
                    out[i + 1] = (argb ushr 8).toByte()
                    out[i + 2] = argb.toByte()
                    out[i + 3] = if (opaque) 0xFF.toByte() else (argb ushr 24).toByte()
                }
            }
            return PolyBitmap(size, size, out)
        }
        return PlayerSkinLayers(texture, layer(8, opaque = !rawFace, visible = true), layer(40, opaque = false, visible = showHat))
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

    private fun isOpaque(width: Int, height: Int, rgb: (Int, Int) -> Int, x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        for (x in x0 until minOf(x1, width)) {
            for (y in y0 until minOf(y1, height)) {
                if (rgb(x, y) ushr 24 < 128) return false
            }
        }
        return true
    }

    private companion object {
        private val LOGGER = LogManager.getLogger("OneConfig/PlayerHeads")
        private const val MAX_ENTRIES = 256
        private const val FACE_SIZE = 64
        private const val MAX_LAYER_SCALE = 4
        private const val TIMEOUT_MS = 8000
        private const val MAX_ATTEMPTS = 6
        private const val MAX_DOUBLINGS = 5
        private const val RETRY_BASE_MS = 30_000L
        private const val RETRY_MAX_MS = 10L * 60 * 1000
        private const val RETRY_JITTER_MS = 5_000L
        private const val LOADED_RETRY_MS = 1_000L
        private val TEXTURES_VALUE = Regex("\"name\"\\s*:\\s*\"textures\"\\s*,\\s*\"value\"\\s*:\\s*\"([^\"]+)\"")
        private val SKIN_URL = Regex("\"SKIN\"\\s*:\\s*\\{\\s*\"url\"\\s*:\\s*\"([^\"]+)\"")
    }
}
