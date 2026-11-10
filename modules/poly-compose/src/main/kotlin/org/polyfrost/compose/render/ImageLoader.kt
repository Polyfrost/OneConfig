package org.polyfrost.compose.render

import java.io.File
import java.util.concurrent.ConcurrentHashMap

public object ImageLoader {
    private val cache = ConcurrentHashMap<String, PolyBitmap>()

    public fun fromResource(path: String, loader: ClassLoader? = null): PolyBitmap? =
        cache.getOrPut("res:$path") {
            val cl = loader ?: ImageLoader::class.java.classLoader
            (cl?.getResourceAsStream(path) ?: ImageLoader::class.java.getResourceAsStream(path))
                ?.use { fromBytes(it.readBytes()) }
        }

    public fun fromFile(path: String): PolyBitmap? =
        cache.getOrPut("file:$path") {
            runCatching { fromBytes(File(path).readBytes()) }.getOrNull()
        }

    public fun fromBytes(bytes: ByteArray): PolyBitmap? =
        PolyBitmap.decode(bytes)

    public fun put(key: String, image: PolyBitmap) {
        cache["custom:$key"] = image
    }

    public fun get(key: String): PolyBitmap? = cache["custom:$key"]

    public fun evict(resourcePath: String) {
        cache.remove("res:$resourcePath")
        cache.remove("file:$resourcePath")
    }

    public fun evictKey(key: String) {
        cache.remove("custom:$key")
    }

    public fun clear(): Unit = cache.clear()
}
