package org.polyfrost.oneconfig.internal.compat

//? modmenu_compat {
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screens.Screen
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen

internal object ModMenuEntrypoint : ModMenuApi {

    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        if (CompatLoader.hasMod(BOOTSTRAP_MOD_ID)) return ConfigScreenFactory<Screen> { null }
        return ConfigScreenFactory { OneConfigUIScreen() }
    }

    override fun getProvidedConfigScreenFactories(): Map<String, ConfigScreenFactory<*>> {
        val factories = ConfigManager.active().trees()
            .filter { it.id != null }
            .associateTo(mutableMapOf()) { it.id to ConfigScreenFactory { _ -> OneConfigUIScreen(initialTree = it) } }
        factories.putIfAbsent(BOOTSTRAP_MOD_ID, ConfigScreenFactory { _ -> OneConfigUIScreen() })
        return factories
    }

    internal const val BOOTSTRAP_MOD_ID = "oneconfigbootstrap"

    internal const val PLATFORM_MOD_ID = "oneconfigv1"

}
//? }
