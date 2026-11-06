package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.hud.cardsSupersededByHudCards
import org.polyfrost.oneconfig.internal.ui.hud.hudModCardConfigs
import org.polyfrost.oneconfig.internal.ui.keybind.MinecraftKeybindRegistrar
import org.polyfrost.oneconfig.internal.ui.search.ConfigDocumentSource
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus

object ConfigRegistry {
    private val logger = LogManager.getLogger("OneConfig/ConfigRegistry")

    private val hiddenModCardIds = setOf(
        "oneconfig.json",
        "themes.json",
        "oneconfig.builtin", // built-in huds
        "minecraft",
        "resourcefulconfig",
        "modmenu",
        "badoptimizations",
        "respackopts",
        "midnightlib",
        "walksylib",
        "ukulib",
        "meowdding-lib",
        "meowdding-lib/config",
        "architectury",
        "fzzy_config",
        "tcdcommons",
        "modernconfig",
        "collective",
        "cloth-config",
        "compose-bundle",
        "trender",
        "libjf-config-core-v2",
        "libjf-config-network-v0",
        "libjf-web-v1",
        "libjf-translate-v1",
    )

    private val dedicatedScreenConfigIds = setOf(
        "oneconfig.json",
        "themes.json",
    )

    private val hiddenSearchIds = hiddenModCardIds - dedicatedScreenConfigIds

    private val hiddenModCardTitles = setOf(
        "trender",
        "libjf config",
        "libjf config: network",
        "libjf web",
        "libjf translate",
    )

    val configs: SnapshotStateList<ConfigData> = mutableStateListOf()

    val modCardConfigs: List<ConfigData>
        get() {
            val hudCards = hudModCardConfigs()
            val superseded = cardsSupersededByHudCards(hudCards)
            return configs.filter { shouldShowModCard(it) && it !in superseded } + hudCards
        }

    var revision by mutableIntStateOf(0)
        private set

    /** Configs of mods turned off live, kept for when they come back but out of reach as their callbacks are that mod's code */
    private var offIds by mutableStateOf(emptySet<String>())

    // shortcut: a bind a mod registers itself outside any config has no known owner so keeps firing, Hotwire taking those out would close that
    fun setOff(ids: Set<String>) {
        if (ids == offIds) return
        val before = offIds
        offIds = ids
        val caughtUp = ConfigManager.setOff(ids)
        // read from the manager, as Hotwire takes the cards of a mod it turns off out of this registry itself
        ConfigManager.active().trees().forEach { tree ->
            val id = tree.id ?: return@forEach
            if (id in ids && id !in before) MinecraftKeybindRegistrar.unregister(tree)
            else if (id !in ids && id in before) MinecraftKeybindRegistrar.scan(tree)
        }
        // profile rebinds don't announce their trees, which the listener above leaves to this
        if (caughtUp) loadFrom(ConfigManager.active(), ConfigSource.OC)
        SearchCorpus.invalidate(ConfigDocumentSource)
        revision++
    }

    init {
        ModToggleStore.install()
        // Index configs as they come in (compat layers etc...)
        ConfigManager.addTreeRegistrationListener { tree ->
            if (ConfigManager.isRebindingProfiles()) return@addTreeRegistrationListener
            // Profile rebinding may register trees from a background worker. Registry state and
            // Minecraft's key-mapping array both belong to the UI thread.
            Platform.screen().runOnUiThread {
                try {
                    // A queued registration from an older profile must not overwrite the active one.
                    if (ConfigManager.active().trees().any { it === tree }) {
                        registerTree(tree, ConfigSource.OC)
                    }
                } catch (failure: Throwable) {
                    logger.error("Failed to register config tree {}", tree.id, failure)
                }
            }
        }
        ConfigManager.addProfileChangeListener {
            Platform.screen().runOnUiThread {
                try {
                    loadFrom(ConfigManager.active(), ConfigSource.OC)
                } catch (failure: Throwable) {
                    logger.error("Failed to reload configs after a profile change", failure)
                }
            }
        }
    }

    fun shouldShowModCard(config: ConfigData): Boolean =
        config.id !in offIds && config.id.lowercase() !in hiddenModCardIds && config.title.asRenderText().lowercase() !in hiddenModCardTitles

    fun shouldShowModCardId(id: String): Boolean {
        val lower = id.lowercase()
        return lower !in hiddenModCardIds && lower.removeSuffix(".json") !in hiddenModCardIds
    }

    fun shouldShowInSearch(config: ConfigData): Boolean =
        config.id !in offIds && config.id.lowercase() !in hiddenSearchIds && config.title.asRenderText().lowercase() !in hiddenModCardTitles

    /**
     * Loads all trees from the given [ConfigManager] as [source] entries
     *
     * Call this after [ConfigManager.initialize] during OneConfig startup
     */
    fun loadFrom(manager: ConfigManager, source: ConfigSource) {
        val seenIds = HashSet<String>()
        var changed = false
        manager.trees().forEach { tree ->
            tree.id?.let(seenIds::add)
            if (registerTree(tree, source, bumpRevision = false)) changed = true
        }
        if (configs.removeAll { it.source == source && it is TreeConfigData && it.id !in seenIds }) changed = true
        if (!changed) return
        SearchCorpus.invalidate(ConfigDocumentSource)
        revision++
    }

    /** Returns whether the registry actually changed */
    @JvmOverloads
    fun registerTree(
        tree: Tree,
        source: ConfigSource,
        onOpen: (() -> Unit)? = null,
        bumpRevision: Boolean = true
    ): Boolean {
        // a config replaced after its mod updated leaves binds behind that would fire alongside the new ones
        tree.id?.let(::findTree)?.takeIf { it !== tree }?.let { MinecraftKeybindRegistrar.unregister(it, kept = tree) }
        if (tree.id !in offIds) MinecraftKeybindRegistrar.scan(tree, force = bumpRevision)
        if (tree.id == null || tree.title == null) return false
        if (tree.getMetadata<Any?>("hidden") != null) return false
        return upsert(TreeConfigData(tree, source, onOpen), bumpRevision)
    }

    fun register(data: ConfigData) {
        upsert(data, true)
    }

    fun unregister(id: String) {
        if (configs.removeAll { it.id == id }) {
            SearchCorpus.invalidate(ConfigDocumentSource)
            revision++
        }
    }

    fun findById(id: String): ConfigData? = configs.find { it.id == id }

    fun findTree(id: String): Tree? = (findById(id) as? TreeConfigData)?.tree

    private fun upsert(data: ConfigData, bumpRevision: Boolean): Boolean {
        val index = configs.indexOfFirst { it.id == data.id }
        if (index >= 0) {
            if (configs[index].wraps(data)) return false
            configs[index] = data
        } else {
            configs.add(data)
        }
        SearchCorpus.invalidate(ConfigDocumentSource)
        if (bumpRevision) {
            revision++
        }
        return true
    }

    /** Whether two config data instances provide the same tree information */
    private fun ConfigData.wraps(other: ConfigData): Boolean {
        if (this === other) return true
        if (this !is TreeConfigData || other !is TreeConfigData) return false
        return tree === other.tree && source == other.source && explicitOnOpen === other.explicitOnOpen
    }
}
