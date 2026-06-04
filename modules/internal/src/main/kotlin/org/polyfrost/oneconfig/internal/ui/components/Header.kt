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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.navigation.compose.currentBackStackEntryAsState
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.TreeConfigData
import org.polyfrost.oneconfig.internal.ui.LocalCloseRequest
import org.polyfrost.oneconfig.internal.ui.navigation.searchPlaceholder
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
    val displayName: Any
    val icon: String?
}

internal data class ModResult(val config: ConfigData) : SearchResult {
    override val displayName get() = config.title
    override val icon get() = config.icon
}

internal data class OptionResult(
    val modId: String,
    val modTitle: Any,
    val optionTitle: Any,
    val category: String?,
    override val icon: String?,
    val prop: Property<*>?,
) : SearchResult {
    override val displayName get() = optionTitle
}

/**
 * Computes the Levenshtein (edit) distance between two strings, capped early once it exceeds [max].
 */
private fun levenshtein(a: String, b: String, max: Int): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    if (kotlin.math.abs(a.length - b.length) > max) return max + 1
    var prev = IntArray(b.length + 1) { it }
    var curr = IntArray(b.length + 1)
    for (i in 1..a.length) {
        curr[0] = i
        var rowMin = curr[0]
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
            if (curr[j] < rowMin) rowMin = curr[j]
        }
        if (rowMin > max) return max + 1
        val tmp = prev; prev = curr; curr = tmp
    }
    return prev[b.length]
}

/**
 * Returns true if [text] matches [q] either as a substring or, when "Search Distance" > 0, by a fuzzy
 * (Levenshtein) match against the whole string or any of its words. [q] is expected to be lowercase.
 */
private fun searchMatches(text: String, q: String): Boolean {
    val t = text.lowercase()
    if (t.contains(q)) return true
    val dist = OneConfigConfig.searchDistance
    if (dist <= 0) return false
    if (levenshtein(t, q, dist) <= dist) return true
    return t.split(' ', '.', '_', '-', '/').any { it.isNotEmpty() && levenshtein(it, q, dist) <= dist }
}

internal fun performSearch(query: String): Map<String, List<SearchResult>> {
    if (query.isBlank()) return emptyMap()
    val q = query.trim().lowercase()
    val results = LinkedHashMap<String, MutableList<SearchResult>>()

    val matchingMods = ConfigRegistry.configs.filter { searchMatches(it.title.asRenderText(), q) }
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
                    if (searchMatches(title.asRenderText(), q)) {
                        val cat = node.getMetadata<String>("category")
                        matchingOptions += OptionResult(configData.id, configData.title, title, cat, configData.icon, node)
                    }
                }
                is Tree -> {
                    val subTitle = node.title
                    if (subTitle != null && searchMatches(subTitle.asRenderText(), q)) {
                        val cat = node.getMetadata<String>("category")
                        matchingOptions += OptionResult(configData.id, configData.title, subTitle, cat, configData.icon, null)
                    }
                    node.map.values.filterIsInstance<Property<*>>().forEach { prop ->
                        val pt = prop.title ?: return@forEach
                        if (searchMatches(pt.asRenderText(), q)) {
                            val cat = prop.getMetadata<String>("category")
                            matchingOptions += OptionResult(configData.id, configData.title, pt, cat, configData.icon, prop)
                        }
                    }
                }
            }
        }
        if (matchingOptions.isNotEmpty()) {
            results[configData.title.asRenderText()] = matchingOptions
        }
    }

    return results
}

@Composable
fun GlobalSearchBar() {
    val navController = LocalNavController.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val placeholder = searchPlaceholder(backStackEntry?.destination)

    var searchText by remember { mutableStateOf(ShellState.searchQuery) }
    val interactionSource = rememberInteractionSource()
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(searchText) { ShellState.searchQuery = searchText }
    LaunchedEffect(ShellState.searchQuery) { if (ShellState.searchQuery != searchText) searchText = ShellState.searchQuery }

    val searchTextStyle = TextStyle(
        color = LocalTheme.current.textColorSecondary,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        fontFamily = LocalTheme.current.typography.family,
        lineHeight = 12.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both,
        ),
    )

    BasicTextField(
        value = searchText,
        onValueChange = { searchText = it },
        singleLine = true,
        cursorBrush = SolidColor(LocalTheme.current.textColorSecondary),
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        "search",
                        color = LocalTheme.current.textColorSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (searchText.isEmpty() && !isFocused) {
                        Text(
                            placeholder,
                            color = LocalTheme.current.textColorSecondary,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 12.sp,
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


