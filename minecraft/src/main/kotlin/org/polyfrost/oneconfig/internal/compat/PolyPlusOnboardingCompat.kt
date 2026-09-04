package org.polyfrost.oneconfig.internal.compat

object PolyPlusOnboardingCompat {
    private const val CONFIG_CLASS = "org.polyfrost.polyplus.client.PolyPlusConfig"

    private val configClass: Class<*>? by lazy {
        runCatching { Class.forName(CONFIG_CLASS, false, javaClass.classLoader) }.getOrNull()
    }

    val present: Boolean get() = configClass != null

    fun completed(): Boolean {
        val clazz = configClass ?: return false
        return runCatching {
            clazz.getMethod("getOnboardingCompleted").invoke(null) as? Boolean
        }.getOrNull() ?: false
    }
}
