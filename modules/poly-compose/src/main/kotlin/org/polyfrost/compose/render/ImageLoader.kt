package org.polyfrost.compose.render

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.skia.Image

object ImageLoader {
    private val cache = ConcurrentHashMap<String, PolyBitmap>()

    fun fromResource(path: String, loader: ClassLoader? = null): PolyBitmap? =
        cache.getOrPut("res:$path") {
            val cl = loader ?: ImageLoader::class.java.classLoader
            (cl?.getResourceAsStream(path) ?: ImageLoader::class.java.getResourceAsStream(path))
                ?.use { fromBytes(it.readBytes()) }
        }

    fun fromFile(path: String): PolyBitmap? =
        cache.getOrPut("file:$path") {
            runCatching { fromBytes(File(path).readBytes()) }.getOrNull()
        }

    fun fromBytes(bytes: ByteArray): PolyBitmap? =
        PolyBitmap.decode(bytes)

    fun put(key: String, image: PolyBitmap) {
        cache["custom:$key"] = image
    }

    fun get(key: String): PolyBitmap? = cache["custom:$key"]

    fun evict(resourcePath: String) {
        cache.remove("res:$resourcePath")
        cache.remove("file:$resourcePath")
    }

    fun evictKey(key: String) {
        cache.remove("custom:$key")
    }

    fun clear() = cache.clear()

    @Deprecated("Skia is no longer supported", level = DeprecationLevel.HIDDEN)
    @JvmName("fromResource")
    fun skiaFromResource(path: String, loader: ClassLoader? = null): Image? = null

    @Deprecated("Skia is no longer supported", level = DeprecationLevel.HIDDEN)
    @JvmName("fromFile")
    fun skiaFromFile(path: String): Image? = null

    @Deprecated("Skia is no longer supported", level = DeprecationLevel.HIDDEN)
    @JvmName("fromBytes")
    fun skiaFromBytes(bytes: ByteArray): Image? = null

    @Deprecated("Skia is no longer supported", level = DeprecationLevel.HIDDEN)
    @JvmName("put")
    fun skiaPut(key: String, image: Image?) {}

    @Deprecated("Skia is no longer supported", level = DeprecationLevel.HIDDEN)
    @JvmName("get")
    fun skiaGet(key: String): Image? = null
}
