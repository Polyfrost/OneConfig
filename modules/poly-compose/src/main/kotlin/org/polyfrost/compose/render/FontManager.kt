package org.polyfrost.compose.render

import java.util.concurrent.ConcurrentHashMap

public object FontManager {
    private val fontData = ConcurrentHashMap<String, ByteArray>()
    private val pixelFonts = ConcurrentHashMap.newKeySet<String>()

    public const val DEFAULT_KEY: String = "__default__"

    @Volatile
    public var revision: Int = 0
        private set

    public fun invalidateMetrics() {
        revision++
    }

    /** @throws IllegalArgumentException if [bytes] isn't a TrueType or OpenType font */
    @JvmOverloads
    public fun register(name: String, bytes: ByteArray, pixelAligned: Boolean = false) {
        require(isReadableFont(bytes)) { "$name is not a TrueType or OpenType font" }
        fontData[name] = bytes
        if (pixelAligned) pixelFonts.add(name) else pixelFonts.remove(name)
        revision++
    }

    public fun loadFromResource(path: String, name: String = DEFAULT_KEY, loader: ClassLoader? = null, pixelAligned: Boolean = false): Boolean {
        val cl = loader ?: FontManager::class.java.classLoader
        val bytes = (cl?.getResourceAsStream(path) ?: FontManager::class.java.getResourceAsStream(path))
            ?.use { it.readBytes() }?.takeIf(::isReadableFont) ?: return false
        register(name, bytes, pixelAligned)
        return true
    }

    public fun setDefault(name: String) {
        val bytes = fontData[name] ?: return
        register(DEFAULT_KEY, bytes, name in pixelFonts)
    }

    public fun fontData(name: String): ByteArray? = fontData[name]

    public fun isPixelAligned(name: String): Boolean = name in pixelFonts

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    @JvmName("loadFromResource")
    public fun loadFromResourceCompat(path: String, name: String = DEFAULT_KEY, loader: ClassLoader? = null): Boolean =
        loadFromResource(path, name, loader, false)

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    @JvmName("loadFromResource")
    public fun loadDefaultFromResource(path: String) {
        loadFromResource(path)
    }
}
