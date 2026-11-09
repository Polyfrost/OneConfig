package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

object GlassTint {
    const val CLEAR = 0
    const val STAINED = 1
    const val BIOME = 2

    @JvmStatic
    var mode by mutableIntStateOf(CLEAR)
}

object ReduceTransparency {
    @JvmStatic
    var enabled by mutableStateOf(false)
}

class PaletteAccentOption(val name: String, val color: Color)

class PaletteInfo(
    val group: String,
    val label: String,
    val accents: List<PaletteAccentOption>,
    val defaultAccent: Int,
)

object PaletteAccent {
    const val PLAYER = -1
    const val RECOMMENDED = -2

    private val choices = mutableStateMapOf<String, Int>()

    fun choice(group: String): Int = choices[group] ?: RECOMMENDED

    fun set(group: String, value: Int) {
        if (value == RECOMMENDED) choices.remove(group) else choices[group] = value
    }

    @JvmStatic
    fun encode(): String = choices.entries.joinToString(",") { "${it.key}=${it.value}" }

    @JvmStatic
    fun load(encoded: String) {
        if (encoded == encode()) return
        val parsed = HashMap<String, Int>()
        encoded.split(',').forEach { entry ->
            val group = entry.substringBefore('=', "")
            val value = entry.substringAfter('=', "").toIntOrNull()
            if (group.isNotEmpty() && value != null) parsed[group] = value
        }
        choices.keys.retainAll(parsed.keys)
        parsed.forEach { (group, value) -> if (choices[group] != value) choices[group] = value }
        ThemeRegistry.activeTheme?.let(::syncNotificationTheme)
    }

    fun resolve(info: PaletteInfo): Color? = when (val choice = choice(info.group)) {
        PLAYER -> null
        RECOMMENDED -> info.accents.getOrNull(info.defaultAccent)?.color
        else -> info.accents.getOrNull(choice)?.color ?: info.accents.getOrNull(info.defaultAccent)?.color
    }
}

private val palettes = mutableStateMapOf<String, PaletteInfo>()

val UITheme.palette: PaletteInfo? get() = palettes[previewImage]

fun UITheme.withPalette(info: PaletteInfo): UITheme =
    also { palettes[previewImage] = info }

internal fun clearPalette(id: String) {
    palettes.remove(id)
}
