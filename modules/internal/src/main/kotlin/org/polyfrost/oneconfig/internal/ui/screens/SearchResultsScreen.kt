package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.ModToggles
import org.polyfrost.oneconfig.api.ui.v1.components.EmptyState
import org.polyfrost.oneconfig.api.ui.v1.components.LoadState
import org.polyfrost.oneconfig.api.ui.v1.components.ScrollContainer
import org.polyfrost.oneconfig.api.ui.v1.components.SectionHeader
import org.polyfrost.oneconfig.api.ui.v1.components.SectionHeaderDefaults
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.PropertyModToggle
import org.polyfrost.oneconfig.internal.ui.api.isLockExempt
import org.polyfrost.oneconfig.internal.ui.api.locksOptions
import org.polyfrost.oneconfig.internal.ui.components.blockInteraction
import org.polyfrost.oneconfig.internal.ui.components.rememberModEnabled
import org.polyfrost.oneconfig.internal.ui.components.scrollFade
import org.polyfrost.oneconfig.internal.ui.components.settings.LocalOpenOptionPage
import org.polyfrost.oneconfig.internal.ui.components.settings.asPlainLabel
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.PreferencesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ThemesGraph
import org.polyfrost.oneconfig.internal.ui.search.GlobalSettingIndex
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus
import org.polyfrost.oneconfig.internal.ui.search.SearchDocument
import org.polyfrost.oneconfig.internal.ui.search.SearchRow
import org.polyfrost.oneconfig.internal.ui.search.SearchScope
import org.polyfrost.oneconfig.internal.ui.search.SettingNode
import org.polyfrost.oneconfig.internal.ui.search.anchor
import org.polyfrost.oneconfig.internal.ui.search.searchNode
import org.polyfrost.oneconfig.internal.ui.shell.LocalShell
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.shell.ShellNavigation
import org.polyfrost.oneconfig.internal.ui.shell.rememberRestorableLazyListState

private const val MOD_GRID_COLUMNS = 4
private val MOD_GRID_GAP = 19.dp
private val LIST_GAP = 8.dp

private val MOD_ROW_EXTRA_GAP = MOD_GRID_GAP - LIST_GAP

@Composable
fun SearchResultsScreen(query: String, columns: Int = MOD_GRID_COLUMNS) {
    var searchedQuery by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<Map<SearchRow?, List<SearchDocument<*>>>>(emptyMap()) }
    LaunchedEffect(query) {
        val found = withContext(Dispatchers.Default) {
            SearchCorpus.searchGrouped(query, setOf(SearchScope.Mods, SearchScope.Options), GlobalSettingIndex::rowOf)
        }
        results = found
        searchedQuery = query
    }

    val matchingMods: List<ConfigData> = remember(results) {
        results[null].orEmpty().map { it.payload }.filterIsInstance<ConfigData>()
    }
    // grouped by owning mod in order of first appearance so a mod is headed once while keeping its best
    // hit's rank and accordions collapse into one row each
    val groupedOptions: Map<String, List<Pair<SettingNode, SearchRow>>> = remember(results) {
        val byGroup = LinkedHashMap<String, MutableList<Pair<SettingNode, SearchRow>>>()
        results.forEach { (row, documents) ->
            if (row == null || documents.isEmpty()) return@forEach
            val node = searchNode(row.node, documents) ?: return@forEach
            val mod = row.modTitle ?: translated("oneconfig.search.other", "Other")
            byGroup.getOrPut(row.groupLabel?.let { "$mod / $it" } ?: mod) { ArrayList() } += (node to row)
        }
        byGroup
    }

    val modRows = remember(matchingMods, columns) { matchingMods.chunked(columns) }

    // results already revealed in this search session skip the reveal when a later query keeps them
    val revealed = remember { HashSet<Any>() }

    val listState = rememberRestorableLazyListState("global-search", query, searchedQuery)

    if (matchingMods.isEmpty() && groupedOptions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // nothing to say until the first search comes back
            val searched = searchedQuery
            if (searched != null) {
                EmptyState(translated("oneconfig.search.no_results", "No results for \"%s\"", searched), icon = "search")
            } else {
                EmptyState(translated("oneconfig.search.searching", "Searching..."), state = LoadState.Loading)
            }
        }
        return
    }

    ScrollContainer(listState, Modifier.fillMaxSize(), gutter = PageGutter) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().scrollFade(listState),
            contentPadding = PaddingValues(horizontal = PageGutter),
            verticalArrangement = Arrangement.spacedBy(LIST_GAP),
        ) {
            // each item's place in the list staggers its reveal, see Modifier.searchReveal
            var order = 0
            if (matchingMods.isNotEmpty()) {
                val headerOrder = order++
                item(key = "header:mods") {
                    SectionHeader(
                        translated("oneconfig.search.mods_header", "MODS"),
                        Modifier.searchReveal(revealed, listState, "header:mods", headerOrder),
                        textStyle = SectionHeaderDefaults.labelStyle,
                        color = OneConfigTheme.current.textColorSecondary,
                        uppercase = false,
                    )
                }
                val firstRowOrder = order
                order += modRows.size
                items(modRows.size, key = { "mods-row:$it" }) { index ->
                    BoxWithConstraints(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (index == modRows.lastIndex) 0.dp else MOD_ROW_EXTRA_GAP)
                    ) {
                        val cellWidth = with(LocalDensity.current) {
                            val gap = MOD_GRID_GAP.roundToPx()
                            ((maxWidth.roundToPx() - gap * (columns - 1)) / columns).toDp()
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(MOD_GRID_GAP)) {
                            modRows[index].forEach { mod ->
                                Box(Modifier.searchReveal(revealed, listState, "mod:${mod.id}", firstRowOrder + index).width(cellWidth)) { ModCard(mod) }
                            }
                        }
                    }
                }
            }

            groupedOptions.forEach { (group, nodes) ->
                val groupOrder = order++
                item(key = "header:opts:$group") {
                    SectionHeader(
                        group.asPlainLabel(),
                        Modifier.searchReveal(revealed, listState, "header:opts:$group", groupOrder).padding(top = 4.dp),
                        textStyle = SectionHeaderDefaults.labelStyle,
                        color = OneConfigTheme.current.textColorSecondary,
                    )
                }
                nodes.forEachIndexed { idx, (node, row) ->
                    val nodeOrder = order++
                    val mod = row.mod
                    item(key = "opt:$group:$idx") {
                        val toggle = remember(mod, ModToggles.revision) { mod?.toggle }
                        val enabled by rememberModEnabled(toggle)
                        val ownSwitch = (toggle as? PropertyModToggle)?.property
                        val isOwnSwitch = ownSwitch != null && (node as? SettingNode.Leaf)?.prop === ownSwitch
                        val exempt = when (node) {
                            is SettingNode.Leaf -> node.prop.isLockExempt()
                            is SettingNode.Accordion -> node.tree.isLockExempt()
                            is SettingNode.Submenu -> node.tree.isLockExempt()
                        }
                        val lock = !enabled && toggle.locksOptions() && !isOwnSwitch && !exempt
                        val navigation = LocalShell.current.navigation
                        val openPage = remember(row) { { option: Property<*> -> navigation.openPageOf(row, option) } }
                        CompositionLocalProvider(
                            LocalConfigId provides mod?.id,
                            LocalSearchExpands provides true,
                            LocalOpenOptionPage provides openPage,
                        ) {
                            Box(
                                Modifier.searchReveal(revealed, listState, node.anchor(), nodeOrder)
                                    .alpha(if (lock) 0.45f else 1f).blockInteraction(lock, keyboard = true)
                            ) { SettingEntryRow(node) }
                        }
                    }
                }
            }
        }
    }
}

