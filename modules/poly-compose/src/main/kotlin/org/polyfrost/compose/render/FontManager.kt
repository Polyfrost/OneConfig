package org.polyfrost.compose.render

import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.skia.Font
import org.jetbrains.skia.Typeface

object FontManager {
    private val fontData = ConcurrentHashMap<String, ByteArray>()
    private val pixelFonts = ConcurrentHashMap.newKeySet<String>()

    const val DEFAULT_KEY = "__default__"

    @Volatile
    var revision = 0
        private set

    fun invalidateMetrics() {
        revision++
    }

    /** @throws IllegalArgumentException if [bytes] isn't a TrueType or OpenType font */
    @JvmOverloads
    fun register(name: String, bytes: ByteArray, pixelAligned: Boolean = false) {
        require(isFont(bytes)) { "$name is not a TrueType or OpenType font" }
        fontData[name] = bytes
        if (pixelAligned) pixelFonts.add(name) else pixelFonts.remove(name)
        revision++
    }

    fun loadFromResource(path: String, name: String = DEFAULT_KEY, loader: ClassLoader? = null, pixelAligned: Boolean = false): Boolean {
        val cl = loader ?: FontManager::class.java.classLoader
        val bytes = (cl?.getResourceAsStream(path) ?: FontManager::class.java.getResourceAsStream(path))
            ?.use { it.readBytes() }?.takeIf(::isFont) ?: return false
        register(name, bytes, pixelAligned)
        return true
    }

    fun setDefault(name: String) {
        val bytes = fontData[name] ?: return
        register(DEFAULT_KEY, bytes, name in pixelFonts)
    }

    fun fontData(name: String): ByteArray? = fontData[name]

    fun isPixelAligned(name: String) = name in pixelFonts

    // sfnt version tags for TrueType, CFF OpenType, Apple TrueType and font collections
    private fun isFont(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val tag = (bytes[0].toInt() and 0xFF shl 24) or (bytes[1].toInt() and 0xFF shl 16) or (bytes[2].toInt() and 0xFF shl 8) or (bytes[3].toInt() and 0xFF)
        return tag == 0x00010000 || tag == 0x4F54544F || tag == 0x74727565 || tag == 0x74746366
    }

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    @JvmName("loadFromResource")
    fun loadFromResourceCompat(path: String, name: String = DEFAULT_KEY, loader: ClassLoader? = null): Boolean =
        loadFromResource(path, name, loader, false)

    @Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
    @JvmName("loadFromResource")
    fun loadDefaultFromResource(path: String) {
        loadFromResource(path)
    }

    @Deprecated("Skia is no longer supported", level = DeprecationLevel.HIDDEN)
    var typeface: Typeface?
        get() = null
        set(_) {}

    @Deprecated("Skia is no longer supported", level = DeprecationLevel.HIDDEN)
    @JvmName("register")
    @JvmOverloads
    fun registerTypeface(name: String, tf: Typeface?, pixelAligned: Boolean = false) {}

    @Deprecated("Skia is no longer supported", level = DeprecationLevel.HIDDEN)
    fun getFont(size: Float, name: String = DEFAULT_KEY): Font? = null

    @Deprecated("Skia is no longer supported", level = DeprecationLevel.HIDDEN)
    fun getBold(size: Float): Font? = null

    @Deprecated("Skia is no longer supported", level = DeprecationLevel.HIDDEN)
    fun getItalic(size: Float): Font? = null

    @Deprecated("Skia is no longer supported", level = DeprecationLevel.HIDDEN)
    fun flushCache(name: String) {}

    @Deprecated("Skia is no longer supported", level = DeprecationLevel.HIDDEN)
    fun clearCache() {}
}
