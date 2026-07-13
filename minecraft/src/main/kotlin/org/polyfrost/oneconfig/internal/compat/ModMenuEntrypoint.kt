package org.polyfrost.oneconfig.internal.compat

//? modmenu_compat {
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen

internal object ModMenuEntrypoint : ModMenuApi {

    override fun getModConfigScreenFactory(): ConfigScreenFactory<*>? {
        if (CompatLoader.hasMod(BOOTSTRAP_MOD_ID)) return null
        return { it -> OneConfigUIScreen() }
    }

    override fun getProvidedConfigScreenFactories(): Map<String, ConfigScreenFactory<*>> {
        val factories = ConfigManager.active().trees()
            .associateTo(mutableMapOf()) { it.id.toString() to ConfigScreenFactory { _ -> OneConfigUIScreen(initialTree = it) } }
        factories.putIfAbsent(BOOTSTRAP_MOD_ID, ConfigScreenFactory { _ -> OneConfigUIScreen() })
        return factories
    }

    private const val BOOTSTRAP_MOD_ID = "oneconfigbootstrap"

}
//? }
