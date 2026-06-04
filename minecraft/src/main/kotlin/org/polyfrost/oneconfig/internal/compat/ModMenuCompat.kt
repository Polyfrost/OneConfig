package org.polyfrost.oneconfig.internal.compat

//? modmenu_compat {
import com.terraformersmc.modmenu.ModMenu
import com.terraformersmc.modmenu.util.mod.Mod
import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform

object ModMenuCompat {

    val mods: MutableList<Mod> = mutableListOf()

    fun preLoad() = CompatLoader.requireTranslations(-1000, true) {
        ModMenu.ROOT_MODS.forEach { (_, mod) ->
            // doing this to force create *all* config screens, this allows for direct integration without requiring manul opening first.
            runCatching { ModMenu.getConfigScreen(mod.id, Minecraft.getInstance().screen) }.getOrNull() ?: return@forEach
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

                modMenuTree.id = mod.id
                modMenuTree.title = mod.name
                modMenuTree.description = "(Mod Menu Compat)"
                ModInfo.loadedMods.firstOrNull { it.id == mod.id }?.modIconPath?.let { iconPath ->
                    modMenuTree.addMetadata("icon_path", iconPath)
                }
                modMenuTree.addMetadata("on_click") {
                    Platform.screen().display(ModMenu.getConfigScreen(mod.id, Platform.screen().current()))
                }

                ConfigManager.active().register(modMenuTree)
                CompatLoader.markFirstModAsSkip()
            }
    }

}
//? }