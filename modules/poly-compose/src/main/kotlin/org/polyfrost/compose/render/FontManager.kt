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

    // checks the table directory and the tables stb_truetype needs, so unreadable fonts fail here rather than when first drawn
    private fun isFont(bytes: ByteArray, at: Int = 0): Boolean {
        fun u16(o: Int) = (bytes[o].toInt() and 0xFF shl 8) or (bytes[o + 1].toInt() and 0xFF)
        fun u32(o: Int) = (u16(o).toLong() shl 16) or u16(o + 2).toLong()
        if (at < 0 || bytes.size - at < 12) return false
        when (String(bytes, at, 4, Charsets.ISO_8859_1)) {
            "ttcf" -> return at == 0 && bytes.size >= 16 && u32(12) < bytes.size && isFont(bytes, u32(12).toInt())
            "\u0000\u0001\u0000\u0000", "OTTO", "true" -> {}
            else -> return false
        }
        val count = u16(at + 4)
        if (at + 12 + count * 16 > bytes.size) return false
        val tables = HashSet<String>()
        for (i in 0 until count) {
            val record = at + 12 + i * 16
            if (u32(record + 8) + u32(record + 12) > bytes.size) return false
            tables += String(bytes, record, 4, Charsets.ISO_8859_1)
        }
        return tables.containsAll(REQUIRED_TABLES) && ("CFF " in tables || "glyf" in tables && "loca" in tables)
    }

    private val REQUIRED_TABLES = listOf("cmap", "head", "hhea", "hmtx")

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
