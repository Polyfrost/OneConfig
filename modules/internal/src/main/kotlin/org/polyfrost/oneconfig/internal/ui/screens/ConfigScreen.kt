package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.TooltipArea
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.Spacer
import org.polyfrost.compose.foundation.layout.align
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.widthIn
import org.polyfrost.compose.foundation.lazy.LazyColumn
import org.polyfrost.compose.foundation.shape.RoundedCornerShape
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.geometry.Offset
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.layout.onGloballyPositioned
import org.polyfrost.compose.ui.layout.onPlaced
import org.polyfrost.compose.ui.layout.positionInRoot
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.IntOffset
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.ui.v1.SettingRowDecorator
import org.polyfrost.oneconfig.api.ui.v1.SettingRowDecorators
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.Chip
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
import org.polyfrost.oneconfig.internal.ui.components.settings.LocalOptionWidth
import org.polyfrost.oneconfig.internal.ui.components.settings.Option
import org.polyfrost.oneconfig.internal.ui.components.settings.OptionActionButton
import org.polyfrost.oneconfig.internal.ui.components.settings.OptionContextMenu
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindRecordingBus
import org.polyfrost.oneconfig.internal.ui.poly.AnimatedVisibility
import org.polyfrost.oneconfig.internal.ui.poly.GUTTER
import org.polyfrost.oneconfig.internal.ui.poly.richText
import org.polyfrost.oneconfig.internal.ui.search.CategoryGroup
import org.polyfrost.oneconfig.internal.ui.search.ConfigListEntry
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus
import org.polyfrost.oneconfig.internal.ui.search.SearchDocument
import org.polyfrost.oneconfig.internal.ui.search.SearchRow
import org.polyfrost.oneconfig.internal.ui.search.SearchScope
import org.polyfrost.oneconfig.internal.ui.search.SettingNode
import org.polyfrost.oneconfig.internal.ui.search.SubcategoryGroup
import org.polyfrost.oneconfig.internal.ui.search.buildCategories
import org.polyfrost.oneconfig.internal.ui.search.buildSearchIndex
import org.polyfrost.oneconfig.internal.ui.search.filterHiddenNodes
import org.polyfrost.oneconfig.internal.ui.search.flattenEntries
import org.polyfrost.oneconfig.internal.ui.search.flattenSearchEntries
import org.polyfrost.oneconfig.internal.ui.search.searchMatches
import org.polyfrost.oneconfig.internal.ui.search.searchNode
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.shell.rememberRestorableLazyListState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun ConfigScreen(
    tree: Tree,
    initialCategory: String? = null,
    pageKey: String,
    locked: Boolean = false,
    lockExempt: Property<*>? = null,
    searchQuery: String = "",
    onCategory: ((from: String?, to: String) -> Unit)? = null,
) {
    key(System.identityHashCode(tree)) {
        ConfigScreenContent(tree, initialCategory, pageKey, locked, lockExempt, searchQuery, onCategory)
    }
}

