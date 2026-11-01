package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.ui.v1.SettingRowDecorator
import org.polyfrost.oneconfig.api.ui.v1.SettingRowDecorators
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.Tooltip
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
import org.polyfrost.oneconfig.internal.ui.shell.LocalShell
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.shell.pageSearchQuery
import org.polyfrost.oneconfig.internal.ui.shell.rememberRestorableLazyListState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.util.LayoutRef

/**
 * @param locked shows every option but [lockExempt] dimmed and out of reach, for a mod that is switched off
 */
@Composable
fun ConfigScreen(
    tree: Tree,
    initialCategory: String? = null,
    pageKey: String,
    locked: Boolean = false,
    lockExempt: Property<*>? = null,
) {
    key(System.identityHashCode(tree)) {
        ConfigScreenContent(tree, initialCategory, pageKey, locked, lockExempt)
    }
}

@Composable
private fun ConfigScreenContent(
    tree: Tree,
    initialCategory: String?,
    pageKey: String,
    locked: Boolean,
    lockExempt: Property<*>?,
) {
    val categories = remember(tree) { buildCategories(tree) }
    val localSearchQuery = pageSearchQuery().trim()
    val navigation = LocalShell.current.navigation

    val savedCategory = navigation.selectedCategory(pageKey) ?: initialCategory
    val selectedCategory = categories.firstOrNull { it.name.equals(savedCategory, ignoreCase = true) }
        ?: categories.firstOrNull()

    fun selectCategory(category: CategoryGroup) {
        navigation.selectCategory(pageKey, category.name)
    }

    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        if (localSearchQuery.isBlank() && categories.size > 1) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = PageGutter),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    Chip(
                        label = category.name,
                        selected = selectedCategory == category,
                        onClick = { selectCategory(category) }
                    )
                }
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

        val lazyListState = rememberRestorableLazyListState(pageKey, localSearchQuery, search.query)

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

        ScrollbarBox(lazyListState, PageGutter, Modifier.fillMaxSize()) {
            LazyColumn(
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = PageGutter),
            ) {
                items(entries) { entry ->
                    val option = ((entry as? ConfigListEntry.Item)?.node as? SettingNode.Leaf)?.prop
                    val lock = locked && (option == null || option !== lockExempt)
                    Box(Modifier.alpha(if (lock) 0.45f else 1f).blockInteraction(lock, keyboard = true)) { ConfigListRow(entry) }
                }
            }
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
    var expanded by remember(node) {
        mutableStateOf(
            if (node.tree.getMetadata<Boolean>("collapsed") == true) false
            else node.head?.shown() != false
        )
    }
    val headDisplay = node.head?.let { rememberDisplay(it) } ?: Property.Display.SHOWN
    var headChecked by remember(node) { mutableStateOf(node.head?.shown() == true) }
    if (node.head != null) {
        DisposableEffect(node.head) {
            val listener = Predicate<Boolean> { value -> headChecked = value == true; false }
            node.head.addCallback(listener)
            onDispose { node.head.removeCallback(listener) }
        }
    }

    Accordion(
        title = node.tree.localizedTitle(),
        expanded = expanded,
        onExpandedChange = { expanded = it },
        checked = if (node.head != null) headChecked else null,
        onCheckedChange = {
            headChecked = it
            node.head?.set(it)
            expanded = it
        },
        headEnabled = headDisplay != Property.Display.DISABLED,
        description = node.tree.localizedDescription(),
        icon = node.tree.getMetadata<String>("icon"),
    ) {
        AccordionOptionsGrid(node.body, compact = compact)
    }
}

private const val FOLD_STIFFNESS = Spring.StiffnessMediumLow

