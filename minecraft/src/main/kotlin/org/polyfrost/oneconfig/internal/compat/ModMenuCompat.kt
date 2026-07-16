package org.polyfrost.oneconfig.internal.compat

//? modmenu_compat {
import com.terraformersmc.modmenu.ModMenu
import com.terraformersmc.modmenu.util.mod.Mod
import net.minecraft.client.gui.screens.Screen
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.backend.Backend
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController

object ModMenuCompat {

    val mods: MutableList<Mod> = mutableListOf()

    private val ownModIds = setOf(
        ModMenuEntrypoint.BOOTSTRAP_MOD_ID,
        ModMenuEntrypoint.PLATFORM_MOD_ID,
        "oneconfig",
    )

    fun preLoad() = CompatLoader.requireTranslations(-1000, true) {
        ModMenu.ROOT_MODS.forEach { (_, mod) ->
            if (mod.id in ownModIds) return@forEach
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

        mods.filterNot {
            CompatLoader.nativeLoadedConfigs.contains(it.id) ||
                it.id in nativeCoveredMods ||
                hasNativeOcConfig(it.id)
        }
            .forEach { mod ->
                val modMenuTree = Tree.tree()

                modMenuTree.id = mod.id
                modMenuTree.title = mod.name
                modMenuTree.description = "(Mod Menu Compat)"
                ModInfo.loadedMods.firstOrNull { it.id == mod.id }?.extractIconFile()?.let { iconPath ->
                    modMenuTree.addMetadata("icon_path", iconPath)
                }
                modMenuTree.addMetadata("on_click") {
                    val foreignScreen = runCatching {
                        var screen: Screen? = null
                        CompatLoader.withForcedModId(mod.id) {
                            screen = ModMenu.getConfigScreen(mod.id, Platform.screen().current())
                        }
                        screen
                    }.getOrNull()

                    val nativeTree = runCatching { ConfigManager.active().get(mod.id) }.getOrNull()
                    val hasNative = nativeTree != null &&
                        nativeTree !== modMenuTree &&
                        nativeTree.getMetadata<Boolean>(Backend.UI_ONLY_METADATA) != true

                    if (hasNative && nativeTree != null) {
                        ConfigRegistry.registerTree(nativeTree, ConfigSource.OC)
                    }

                    when {
                        hasNative && LocalNavController.isReady ->
                            LocalNavController.wrapper.navigate(ModConfigRoute(mod.id))
                        hasNative -> Platform.screen().display(OneConfigUIScreen(initialTreeId = mod.id))
                        foreignScreen != null -> Platform.screen().display(foreignScreen)
                    }
                }
                // Listed in the mods menu and opened via Mod Menu only — never persisted by OneConfig.
                modMenuTree.addMetadata(Backend.UI_ONLY_METADATA, true)
                modMenuTree.addMetadata(Backend.UI_PLACEHOLDER_METADATA, true)

                ConfigManager.active().register(modMenuTree)
                CompatLoader.markFirstModAsSkip()
            }
    }

    // A mod can ship BOTH a native OneConfig config and a Mod Menu entrypoint. The native config
    // registers under its file id (e.g. "legacyskyblock.json") while the Mod Menu mod id is the bare
    // loader id ("legacyskyblock"), so they have different ConfigRegistry ids and show as two cards.
    // nativeLoadedConfigs only tracks foreign-compat configs, never native OneConfig ones, so detect
    // the native tree directly from the config registry (matching the source of truth used by on_click)
    // and skip building a duplicate Mod Menu Compat card for it.
    private fun hasNativeOcConfig(modId: String): Boolean = runCatching {
        ConfigManager.active().trees().any { tree ->
            val id = tree.id ?: return@any false
            id.removeSuffix(".json") == modId &&
                tree.getMetadata<Boolean>(Backend.UI_ONLY_METADATA) != true
        }
    }.getOrDefault(false)

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