@Composable
private fun ConfigScreenContent(
    tree: Tree,
    initialCategory: String?,
    pageKey: String,
    locked: Boolean,
    lockExempt: Property<*>?,
    searchQuery: String,
    onCategory: ((from: String?, to: String) -> Unit)?,
) {
    val categories = remember(tree) { buildCategories(tree) }
    val localSearchQuery = searchQuery.trim()

    val savedCategory = initialCategory ?: ShellState.selectedCategories[pageKey]
    val selectedCategory = categories.firstOrNull { it.name.equals(savedCategory, ignoreCase = true) }
        ?: categories.firstOrNull()

    fun selectCategory(category: CategoryGroup) {
        ShellState.selectedCategories[pageKey] = category.name
        if (category != selectedCategory) onCategory?.invoke(selectedCategory?.name, category.name)
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(19.dp)) {
        if (localSearchQuery.isBlank() && categories.size > 1) {
            CategoryChips(categories, gutter = GUTTER.dp) { category ->
                Chip(
                    label = category.name,
                    selected = selectedCategory == category,
                    onClick = { selectCategory(category) }
                )
            }
        }

        val revision = rememberDisplayRevision(categories)
        val index = remember(categories) { buildSearchIndex(categories) }
        val search = rememberSearchResults(index, localSearchQuery, pageKey)
        val results = search.results
        val entries = remember(index, selectedCategory, localSearchQuery, revision, results) {
            when {
                localSearchQuery.isBlank() ->
                    selectedCategory?.let(::filterHiddenNodes)?.let(::flattenEntries).orEmpty()
                results == null -> emptyList()
                else -> flattenSearchEntries(searchCategories(results))
            }
        }

        val lazyListState = rememberRestorableLazyListState("$pageKey/${selectedCategory?.name}", localSearchQuery, search.query)

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val message = when {
                    localSearchQuery.isBlank() -> translated("oneconfig.search.no_settings", "No settings available.")
                    results == null -> translated("oneconfig.search.searching", "Searching...")
                    else -> translated("oneconfig.search.no_settings_match", "No settings match \"%s\"", localSearchQuery)
                }
                Text(message, color = LocalTheme.current.textColorSecondary)
            }
            return@Column
        }

        ScrollbarBox(lazyListState, GUTTER.dp, Modifier.weight(1f).fillMaxWidth().clip()) {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = GUTTER.dp),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(entries, key = ::entryKey) { entry ->
                    val option = ((entry as? ConfigListEntry.Item)?.node as? SettingNode.Leaf)?.prop
                    val lock = locked && (option == null || option !== lockExempt)
                    Box(Modifier.fillMaxWidth().alpha(if (lock) 0.45f else 1f).blockInteraction(lock, keyboard = true)) { ConfigListRow(entry) }
                }
            }
        }
    }
}

private fun entryKey(entry: ConfigListEntry): Any = when (entry) {
    is ConfigListEntry.CategoryHeader -> "c:" + entry.title
    is ConfigListEntry.SubcategoryHeader -> "s:" + entry.title
    is ConfigListEntry.Item -> when (val node = entry.node) {
        is SettingNode.Leaf -> node.prop
        is SettingNode.Accordion -> node.tree
    }
}

@Composable
private fun CategoryChips(categories: List<CategoryGroup>, gutter: Dp, chip: @Composable (CategoryGroup) -> Unit) {
    val theme = LocalTheme.current
    var width by remember { mutableFloatStateOf(0f) }
    val font = PolyFont(theme.typography.family.resolve(FontWeight.Normal), 14f)
    val lines = remember(categories, width, font) {
        val out = ArrayList<MutableList<CategoryGroup>>()
        var used = Float.MAX_VALUE
        for (category in categories) {
            val w = font.measureTextWidth(category.name) + 24f
            if (width <= 0f || used + 8f + w > width) { out += ArrayList<CategoryGroup>(); used = w } else used += 8f + w
            out.last() += category
        }
        out
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = gutter).onPlaced { width = it.width - 2 * gutter.value },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (line in lines) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (category in line) key(category.name) { chip(category) }
        }
    }
}

private fun categoryProperties(categories: List<CategoryGroup>): List<Property<*>> {
    return categories.flatMap { category ->
        category.subcategories.flatMap { subcategory ->
            subcategory.nodes.flatMap { node ->
                when (node) {
                    is SettingNode.Leaf -> listOf(node.prop)
                    is SettingNode.Accordion -> node.body + listOfNotNull(node.head)
                }
            }
        }
    }
}

/**
 * A counter incremented whenever any property in [categories] changes its display state so the entry list
 * can be rebuilt
 */
@Composable
private fun rememberDisplayRevision(categories: List<CategoryGroup>): Int {
    val props = remember(categories) { categoryProperties(categories) }
    var revision by remember(props) { mutableStateOf(0) }
    DisposableEffect(props) {
        val listener = Consumer<Property.Display> { revision++ }
        props.forEach { it.addDisplayListener(listener) }
        onDispose { props.forEach { it.removeDisplayListener(listener) } }
    }
    return revision
}

private class LocalSearchResults(
    val results: Map<SearchRow?, List<SearchDocument<*>>>?,
    val query: String?,
)

@Composable
private fun rememberSearchResults(
    index: Map<Node, SearchRow>,
    query: String,
    pageKey: String,
): LocalSearchResults {
    var results by remember(pageKey) { mutableStateOf<Map<SearchRow?, List<SearchDocument<*>>>?>(null) }
    var searchedQuery by remember(pageKey) { mutableStateOf<String?>(null) }
    LaunchedEffect(index, query, pageKey) {
        results = if (query.isBlank()) emptyMap() else withContext(Dispatchers.Default) {
            SearchCorpus.searchGrouped(query, setOf(SearchScope.Config(pageKey))) { document ->
                (document.payload as? Node)?.let(index::get)
            }
        }
        searchedQuery = query
    }
    return LocalSearchResults(results, searchedQuery)
}

