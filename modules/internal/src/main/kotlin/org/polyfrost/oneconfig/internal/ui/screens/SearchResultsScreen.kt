package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.components.ModResult
import org.polyfrost.oneconfig.internal.ui.components.OptionResult
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.performSearch
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.api.config.v1.Property

@Composable
fun SearchResultsScreen(query: String) {
    val theme = LocalTheme.current
    val results by remember(query) { derivedStateOf { performSearch(query) } }

    val matchingMods: List<ConfigData> = remember(results) {
        results.values.flatten().filterIsInstance<ModResult>().map { it.config }
    }
    val groupedOptions: Map<String, List<Property<*>>> = remember(results) {
        val map = LinkedHashMap<String, MutableList<Property<*>>>()
        results.forEach { (group, items) ->
            items.filterIsInstance<OptionResult>().forEach { opt ->
                if (opt.prop != null) {
                    map.getOrPut(group) { mutableListOf() }.add(opt.prop)
                }
            }
        }
        map
    }

    if (matchingMods.isEmpty() && groupedOptions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No results for \"$query\"", color = theme.textColorSecondary, fontSize = 15.sp)
        }
        return
    }

    val listState = rememberLazyListState()
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
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(19.dp),
                        verticalArrangement = Arrangement.spacedBy(19.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 4,
                    ) {
                        matchingMods.forEach { mod ->
                            Box(Modifier.weight(1f)) {
                                ModCard(mod)
                            }
                        }
                        val remainder = (4 - matchingMods.size % 4) % 4
                        repeat(remainder) {
                            Box(Modifier.weight(1f))
                        }
                    }
                }
            }

            groupedOptions.forEach { (group, props) ->
                item(key = "header:opts:$group") {
                    Text(
                        group.uppercase(),
                        color = theme.textColorSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }
                props.forEachIndexed { idx, prop ->
                    item(key = "opt:$group:${prop.id}:$idx") {
                        SettingRow(prop)
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
