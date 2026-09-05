package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.mutableIntStateOf
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.localizedTitle
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindProviderRegistry
import org.polyfrost.oneconfig.internal.ui.keybind.collectAllKeybindGroups

internal object KeybindConflicts {
    val revision = mutableIntStateOf(0)

    private var cachedKey = Long.MIN_VALUE
    private var cached: Map<Property<*>, List<Property<*>>> = emptyMap()

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
        val key = (ConfigRegistry.revision * 31L + KeybindProviderRegistry.revision.intValue) * 31 +
            revision.intValue
        if (key != cachedKey) {
            cached = computeConflictMap()
            cachedKey = key
        }
        return cached
    }

    private fun computeConflictMap(): Map<Property<*>, List<Property<*>>> {
        val bound = boundKeybinds()
        if (bound.size < 2) return emptyMap()
        val conflicting = HashMap<Property<*>, MutableList<Property<*>>>()
        for (i in bound.indices) {
            for (j in i + 1 until bound.size) {
                if (isContextGated(bound[i].first) || isContextGated(bound[j].first)) continue
                if (bound[i].second.conflictsWith(bound[j].second)) {
                    conflicting.getOrPut(bound[i].first) { ArrayList() } += bound[j].first
                    conflicting.getOrPut(bound[j].first) { ArrayList() } += bound[i].first
                }
            }
        }
        return conflicting
    }

    private fun isContextGated(prop: Property<*>): Boolean {
        val id = prop.id ?: return false
        return CONTEXT_GATED_ID_PREFIXES.any { id.startsWith(it) }
    }

    private val CONTEXT_GATED_ID_PREFIXES = listOf(
        "minecraft.key.debug.",
        "minecraft.key.spectator",
        "minecraft.key.toggleSpectatorShaderEffects",
    )

    fun conflictingProps(): Set<Property<*>> = conflictMap().keys

    fun displayName(prop: Property<*>): String =
        prop.localizedTitle().asRenderText().takeIf { it.isNotEmpty() } ?: prop.id ?: "Unknown"

    private fun OneConfigKeybind.hasConflictBinding(): Boolean {
        val hasKey = keyCodes?.any { it > 0 } == true
        val hasMouse = mouseBtns?.any { it >= 0 } == true
        return hasKey || hasMouse
    }
}