/** Rebuild categories from matched search results */
private fun searchCategories(grouped: Map<SearchRow?, List<SearchDocument<*>>>): List<CategoryGroup> {
    val byCategory = LinkedHashMap<String, LinkedHashMap<String, MutableList<SettingNode>>>()
    grouped.forEach { (row, documents) ->
        if (row == null || documents.isEmpty()) return@forEach
        val node = searchNode(row.node, documents) ?: return@forEach
        byCategory.getOrPut(row.category) { LinkedHashMap() }.getOrPut(row.subcategory) { ArrayList() } += node
    }
    return byCategory.map { (category, subcategories) ->
        CategoryGroup(category, subcategories.map { (name, nodes) -> SubcategoryGroup(name, nodes) })
    }
}

private fun filterCategories(categories: List<CategoryGroup>, query: String): List<CategoryGroup> {
    val q = query.lowercase()
    return categories.mapNotNull { category ->
        val categoryMatches = searchMatches(category.name, q)
        val subcategories = category.subcategories.mapNotNull { subcategory ->
            val subcategoryMatches = categoryMatches || searchMatches(subcategory.name, q)
            val nodes = subcategory.nodes.mapNotNull { node ->
                filterSettingNode(node, category.name, subcategory.name, q, subcategoryMatches)
            }
            if (nodes.isEmpty()) null else subcategory.copy(nodes = nodes)
        }
        if (subcategories.isEmpty()) null else category.copy(subcategories = subcategories)
    }
}

private fun filterSettingNode(
    node: SettingNode,
    category: String,
    subcategory: String,
    query: String,
    groupMatches: Boolean,
): SettingNode? {
    return when (node) {
        is SettingNode.Leaf -> if (groupMatches || node.prop.matchesLocalSearch(category, subcategory, query)) node else null
        is SettingNode.Accordion -> {
            val accordionMatches = groupMatches || node.tree.matchesLocalSearch(category, subcategory, query) ||
                node.head?.matchesLocalSearch(category, subcategory, query) == true
            val body = if (accordionMatches) node.body
            else node.body.filter { it.matchesLocalSearch(category, subcategory, query) }

            if (body.isEmpty()) null else node.copy(body = body)
        }
    }
}

private fun Property<*>.matchesLocalSearch(category: String, subcategory: String, query: String): Boolean {
    return listOfNotNull(localizedTitle(), id, localizedDescription(), category, subcategory)
        .any { searchMatches(it.asRenderText(), query) }
}

private fun Tree.matchesLocalSearch(category: String, subcategory: String, query: String): Boolean {
    return listOfNotNull(localizedTitle(), id, localizedDescription(), category, subcategory)
        .any { searchMatches(it.asRenderText(), query) }
}

/** Width a slider-style control needs beside its track, or null when the control is compact */
private fun sliderTrailingWidth(prop: Property<*>): Dp? {
    return when (prop.getMetadata<Any?>("visualizer")) {
        Visualizer.SliderVisualizer::class.java, is Visualizer.SliderVisualizer -> SLIDER_SPINNER_WIDTH
        // two spinners, or a spinner plus the inherit chip
        Visualizer.RangeSliderVisualizer::class.java, is Visualizer.RangeSliderVisualizer,
        Visualizer.InheritableSliderVisualizer::class.java, is Visualizer.InheritableSliderVisualizer -> SLIDER_SPINNER_WIDTH * 2
        else -> null
    }
}

private fun isFullWidth(prop: Property<*>): Boolean = prop.getMetadata<Boolean>("fullWidth") == true

private fun isWideControl(prop: Property<*>): Boolean = sliderTrailingWidth(prop) != null || isFullWidth(prop)

/**
 * Renders one entry of a flattened settings list
 *
 * [compact] decides per node whether its row stacks the label above the control which the HUD editor needs
 */
@Composable
internal fun ConfigListRow(entry: ConfigListEntry, compact: (SettingNode) -> Boolean = { false }) {
    when (entry) {
        is ConfigListEntry.CategoryHeader -> CategoryHeader(entry.title)
        is ConfigListEntry.SubcategoryHeader -> SubcategoryHeader(entry.title)
        is ConfigListEntry.Item -> SettingEntryRow(entry.node, compact(entry.node))
    }
}

