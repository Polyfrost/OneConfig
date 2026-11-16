package org.polyfrost.oneconfig.internal.compat

//? modmenu_compat {
import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screens.Screen
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen

internal object ModMenuEntrypoint : ModMenuApi {

    override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
        return try {
            ConfigScreenFactory { OneConfigUIScreen.open() }
        } catch (e: LinkageError) {
            reportUnlinkable(e)
            ConfigScreenFactory<Screen> { null }
        }
    }

    override fun getProvidedConfigScreenFactories(): Map<String, ConfigScreenFactory<*>> {
        return try {
            val manager = ConfigManager.active()
            val trees = manager.trees()
            val cached = cachedFactories
            if (cached != null && manager === cachedManager && sameTrees(trees)) return cached
            cachedManager = manager
            cachedTrees = trees.toList()
            trees
                .filter { it.id != null }
                .associate { it.id to ConfigScreenFactory { _ -> OneConfigUIScreen(initialTree = it) } }
                .also { cachedFactories = it }
        } catch (e: LinkageError) {
            reportUnlinkable(e)
            emptyMap()
        }
    }

    private fun sameTrees(trees: Collection<Tree>): Boolean {
        val previous = cachedTrees
        if (previous.size != trees.size) return false
        var i = 0
        for (tree in trees) if (tree !== previous[i++]) return false
        return true
    }

    private var cachedManager: ConfigManager? = null
    private var cachedTrees: List<Tree> = emptyList()
    private var cachedFactories: Map<String, ConfigScreenFactory<*>>? = null

    private fun reportUnlinkable(e: LinkageError) {
        LOGGER.error(
            "OneConfig's screen class could not be linked, so its ModMenu entries are unavailable. " +
                "This is usually another mod's mixin failing to apply to OneConfigUIScreen - check the " +
                "log above for the Mixin error naming the responsible mod.",
            e,
        )
    }

    private val LOGGER = LogManager.getLogger("OneConfig/ModMenu")

    internal const val MOD_ID = "oneconfig"

}
//? }
