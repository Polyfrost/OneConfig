package org.polyfrost.compose.render

import org.jetbrains.skia.Image
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object ImageLoader {
    private val cache = ConcurrentHashMap<String, Image>()

    fun fromResource(path: String, loader: ClassLoader? = null): Image? =
        cache.getOrPut("res:$path") {
            val cl = loader ?: ImageLoader::class.java.classLoader
            (cl?.getResourceAsStream(path) ?: ImageLoader::class.java.getResourceAsStream(path))
                ?.use { fromBytes(it.readBytes()) }
        }

    fun fromFile(path: String): Image? =
        cache.getOrPut("file:$path") {
            runCatching { fromBytes(File(path).readBytes()) }.getOrNull()
        }

    fun fromBytes(bytes: ByteArray): Image? =
        runCatching { Image.makeFromEncoded(bytes) }.getOrNull()

    fun put(key: String, image: Image) {
        cache["custom:$key"] = image
    }

    fun get(key: String): Image? = cache["custom:$key"]

    fun evict(resourcePath: String) {
        cache.remove("res:$resourcePath")
        cache.remove("file:$resourcePath")
    }

    fun evictKey(key: String) {
        cache.remove("custom:$key")
    }

    fun clear() = cache.clear()
}
