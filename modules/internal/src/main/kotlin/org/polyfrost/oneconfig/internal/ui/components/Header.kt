package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.TreeConfigData
import org.polyfrost.oneconfig.internal.ui.LocalCloseRequest
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Header() {
    val controller = LocalNavController.wrapper
    val closeRequest = LocalCloseRequest.current

    Row(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton("left-arrow") { controller.back() }
                IconButton("right-arrow") { controller.forward() }
            }
            AnimatedContent(
                ShellState.title,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { text ->
                text?.let {
                    Text(it, color = LocalTheme.current.textColor, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            GlobalSearchBar()
            IconButton("close") { closeRequest() }
        }
    }
}

internal sealed interface SearchResult {
    val displayName: String
    val icon: String?
}

internal data class ModResult(val config: ConfigData) : SearchResult {
    override val displayName get() = config.title
    override val icon get() = config.icon
}

internal data class OptionResult(
    val modId: String,
    val modTitle: String,
    val optionTitle: String,
    val category: String?,
    override val icon: String?,
    val prop: Property<*>?,
) : SearchResult {
    override val displayName get() = optionTitle
}

internal fun performSearch(query: String): Map<String, List<SearchResult>> {
    if (query.isBlank()) return emptyMap()
    val q = query.trim().lowercase()
    val results = LinkedHashMap<String, MutableList<SearchResult>>()

    val matchingMods = ConfigRegistry.configs.filter { it.title.lowercase().contains(q) }
    if (matchingMods.isNotEmpty()) {
        results["Mods"] = matchingMods.map { ModResult(it) }.toMutableList()
    }

    for (configData in ConfigRegistry.configs) {
        val tree = (configData as? TreeConfigData)?.tree ?: continue
        val matchingOptions = mutableListOf<SearchResult>()
        tree.map.values.forEach { node ->
            when (node) {
                is Property<*> -> {
                    val title = node.title ?: return@forEach
                    if (title.lowercase().contains(q)) {
                        val cat = node.getMetadata<String>("category")
                        matchingOptions += OptionResult(configData.id, configData.title, title, cat, configData.icon, node)
                    }
                }
                is Tree -> {
                    val subTitle = node.title
                    if (subTitle != null && subTitle.lowercase().contains(q)) {
                        val cat = node.getMetadata<String>("category")
                        matchingOptions += OptionResult(configData.id, configData.title, subTitle, cat, configData.icon, null)
                    }
                    node.map.values.filterIsInstance<Property<*>>().forEach { prop ->
                        val pt = prop.title ?: return@forEach
                        if (pt.lowercase().contains(q)) {
                            val cat = prop.getMetadata<String>("category")
                            matchingOptions += OptionResult(configData.id, configData.title, pt, cat, configData.icon, prop)
                        }
                    }
                }
            }
        }
        if (matchingOptions.isNotEmpty()) {
            results[configData.title] = matchingOptions
        }
    }

    return results
}

@Composable
fun GlobalSearchBar() {
    var searchText by remember { mutableStateOf(ShellState.searchQuery) }
    val interactionSource = rememberInteractionSource()
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(searchText) { ShellState.searchQuery = searchText }
    LaunchedEffect(ShellState.searchQuery) { if (ShellState.searchQuery != searchText) searchText = ShellState.searchQuery }

    val searchTextStyle = TextStyle(
        color = LocalTheme.current.textColor,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        fontFamily = LocalTheme.current.typography.family,
        lineHeight = 14.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both,
        ),
    )

    BasicTextField(
        value = searchText,
        onValueChange = { searchText = it },
        singleLine = true,
        cursorBrush = SolidColor(LocalTheme.current.textColor),
        textStyle = searchTextStyle,
        interactionSource = interactionSource
    ) { innerTextField ->
        Box(
            modifier = Modifier
                .width(256.dp).height(32.dp)
                .background(
                    LocalTheme.current.chipBackground,
                    LocalTheme.current.sideBarNavigationEntryShape
                )
                .border(
                    1.dp,
                    LocalTheme.current.borderColor,
                    LocalTheme.current.sideBarNavigationEntryShape
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon("search", color = LocalTheme.current.textColorSecondary)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (searchText.isEmpty() && !isFocused) {
                        Text(
                            "Search for ...",
                            color = LocalTheme.current.textColorSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            lineHeight = 14.sp,
                        )
                    }
                    innerTextField()
                }
                if (searchText.isNotEmpty()) {
                    IconButton("close", modifier = Modifier.size(16.dp)) {
                        searchText = ""
                    }
                }
            }
        }
    }
}


