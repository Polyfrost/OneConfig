package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.function.Consumer
import kotlin.math.roundToInt
import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.Tooltip
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.blockInteraction
import org.polyfrost.oneconfig.internal.ui.components.isEmptyText
import org.polyfrost.oneconfig.internal.ui.components.localizedDescription
import org.polyfrost.oneconfig.internal.ui.components.localizedGroup
import org.polyfrost.oneconfig.internal.ui.components.localizedTitle
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.searchMatches
import org.polyfrost.oneconfig.internal.ui.components.settings.LocalOptionWidth
import org.polyfrost.oneconfig.internal.ui.components.settings.Option
import org.polyfrost.oneconfig.internal.ui.components.settings.OptionActionButton
import org.polyfrost.oneconfig.internal.ui.components.settings.OptionContextMenu
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private sealed interface SettingNode {
    data class Leaf(val prop: Property<*>) : SettingNode
    data class Accordion(val tree: Tree, val head: Property<Boolean>?, val body: List<Property<*>>) : SettingNode
}

private data class CategoryGroup(
    val name: String,
    val subcategories: List<SubcategoryGroup>
)

private data class SubcategoryGroup(
    val name: String,
    val nodes: List<SettingNode>
)

private sealed interface ConfigListEntry {
    data class CategoryHeader(val title: String) : ConfigListEntry
    data class SubcategoryHeader(val title: String) : ConfigListEntry
    data class Item(val node: SettingNode) : ConfigListEntry
}

