package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.util.function.Predicate
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.ui.v1.ModToggles
import org.polyfrost.oneconfig.api.ui.v1.components.ControlStyle
import org.polyfrost.oneconfig.api.ui.v1.components.EmptyState
import org.polyfrost.oneconfig.api.ui.v1.components.TextButton
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.PropertyModToggle
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.search.submenuTrail

private val LOGGER = LogManager.getLogger("OneConfig/UI")

@Composable
fun ModConfig(id: String, initialCategory: String? = null, submenu: String? = null) {
    val revision = ConfigRegistry.revision
    val config = ConfigRegistry.findById(id)
    val tree = ConfigRegistry.findTree(id)

    val externalOpen = config?.onOpen

    when {
        config == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                translated("oneconfig.modconfig.unknown", "Mod not found"),
                icon = "alert-circle",
                text = translated("oneconfig.modconfig.unknown.text", "No mod with the id %s is loaded.", id),
            )
        }
        tree?.getMetadata<Boolean>("ui_only") == true && externalOpen != null -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                translated("oneconfig.modconfig.external", "This mod opens its own settings screen."),
                icon = "external-link",
                action = {
                    TextButton(
                        translated("oneconfig.modconfig.external.open", "Open settings"),
                        "external-link",
                        externalOpen,
                        style = ControlStyle.Primary,
                    )
                },
            )
        }
        tree == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                translated("oneconfig.modconfig.own", "This mod manages its own configuration."),
                icon = "settings",
            )
        }
        else -> {
            val toggleRevision = ModToggles.revision
            val toggle = remember(id, toggleRevision) { config.toggle }
            val ownSwitch = (toggle as? PropertyModToggle)?.property
            var ownSwitchFlips by remember(System.identityHashCode(ownSwitch)) { mutableIntStateOf(0) }
            DisposableEffect(System.identityHashCode(ownSwitch)) {
                val listener = Predicate<Boolean> { ownSwitchFlips++; false }
                ownSwitch?.addCallback(listener)
                onDispose { ownSwitch?.removeCallback(listener) }
            }
            val locked = remember(toggle, ownSwitchFlips) { toggle?.isEnabled() == false }

            // switching a mod can swap the values of its options so the page is rebuilt afterwards
            key(revision, tree) {
                val trail = remember(tree, submenu) {
                    submenu?.let(tree::submenuTrail).also {
                        if (submenu != null && it == null) LOGGER.warn("Config {} has no submenu {}", id, submenu)
                    }
                }
                val path = submenu.takeIf { trail != null }
                ConfigScreen(
                    trail?.last() ?: tree,
                    initialCategory,
                    pageKey = ModConfigRoute(id, submenu = path).pageKey,
                    locked = locked,
                    lockExempt = ownSwitch,
                    configId = id,
                    submenu = path,
                )
            }
        }
    }
}
