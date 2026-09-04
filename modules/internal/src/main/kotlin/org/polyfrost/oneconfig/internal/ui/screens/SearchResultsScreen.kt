package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.search.GlobalSettingIndex
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus
import org.polyfrost.oneconfig.internal.ui.search.SearchDocument
import org.polyfrost.oneconfig.internal.ui.search.SearchRow
import org.polyfrost.oneconfig.internal.ui.search.SearchScope
import org.polyfrost.oneconfig.internal.ui.search.SettingNode
import org.polyfrost.oneconfig.internal.ui.search.searchNode
import org.polyfrost.oneconfig.internal.ui.shell.rememberRestorableLazyListState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private const val MOD_GRID_COLUMNS = 4
private val MOD_GRID_GAP = 19.dp
private val LIST_GAP = 8.dp

/** The list already spaces its items, so a mod row only adds the difference up to the grid gap */
private val MOD_ROW_EXTRA_GAP = MOD_GRID_GAP - LIST_GAP

@Composable
fun SearchResultsScreen(query: String) {
    val theme = LocalTheme.current

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
    val groupedOptions: Map<String, List<SettingNode>> = remember(results) {
        val byGroup = LinkedHashMap<String, MutableList<SettingNode>>()
        results.forEach { (row, documents) ->
            if (row == null || documents.isEmpty()) return@forEach
            val node = searchNode(row.node, documents) ?: return@forEach
            val mod = row.modTitle ?: "Other"
            byGroup.getOrPut(row.groupLabel?.let { "$mod / $it" } ?: mod) { ArrayList() } += node
        }
        byGroup
    }

    val modRows = remember(matchingMods) { matchingMods.chunked(MOD_GRID_COLUMNS) }

    val listState = rememberRestorableLazyListState("global-search", query, searchedQuery)

    if (matchingMods.isEmpty() && groupedOptions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // nothing to say until the first search comes back
            searchedQuery?.also {
                Text("No results for \"$it\"", color = theme.textColorSecondary, fontSize = 15.sp)
            } ?: Text("Searching...", color = theme.textColorSecondary, fontSize = 15.sp)
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(LIST_GAP),
        ) {
            if (matchingMods.isNotEmpty()) {
                item(key = "header:mods") {
                    Text(
                        "MODS",
                        color = theme.textColorSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                // one item per row, not one item holding every row: the whole grid in a single item
                // composes every match on the keystroke that produced it, so the list stopped being lazy
                items(modRows.size, key = { "mods-row:$it" }) { index ->
                    BoxWithConstraints(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (index == modRows.lastIndex) 0.dp else MOD_ROW_EXTRA_GAP)
                    ) {
                        // four cells and three gaps come to exactly maxWidth and each rounds up on its
                        // own, so divide in pixels and leave the remainder as slack, as the grid does
                        val cellWidth = with(LocalDensity.current) {
                            val gap = MOD_GRID_GAP.roundToPx()
                            ((maxWidth.roundToPx() - gap * (MOD_GRID_COLUMNS - 1)) / MOD_GRID_COLUMNS).toDp()
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(MOD_GRID_GAP)) {
                            modRows[index].forEach { mod ->
                                Box(Modifier.width(cellWidth)) { ModCard(mod) }
                            }
                        }
                    }
                }
            }

            groupedOptions.forEach { (group, nodes) ->
                item(key = "header:opts:$group") {
                    Text(
                        group.uppercase(),
                        color = theme.textColorSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                nodes.forEachIndexed { idx, node ->
                    item(key = "opt:$group:$idx") {
                        SettingEntryRow(node)
                    }
                }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(listState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}
