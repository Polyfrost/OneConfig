package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.TreeConfigData
import org.polyfrost.oneconfig.internal.ui.api.settings.KeybindOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.isEmptyText
import org.polyfrost.oneconfig.internal.ui.components.settings.KeybindOption
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private data class KeybindGroup(
    val modId: String,
    val modTitle: Any,
    val modIcon: String,
    val entries: List<KeybindEntry>,
)

private data class KeybindEntry(
    val path: String,
    val category: String,
    val subcategory: String,
    val prop: Property<*>,
)

@Composable
fun Keybinds() {
    val revision = ConfigRegistry.revision

    DisposableEffect(Unit) {
        ShellState.title = "Keybinds"
        onDispose { }
    }

    val configs = ConfigRegistry.configs.toList()
    val groups = remember(revision, configs) { collectKeybindGroups(configs.filterIsInstance<TreeConfigData>()) }

    if (groups.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No keybinds available.", color = LocalTheme.current.textColorSecondary, fontSize = 15.sp)
        }
        return
    }

    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            groups.forEach { group ->
                item(key = "header:$revision:${group.modId}") {
                    KeybindGroupHeader(group)
                }
                items(
                    items = group.entries,
                    key = { entry -> "$revision:${group.modId}:${entry.path}" },
                ) { entry ->
                    key(revision, entry.prop) {
                        KeybindRow(entry)
                    }
                }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
        )
    }
}

@Composable
private fun KeybindGroupHeader(group: KeybindGroup) {
    val theme = LocalTheme.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(group.modIcon, color = theme.textColorSecondary, modifier = Modifier.size(18.dp))
        Text(
            group.modTitle,
            color = theme.textColorSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun KeybindRow(entry: KeybindEntry) {
    val theme = LocalTheme.current
    val shape = theme.modCardShape
    val prop = entry.prop

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.modCardBackground, shape)
            .border(
                1.dp,
                Brush.verticalGradient(listOf(theme.borderColor, theme.borderColor.copy(0f))),
                shape,
            )
    ) {
        val vignetteColor = theme.textColor
        Box(
            Modifier.fillMaxSize().drawWithCache {
                val gradient = Brush.radialGradient(
                    colors = listOf(
                        vignetteColor.copy(alpha = 0f),
                        vignetteColor.copy(alpha = 0.02f),
                        vignetteColor.copy(alpha = 0.05f),
                    ),
                    center = size.center,
                    radius = size.minDimension * 0.9f,
                )
                onDrawBehind { drawRect(gradient) }
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                prop.getMetadata<String>("icon")?.let {
                    Icon(it, color = theme.textColor, modifier = Modifier.size(32.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        prop.title ?: prop.id ?: "",
                        color = theme.textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    prop.description?.takeUnless { it.isEmptyText() }?.let {
                        Text(it, color = theme.textColorSecondary, fontSize = 13.sp)
                    }
                    Text(
                        "${entry.category} / ${entry.subcategory}",
                        color = theme.textColorSecondary,
                        fontSize = 11.sp,
                    )
                }
            }
            KeybindOption(KeybindOptionData(prop))
        }
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
                    category = node.groupName("category", ConfigVisualizer.DEFAULT_CATEGORY),
                    subcategory = node.groupName("subcategory", ConfigVisualizer.DEFAULT_SUBCATEGORY),
                    prop = node,
                )
            }
        }
        is Tree -> {
            node.map.forEach { (id, child) -> collectKeybindEntries(child, "$path.$id", entries) }
        }
    }
}

private fun Property<*>.isKeybindProperty(): Boolean {
    return when (val visualizer = getMetadata<Any?>("visualizer")) {
        Visualizer.KeybindVisualizer::class.java -> true
        is Visualizer.KeybindVisualizer -> true
        else -> false
    }
}

private fun Node.groupName(key: String, default: String): String {
    val value = getMetadata<String>(key)?.trim()
    return value?.takeUnless(String::isEmpty) ?: default
}
