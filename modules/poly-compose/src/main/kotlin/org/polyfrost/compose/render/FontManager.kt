package org.polyfrost.compose.render

object FontManager {
    private val fontData = HashMap<String, ByteArray>()
    private val pixelFonts = HashSet<String>()

    const val DEFAULT_KEY = "__default__"

    @Volatile
    var revision = 0
        private set

    @JvmOverloads
    fun register(name: String, bytes: ByteArray, pixelAligned: Boolean = false) {
        fontData[name] = bytes
        if (pixelAligned) pixelFonts.add(name) else pixelFonts.remove(name)
        revision++
    }

    @JvmOverloads
    fun loadFromResource(path: String, name: String = DEFAULT_KEY, loader: ClassLoader? = null, pixelAligned: Boolean = false): Boolean {
        val cl = loader ?: FontManager::class.java.classLoader
        val bytes = (cl?.getResourceAsStream(path) ?: FontManager::class.java.getResourceAsStream(path))
            ?.use { it.readBytes() } ?: return false
        register(name, bytes, pixelAligned)
        return true
    }

    fun setDefault(name: String) {
        val bytes = fontData[name] ?: return
        register(DEFAULT_KEY, bytes, name in pixelFonts)
    }

    fun fontData(name: String): ByteArray? = fontData[name]

    fun isPixelAligned(name: String) = name in pixelFonts
}
