package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/** Cards per row, matching the mods screen grid. */
private const val MOD_COLUMNS = 4

@Composable
fun SearchResultsScreen(query: String) {
    val theme = LocalTheme.current

    // Run search asynchronously
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
    // Grouped by owning mod, in order of first appearance, so a mod is only headed once while keeping its best hit's
    // rank. Accordions collapse into one row per accordion rather than one per matching option inside it.
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

    val listState = rememberRestorableLazyListState("global-search", query)

    if (matchingMods.isEmpty() && groupedOptions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Nothing to say until the first search comes back.
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                item(key = "mods-grid") {
                    // Use fixed width or HUD mod card dies when rendering
                    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
                        matchingMods.chunked(MOD_COLUMNS).forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(19.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                row.forEach { mod ->
                                    Box(Modifier.weight(1f)) {
                                        ModCard(mod)
                                    }
                                }
                                repeat(MOD_COLUMNS - row.size) {
                                    Box(Modifier.weight(1f))
                                }
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
