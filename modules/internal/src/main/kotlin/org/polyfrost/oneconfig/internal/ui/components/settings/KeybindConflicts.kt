package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.mutableIntStateOf
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.keybind.collectAllKeybindGroups

internal object KeybindConflicts {
    val revision = mutableIntStateOf(0)

    private fun boundKeybinds(): List<Pair<Property<*>, OneConfigKeybind>> {
        return collectAllKeybindGroups().flatMap { group ->
            group.entries.mapNotNull { entry ->
                (entry.prop.get() as? OneConfigKeybind)
                    ?.takeIf { it.hasConflictBinding() }
                    ?.let { entry.prop to it }
            }
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

    private fun OneConfigKeybind.hasConflictBinding(): Boolean {
        val hasKey = keyCodes?.any { it > 0 } == true
        val hasMouse = mouseBtns?.any { it >= 0 } == true
        return hasKey || hasMouse
    }
}