@Composable
fun ConfigScreen(tree: Tree, initialCategory: String? = null, pageKey: String) {
    val categories = remember(tree) { buildCategories(tree) }
    val localSearchQuery = if (ShellState.globalSearchActive) "" else ShellState.searchQuery.trim()

    val savedCategory = ShellState.selectedCategories[pageKey] ?: initialCategory
    val selectedCategory = categories.firstOrNull { it.name.equals(savedCategory, ignoreCase = true) }
        ?: categories.firstOrNull()

    fun selectCategory(category: CategoryGroup) {
        LocalNavController.wrapper.selectCategory(pageKey, category.name)
    }

    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        if (localSearchQuery.isBlank() && categories.size > 1) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
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
        val entries = remember(categories, selectedCategory, localSearchQuery, revision) {
            if (localSearchQuery.isBlank()) {
                selectedCategory?.let(::filterHiddenNodes)?.let(::flattenEntries).orEmpty()
            } else {
                flattenSearchEntries(filterCategories(categories, localSearchQuery).mapNotNull(::filterHiddenNodes))
            }
        }

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val message = if (localSearchQuery.isBlank()) "No settings available."
                else "No settings match \"$localSearchQuery\""
                Text(message, color = LocalTheme.current.textColorSecondary)
            }
            return@Column
        }

        val lazyListState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(end = 16.dp)
            ) {
                items(entries) { entry ->
                    when (entry) {
                        is ConfigListEntry.CategoryHeader -> CategoryHeader(entry.title)
                        is ConfigListEntry.SubcategoryHeader -> SubcategoryHeader(entry.title)
                        is ConfigListEntry.Item -> when (val node = entry.node) {
                            is SettingNode.Leaf -> SettingRow(node.prop)
                            is SettingNode.Accordion -> AccordionRow(node)
                        }
                    }
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(lazyListState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}

private fun flattenEntries(category: CategoryGroup): List<ConfigListEntry> {
    val showHeaders = category.subcategories.size > 1 || category.subcategories.any {
        it.name != ConfigVisualizer.DEFAULT_SUBCATEGORY
    }
    return buildList {
        category.subcategories.forEach { subcategory ->
            if (showHeaders) {
                add(ConfigListEntry.SubcategoryHeader(subcategory.name))
            }
            subcategory.nodes.forEach { add(ConfigListEntry.Item(it)) }
        }
    }
}

private fun flattenSearchEntries(categories: List<CategoryGroup>): List<ConfigListEntry> {
    return buildList {
        val showCategoryHeaders = categories.size > 1
        categories.forEach { category ->
            if (showCategoryHeaders) {
                add(ConfigListEntry.CategoryHeader(category.name))
            }
            addAll(flattenEntries(category))
        }
    }
}

/**
 * Drop nodes which are currently hidden by an unmet dependency, so that they never occupy an entry in the
 * (lazy) setting list. Emitting a zero-height row for them instead leaves stray gaps and empty headers, and the
 * row cannot observe its own display state while it is scrolled out of composition.
 */
private fun filterHiddenNodes(category: CategoryGroup): CategoryGroup? {
    val subcategories = category.subcategories.mapNotNull { subcategory ->
        val nodes = subcategory.nodes.filter { node ->
            when (node) {
                is SettingNode.Leaf -> !node.prop.isHidden()
                is SettingNode.Accordion -> node.head?.isHidden() == false || node.body.any { !it.isHidden() }
            }
        }
        if (nodes.isEmpty()) null else subcategory.copy(nodes = nodes)
    }
    return if (subcategories.isEmpty()) null else category.copy(subcategories = subcategories)
}

private fun Property<*>.isHidden(): Boolean = display == Property.Display.HIDDEN

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
 * Returns a counter which increments whenever any property in [categories] changes its display state, so that the
 * entry list can be rebuilt.
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

private fun buildCategories(tree: Tree): List<CategoryGroup> {
    val grouped = LinkedHashMap<String, LinkedHashMap<String, MutableList<SettingNode>>>()
    tree.map.values.forEach { node ->
        val category = nodeGroup(node, "category", ConfigVisualizer.DEFAULT_CATEGORY)
        val subcategory = nodeGroup(node, "subcategory", ConfigVisualizer.DEFAULT_SUBCATEGORY)
        val bucket = grouped.getOrPut(category) { LinkedHashMap() }.getOrPut(subcategory) { ArrayList() }

        when (node) {
            is Property<*> -> {
                if (isRenderableProperty(node)) {
                    bucket += SettingNode.Leaf(node)
                }
            }
            is Tree -> buildAccordionNode(node)?.let(bucket::add)
        }
    }

    return grouped.mapNotNull { (category, subcategories) ->
        val groups = subcategories.mapNotNull { (subcategory, nodes) ->
            if (nodes.isEmpty()) null else SubcategoryGroup(subcategory, nodes.toList())
        }
        if (groups.isEmpty()) null else CategoryGroup(category, groups)
    }
}

private fun buildAccordionNode(tree: Tree): SettingNode.Accordion? {
    val properties = tree.map.values.filterIsInstance<Property<*>>()
    if (properties.isEmpty()) {
        return null
    }

    @Suppress("UNCHECKED_CAST")
    val head = properties.firstOrNull(::isAccordionToggle) as? Property<Boolean>
    val body = properties
        .filter { it !== head }
        .filter(::isRenderableProperty)

    if (body.isEmpty()) {
        return null
    }

    return SettingNode.Accordion(tree, head, body)
}

private fun isAccordionToggle(prop: Property<*>): Boolean {
    val isBoolean = prop.type == Boolean::class.java || prop.type == Boolean::class.javaPrimitiveType
    return isBoolean && prop.getMetadata<Any?>("visualizer") == null
}

private fun isRenderableProperty(prop: Property<*>): Boolean {
    if (prop.getMetadata<Any?>("hidden") != null) return false
    return (prop.getMetadata<Any?>("visualizer") != null) || prop.canDisplay()
}

private fun isWideControl(prop: Property<*>): Boolean {
    return when (prop.getMetadata<Any?>("visualizer")) {
        Visualizer.SliderVisualizer::class.java -> true
        is Visualizer.SliderVisualizer -> true
        else -> false
    }
}

private fun nodeGroup(node: Node, key: String, default: String): String {
    return node.localizedGroup(key, "${key}Key", default)
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
            else node.head?.get() != false
        )
    }
    val headerInteraction = rememberInteractionSource()
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 90f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.modCardBackground, shape)
            .border(
                1.dp,
                Brush.verticalGradient(listOf(theme.borderColor, theme.borderColor.copy(0f))),
                shape
            )
            .drawWithCache {
                val color = theme.textColor
                val gradient = Brush.radialGradient(
                    colors = listOf(color.copy(0f), color.copy(0.02f), color.copy(0.05f)),
                    center = Offset(size.width / 2f, 0f),
                    radius = size.width * 0.8f,
                )
                onDrawBehind { drawRect(gradient) }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onClick(headerInteraction) { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                node.tree.getMetadata<String>("icon")?.let { Icon(it, color = theme.textColor, modifier = Modifier.size(32.dp)) }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        node.tree.localizedTitle(),
                        color = theme.textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    node.tree.localizedDescription()?.let {
                        Text(it, color = theme.textColorSecondary, fontSize = 13.sp)
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (node.head != null) {
                    var headChecked by remember(node) { mutableStateOf(node.head.get() == true) }
                    SwitchControl(headChecked) {
                        headChecked = it
                        node.head.set(it)
                        expanded = it
                    }
                }
                Icon(
                    "up",
                    color = theme.textColorSecondary,
                    modifier = Modifier
                        .size(12.dp)
                        .rotate(chevronRotation),
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(listOf(theme.borderColor.copy(0f), theme.borderColor)),
                        shape = shape
                    )
                    .padding(vertical = 12.dp)
            ) {
                AccordionOptionsGrid(node.body, compact = compact)
            }
        }
    }
}

