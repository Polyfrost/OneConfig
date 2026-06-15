package org.polyfrost.oneconfig.internal.compat

//? modmenu_compat {
import com.terraformersmc.modmenu.ModMenu
import com.terraformersmc.modmenu.util.mod.Mod
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.backend.Backend
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform

object ModMenuCompat {

    val mods: MutableList<Mod> = mutableListOf()

    fun preLoad() = CompatLoader.requireTranslations(-1000, true) {
        ModMenu.ROOT_MODS.forEach { (_, mod) ->
            // Only record which mods expose a config screen; do NOT build the screen here.
            // getConfigScreen() instantiates the screen's widget tree (e.g. Cloth EditBox), which
            // lazily bakes font glyphs. preLoad runs inside the ResourceFinishedLoading event —
            // off-frame, with the GL texture-bind cache desynced — so baking there corrupts the
            // vanilla font atlas (garbled glyphs until F3+T). hasConfigScreen() only looks up the
            // registered factory, so the actual screen is built lazily on open (in a render frame).
            if (runCatching { ModMenu.hasConfigScreen(mod.id) }.getOrDefault(false)) {
                mods.add(mod)
            }
        }
    }

    @JvmStatic
    fun enable() {
        preLoad()
        postLoad()
    }

    fun postLoad() = CompatLoader.requireTranslations(1000, true) {
        val nativeCoveredMods = mods.mapNotNull { mod ->
            val nativeChild = mod.findNativeLoadedChild() ?: return@mapNotNull null
            mod.aliasNativeChildConfig(nativeChild)
            mod.id
        }.toSet()

        mods.filterNot { CompatLoader.nativeLoadedConfigs.contains(it.id) || it.id in nativeCoveredMods }
            .forEach { mod ->
                val modMenuTree = Tree.tree()

                modMenuTree.id = mod.id
                modMenuTree.title = mod.name
                modMenuTree.description = "(Mod Menu Compat)"
                ModInfo.loadedMods.firstOrNull { it.id == mod.id }?.extractIconFile()?.let { iconPath ->
                    modMenuTree.addMetadata("icon_path", iconPath)
                }
                modMenuTree.addMetadata("on_click") {
                    Platform.screen().display(ModMenu.getConfigScreen(mod.id, Platform.screen().current()))
                }
                // Listed in the mods menu and opened via Mod Menu only — never persisted by OneConfig.
                modMenuTree.addMetadata(Backend.UI_ONLY_METADATA, true)

                ConfigManager.active().register(modMenuTree)
                CompatLoader.markFirstModAsSkip()
            }
    }

    private fun Mod.findNativeLoadedChild(): Mod? {
        return ModMenu.PARENT_MAP.get(this).firstNotNullOfOrNull { child ->
            if (CompatLoader.nativeLoadedConfigs.contains(child.id)) {
                child
            } else {
                child.findNativeLoadedChild()
            }
        }
    }

    private fun Mod.aliasNativeChildConfig(child: Mod) {
        val tree = runCatching { ConfigManager.active().get(child.id) }.getOrNull() ?: return
        tree.addMetadata("mod_card_title", name)
        ModInfo.loadedMods.firstOrNull { it.id == id }?.extractIconFile()?.let { iconPath ->
            tree.addMetadata("mod_card_icon_path", iconPath)
        }
    }

}
//? }
