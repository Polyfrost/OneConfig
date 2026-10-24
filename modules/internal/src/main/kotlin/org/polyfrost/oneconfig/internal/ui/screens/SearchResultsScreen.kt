package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.lazy.LazyColumn
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ModToggles
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.PropertyModToggle
import org.polyfrost.oneconfig.internal.ui.components.ScrollbarBox
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.blockInteraction
import org.polyfrost.oneconfig.internal.ui.components.rememberModEnabled
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.poly.GUTTER
import org.polyfrost.oneconfig.internal.ui.search.GlobalSettingIndex
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus
import org.polyfrost.oneconfig.internal.ui.search.SearchDocument
import org.polyfrost.oneconfig.internal.ui.search.SearchRow
import org.polyfrost.oneconfig.internal.ui.search.SearchScope
import org.polyfrost.oneconfig.internal.ui.search.SettingNode
import org.polyfrost.oneconfig.internal.ui.search.searchNode
import org.polyfrost.oneconfig.internal.ui.shell.ShellHost
import org.polyfrost.oneconfig.internal.ui.shell.rememberRestorableLazyListState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private const val MOD_GRID_COLUMNS = 4
private val MOD_GRID_GAP = 19.dp
private val LIST_GAP = 8.dp

private val MOD_ROW_EXTRA_GAP = MOD_GRID_GAP - LIST_GAP

@Composable
fun SearchResultsScreen(query: String, host: ShellHost) {
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
    val groupedOptions: Map<String, List<Pair<SettingNode, ConfigData?>>> = remember(results) {
        val byGroup = LinkedHashMap<String, MutableList<Pair<SettingNode, ConfigData?>>>()
        results.forEach { (row, documents) ->
            if (row == null || documents.isEmpty()) return@forEach
            val node = searchNode(row.node, documents) ?: return@forEach
            val mod = row.modTitle ?: "Other"
            byGroup.getOrPut(row.groupLabel?.let { "$mod / $it" } ?: mod) { ArrayList() } += (node to row.mod)
        }
        byGroup
    }

    val modRows = remember(matchingMods) { matchingMods.chunked(MOD_GRID_COLUMNS) }

    val listState = rememberRestorableLazyListState("global-search", query, searchedQuery)

    if (matchingMods.isEmpty() && groupedOptions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // nothing to say until the first search comes back
            searchedQuery?.also {
                Text(translated("oneconfig.search.no_results", "No results for \"%s\"", it), color = theme.textColorSecondary, fontSize = 15.sp)
            } ?: Text(translated("oneconfig.search.searching", "Searching..."), color = theme.textColorSecondary, fontSize = 15.sp)
        }
        return
    }

    ScrollbarBox(listState, GUTTER.dp, Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = GUTTER.dp),
            verticalArrangement = Arrangement.spacedBy(LIST_GAP),
        ) {
            if (matchingMods.isNotEmpty()) {
                item(key = "header:mods") {
                    Text(
                        translated("oneconfig.search.mods_header", "MODS"),
                        color = theme.textColorSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(modRows.size, key = { "mods-row:$it" }) { index ->
                    Row(
                        Modifier
                            .padding(bottom = if (index == modRows.lastIndex) 0.dp else MOD_ROW_EXTRA_GAP)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MOD_GRID_GAP),
                    ) {
                        modRows[index].forEach { mod ->
                            key(mod.id) {
                                Box(Modifier.weight(1f).height(140.dp)) { ModCard(mod, host) }
                            }
                        }
                        repeat(MOD_GRID_COLUMNS - modRows[index].size) { Box(Modifier.weight(1f).height(140.dp)) }
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
                nodes.forEachIndexed { idx, (node, mod) ->
                    item(key = "opt:$group:$idx") {
                        val toggle = remember(mod, ModToggles.revision) { mod?.toggle }
                        val enabled by rememberModEnabled(toggle)
                        val ownSwitch = (toggle as? PropertyModToggle)?.property
                        val isOwnSwitch = ownSwitch != null && (node as? SettingNode.Leaf)?.prop === ownSwitch
                        val lock = !enabled && !isOwnSwitch
                        Box(Modifier.fillMaxWidth().alpha(if (lock) 0.45f else 1f).blockInteraction(lock, keyboard = true)) { SettingEntryRow(node) }
                    }
                }
            }
        }
    }
}
