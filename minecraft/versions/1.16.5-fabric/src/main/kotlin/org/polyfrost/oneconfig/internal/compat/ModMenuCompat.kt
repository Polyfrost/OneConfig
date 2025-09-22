package org.polyfrost.oneconfig.internal.compat
//#if FABRIC
import com.terraformersmc.modmenu.ModMenu
import com.terraformersmc.modmenu.util.mod.Mod
import dev.deftu.omnicore.api.client.screen.currentScreen
import org.polyfrost.oneconfig.api.config.v1.Tree

object ModMenuCompat {

    val mods: MutableList<Mod> = mutableListOf()

    fun preLoad() = CompatLoader.requireTranslations(-1000, true) {
        ModMenu.ROOT_MODS.forEach { (_, mod) ->
            // doing this to force create *all* config screens, this allows for direct integration without requiring manul open first.
            runCatching { ModMenu.getConfigScreen(mod.id, currentScreen) }.getOrNull() ?: return@forEach
            mods.add(mod)
        }
    }

    @JvmStatic
    fun enable() {
        preLoad()
        postLoad()
    }

    fun postLoad() = CompatLoader.requireTranslations(1000, true) {
        mods.filterNot { CompatLoader.nativeLoadedConfigs.contains(it.id) }
            .forEach { mod ->
                val modMenuTree = Tree.tree()

                modMenuTree.title = mod.name
                modMenuTree.description = "(Mod Menu Compat)"
                //TODO icon, idfk how to set a native image as PolyImage :sob:
                modMenuTree.addMetadata("on_click") {
                    currentScreen = ModMenu.getConfigScreen(mod.id, currentScreen)
                }

                CompatLoader.extraCompatConfigs.add(modMenuTree)
            }
    }

}
//#endif