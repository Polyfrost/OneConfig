package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree

object ConfigRegistry {
    val configs: SnapshotStateList<ConfigData> = mutableStateListOf()

    /**
     * Loads all trees from the given [ConfigManager] as [source] entries.
     * Call this after [ConfigManager.initialize] during OneConfig startup.
     */
    fun loadFrom(manager: ConfigManager, source: ConfigSource) {
        val seenIds = HashSet<String>()
        manager.trees().forEach { tree ->
            tree.id?.let(seenIds::add)
            registerTree(tree, source)
        }
        configs.removeAll { it.source == source && it.id !in seenIds }
    }

    @JvmOverloads
    fun registerTree(tree: Tree, source: ConfigSource, onOpen: (() -> Unit)? = null) {
        if (tree.id == null || tree.title == null) return
        if (tree.getMetadata<Any?>("hidden") != null) return
        upsert(TreeConfigData(tree, source, onOpen))
    }

    fun register(data: ConfigData) {
        upsert(data)
    }

    fun unregister(id: String) {
        configs.removeAll { it.id == id }
    }

    fun findById(id: String): ConfigData? = configs.find { it.id == id }

    fun findTree(id: String): Tree? = (findById(id) as? TreeConfigData)?.tree

    private fun upsert(data: ConfigData) {
        val index = configs.indexOfFirst { it.id == data.id }
        if (index >= 0) {
            configs[index] = data
        } else {
            configs.add(data)
        }
    }
}
