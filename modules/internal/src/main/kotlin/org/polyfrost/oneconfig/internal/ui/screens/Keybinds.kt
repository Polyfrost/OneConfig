package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.IdentityHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.components.EmptyState
import org.polyfrost.oneconfig.api.ui.v1.components.LoadState
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.CollapsibleHeader
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.ScrollbarBox
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.blockInteraction
import org.polyfrost.oneconfig.internal.ui.components.hoverGlow
import org.polyfrost.oneconfig.internal.ui.components.isEmptyText
import org.polyfrost.oneconfig.internal.ui.components.localizedDescription
import org.polyfrost.oneconfig.internal.ui.components.localizedTitle
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.KeybindConflicts
import org.polyfrost.oneconfig.internal.ui.components.settings.Option
import org.polyfrost.oneconfig.internal.ui.components.settings.OptionMoreButton
import org.polyfrost.oneconfig.internal.ui.components.settings.optionContextMenu
import org.polyfrost.oneconfig.internal.ui.components.settings.rememberOptionMenu
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindEntry
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindGroup
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindGroupCollapseStore
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindProviderRegistry
import org.polyfrost.oneconfig.internal.ui.keybind.collectAllKeybindGroups
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus
import org.polyfrost.oneconfig.internal.ui.search.SearchDocument
import org.polyfrost.oneconfig.internal.ui.search.SearchScope
import org.polyfrost.oneconfig.internal.ui.search.searchMatches
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.shell.pageSearchQuery
import org.polyfrost.oneconfig.internal.ui.shell.rememberRestorableLazyListState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Keybinds() {
    val revision = ConfigRegistry.revision

    val configs = ConfigRegistry.configs.toList()
    val providerRevision = KeybindProviderRegistry.revision.intValue
    val groups = remember(revision, providerRevision, configs) { collectAllKeybindGroups() }
    val localSearchQuery = pageSearchQuery().trim()
    val search = rememberKeybindSearchResults(groups, localSearchQuery)
    val searchResults = search.groups
    val visibleGroups = if (localSearchQuery.isBlank()) groups else searchResults.orEmpty()

    val listState = rememberRestorableLazyListState("keybinds", localSearchQuery, search.query)

    if (visibleGroups.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when {
                localSearchQuery.isBlank() ->
                    EmptyState(translated("oneconfig.search.no_keybinds", "No keybinds available."), icon = "keyboard")
                // nothing to say until the first search comes back
                searchResults == null ->
                    EmptyState(translated("oneconfig.search.searching", "Searching..."), state = LoadState.Loading)
                else ->
                    EmptyState(translated("oneconfig.search.no_keybinds_match", "No keybinds match \"%s\"", localSearchQuery), icon = "search")
            }
        }
        return
    }

    val conflicts = KeybindConflicts.conflictMap()

    val searching = localSearchQuery.isNotBlank()

    ScrollbarBox(listState, PageGutter, Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = PageGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            visibleGroups.forEach { group ->
                val storedCollapsed = KeybindGroupCollapseStore.isCollapsed(group.modId)
                val expanded = searching || !storedCollapsed
                item(key = "header:$revision:${group.modId}") {
                    KeybindGroupHeader(
                        group = group,
                        expanded = expanded,
                        onToggle = { KeybindGroupCollapseStore.setCollapsed(group.modId, !storedCollapsed) },
                    )
                }
                if (expanded) {
                    items(
                        items = group.entries,
                        key = { entry -> "$revision:${group.modId}:${entry.path}" },
                    ) { entry ->
                        key(revision, entry.prop) {
                            KeybindRow(entry, conflictsWith = conflicts[entry.prop].orEmpty())
                        }
                    }
                }
            }
        }
    }
}

/** The group and entry one keybind property renders as for resolving corpus hits back to rows */
private class KeybindOwner(val group: KeybindGroup, val entry: KeybindEntry)

private class KeybindSearchResults(val groups: List<KeybindGroup>?, val query: String?)

