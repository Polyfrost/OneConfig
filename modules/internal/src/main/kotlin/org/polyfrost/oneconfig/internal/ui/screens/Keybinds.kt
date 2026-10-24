package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.IdentityHashMap
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.absoluteOffset
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.lazy.LazyColumn
import org.polyfrost.compose.foundation.lazy.LazyListState
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.geometry.Offset
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.RadialGradient
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.layout.onGloballyPositioned
import org.polyfrost.compose.ui.layout.positionInRoot
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.unit.IntOffset
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.settings.KeybindOptionData
import org.polyfrost.oneconfig.internal.ui.components.GlobalSearchBar
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.ScrollbarBox
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.blockInteraction
import org.polyfrost.oneconfig.internal.ui.components.isEmptyText
import org.polyfrost.oneconfig.internal.ui.components.localizedDescription
import org.polyfrost.oneconfig.internal.ui.components.localizedTitle
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.KeybindConflicts
import org.polyfrost.oneconfig.internal.ui.components.settings.KeybindOption
import org.polyfrost.oneconfig.internal.ui.components.settings.OptionActionButton
import org.polyfrost.oneconfig.internal.ui.components.settings.OptionContextMenu
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindEntry
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindGroup
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindGroupCollapseStore
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindProviderRegistry
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus
import org.polyfrost.oneconfig.internal.ui.keybind.collectAllKeybindGroups
import org.polyfrost.oneconfig.internal.ui.poly.PolyWindow
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus
import org.polyfrost.oneconfig.internal.ui.search.SearchDocument
import org.polyfrost.oneconfig.internal.ui.search.SearchScope
import org.polyfrost.oneconfig.internal.ui.search.searchMatches
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val CONFLICT_COLOR = Color(0xFFE0524F)

@Composable
fun Keybinds(searchQuery: String) {
    val revision = ConfigRegistry.revision

    val configs = ConfigRegistry.configs.toList()
    val providerRevision = KeybindProviderRegistry.revision.intValue
    val groups = remember(revision, providerRevision, configs) { collectAllKeybindGroups() }
    val localSearchQuery = searchQuery.trim()
    val search = rememberKeybindSearchResults(groups, localSearchQuery)
    val searchResults = search.groups
    val visibleGroups = if (localSearchQuery.isBlank()) groups else searchResults.orEmpty()

    val listState = remember(localSearchQuery, search.query) { LazyListState() }

    if (visibleGroups.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val message = when {
                localSearchQuery.isBlank() -> "No keybinds available."
                // nothing to say until the first search comes back
                searchResults == null -> "Searching..."
                else -> "No keybinds match \"$localSearchQuery\""
            }
            Text(message, color = LocalTheme.current.textColorSecondary, fontSize = 15.sp)
        }
        return
    }

    val conflicts = KeybindConflicts.conflictMap()

    val searching = localSearchQuery.isNotBlank()

    ScrollbarBox(listState, PageGutter, Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = PageGutter),
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
    val interaction = rememberInteractionSource()
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 90f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onClick(interaction, onClick = onToggle)
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
        Icon("up", theme.textColorSecondary, Modifier.size(12.dp), rotation = chevronRotation)
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
    var menuOpen by remember(System.identityHashCode(prop)) { mutableStateOf(false) }
    var menuOffset by remember(System.identityHashCode(prop)) { mutableStateOf(IntOffset.Zero) }
    val rowOrigin = remember(System.identityHashCode(prop)) { arrayOf(Offset.Zero) }
    val actionMenuOffset = remember(System.identityHashCode(prop)) { arrayOf(IntOffset.Zero) }
    val rowInteraction = rememberInteractionSource()
    val isRowHovered by rowInteraction.collectIsHoveredAsState()
    val showActionButton = OneConfigConfig.showOptionActionButtons
    fun openMenuFromActionButton() {
        menuOffset = actionMenuOffset[0]
        menuOpen = true
    }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(displayAlpha(display))
            .hoverable(rowInteraction)
            .onGloballyPositioned { rowOrigin[0] = it.positionInRoot() }
            .onPointerEvent { event ->
                if (event.type != PointerEventType.Press || event.button != 1 || KeybindRecordingBus.isRecording) {
                    return@onPointerEvent false
                }
                menuOffset = with(density) {
                    IntOffset((event.x - event.node.x).dp.roundToPx(), (event.y - event.node.y).dp.roundToPx())
                }
                menuOpen = true
                true
            }
            .background(theme.modCardBackground, shape)
            .border(
                1.dp,
                Brush.verticalGradient(listOf(theme.borderColor, theme.borderColor.copy(0f))),
                shape,
            )
    ) {
        val vignetteColor = theme.textColor
        Canvas(Modifier.fillMaxSize()) { x, y, w, h ->
            val gradient = RadialGradient(
                colors = listOf(
                    vignetteColor.copy(alpha = 0f),
                    vignetteColor.copy(alpha = 0.02f),
                    vignetteColor.copy(alpha = 0.05f),
                ),
                radius = 0.9f,
            )
            rectGradient(x, y, w, h, gradient)
        }
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
                        prop.localizedTitle(),
                        color = theme.textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        softWrap = true,
                    )
                    prop.localizedDescription()?.takeUnless { it.isEmptyText() }?.let {
                        Text(it, color = theme.textColorSecondary, fontSize = 13.sp, softWrap = true)
                    }
                    Text(
                        "${entry.category} / ${entry.subcategory}",
                        color = theme.textColorSecondary,
                        fontSize = 11.sp,
                        softWrap = true,
                    )
                    if (conflictsWith.isNotEmpty()) {
                        val names = conflictsWith.joinToString(", ") { KeybindConflicts.displayName(it) }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon("alert-circle", color = CONFLICT_COLOR, modifier = Modifier.size(12.dp))
                            Text(
                                "Conflicts with $names",
                                color = CONFLICT_COLOR,
                                fontSize = 11.sp,
                                softWrap = true,
                            )
                        }
                    }
                }
            }
            if (showActionButton) {
                OptionActionButton(
                    visible = isRowHovered || menuOpen,
                    modifier = Modifier.onGloballyPositioned {
                        val pos = it.positionInRoot() - rowOrigin[0]
                        actionMenuOffset[0] = IntOffset(pos.x.roundToInt(), (pos.y + it.size.height).roundToInt())
                    },
                    onClick = ::openMenuFromActionButton,
                )
            }
            if (prop.get() is OneConfigKeybind) {
                Box(Modifier.blockInteraction(!enabled)) {
                    KeybindOption(KeybindOptionData(prop))
                }
            } else {
                val shown = prop.get()
                Text(
                    when (shown) {
                        is Boolean -> if (shown) "On" else "Off"
                        null -> "—"
                        else -> shown.toString()
                    },
                    color = theme.textColorSecondary,
                )
            }
        }

        OptionContextMenu(prop, menuOpen, menuOffset, onDismiss = { menuOpen = false })
    }
}

const val KEYBINDS_PAGE_TOP = 19f + 32f + 16f

class UiClipboard(val read: () -> String?, val write: (String) -> Unit)

val LocalUiClipboard = staticCompositionLocalOf { UiClipboard({ null }, {}) }

@Composable
fun KeybindsWindow(onClose: () -> Unit) {
    val search = remember { TextFieldState() }
    PolyWindow(onClose) {
        GlobalSearchBar(search, "Search keybinds...", Modifier.absoluteOffset(PageGutter, 19.dp))
        Box(Modifier.fillMaxSize().padding(top = KEYBINDS_PAGE_TOP.dp, bottom = 19.dp)) {
            Keybinds(search.text.trim())
        }
    }
}