private const val REVEAL_MS = 520
private const val REVEAL_STAGGER_MS = 45
private const val REVEAL_MAX_DELAY_MS = 360
private const val SCROLL_REVEAL_MS = 260
private val REVEAL_RISE = 14.dp
private const val REVEAL_SCALE = 0.94f

/**
 * Fades, scales and lifts an item into place when "Animate Search Results" is on
 *
 * [id] identifies the result so it only reveals the first time it shows up in [revealed], and [order]
 * staggers items top to bottom unless they come into view from scrolling, which reveals them at once and faster
 */
@Composable
private fun Modifier.searchReveal(revealed: MutableSet<Any>, listState: LazyListState, id: Any, order: Int): Modifier {
    if (!OneConfigConfig.animateSearchResults) return this
    val progress = remember(id) { Animatable(if (id in revealed) 1f else 0f) }
    LaunchedEffect(progress) {
        if (!revealed.add(id)) return@LaunchedEffect
        val spec = if (listState.isScrollInProgress) tween<Float>(SCROLL_REVEAL_MS, easing = EaseOutCubic)
            else tween(REVEAL_MS, delayMillis = (order * REVEAL_STAGGER_MS).coerceAtMost(REVEAL_MAX_DELAY_MS), easing = EaseOutCubic)
        progress.animateTo(1f, spec)
    }
    return graphicsLayer {
        val remaining = 1f - progress.value
        alpha = progress.value
        translationY = remaining * REVEAL_RISE.toPx()
        val scale = 1f - remaining * (1f - REVEAL_SCALE)
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Opens the page [row] is on and has that page scroll to and highlight [option], which in the minimal menu also
 * expands it to the full menu
 */
private fun ShellNavigation.openPageOf(row: SearchRow, option: Property<*>) {
    val id = row.mod?.id ?: return
    when {
        row.submenu != null -> navigate(ModConfigRoute(id, submenu = row.submenu))
        // OneConfig's own configs have pages of their own rather than a mod page
        id == PREFERENCES_ID -> navigate(PreferencesGraph)
        id == THEMES_ID -> navigate(ThemesGraph)
        else -> navigate(ModConfigRoute(id))
    }
    // the page opens on the tab holding the option
    jumpTo(option)
}