@Composable
private fun rememberKeybindSearchResults(groups: List<KeybindGroup>, query: String): KeybindSearchResults {
    var results by remember { mutableStateOf<List<KeybindGroup>?>(null) }
    var searchedQuery by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(groups, query) {
        results = if (query.isBlank()) null
        else withContext(Dispatchers.Default) { searchKeybindGroups(groups, query) }
        searchedQuery = query
    }
    return KeybindSearchResults(results, searchedQuery)
}

private fun searchKeybindGroups(groups: List<KeybindGroup>, query: String): List<KeybindGroup> {
    val owners = IdentityHashMap<Property<*>, KeybindOwner>()
    groups.forEach { group -> group.entries.forEach { owners[it.prop] = KeybindOwner(group, it) } }
    fun ownerOf(document: SearchDocument<*>) = (document.payload as? Property<*>)?.let(owners::get)

    val hits = SearchCorpus.searchGrouped(query, setOf(SearchScope.Keybinds)) { ownerOf(it)?.group?.modId }
        .mapNotNull { (modId, documents) ->
            if (modId == null) return@mapNotNull null
            modId to documents.mapNotNull { ownerOf(it)?.entry }
        }.toMap()

    // group headers are not corpus documents so surface them here explicitly
    val q = query.lowercase()
    return groups.mapNotNull { group ->
        if (searchMatches(group.modTitle.asRenderText(), q) || searchMatches(group.modId, q)) return@mapNotNull group
        hits[group.modId]?.takeIf { it.isNotEmpty() }?.let { group.copy(entries = it) }
    }
}

@Composable
private fun KeybindGroupHeader(group: KeybindGroup, expanded: Boolean, onToggle: () -> Unit) {
    val theme = LocalTheme.current
    CollapsibleHeader(
        label = group.modTitle.asRenderText(),
        expanded = expanded,
        onToggle = onToggle,
        contentPadding = PaddingValues(top = 4.dp, bottom = 2.dp),
    ) {
        group.modIcon?.let { icon ->
            Icon(icon, color = theme.textColorSecondary, modifier = Modifier.size(18.dp))
        }
        Text(
            group.modTitle,
            color = theme.textColorSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun KeybindRow(entry: KeybindEntry, conflictsWith: List<Property<*>>) {
    val theme = LocalTheme.current
    val shape = theme.modCardShape
    val prop = entry.prop
    val display = rememberDisplay(prop)
    if (display == Property.Display.HIDDEN) return
    val enabled = display != Property.Display.DISABLED
    val menu = rememberOptionMenu(prop)
    val rowInteraction = rememberInteractionSource()
    val isRowHovered by rowInteraction.collectIsHoveredAsState()
    val showActionButton = OneConfigConfig.showOptionActionButtons

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(displayAlpha(display))
            .hoverable(rowInteraction)
            .optionContextMenu(menu)
            .hoverGlow(shape)
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
                (prop.getMetadata<Any?>("icon") as? String)?.let {
                    Icon(it, color = theme.textColor, modifier = Modifier.size(32.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        prop.localizedTitle(),
                        color = theme.textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    prop.localizedDescription()?.takeUnless { it.isEmptyText() }?.let {
                        Text(it, color = theme.textColorSecondary, fontSize = 13.sp)
                    }
                    Text(
                        "${entry.category} / ${entry.subcategory}",
                        color = theme.textColorSecondary,
                        fontSize = 11.sp,
                    )
                    if (conflictsWith.isNotEmpty()) {
                        val names = conflictsWith.joinToString(", ") { KeybindConflicts.displayName(it) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon("alert-circle", color = theme.error, modifier = Modifier.size(12.dp))
                            Text(
                                translated("oneconfig.keybind.conflicts", "Conflicts with %s", names),
                                color = theme.error,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
            if (showActionButton) OptionMoreButton(menu, visible = isRowHovered)
            Box(Modifier.blockInteraction(!enabled)) {
                Option(prop)
            }
        }
    }
}
