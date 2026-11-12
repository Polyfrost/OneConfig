package org.polyfrost.oneconfig.internal.compat

//? if >= 1.21.11 {
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import org.polyfrost.oneconfig.api.platform.v1.ModInfo

interface GnetumOffscreenState {
    fun `oneconfig$markOffscreen`()
}

object GnetumCompat {
    private val rendering: Field? by lazy {
        runCatching {
            if (ModInfo.loadedMods.none { it.id == "gnetum" }) return@runCatching null
            Class.forName("me.decce.gnetum.Gnetum").getField("rendering")
                .takeIf { it.type == Boolean::class.javaPrimitiveType && Modifier.isStatic(it.modifiers) && !Modifier.isFinal(it.modifiers) }
        }.getOrNull()
    }

    private var suppressed = false

    @JvmStatic
    fun suppress() {
        if (suppressed) return
        val field = rendering ?: return
        if (runCatching { field.getBoolean(null) }.getOrDefault(false) == false) return
        if (runCatching { field.setBoolean(null, false) }.isSuccess) suppressed = true
    }

    @JvmStatic
    fun restore() {
        if (!suppressed) return
        suppressed = false
        runCatching { rendering?.setBoolean(null, true) }
    }

    internal inline fun <T> restoring(block: () -> T): T {
        try {
            return block()
        } finally {
            restore()
        }
    }
}
//?}
