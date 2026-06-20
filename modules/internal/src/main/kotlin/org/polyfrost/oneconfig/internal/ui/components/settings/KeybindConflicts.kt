package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.mutableIntStateOf
import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.api.TreeConfigData

internal object KeybindConflicts {
    val revision = mutableIntStateOf(0)

    private fun boundKeybinds(): List<Pair<Property<*>, OneConfigKeybind>> {
        val out = ArrayList<Pair<Property<*>, OneConfigKeybind>>()
        ConfigRegistry.configs.forEach { config ->
            val tree = (config as? TreeConfigData)?.tree ?: return@forEach
            collect(tree, out)
        }
        return out
    }

    private fun collect(node: Node, out: MutableList<Pair<Property<*>, OneConfigKeybind>>) {
        when (node) {
            is Property<*> -> if (node.isKeybindProperty()) {
                (node.get() as? OneConfigKeybind)?.takeIf { it.isBound }?.let { out += node to it }
            }
            is Tree -> node.map.values.forEach { collect(it, out) }
        }
    }

    fun conflictMap(): Map<Property<*>, List<Property<*>>> {
        val bound = boundKeybinds()
        if (bound.size < 2) return emptyMap()
        val conflicting = HashMap<Property<*>, MutableList<Property<*>>>()
        for (i in bound.indices) {
            for (j in i + 1 until bound.size) {
                if (bound[i].second.conflictsWith(bound[j].second)) {
                    conflicting.getOrPut(bound[i].first) { ArrayList() } += bound[j].first
                    conflicting.getOrPut(bound[j].first) { ArrayList() } += bound[i].first
                }
            }
        }
        return conflicting
    }

    fun conflictingProps(): Set<Property<*>> = conflictMap().keys

    fun displayName(prop: Property<*>): String =
        prop.title?.asRenderText()?.takeIf { it.isNotEmpty() } ?: prop.id ?: "Unknown"

    private fun Property<*>.isKeybindProperty(): Boolean = when (getMetadata<Any?>("visualizer")) {
        Visualizer.KeybindVisualizer::class.java -> true
        is Visualizer.KeybindVisualizer -> true
        else -> false
    }
}
