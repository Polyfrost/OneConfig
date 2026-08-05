package org.polyfrost.oneconfig.internal.compat

//? modmenu_compat {
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screens.Screen
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen

internal object ModMenuEntrypoint : ModMenuApi {

    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        if (CompatLoader.hasMod(BOOTSTRAP_MOD_ID)) return ConfigScreenFactory<Screen> { null }
        return try {
            ConfigScreenFactory { OneConfigUIScreen() }
        } catch (e: LinkageError) {
            reportUnlinkable(e)
            ConfigScreenFactory<Screen> { null }
        }
    }

    override fun getProvidedConfigScreenFactories(): Map<String, ConfigScreenFactory<*>> {
        return try {
            val factories = ConfigManager.active().trees()
                .filter { it.id != null }
                .associateTo(mutableMapOf()) { it.id to ConfigScreenFactory { _ -> OneConfigUIScreen(initialTree = it) } }
            factories.putIfAbsent(BOOTSTRAP_MOD_ID, ConfigScreenFactory { _ -> OneConfigUIScreen() })
            factories
        } catch (e: LinkageError) {
            reportUnlinkable(e)
            emptyMap()
        }
    }

    private fun reportUnlinkable(e: LinkageError) {
        LOGGER.error(
            "OneConfig's screen class could not be linked, so its ModMenu entries are unavailable. " +
                "This is usually another mod's mixin failing to apply to OneConfigUIScreen - check the " +
                "log above for the Mixin error naming the responsible mod.",
            e,
        )
    }

    private val LOGGER = LogManager.getLogger("OneConfig/ModMenu")

    internal const val BOOTSTRAP_MOD_ID = "oneconfigbootstrap"

    internal const val PLATFORM_MOD_ID = "oneconfigv1"

}
//? }