@Composable
private fun Accordion(
    title: Any,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    checked: Boolean? = null,
    onCheckedChange: (Boolean) -> Unit = {},
    headEnabled: Boolean = true,
    description: Any? = null,
    icon: String? = null,
    body: @Composable () -> Unit,
) {
    val theme = LocalTheme.current
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 90f)
    var headerHeight by remember { mutableIntStateOf(0) }
    val openness by animateFloatAsState(if (expanded) 1f else 0f, spring(stiffness = FOLD_STIFFNESS))
    Column(Modifier.optionCard(shineHeight = headerHeight).accordionBorder(openness)) {
        CardHeader(
            title,
            description,
            icon,
            Modifier
                .onSizeChanged { headerHeight = it.height }
                .onClick(rememberInteractionSource()) { onExpandedChange(!expanded) }
                .alpha(if (headEnabled) 1f else displayAlpha(Property.Display.DISABLED)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (checked != null) {
                    Box(Modifier.blockInteraction(!headEnabled)) { SwitchControl(checked, onCheckedChange) }
                }
                Icon("up", color = theme.textColorSecondary, modifier = Modifier.size(12.dp).rotate(chevronRotation))
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(stiffness = FOLD_STIFFNESS, visibilityThreshold = IntSize.VisibilityThreshold)),
            exit = shrinkVertically(spring(stiffness = FOLD_STIFFNESS, visibilityThreshold = IntSize.VisibilityThreshold)),
        ) {
            Box(Modifier.fillMaxWidth().padding(bottom = 12.dp)) { body() }
        }
    }
}

@Composable
private fun CardHeader(
    title: Any,
    description: Any? = null,
    icon: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val theme = LocalTheme.current
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            icon?.let { Icon(it, color = theme.textColor, modifier = Modifier.size(32.dp)) }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = theme.textColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                description?.let { Text(it, color = theme.textColorSecondary, fontSize = 13.sp) }
            }
        }
        trailing()
    }
}

@Composable
private fun Modifier.accordionBorder(alpha: Float): Modifier {
    val theme = LocalTheme.current
    val color = theme.borderColor
    val border = remember(color, alpha) { Brush.verticalGradient(listOf(color.copy(0f), color.copy(color.alpha * alpha))) }
    return border(1.dp, border, theme.modCardShape)
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

    Box(Modifier.optionCard()) {
        Box(Modifier.alpha(displayAlpha(display))) {
            SettingContent(prop, compact = compact, enabled = display != Property.Display.DISABLED)
        }
    }
}

@Composable
private fun Modifier.optionCard(shineHeight: Int = 0): Modifier {
    val theme = LocalTheme.current
    val shape = theme.modCardShape
    val border = remember(theme.borderColor) { Brush.verticalGradient(listOf(theme.borderColor, theme.borderColor.copy(0f))) }
    return fillMaxWidth()
        .clip(shape)
        .background(theme.modCardBackground, shape)
        .border(1.dp, border, shape)
        .drawWithCache {
            val color = theme.textColor
            val height = if (shineHeight > 0) shineHeight.toFloat() else size.height
            val gradient = Brush.radialGradient(
                colors = listOf(color.copy(0f), color.copy(0.02f), color.copy(0.05f)),
                center = Offset(size.width / 2f, height / 2f),
                radius = minOf(size.width, height) * 0.9f,
            )
            onDrawBehind { drawRect(gradient) }
        }
}

@Composable
private fun AccordionSettingRow(prop: Property<*>, display: Property.Display, modifier: Modifier, compact: Boolean = false) {
    Box(modifier.alpha(displayAlpha(display))) {
        SettingContent(prop, nested = true, compact = compact, enabled = display != Property.Display.DISABLED)
    }
}