@Composable
internal fun SettingEntryRow(node: SettingNode, compact: Boolean = false) {
    when (node) {
        is SettingNode.Leaf -> SettingRow(node.prop, compact = compact)
        is SettingNode.Accordion -> key(System.identityHashCode(node.tree)) {
            AccordionRow(node, compact = compact)
        }
    }
}

@Composable
private fun CategoryHeader(title: String) {
    Text(
        title,
        color = LocalTheme.current.textColorSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun SubcategoryHeader(title: String) {
    Text(
        title,
        color = LocalTheme.current.textColor,
        fontSize = 22.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun AccordionRow(node: SettingNode.Accordion, compact: Boolean = false) {
    val theme = LocalTheme.current
    val shape = theme.modCardShape

    var expanded by remember(node) {
        mutableStateOf(
            if (node.tree.getMetadata<Boolean>("collapsed") == true) false
            else node.head?.shown() != false
        )
    }
    val headerInteraction = rememberInteractionSource()
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 90f)
    val headDisplay = node.head?.let { rememberDisplay(it) } ?: Property.Display.SHOWN

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.modCardBackground, shape)
            .border(1.dp, remember(theme.borderColor) {
                Brush.verticalGradient(listOf(theme.borderColor, theme.borderColor.copy(0f)))
            }, shape)
    ) {
        val color = theme.textColor
        Canvas(Modifier.fillMaxSize()) { x, y, w, h ->
            if (w <= 0f || h <= 0f) return@Canvas
            val gradient = Brush.radialGradient(
                colors = listOf(color.copy(0f), color.copy(0.02f), color.copy(0.05f)),
                centerY = 0f,
                radius = 0.8f * w / min(w, h),
            )
            rectGradient(x, y, w, h, gradient, shape.radius)
        }
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onClick(headerInteraction) { expanded = !expanded }
                    .alpha(displayAlpha(headDisplay))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    node.tree.getMetadata<String>("icon")?.let { Icon(it, color = theme.textColor, modifier = Modifier.size(32.dp)) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            node.tree.localizedTitle(),
                            color = theme.textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            softWrap = true,
                        )
                        node.tree.localizedDescription()?.let {
                            Text(it, color = theme.textColorSecondary, fontSize = 13.sp, softWrap = true)
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (node.head != null) {
                        var headChecked by remember(node) { mutableStateOf(node.head.shown() == true) }
                        DisposableEffect(node.head) {
                            val listener = Predicate<Boolean> { value -> headChecked = value == true; false }
                            node.head.addCallback(listener)
                            onDispose { node.head.removeCallback(listener) }
                        }
                        SwitchControl(headChecked) {
                            if (headDisplay != Property.Display.DISABLED) {
                                headChecked = it
                                node.head.set(it)
                                expanded = it
                            }
                        }
                    }
                    Icon("up", theme.textColorSecondary, Modifier.size(12.dp), rotation = chevronRotation)
                }
            }

            var bodyHeight by remember { mutableFloatStateOf(0f) }
            AnimatedVisibility(visible = expanded) { open ->
                Box(if (open >= 1f) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().height((bodyHeight * open).dp).clip()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                brush = remember(theme.borderColor) {
                                    Brush.verticalGradient(listOf(theme.borderColor.copy(0f), theme.borderColor))
                                },
                                shape = shape
                            )
                            .padding(bottom = 12.dp)
                            .onPlaced { bodyHeight = it.height }
                    ) {
                        AccordionOptionsGrid(node.body, compact = compact)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingRow(prop: Property<*>, compact: Boolean = false) {
    DecoratedSettingRow(SettingRowDecorators.all(), 0, prop, compact)
}

@Composable
private fun DecoratedSettingRow(decorators: List<SettingRowDecorator>, index: Int, prop: Property<*>, compact: Boolean) {
    val decorator = decorators.getOrNull(index) ?: return SettingRowContent(prop, compact)
    decorator.Decorate(prop) { DecoratedSettingRow(decorators, index + 1, prop, compact) }
}

@Composable
private fun SettingRowContent(prop: Property<*>, compact: Boolean) {
    val display = rememberDisplay(prop)
    if (display == Property.Display.HIDDEN) {
        return
    }

    val theme = LocalTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(displayAlpha(display))
            .background(theme.modCardBackground, theme.modCardShape)
            .border(1.dp, remember(theme.borderColor) {
                Brush.verticalGradient(listOf(theme.borderColor, theme.borderColor.copy(0f)))
            }, theme.modCardShape)
    ) {
        val vignetteColor = theme.textColor
        Canvas(Modifier.fillMaxSize()) { x, y, w, h ->
            if (w <= 0f || h <= 0f) return@Canvas
            val gradient = Brush.radialGradient(
                colors = listOf(
                    vignetteColor.copy(alpha = 0f),
                    vignetteColor.copy(alpha = 0.02f),
                    vignetteColor.copy(alpha = 0.05f)
                ),
                radius = 0.9f
            )
            rectGradient(x, y, w, h, gradient, theme.modCardShape.radius)
        }
        SettingContent(prop, compact = compact, enabled = display != Property.Display.DISABLED)
    }
}

@Composable
private fun AccordionSettingRow(prop: Property<*>, display: Property.Display, modifier: Modifier, compact: Boolean = false) {
    Box(modifier.alpha(displayAlpha(display))) {
        SettingContent(prop, nested = true, compact = compact, enabled = display != Property.Display.DISABLED)
    }
}

@Composable
private fun rememberVisibleOptions(body: List<Property<*>>): List<kotlin.Pair<Property<*>, Property.Display>> {
    val visible = ArrayList<kotlin.Pair<Property<*>, Property.Display>>(body.size)
    for (prop in body) {
        val display = rememberDisplay(prop)
        if (display != Property.Display.HIDDEN) visible += prop to display
    }
    return visible
}

private val OPTION_ROW_GAP = 8.dp

private val OPTION_CHROME_WIDTH = 108.dp
private val OPTION_ICON_WIDTH = 44.dp

private val OPTION_ROW_CHROME = 32.dp + 16.dp

private val SLIDER_SPINNER_WIDTH = 80.dp + 16.dp

private fun optionReservedWidth(prop: Property<*>, optionWidth: Dp): Dp {
    val control = sliderTrailingWidth(prop)?.let { optionWidth + it + OPTION_ROW_CHROME } ?: OPTION_CHROME_WIDTH
    return control + (if (prop.getMetadata<String>("icon") != null) OPTION_ICON_WIDTH else 0.dp)
}

private fun titleWidth(font: PolyFont, prop: Property<*>): Dp =
    font.measureTextWidth(richText(prop.localizedTitle(), font.name, font.name, font.name).first).dp

private sealed interface OptionRow {
    data class Pair(val first: Property<*>, val second: Property<*>) : OptionRow
    data class Single(val prop: Property<*>, val wide: Boolean) : OptionRow
}

private inline fun buildOptionRows(body: List<Property<*>>, isWide: (Property<*>) -> Boolean): List<OptionRow> {
    val rows = ArrayList<OptionRow>(body.size)
    var pending: Property<*>? = null
    for (prop in body) {
        if (isWide(prop)) {
            pending?.let { rows += OptionRow.Single(it, wide = false); pending = null }
            rows += OptionRow.Single(prop, wide = true)
        } else if (pending == null) {
            pending = prop
        } else {
            rows += OptionRow.Pair(pending, prop)
            pending = null
        }
    }
    pending?.let { rows += OptionRow.Single(it, wide = false) }
    return rows
}

@Composable
private fun AccordionOptionsGrid(body: List<Property<*>>, compact: Boolean) {
    val visible = rememberVisibleOptions(body)
    val displays = remember(visible) { visible.toMap() }
    val visibleProps = remember(visible) { visible.map { it.first } }

    if (compact) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(OPTION_ROW_GAP)) {
            visible.forEach { (prop, display) ->
                key(prop) { AccordionSettingRow(prop, display, Modifier.fillMaxWidth(), compact = true) }
            }
        }
        return
    }

    val theme = LocalTheme.current
    val optionWidth = LocalOptionWidth.current
    var maxWidth by remember { mutableFloatStateOf(0f) }
    val columnWidth = (maxWidth.dp - OPTION_ROW_GAP) / 2
    val titleFont = PolyFont(theme.typography.family.resolve(FontWeight.Medium), 15f)
    val rows = remember(visibleProps, columnWidth, optionWidth, titleFont) {
        buildOptionRows(visibleProps) { prop ->
            titleWidth(titleFont, prop) + optionReservedWidth(prop, optionWidth) > columnWidth
        }
    }
    Column(Modifier.fillMaxWidth().onPlaced { maxWidth = it.width }, verticalArrangement = Arrangement.spacedBy(OPTION_ROW_GAP)) {
        if (maxWidth <= 0f) return@Column
        rows.forEach { row ->
            when (row) {
                is OptionRow.Single -> key(row.prop) {
                    if (row.wide) {
                        AccordionSettingRow(row.prop, displays.getValue(row.prop), Modifier.fillMaxWidth())
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OPTION_ROW_GAP)) {
                            AccordionSettingRow(row.prop, displays.getValue(row.prop), Modifier.weight(1f))
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                is OptionRow.Pair -> key(row.first, row.second) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OPTION_ROW_GAP)) {
                        AccordionSettingRow(row.first, displays.getValue(row.first), Modifier.weight(1f))
                        AccordionSettingRow(row.second, displays.getValue(row.second), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
internal fun rememberDisplay(prop: Property<*>): Property.Display {
    return key(System.identityHashCode(prop)) {
        var display by remember { mutableStateOf(prop.display) }

        DisposableEffect(Unit) {
            val listener = Consumer<Property.Display> { display = it }
            prop.addDisplayListener(listener)
            onDispose { prop.removeDisplayListener(listener) }
        }

        display
    }
}

internal fun displayAlpha(display: Property.Display): Float {
    return if (display == Property.Display.DISABLED) 0.65f else 1f
}

@Composable
private fun SettingContent(prop: Property<*>, nested: Boolean = false, compact: Boolean = false, enabled: Boolean = true) {
    val theme = LocalTheme.current
    val verticalPadding = if (nested) 0.dp else 12.dp

    val visualizer = prop.getMetadata<Any?>("visualizer")
    if (visualizer == Visualizer.InfoVisualizer::class.java || visualizer is Visualizer.InfoVisualizer) {
        Row(modifier = Modifier.fillMaxWidth().blockInteraction(!enabled).padding(horizontal = 16.dp, vertical = verticalPadding)) {
            Option(prop)
        }
        return
    }

    var menuOpen by remember(System.identityHashCode(prop)) { mutableStateOf(false) }
    var menuOffset by remember(System.identityHashCode(prop)) { mutableStateOf(IntOffset.Zero) }
    val rowOrigin = remember(System.identityHashCode(prop)) { arrayOf(Offset.Zero) }
    val actionMenuOffset = remember(System.identityHashCode(prop)) { arrayOf(IntOffset.Zero) }
    val rowInteraction = rememberInteractionSource()
    val isRowHovered by rowInteraction.collectIsHoveredAsState()
    val showActionButton = OneConfigConfig.showOptionActionButtons && enabled
    LaunchedEffect(enabled) { if (!enabled) menuOpen = false }
    fun openMenuFromActionButton() {
        menuOffset = actionMenuOffset[0]
        menuOpen = true
    }

    val density = LocalDensity.current
    val optionWidth = LocalOptionWidth.current
    val titleFont = PolyFont(theme.typography.family.resolve(FontWeight.Medium), 15f)
    val propTitleWidth = titleWidth(titleFont, prop)
    var maxWidth by remember(System.identityHashCode(prop)) { mutableFloatStateOf(0f) }
    var squeezed by remember(System.identityHashCode(prop), maxWidth) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(rowInteraction)
            .onGloballyPositioned { rowOrigin[0] = it.positionInRoot() }
            .onPlaced { maxWidth = it.width }
            .onPointerEvent { event ->
                if (!enabled || event.type != PointerEventType.Press || event.button != 1 || KeybindRecordingBus.isRecording) {
                    return@onPointerEvent false
                }
                menuOffset = with(density) {
                    IntOffset((event.x - event.node.x).dp.roundToPx(), (event.y - event.node.y).dp.roundToPx())
                }
                menuOpen = true
                true
            }
    ) {
        val actionButton = @Composable {
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
        }
        val fullWidth = isFullWidth(prop)
        val stacked = compact || fullWidth || squeezed ||
            (maxWidth > 0f && propTitleWidth + optionReservedWidth(prop, optionWidth) > maxWidth.dp)

        if (stacked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .blockInteraction(!enabled)
                    .padding(start = 16.dp, end = 16.dp, top = verticalPadding, bottom = if (nested) 8.dp else verticalPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (fullWidth) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingLabel(prop, nested = nested, modifier = Modifier.weight(1f))
                        actionButton()
                    }
                    var fullOptionWidth by remember { mutableFloatStateOf(0f) }
                    Box(Modifier.fillMaxWidth().onPlaced { fullOptionWidth = it.width }) {
                        CompositionLocalProvider(LocalOptionWidth provides if (fullOptionWidth > 0f) fullOptionWidth.dp else optionWidth) { Option(prop) }
                    }
                } else {
                    SettingLabel(prop, nested = nested)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    ) {
                        actionButton()
                        Option(prop)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .blockInteraction(!enabled)
                    .padding(horizontal = 16.dp, vertical = verticalPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingLabel(
                    prop = prop,
                    nested = nested,
                    modifier = Modifier.weight(1f).onPlaced { if (it.width < min(propTitleWidth.value, 120f)) squeezed = true },
                )

                actionButton()
                Option(prop)
            }
        }

        OptionContextMenu(prop, menuOpen, menuOffset, onDismiss = { menuOpen = false })
    }
}

@Composable
private fun SettingLabel(
    prop: Property<*>,
    nested: Boolean,
    modifier: Modifier = Modifier,
) {
    val theme = LocalTheme.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        prop.getMetadata<String>("icon")?.let {
            Icon(it, color = theme.textColor, modifier = Modifier.size(32.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val title = @Composable {
                Text(
                    prop.localizedTitle(),
                    color = theme.textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    softWrap = true,
                )
            }
            val description = prop.localizedDescription()?.takeUnless { it.isEmptyText() }

            if (nested && description != null) {
                TooltipArea(
                    tooltip = {
                        Box(
                            Modifier.widthIn(max = 260.dp)
                                .background(theme.popupBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, theme.borderColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(description, color = theme.textColor, fontSize = 12.sp, softWrap = true)
                        }
                    },
                ) {
                    title()
                }
            } else {
                title()
            }

            if (!nested) {
                description?.let { Text(it, color = theme.textColorSecondary, fontSize = 13.sp, softWrap = true) }
            }
        }
    }
}

@Composable
fun HudConfigScreen(tree: Tree, searchQuery: String) {
    key(System.identityHashCode(tree)) { HudConfigScreenContent(tree, searchQuery) }
}

@Composable
private fun HudConfigScreenContent(tree: Tree, searchQuery: String) {
    val categories = remember(tree) { buildCategories(tree) { !isHudInternal(it) } }
    var selectedCategory by remember(tree) { mutableStateOf(categories.firstOrNull()) }

    val revision = rememberDisplayRevision(categories)
    val entries = remember(categories, selectedCategory, searchQuery, revision) {
        if (searchQuery.isEmpty()) {
            selectedCategory?.let(::filterHiddenNodes)?.let(::flattenEntries).orEmpty()
        } else {
            flattenSearchEntries(filterCategories(categories, searchQuery).mapNotNull(::filterHiddenNodes))
        }
    }
    // the panel's built-in sections may still match so an empty search result here stays silent
    if (entries.isEmpty() && searchQuery.isNotEmpty()) return

    CompositionLocalProvider(LocalOptionWidth provides 220.dp) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(19.dp)) {
        if (searchQuery.isEmpty() && categories.size > 1) {
            CategoryChips(categories, gutter = 0.dp) { category ->
                Chip(
                    label = category.name,
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category }
                )
            }
        }

        if (entries.isEmpty()) {
            Text(
                "No settings available.",
                color = LocalTheme.current.textColorSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            return@Column
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { entry -> key(entryKey(entry)) { ConfigListRow(entry, ::hasWideControl) } }
        }
    }
    }
}

private fun isHudInternal(node: Node): Boolean {
    return node.getMetadata<Any?>("hudInternal") != null
}

/** The HUD editor's column is narrow so any row holding a wide control stacks its label above it */
private fun hasWideControl(node: SettingNode): Boolean = when (node) {
    is SettingNode.Leaf -> isWideControl(node.prop)
    is SettingNode.Accordion -> node.body.any(::isWideControl)
}
