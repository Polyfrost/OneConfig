package org.polyfrost.oneconfig.internal.ui.keybind

import androidx.compose.runtime.mutableIntStateOf
import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.TreeConfigData
import org.polyfrost.oneconfig.internal.ui.components.localizedGroup
import org.polyfrost.oneconfig.internal.ui.search.KeybindDocumentSource
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus

data class KeybindGroup(
    val modId: String,
    val modTitle: Any,
    val modIcon: String?,
    val entries: List<KeybindEntry>,
)

data class KeybindEntry(
    val path: String,
    val category: String,
    val subcategory: String,
    val prop: Property<*>,
)

fun interface KeybindGroupProvider {
    fun groups(): List<KeybindGroup>
}

object KeybindProviderRegistry {
    val revision = mutableIntStateOf(0)

    private val providers = ArrayList<KeybindGroupProvider>()

    fun register(provider: KeybindGroupProvider) {
        if (provider in providers) return
        providers += provider
        SearchCorpus.invalidate(KeybindDocumentSource)
        revision.intValue++
    }

    fun groups(): List<KeybindGroup> = providers.flatMap { it.groups() }
}

fun collectAllKeybindGroups(): List<KeybindGroup> {
    val configGroups = collectKeybindGroups(ConfigRegistry.configList.filterIsInstance<TreeConfigData>())
    return (configGroups + KeybindProviderRegistry.groups()).withUniqueModIds()
}

private fun List<KeybindGroup>.withUniqueModIds(): List<KeybindGroup> {
    val seen = HashSet<String>(size)
    return map { group ->
        if (seen.add(group.modId)) return@map group
        var suffix = 2
        var candidate = "${group.modId}~$suffix"
        while (!seen.add(candidate)) {
            candidate = "${group.modId}~${++suffix}"
        }
        group.copy(modId = candidate)
    }
}

private fun collectKeybindGroups(configs: List<TreeConfigData>): List<KeybindGroup> {
    return configs.mapNotNull { config ->
        val entries = collectKeybindEntries(config.tree)
        if (entries.isEmpty()) null
        else KeybindGroup(config.id, config.title, config.icon, entries)
    }
}

private fun collectKeybindEntries(tree: Tree): List<KeybindEntry> {
    val entries = ArrayList<KeybindEntry>()
    tree.map.forEach { (id, node) ->
        collectKeybindEntries(node, id, entries)
    }
    return entries
}

private fun collectKeybindEntries(node: Node, path: String, entries: MutableList<KeybindEntry>) {
    when (node) {
        is Property<*> -> {
            if (node.isKeybindProperty()) {
                entries += KeybindEntry(
                    path = path,
                    category = node.localizedGroup("category", "categoryKey", ConfigVisualizer.DEFAULT_CATEGORY),
                    subcategory = node.localizedGroup("subcategory", "subcategoryKey", ConfigVisualizer.DEFAULT_SUBCATEGORY),
                    prop = node,
                )
            }
        }
        is Tree -> {
            node.map.forEach { (id, child) -> collectKeybindEntries(child, "$path.$id", entries) }
        }
    }
}

fun Property<*>.isKeybindProperty(): Boolean {
    return when (val visualizer = getMetadata<Any?>("visualizer")) {
        Visualizer.KeybindVisualizer::class.java -> true
        is Visualizer.KeybindVisualizer -> true
        else -> false
    }
}