@Composable
private fun rememberVisibleOptions(body: List<Property<*>>): List<Pair<Property<*>, Property.Display>> {
    val visible = ArrayList<Pair<Property<*>, Property.Display>>(body.size)
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
        Column(verticalArrangement = Arrangement.spacedBy(OPTION_ROW_GAP)) {
            visible.forEach { (prop, display) ->
                AccordionSettingRow(prop, display, Modifier.fillMaxWidth(), compact = true)
            }
        }
        return
    }

    val theme = LocalTheme.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val optionWidth = LocalOptionWidth.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columnWidth = (maxWidth - OPTION_ROW_GAP) / 2
        val titleStyle = remember(theme) {
            TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = theme.typography.family)
        }
        val rows = remember(visibleProps, columnWidth, optionWidth, titleStyle) {
            buildOptionRows(visibleProps) { prop ->
                val titleWidth = with(density) {
                    measurer.measure(
                        prop.localizedTitle().asRenderText(),
                        style = titleStyle,
                        softWrap = false,
                        maxLines = 1,
                    ).size.width.toDp()
                }
                titleWidth + optionReservedWidth(prop, optionWidth) > columnWidth
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(OPTION_ROW_GAP)) {
            rows.forEach { row ->
                when (row) {
                    is OptionRow.Single -> {
                        if (row.wide) {
                            AccordionSettingRow(row.prop, displays.getValue(row.prop), Modifier.fillMaxWidth())
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(OPTION_ROW_GAP)) {
                                AccordionSettingRow(row.prop, displays.getValue(row.prop), Modifier.weight(1f))
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    is OptionRow.Pair -> Row(horizontalArrangement = Arrangement.spacedBy(OPTION_ROW_GAP)) {
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
    val rowOrigin = remember(System.identityHashCode(prop)) { LayoutRef(Offset.Zero) }
    val actionMenuOffset = remember(System.identityHashCode(prop)) { LayoutRef(IntOffset.Zero) }
    val rowInteraction = rememberInteractionSource()
    val isRowHovered by rowInteraction.collectIsHoveredAsState()
    val showActionButton = OneConfigConfig.showOptionActionButtons && enabled
    LaunchedEffect(enabled) { if (!enabled) menuOpen = false }
    fun openMenuFromActionButton() {
        menuOffset = actionMenuOffset.value
        menuOpen = true
    }

    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val optionWidth = LocalOptionWidth.current
    val titleStyle = remember(theme) {
        TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = theme.typography.family)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(rowInteraction)
            .onGloballyPositioned { rowOrigin.value = it.positionInRoot() }
            .pointerInput(prop, enabled) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed && !KeybindRecordingBus.isRecording) {
                            val pos = event.changes.first().position
                            menuOffset = IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
                            menuOpen = true
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
    ) {
        val actionButton = @Composable {
            if (showActionButton) {
                OptionActionButton(
                    visible = isRowHovered || menuOpen,
                    modifier = Modifier.onGloballyPositioned {
                        val pos = it.positionInRoot() - rowOrigin.value
                        actionMenuOffset.value = IntOffset(pos.x.roundToInt(), (pos.y + it.size.height).roundToInt())
                    },
                    onClick = ::openMenuFromActionButton,
                )
            }
        }
        val fullWidth = isFullWidth(prop)
        val stacked = compact || fullWidth || run {
            val titleWidth = with(density) {
                measurer.measure(
                    prop.localizedTitle().asRenderText(),
                    style = titleStyle,
                    softWrap = false,
                    maxLines = 1,
                ).size.width.toDp()
            }
            titleWidth + optionReservedWidth(prop, optionWidth) > maxWidth
        }

        if (stacked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .blockInteraction(!enabled)
                    .padding(start = 16.dp, end = 16.dp, top = verticalPadding, bottom = if (nested) 8.dp else verticalPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (fullWidth) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingLabel(prop, nested = nested, modifier = Modifier.weight(1f))
                        actionButton()
                    }
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        CompositionLocalProvider(LocalOptionWidth provides maxWidth) { Option(prop) }
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
                    modifier = Modifier.weight(1f),
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
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val title = @Composable {
                Text(
                    prop.localizedTitle(),
                    color = theme.textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            val description = prop.localizedDescription()?.takeUnless { it.isEmptyText() }

            if (nested && description != null) {
                Tooltip(
                    text = {
                        Text(description, color = theme.textColor, fontSize = 12.sp)
                    },
                    modifier = Modifier.widthIn(max = 260.dp),
                    anchor = Alignment.TopCenter,
                ) {
                    title()
                }
            } else {
                title()
            }

            if (!nested) {
                description?.let { Text(it, color = theme.textColorSecondary, fontSize = 13.sp) }
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
    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        if (searchQuery.isEmpty() && categories.size > 1) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    Chip(
                        label = category.name,
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }
        }

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No settings available.", color = LocalTheme.current.textColorSecondary)
            }
            return@Column
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { entry -> ConfigListRow(entry, ::hasWideControl) }
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
