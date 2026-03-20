package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.internal.ui.MinecraftDark
import org.polyfrost.oneconfig.internal.ui.MinecraftLight
import org.polyfrost.oneconfig.internal.ui.PolyGlassDark
import org.polyfrost.oneconfig.internal.ui.PolyGlassLight
import org.polyfrost.oneconfig.internal.ui.api.settings.BuiltinVisualizers

object ThemeRegistry {
    internal val registry = mutableStateListOf<UITheme>()
    internal var activeTheme by mutableStateOf<UITheme?>(null)

    private const val CONFIG_ID = "oneconfig/theme"
    private const val THEME_KEY = "activeTheme"
    private var configTree: Tree? = null

    init {
        register(PolyGlassDark)
        register(PolyGlassLight)
        register(MinecraftDark)
        register(MinecraftLight)

        activate(PolyGlassDark)

        BuiltinVisualizers.register()
    }

    fun init() {}

    fun register(theme: UITheme) {
        registry += theme
    }

    fun activate(theme: UITheme) {
        activeTheme = theme
        saveThemeToConfig()
    }

    fun loadFromConfig() {
        try {
            val existing = ConfigManager.active().get(CONFIG_ID)
            if (existing != null) {
                configTree = existing
                val savedName = existing.getProp(THEME_KEY)?.getAs<String?>()
                if (savedName != null) {
                    val theme = registry.find { it.name == savedName }
                    if (theme != null) activeTheme = theme
                }
            } else {
                val tree = Tree.tree(CONFIG_ID)
                tree.addMetadata("hidden", true)
                tree[THEME_KEY] = Properties.simple<String>(activeTheme?.name ?: "PolyGlass Dark")
                ConfigManager.active().register(tree)
                configTree = tree
            }
        } catch (_: Exception) {}
    }

    @Suppress("UNCHECKED_CAST")
    private fun saveThemeToConfig() {
        val tree = configTree ?: return
        val theme = activeTheme ?: return
        try {
            (tree.getProp(THEME_KEY) as? Property<Any>)?.set(theme.name)
        } catch (_: Exception) {}
    }
}