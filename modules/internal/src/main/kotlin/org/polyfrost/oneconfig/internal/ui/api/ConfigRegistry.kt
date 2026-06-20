package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree

object ConfigRegistry {
    private val hiddenModCardIds = setOf(
        "oneconfig.json",
        "themes.json",
        "minecraft",
        "resourcefulconfig",
        "modmenu",
        "badoptimizations",
        "respackopts",
        "midnightlib",
        "ukulib",
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
        get() = configs.filter(::shouldShowModCard)

    var revision by mutableIntStateOf(0)
        private set

    fun shouldShowModCard(config: ConfigData): Boolean =
        config.id.lowercase() !in hiddenModCardIds && config.title.toString().lowercase() !in hiddenModCardTitles

    fun shouldShowInSearch(config: ConfigData): Boolean =
        config.id.lowercase() !in hiddenSearchIds && config.title.toString().lowercase() !in hiddenModCardTitles

    /**
     * Loads all trees from the given [ConfigManager] as [source] entries.
     * Call this after [ConfigManager.initialize] during OneConfig startup.
     */
    fun loadFrom(manager: ConfigManager, source: ConfigSource) {
        val seenIds = HashSet<String>()
        manager.trees().forEach { tree ->
            tree.id?.let(seenIds::add)
            registerTree(tree, source, bumpRevision = false)
        }
        configs.removeAll { it.source == source && it.id !in seenIds }
        revision++
    }

    @JvmOverloads
    fun registerTree(tree: Tree, source: ConfigSource, onOpen: (() -> Unit)? = null, bumpRevision: Boolean = true) {
        if (tree.id == null || tree.title == null) return
        if (tree.getMetadata<Any?>("hidden") != null) return
        upsert(TreeConfigData(tree, source, onOpen), bumpRevision)
    }

    fun register(data: ConfigData) {
        upsert(data, true)
    }

    fun unregister(id: String) {
        if (configs.removeAll { it.id == id }) {
            revision++
        }
    }

    fun findById(id: String): ConfigData? = configs.find { it.id == id }

    fun findTree(id: String): Tree? = (findById(id) as? TreeConfigData)?.tree

    private fun upsert(data: ConfigData, bumpRevision: Boolean) {
        val index = configs.indexOfFirst { it.id == data.id }
        if (index >= 0) {
            configs[index] = data
        } else {
            configs.add(data)
        }
        if (bumpRevision) {
            revision++
        }
    }
}
