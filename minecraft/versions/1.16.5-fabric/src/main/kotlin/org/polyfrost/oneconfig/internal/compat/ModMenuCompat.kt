package org.polyfrost.oneconfig.internal.compat

import com.terraformersmc.modmenu.ModMenu
import dev.deftu.omnicore.client.OmniScreen
import org.polyfrost.oneconfig.api.config.v1.Tree

object ModMenuCompat {

    val collectedMods: MutableList<String> = mutableListOf()

    @JvmStatic
    fun enable() = CompatLoader.requireTranslations(1000) {
        ModMenu.ROOT_MODS.values.toMutableList().filterNot { collectedMods.contains(it.id) }.forEach { mod ->
            collectedMods.forEach { println(it) }
            val modMenuTree = Tree.tree()

            modMenuTree.title = mod.name
            modMenuTree.description = "(Mod Menu Compat)"

            runCatching { ModMenu.getConfigScreen(mod.id, OmniScreen.currentScreen) }.getOrNull() ?: return@forEach
            modMenuTree.addMetadata("on_click") {
                OmniScreen.currentScreen = ModMenu.getConfigScreen(mod.id, OmniScreen.currentScreen)
            }

            CompatLoader.extraCompatConfigs.add(modMenuTree)
        }
    }

}