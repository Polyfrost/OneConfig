package org.polyfrost.compose.render

import java.io.File
import java.util.concurrent.ConcurrentHashMap

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
}
