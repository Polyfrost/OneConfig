package org.polyfrost.compose.render

import org.jetbrains.skia.Data
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface

object FontManager {
    private val typefaces  = HashMap<String, Typeface>()
    private val fontCache  = HashMap<Long, Font>()        // key is name.hashCode() << 32 | sizeAsInt

    private const val DEFAULT_KEY = "__default__"

    var typeface: Typeface?
        get()      = typefaces[DEFAULT_KEY]
        set(value) {
            if (value == null) typefaces.remove(DEFAULT_KEY) else typefaces[DEFAULT_KEY] = value
            flushCache(DEFAULT_KEY)
        }

    fun register(name: String, tf: Typeface) {
        typefaces[name] = tf
        flushCache(name)
    }

    fun loadFromResource(path: String, name: String = DEFAULT_KEY, loader: ClassLoader? = null): Boolean {
        val cl = loader ?: FontManager::class.java.classLoader
        val bytes = (cl?.getResourceAsStream(path) ?: FontManager::class.java.getResourceAsStream(path))
            ?.readBytes() ?: return false
        val tf = FontMgr.default.makeFromData(Data.makeFromBytes(bytes)) ?: return false
        register(name, tf)
        if (name == DEFAULT_KEY) typeface = tf
        return true
    }

    fun loadFromResource(path: String) { loadFromResource(path, DEFAULT_KEY) }

    fun setDefault(name: String) {
        typefaces[name]?.let { typeface = it }
    }

    fun getFont(size: Float, name: String = DEFAULT_KEY): Font {
        val cacheKey = (name.hashCode().toLong() shl 32) or (size.toBits().toLong() and 0xFFFFFFFFL)
        return fontCache.getOrPut(cacheKey) {
            val tf = typefaces[name] ?: Typeface.makeEmpty()
            Font(tf, size)
        }
    }

    fun getBold(size: Float): Font = getFont(size, "bold")

    fun getItalic(size: Float): Font = getFont(size, "italic")

    fun flushCache(name: String) {
        val prefix = name.hashCode().toLong() shl 32
        val keys = fontCache.keys.filter { it ushr 32 == prefix ushr 32 }
        keys.forEach { fontCache.remove(it) }
    }

    fun clearCache() = fontCache.clear()
}