@Composable
fun SettingRow(prop: Property<*>, compact: Boolean = false) {
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
            .border(
                1.dp,
                Brush.verticalGradient(listOf(theme.borderColor, theme.borderColor.copy(0f))),
                theme.modCardShape
            )
    ) {
        val vignetteColor = theme.textColor
        Box(
            Modifier.fillMaxSize().drawWithCache {
                val gradient = Brush.radialGradient(
                    colors = listOf(
                        vignetteColor.copy(alpha = 0f),
                        vignetteColor.copy(alpha = 0.02f),
                        vignetteColor.copy(alpha = 0.05f)
                    ),
                    center = size.center,
                    radius = size.minDimension * 0.9f
                )
                onDrawBehind { drawRect(gradient) }
            }
        )
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
    val control = if (isWideControl(prop)) optionWidth + SLIDER_SPINNER_WIDTH + OPTION_ROW_CHROME else OPTION_CHROME_WIDTH
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
private fun rememberDisplay(prop: Property<*>): Property.Display {
    var display by remember(prop) { mutableStateOf(prop.display) }

    DisposableEffect(prop) {
        val listener = Consumer<Property.Display> { display = it }
        prop.addDisplayListener(listener)
        onDispose { prop.removeDisplayListener(listener) }
    }

    return display
}

private fun displayAlpha(display: Property.Display): Float {
    return if (display == Property.Display.DISABLED) 0.65f else 1f
}

@Composable
private fun SettingContent(prop: Property<*>, nested: Boolean = false, compact: Boolean = false, enabled: Boolean = true) {
    val theme = LocalTheme.current

    if (prop.getMetadata<Any?>("visualizer") == Visualizer.InfoVisualizer::class.java) {
        Row(modifier = Modifier.fillMaxWidth().blockInteraction(!enabled).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Option(prop)
        }
        return
    }

    var menuOpen by remember(prop) { mutableStateOf(false) }
    var menuOffset by remember(prop) { mutableStateOf(IntOffset.Zero) }
    var rowOrigin by remember(prop) { mutableStateOf(Offset.Zero) }
    var actionMenuOffset by remember(prop) { mutableStateOf(IntOffset.Zero) }
    val rowInteraction = rememberInteractionSource()
    val isRowHovered by rowInteraction.collectIsHoveredAsState()
    val showActionButton = OneConfigConfig.showOptionActionButtons && enabled
    LaunchedEffect(enabled) { if (!enabled) menuOpen = false }
    fun openMenuFromActionButton() {
        menuOffset = actionMenuOffset
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
            .onGloballyPositioned { rowOrigin = it.positionInRoot() }
            .pointerInput(prop, enabled) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            val pos = event.changes.first().position
                            menuOffset = IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
                            menuOpen = true
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
    ) {
        val stacked = compact || run {
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingLabel(prop, nested = nested)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    if (showActionButton) {
                        OptionActionButton(
                            visible = isRowHovered || menuOpen,
                            modifier = Modifier.onGloballyPositioned {
                                val pos = it.positionInRoot() - rowOrigin
                                actionMenuOffset = IntOffset(pos.x.roundToInt(), (pos.y + it.size.height).roundToInt())
                            },
                            onClick = ::openMenuFromActionButton,
                        )
                    }
                    Option(prop)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .blockInteraction(!enabled)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingLabel(
                    prop = prop,
                    nested = nested,
                    modifier = Modifier.weight(1f),
                )

                if (showActionButton) {
                    OptionActionButton(
                        visible = isRowHovered || menuOpen,
                        modifier = Modifier.onGloballyPositioned {
                            val pos = it.positionInRoot() - rowOrigin
                            actionMenuOffset = IntOffset(pos.x.roundToInt(), (pos.y + it.size.height).roundToInt())
                        },
                        onClick = ::openMenuFromActionButton,
                    )
                }
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
fun HudConfigScreen(tree: Tree, initialCategory: String? = null) {
    val filteredTree = remember(tree) {
        tree
    }
    val categories = remember(filteredTree) { buildHudCategories(filteredTree) }
    val localSearchQuery = if (ShellState.globalSearchActive) "" else ShellState.searchQuery.trim()
    var selectedCategory by remember(filteredTree, initialCategory) {
        mutableStateOf(
            categories.firstOrNull { it.name.equals(initialCategory, ignoreCase = true) }
                ?: categories.firstOrNull()
        )
    }

    CompositionLocalProvider(LocalOptionWidth provides 220.dp) {
    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        if (localSearchQuery.isBlank() && categories.size > 1) {
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

        val revision = rememberDisplayRevision(categories)
        val entries = remember(categories, selectedCategory, localSearchQuery, revision) {
            if (localSearchQuery.isBlank()) {
                selectedCategory?.let(::filterHiddenNodes)?.let(::flattenEntries).orEmpty()
            } else {
                flattenSearchEntries(filterCategories(categories, localSearchQuery).mapNotNull(::filterHiddenNodes))
            }
        }

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val message = if (localSearchQuery.isBlank()) "No settings available."
                else "No settings match \"$localSearchQuery\""
                Text(message, color = LocalTheme.current.textColorSecondary)
            }
            return@Column
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { entry ->
                when (entry) {
                    is ConfigListEntry.CategoryHeader -> CategoryHeader(entry.title)
                    is ConfigListEntry.SubcategoryHeader -> SubcategoryHeader(entry.title)
                    is ConfigListEntry.Item -> when (val node = entry.node) {
                        is SettingNode.Leaf -> SettingRow(node.prop, compact = isWideControl(node.prop))
                        is SettingNode.Accordion -> AccordionRow(node, compact = node.body.any(::isWideControl))
                    }
                }
            }
        }
    }
    }
}

private fun isHudInternal(node: Node): Boolean {
    return node.getMetadata<Any?>("hudInternal") != null
}

private fun buildHudCategories(tree: Tree): List<CategoryGroup> {
    val grouped = LinkedHashMap<String, LinkedHashMap<String, MutableList<SettingNode>>>()
    tree.map.values.forEach { node ->
        // skip hudInternal nodes
        if (isHudInternal(node)) return@forEach

        val category = nodeGroup(node, "category", ConfigVisualizer.DEFAULT_CATEGORY)
        val subcategory = nodeGroup(node, "subcategory", ConfigVisualizer.DEFAULT_SUBCATEGORY)
        val bucket = grouped.getOrPut(category) { LinkedHashMap() }.getOrPut(subcategory) { ArrayList() }

        when (node) {
            is Property<*> -> {
                if (isRenderableProperty(node)) {
                    bucket += SettingNode.Leaf(node)
                }
            }
            is Tree -> buildAccordionNode(node)?.let(bucket::add)
        }
    }

    return grouped.mapNotNull { (category, subcategories) ->
        val groups = subcategories.mapNotNull { (subcategory, nodes) ->
            if (nodes.isEmpty()) null else SubcategoryGroup(subcategory, nodes.toList())
        }
        if (groups.isEmpty()) null else CategoryGroup(category, groups)
    }
}
