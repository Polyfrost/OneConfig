package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.function.Predicate
import javax.imageio.ImageIO
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.polyfrost.compose.backend.asComposeImageBitmap
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.api.config.v1.MetaKeys
import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.enableToggle
import org.polyfrost.oneconfig.api.config.v1.get
import org.polyfrost.oneconfig.api.ui.v1.IconSpec
import org.polyfrost.oneconfig.api.ui.v1.SettingRowDecorator
import org.polyfrost.oneconfig.api.ui.v1.SettingRowDecorators
import org.polyfrost.oneconfig.api.ui.v1.TreeHeaderDecorator
import org.polyfrost.oneconfig.api.ui.v1.TreeHeaderDecorators
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.EmptyState
import org.polyfrost.oneconfig.api.ui.v1.components.LoadState
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.Tooltip
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.CollapseChevron
import org.polyfrost.oneconfig.internal.ui.components.CollapsibleHeader
import org.polyfrost.oneconfig.internal.ui.components.ContentSlide
import org.polyfrost.oneconfig.internal.ui.components.ContentSlide.tabSlide
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.ScrollbarBox
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.collapsibleToggle
import org.polyfrost.oneconfig.internal.ui.components.hoverGlow
import org.polyfrost.oneconfig.internal.ui.components.isEmptyText
import org.polyfrost.oneconfig.internal.ui.components.localizedDescription
import org.polyfrost.oneconfig.internal.ui.components.localizedTitle
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.LocalOptionEnabled
import org.polyfrost.oneconfig.internal.ui.components.settings.LocalOptionWidth
import org.polyfrost.oneconfig.internal.ui.components.settings.LocalOptionWidthOrNull
import org.polyfrost.oneconfig.internal.ui.components.settings.Option
import org.polyfrost.oneconfig.internal.ui.components.settings.OptionControlHeight
import org.polyfrost.oneconfig.internal.ui.components.settings.OptionMoreButton
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.components.settings.optionContextMenu
import org.polyfrost.oneconfig.internal.ui.components.settings.rememberOptionMenu
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.search.CategoryGroup
import org.polyfrost.oneconfig.internal.ui.search.ConfigListEntry
import org.polyfrost.oneconfig.internal.ui.search.SearchCorpus
import org.polyfrost.oneconfig.internal.ui.search.SearchDocument
import org.polyfrost.oneconfig.internal.ui.search.SearchRow
import org.polyfrost.oneconfig.internal.ui.search.SearchScope
import org.polyfrost.oneconfig.internal.ui.search.SettingNode
import org.polyfrost.oneconfig.internal.ui.search.SubcategoryGroup
import org.polyfrost.oneconfig.internal.ui.search.buildAccordionNode
import org.polyfrost.oneconfig.internal.ui.search.buildCategories
import org.polyfrost.oneconfig.internal.ui.search.buildSearchIndex
import org.polyfrost.oneconfig.internal.ui.search.filterHiddenNodes
import org.polyfrost.oneconfig.internal.ui.search.flattenEntries
import org.polyfrost.oneconfig.internal.ui.search.flattenSearchEntries
import org.polyfrost.oneconfig.internal.ui.search.hasVisualizer
import org.polyfrost.oneconfig.internal.ui.search.isBooleanHead
import org.polyfrost.oneconfig.internal.ui.search.isSubmenu
import org.polyfrost.oneconfig.internal.ui.search.searchMatches
import org.polyfrost.oneconfig.internal.ui.search.searchNode
import org.polyfrost.oneconfig.internal.ui.search.submenuContent
import org.polyfrost.oneconfig.internal.ui.shell.LocalShell
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.shell.pageSearchQuery
import org.polyfrost.oneconfig.internal.ui.shell.rememberRestorableLazyListState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/** The id of the config whose page is showing or null where a submenu row has no page of its config to open */
internal val LocalConfigId = staticCompositionLocalOf<String?> { null }

/**
 * @param locked shows every option but [lockExempt] dimmed and out of reach, for a mod that is switched off
 * @param configId the config [tree] belongs to
 * @param submenu where [tree] sits among the submenus when it is one
 */
@Composable
fun ConfigScreen(
    tree: Tree,
    initialCategory: String? = null,
    pageKey: String,
    locked: Boolean = false,
    lockExempt: Property<*>? = null,
    header: (@Composable () -> Unit)? = null,
    configId: String? = null,
    submenu: String? = null,
) {
    key(System.identityHashCode(tree)) {
        CompositionLocalProvider(LocalConfigId provides configId) {
            ConfigScreenContent(tree, initialCategory, pageKey, locked, lockExempt, header, submenu)
        }
    }
}

@Composable
private fun ConfigScreenContent(
    tree: Tree,
    initialCategory: String?,
    pageKey: String,
    locked: Boolean,
    lockExempt: Property<*>?,
    header: (@Composable () -> Unit)?,
    submenu: String?,
) {
    val categories = remember(tree, submenu) {
        if (tree.isSubmenu()) submenuContent(tree, submenu) else buildCategories(tree)
    }
    val localSearchQuery = pageSearchQuery().trim()
    val navigation = LocalShell.current.navigation
    val groupOff = rememberSwitchValue(tree.takeIf { it.isSubmenu() }?.enableToggle) == false

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
        val search = rememberSearchResults(index, localSearchQuery, LocalConfigId.current ?: pageKey, pageKey)
        val results = search.results

        // a search lists every category at once so only switching categories slides
        val shown = updateTransition(selectedCategory.takeIf { localSearchQuery.isBlank() }, label = "category")
        shown.AnimatedContent(
            // keeps the sliding options inside the page instead of drawing over its edges
            modifier = Modifier.clipToBounds(),
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.KeepUntilTransitionsFinished using null },
            contentKey = { it?.name },
        ) { category ->
            val from = shown.currentState
            val to = shown.targetState
            val direction = when {
                from == null || to == null || from == to -> 0
                categories.indexOf(to) > categories.indexOf(from) -> 1
                else -> -1
            }
            val leaving = transition.targetState != EnterExitState.Visible
            val progress = transition.animateFloat({ ContentSlide.tabProgressSpec(direction != 0) }, label = "slide") {
                if (it == EnterExitState.Visible) 1f else 0f
            }
            // the page header stays put while the options under it slide
            val slide = Modifier.tabSlide({ progress.value }, leaving, direction, fade = !leaving)

            val entries = remember(index, category, localSearchQuery, revision, results) {
                when {
                    localSearchQuery.isBlank() ->
                        category?.let(::filterHiddenNodes)?.let(::flattenEntries).orEmpty()
                    results == null -> emptyList()
                    else -> flattenSearchEntries(searchCategories(results))
                }
            }

            // each category keeps its own place in the list
            val lazyListState = rememberRestorableLazyListState(
                category?.let { "$pageKey/${it.name}" } ?: pageKey,
                localSearchQuery,
                search.query,
            )

            val empty = when {
                entries.isNotEmpty() -> null
                localSearchQuery.isBlank() -> ConfigEmpty(translated("oneconfig.search.no_settings", "No settings available."), "settings")
                results == null -> ConfigEmpty(translated("oneconfig.search.searching", "Searching..."), null, LoadState.Loading)
                else -> ConfigEmpty(translated("oneconfig.search.no_settings_match", "No settings match \"%s\"", localSearchQuery), "search")
            }

            if (empty != null && header == null) {
                Box(Modifier.fillMaxSize().then(slide), contentAlignment = Alignment.Center) {
                    EmptyState(empty.title, icon = empty.icon, state = empty.state)
                }
                return@AnimatedContent
            }

            ScrollbarBox(
                lazyListState,
                PageGutter,
                Modifier.fillMaxSize().then(if (leaving) Modifier.graphicsLayer { alpha = progress.value } else Modifier),
            ) {
                LazyColumn(
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = PageGutter),
                ) {
                    if (header != null) {
                        item(key = "header") {
                            Box(
                                Modifier
                                    .then(if (leaving) Modifier.alpha(0f) else Modifier)
                                    .layout { measurable, constraints ->
                                        val gutter = PageGutter.roundToPx() * 2
                                        val placeable = measurable.measure(
                                            constraints.copy(minWidth = constraints.minWidth + gutter, maxWidth = constraints.maxWidth + gutter)
                                        )
                                        layout(placeable.width - gutter, placeable.height) { placeable.place(-gutter / 2, 0) }
                                    }
                            ) { header() }
                        }
                    }
                    if (empty != null) {
                        item(key = "empty") {
                            Box(Modifier.fillMaxWidth().then(slide), contentAlignment = Alignment.Center) {
                                EmptyState(empty.title, icon = empty.icon, state = empty.state)
                            }
                        }
                    }
                    items(entries) { entry ->
                        val option = ((entry as? ConfigListEntry.Item)?.node as? SettingNode.Leaf)?.prop
                        val lock = locked && (option == null || option !== lockExempt)
                        CompositionLocalProvider(LocalOptionEnabled provides !(lock || groupOff)) {
                            Box(slide) { ConfigListRow(entry) }
                        }
                    }
                }
            }
        }
    }
}

private class ConfigEmpty(val title: String, val icon: String?, val state: LoadState = LoadState.Idle)

private fun categoryProperties(categories: List<CategoryGroup>): List<Property<*>> {
    return categories.flatMap { category ->
        category.subcategories.flatMap { subcategory ->
            subcategory.nodes.flatMap { node ->
                when (node) {
                    is SettingNode.Leaf -> listOf(node.prop)
                    is SettingNode.Accordion -> node.body + listOfNotNull(node.head)
                    is SettingNode.Submenu -> listOfNotNull(node.head) + categoryProperties(node.content)
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
    scopeId: String,
    pageKey: String,
): LocalSearchResults {
    var results by remember(pageKey) { mutableStateOf<Map<SearchRow?, List<SearchDocument<*>>>?>(null) }
    var searchedQuery by remember(pageKey) { mutableStateOf<String?>(null) }
    LaunchedEffect(index, query, pageKey) {
        results = if (query.isBlank()) emptyMap() else withContext(Dispatchers.Default) {
            SearchCorpus.searchGrouped(query, setOf(SearchScope.Config(scopeId))) { document ->
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
        is SettingNode.Submenu -> {
            val submenuMatches = groupMatches || node.tree.matchesLocalSearch(category, subcategory, query) ||
                node.head?.matchesLocalSearch(category, subcategory, query) == true
            if (submenuMatches) node else {
                val content = filterCategories(node.content, query)
                if (content.isEmpty()) null else node.copy(content = content)
            }
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
    return when (prop[MetaKeys.VISUALIZER]) {
        Visualizer.SliderVisualizer::class.java, is Visualizer.SliderVisualizer -> SLIDER_SPINNER_WIDTH
        // two spinners, or a spinner plus the inherit chip
        Visualizer.RangeSliderVisualizer::class.java, is Visualizer.RangeSliderVisualizer,
        Visualizer.InheritableSliderVisualizer::class.java, is Visualizer.InheritableSliderVisualizer -> SLIDER_SPINNER_WIDTH * 2
        else -> null
    }
}

private fun isFullWidth(prop: Property<*>): Boolean = prop[MetaKeys.FULL_WIDTH] == true || hidesLabel(prop)

private fun hidesLabel(prop: Property<*>): Boolean = prop[MetaKeys.HIDE_LABEL] == true

private fun Node.nodeIcon(): Any? = when (val icon = this[MetaKeys.ICON]) {
    is IconSpec -> icon
    is String -> icon.takeIf { it.isNotBlank() }
    else -> null
}

private fun isWideControl(prop: Property<*>): Boolean = sliderTrailingWidth(prop) != null || isFullWidth(prop)

/**
 * Renders one entry of a flattened settings list
 *
 * [compact] decides per node whether its row stacks the label above the control which the HUD editor needs
 */
@Composable
internal fun ConfigListRow(entry: ConfigListEntry, compact: (SettingNode) -> Boolean = { false }) {
    val headerModifier = if (LocalOptionEnabled.current) Modifier else Modifier.alpha(LocalTheme.current.disabledAlpha)
    when (entry) {
        is ConfigListEntry.CategoryHeader -> CategoryHeader(entry.title, headerModifier)
        is ConfigListEntry.SubcategoryHeader -> SubcategoryHeader(entry.title, headerModifier)
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
        is SettingNode.Submenu -> key(System.identityHashCode(node.tree)) {
            SubmenuRow(node, compact = compact)
        }
    }
}

@Composable
private fun CategoryHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        color = LocalTheme.current.textColorSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun SubcategoryHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        color = LocalTheme.current.textColor,
        fontSize = 22.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun AccordionRow(node: SettingNode.Accordion, compact: Boolean = false) {
    val head = node.head
    val toggle = head?.takeIf { it === node.tree.enableToggle }
    var expanded by remember(node) {
        mutableStateOf(
            if (node.tree[MetaKeys.COLLAPSED] == true) false
            else toggle != null || node.head?.shown() != false
        )
    }
    var headOn by remember(node) { mutableStateOf(head?.shown() == true) }
    if (head != null && head.isBooleanHead()) {
        DisposableEffect(head) {
            val boolHead = Property.recast<Boolean>(head)
            val listener = Predicate<Boolean> { value ->
                headOn = value == true
                if (head.hasVisualizer() && toggle == null) expanded = value == true
                false
            }
            boolHead.addCallback(listener)
            onDispose { boolHead.removeCallback(listener) }
        }
    }
    val headDisplay = head?.let { rememberDisplay(it) } ?: Property.Display.SHOWN
    val switchHead = head != null && isSwitchHead(node.tree, head)
    if (switchHead && headDisplay == Property.Display.HIDDEN) return
    val enabled = !switchHead || (headOn && headDisplay != Property.Display.DISABLED)

    GroupCard {
        GroupHeader(
            tree = node.tree,
            head = head,
            expanded = expanded,
            compact = compact,
            onHeadChange = { if (toggle == null) expanded = it; headOn = it },
            onClick = { expanded = !expanded },
        )

        AnimatedVisibility(visible = expanded, enter = foldEnter(), exit = foldExit()) {
            Box(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                AccordionOptionsGrid(node.body, compact = compact, enabled = enabled)
            }
        }
    }
}

private const val FOLD_STIFFNESS = Spring.StiffnessMediumLow

private fun foldEnter() = Motion.enter(
    expandVertically(spring(stiffness = FOLD_STIFFNESS, visibilityThreshold = IntSize.VisibilityThreshold))
)

private fun foldExit() = Motion.exit(
    shrinkVertically(spring(stiffness = FOLD_STIFFNESS, visibilityThreshold = IntSize.VisibilityThreshold))
)

/**
 * A card whose header folds [body] open and closed, or which follows [expanded] alone with no arrow when
 * [onExpandedChange] is null
 *
 * [titleActions] sit just after the title
 */
@Composable
internal fun Accordion(
    title: Any,
    expanded: Boolean,
    onExpandedChange: ((Boolean) -> Unit)?,
    checked: Boolean? = null,
    onCheckedChange: (Boolean) -> Unit = {},
    headEnabled: Boolean = true,
    description: Any? = null,
    icon: String? = null,
    titleActions: @Composable RowScope.() -> Unit = {},
    body: @Composable (enabled: Boolean) -> Unit,
) {
    Column(Modifier.optionCard()) {
        CardHeader(
            title,
            expanded,
            onExpandedChange?.let { change -> { change(!expanded) } },
            description,
            icon,
            titleActions,
            dimmed = !headEnabled,
        ) {
            if (checked != null) SwitchControl(checked, headEnabled, onCheckedChange)
        }
        AnimatedVisibility(visible = expanded, enter = foldEnter(), exit = foldExit()) {
            Box(Modifier.fillMaxWidth().padding(bottom = 12.dp)) { body(checked != false && headEnabled) }
        }
    }
}

@Composable
private fun CardHeader(
    title: Any,
    expanded: Boolean,
    onToggle: (() -> Unit)?,
    description: Any? = null,
    icon: String? = null,
    titleActions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    trailing: @Composable () -> Unit = {},
) {
    val labelAlpha = if (dimmed) LocalTheme.current.disabledAlpha else 1f
    CollapsibleHeader(
        label = title.asRenderText(),
        expanded = expanded,
        onToggle = onToggle,
        modifier = modifier,
        chevron = false,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HeaderLabel(title, description, Modifier.weight(1f).alpha(labelAlpha), titleActions) {
            icon?.let { Icon(it, color = LocalTheme.current.textColor, modifier = Modifier.size(32.dp)) }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            trailing()
            if (onToggle != null) CollapseChevron(expanded)
        }
    }
}

@Composable
private fun HeaderLabel(
    title: Any,
    description: Any?,
    modifier: Modifier,
    titleActions: @Composable RowScope.() -> Unit = {},
    icon: @Composable () -> Unit,
) {
    val theme = LocalTheme.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon()
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = theme.textColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                titleActions()
            }
            description?.let { Text(it, color = theme.textColorSecondary, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun SubmenuRow(node: SettingNode.Submenu, compact: Boolean = false) {
    val configId = LocalConfigId.current
    if (configId == null) {
        SubmenuAccordionRow(node, compact)
        return
    }

    val navigation = LocalShell.current.navigation
    GroupCard {
        GroupHeader(
            tree = node.tree,
            head = node.head,
            expanded = false,
            compact = compact,
            onHeadChange = {},
            onClick = { navigation.navigate(ModConfigRoute(configId, submenu = node.path)) },
        )
    }
}

/** A submenu without a config page to open, which expands in place with everything nested inside it */
@Composable
private fun SubmenuAccordionRow(node: SettingNode.Submenu, compact: Boolean) {
    val toggle = node.head?.takeIf { it === node.tree.enableToggle }
    var expanded by remember(node) { mutableStateOf(toggle != null || node.head?.shown() != false) }
    val groupOff = rememberSwitchValue(toggle) == false
    val revision = rememberDisplayRevision(node.content)
    val entries = remember(node, revision) {
        node.content.mapNotNull(::filterHiddenNodes).flatMap(::flattenEntries)
    }

    GroupCard {
        GroupHeader(
            tree = node.tree,
            head = node.head,
            expanded = expanded,
            compact = compact,
            onHeadChange = {},
            onClick = { expanded = !expanded },
        )

        AnimatedVisibility(visible = expanded, enter = foldEnter(), exit = foldExit()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(OPTION_ROW_GAP),
            ) {
                CompositionLocalProvider(LocalOptionEnabled provides (LocalOptionEnabled.current && !groupOff)) {
                    entries.forEach { ConfigListRow(it) { compact } }
                }
            }
        }
    }
}

@Composable
private fun GroupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.optionCard(), content = content)
}

/**
 * The header row of an accordion or submenu
 *
 * [onHeadChange] is told what a switch of an old style head was flipped to
 */
@Composable
private fun GroupHeader(
    tree: Tree,
    head: Property<*>?,
    expanded: Boolean,
    compact: Boolean,
    onHeadChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    DecoratedGroupHeader(TreeHeaderDecorators.all(), 0, tree) {
        GroupHeaderContent(tree, head, expanded, compact, onHeadChange, onClick)
    }
}

@Composable
private fun DecoratedGroupHeader(decorators: List<TreeHeaderDecorator>, index: Int, tree: Tree, content: @Composable () -> Unit) {
    val decorator = decorators.getOrNull(index) ?: return content()
    decorator.Decorate(tree) { DecoratedGroupHeader(decorators, index + 1, tree, content) }
}

private fun isSwitchHead(tree: Tree, head: Property<*>): Boolean =
    !head.hasVisualizer() || head === tree.enableToggle

@Composable
private fun rememberSwitchValue(prop: Property<*>?): Boolean? {
    if (prop == null || !prop.isBooleanHead()) return null
    return key(System.identityHashCode(prop)) {
        var on by remember { mutableStateOf(prop.shown() == true) }
        DisposableEffect(Unit) {
            val bool = Property.recast<Boolean>(prop)
            val listener = Predicate<Boolean> { value -> on = value == true; false }
            bool.addCallback(listener)
            onDispose { bool.removeCallback(listener) }
        }
        on
    }
}

@Composable
private fun GroupHeaderContent(
    tree: Tree,
    head: Property<*>?,
    expanded: Boolean,
    compact: Boolean,
    onHeadChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val interaction = rememberInteractionSource()
    val headDisplay = head?.let { rememberDisplay(it) } ?: Property.Display.SHOWN
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val optionWidth = LocalOptionWidth.current
    val titleStyle = rememberTitleStyle()

    val isHovered by interaction.collectIsHoveredAsState()
    val groupEnabled = LocalOptionEnabled.current
    val headLocked = headDisplay == Property.Display.DISABLED || !groupEnabled
    val labelModifier = if (groupEnabled) Modifier else Modifier.alpha(LocalTheme.current.disabledAlpha)
    val showActionButton = OneConfigConfig.showOptionActionButtons && !headLocked
    val menu = head?.let { rememberOptionMenu(it) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interaction)
            .then(if (menu != null) Modifier.optionContextMenu(menu, enabled = !headLocked) else Modifier)
            .collapsibleToggle(tree.localizedTitle().asRenderText(), onClick, interaction)
    ) {
        val switch = head != null && isSwitchHead(tree, head)
        val control = head?.takeIf { switch || headDisplay != Property.Display.HIDDEN }
        val stacked = control != null && !switch && (compact || isFullWidth(control) || run {
            val iconWidth = if (tree.nodeIcon() != null) OPTION_ICON_WIDTH else 0.dp
            val reserved = optionReservedWidth(control, optionWidth) + iconWidth + GROUP_CHEVRON_WIDTH
            measurer.titleWidth(tree, titleStyle, density) + reserved > maxWidth
        })
        val headControl = @Composable {
            if (control != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (showActionButton && menu != null) OptionMoreButton(menu, visible = isHovered)
                    HeadControl(control, !headLocked, switch, onHeadChange)
                }
            }
        }

        if (stacked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    GroupLabel(tree, Modifier.weight(1f).then(labelModifier))
                    CollapseChevron(expanded)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { headControl() }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GroupLabel(tree, Modifier.weight(1f).then(labelModifier))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    headControl()
                    CollapseChevron(expanded)
                }
            }
        }
    }
}

@Composable
private fun GroupLabel(tree: Tree, modifier: Modifier) {
    HeaderLabel(tree.localizedTitle(), tree.localizedDescription(), modifier) {
        tree.nodeIcon()?.let { NodeIcon(it, Modifier.size(32.dp)) }
    }
}

@Composable
private fun NodeIcon(icon: Any, modifier: Modifier) {
    if (icon is IconSpec) SpriteIcon(icon, modifier)
    else Icon(icon.toString(), color = LocalTheme.current.textColor, modifier = modifier)
}

private object SpriteSheets {
    private class Entry(val bitmap: ImageBitmap?)

    private val cache = ConcurrentHashMap<String, Entry>()

    fun peek(path: String): ImageBitmap? = cache[path]?.bitmap

    fun load(path: String, loader: ClassLoader?): ImageBitmap? = cache.getOrPut(path) {
        Entry(runCatching { read(path, loader)?.let(::decode) }.getOrNull())
    }.bitmap

    private fun read(path: String, loader: ClassLoader?): ByteArray? {
        File(path).takeIf { it.isAbsolute && it.isFile }?.let { return it.readBytes() }
        val stream = loader?.getResourceAsStream(path.removePrefix("/"))
            ?: SpriteSheets::class.java.getResourceAsStream(if (path.startsWith("/")) path else "/$path")
        return stream?.use { it.readBytes() }
    }

    private fun decode(bytes: ByteArray): ImageBitmap? {
        val buffered = runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull()
            ?: return PolyBitmap.decode(bytes)?.asComposeImageBitmap()
        return PolyBitmap.fromAwt(buffered).asComposeImageBitmap()
    }
}

@Composable
private fun SpriteIcon(spec: IconSpec, modifier: Modifier) {
    val loader = remember { Thread.currentThread().contextClassLoader ?: SpriteSheets::class.java.classLoader }
    val bitmap by produceState(SpriteSheets.peek(spec.path), spec.path) {
        value = SpriteSheets.peek(spec.path)
        if (value == null) value = withContext(Dispatchers.IO) { SpriteSheets.load(spec.path, loader) }
    }
    Canvas(modifier) {
        val image = bitmap ?: return@Canvas
        val scaleX = if (spec.sheetWidth > 0) image.width.toFloat() / spec.sheetWidth else 1f
        val scaleY = if (spec.sheetHeight > 0) image.height.toFloat() / spec.sheetHeight else 1f
        val srcX = (spec.x * scaleX).roundToInt()
        val srcY = (spec.y * scaleY).roundToInt()
        val srcW = min((spec.width * scaleX).roundToInt(), image.width - srcX)
        val srcH = min((spec.height * scaleY).roundToInt(), image.height - srcY)
        if (srcW <= 0 || srcH <= 0) return@Canvas
        val fit = min(size.width / srcW, size.height / srcH)
        val dstW = (srcW * fit).roundToInt()
        val dstH = (srcH * fit).roundToInt()
        drawImage(
            image,
            srcOffset = IntOffset(srcX, srcY),
            srcSize = IntSize(srcW, srcH),
            dstOffset = IntOffset(((size.width - dstW) / 2).roundToInt(), ((size.height - dstH) / 2).roundToInt()),
            dstSize = IntSize(dstW, dstH),
            filterQuality = if (spec.pixelated) FilterQuality.None else FilterQuality.Medium,
        )
    }
}

@Composable
private fun HeadControl(head: Property<*>, enabled: Boolean, switch: Boolean, onHeadChange: (Boolean) -> Unit) {
    CompositionLocalProvider(LocalOptionEnabled provides enabled) {
        when {
            switch -> HeadSwitch(Property.recast(head), enabled, onHeadChange)
            isFullWidth(head) -> BoxWithConstraints(Modifier.fillMaxWidth()) {
                CompositionLocalProvider(LocalOptionWidth provides maxWidth, LocalOptionWidthOrNull provides maxWidth) { Option(head) }
            }
            else -> Option(head)
        }
    }
}

@Composable
private fun HeadSwitch(head: Property<Boolean>, enabled: Boolean, onHeadChange: (Boolean) -> Unit) {
    var checked by remember(head) { mutableStateOf(head.shown() == true) }
    DisposableEffect(head) {
        val listener = Predicate<Boolean> { value -> checked = value == true; false }
        head.addCallback(listener)
        onDispose { head.removeCallback(listener) }
    }
    SwitchControl(checked, enabled) {
        checked = it
        head.set(it)
        onHeadChange(it)
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

    Box(Modifier.optionCard()) {
        SettingContent(prop, compact = compact, enabled = display != Property.Display.DISABLED)
    }
}

/** The card behind an option or accordion */
@Composable
private fun Modifier.optionCard(): Modifier {
    val theme = LocalTheme.current
    val shape = theme.modCardShape
    val border = remember(theme.borderColor) { Brush.verticalGradient(listOf(theme.borderColor, theme.borderColor.copy(0f))) }
    return fillMaxWidth()
        .clip(shape)
        .hoverGlow(shape)
        .background(theme.modCardBackground, shape)
        .border(1.dp, border, shape)
}

@Composable
private fun AccordionSettingRow(
    prop: Property<*>,
    display: Property.Display,
    groupEnabled: Boolean,
    modifier: Modifier,
    compact: Boolean = false,
) {
    Box(modifier) {
        SettingContent(prop, nested = true, compact = compact, enabled = groupEnabled && display != Property.Display.DISABLED)
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
private val OPTION_ACTION_WIDTH = ControlSize.Compact.height + 16.dp

private val OPTION_ROW_CHROME = 32.dp + 16.dp

private val SLIDER_SPINNER_WIDTH = 80.dp + 16.dp

private val GROUP_CHEVRON_WIDTH = 12.dp + 12.dp

/**
 * Space a control needs beside its label. Custom visualizers wider than a default control
 * declare their width in dp through the `controlWidth` metadata
 */
private fun optionReservedWidth(prop: Property<*>, optionWidth: Dp): Dp {
    val control = sliderTrailingWidth(prop)?.let { optionWidth + it + OPTION_ROW_CHROME }
        ?: prop.getMetadata<Number>("controlWidth")?.let { it.toFloat().dp + OPTION_ROW_CHROME }
        ?: (if (prop.getMetadata<Any>("swatch").let { it != null && it != false }) 24.dp + OPTION_ROW_CHROME else null)
        ?: OPTION_CHROME_WIDTH
    return control + (if (prop.nodeIcon() != null) OPTION_ICON_WIDTH else 0.dp) +
        (if (OneConfigConfig.showOptionActionButtons) OPTION_ACTION_WIDTH else 0.dp)
}

@Composable
private fun rememberTitleStyle(): TextStyle {
    val theme = LocalTheme.current
    return remember(theme) {
        TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = theme.typography.family)
    }
}

private fun TextMeasurer.titleWidth(node: Node, style: TextStyle, density: Density): Dp {
    return with(density) {
        measure(node.localizedTitle().asRenderText(), style = style, softWrap = false, maxLines = 1).size.width.toDp()
    }
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
private fun AccordionOptionsGrid(body: List<Property<*>>, compact: Boolean, enabled: Boolean) {
    val visible = rememberVisibleOptions(body)
    val displays = remember(visible) { visible.toMap() }
    val visibleProps = remember(visible) { visible.map { it.first } }

    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(OPTION_ROW_GAP)) {
            visible.forEach { (prop, display) ->
                AccordionSettingRow(prop, display, enabled, Modifier.fillMaxWidth(), compact = isWideControl(prop))
            }
        }
        return
    }

    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val optionWidth = LocalOptionWidth.current
    val titleStyle = rememberTitleStyle()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columnWidth = (maxWidth - OPTION_ROW_GAP) / 2
        val rows = remember(visibleProps, columnWidth, optionWidth, titleStyle, OneConfigConfig.showOptionActionButtons) {
            buildOptionRows(visibleProps) { prop ->
                measurer.titleWidth(prop, titleStyle, density) + optionReservedWidth(prop, optionWidth) > columnWidth
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(OPTION_ROW_GAP)) {
            rows.forEach { row ->
                when (row) {
                    is OptionRow.Single -> {
                        if (row.wide) {
                            AccordionSettingRow(row.prop, displays.getValue(row.prop), enabled, Modifier.fillMaxWidth())
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(OPTION_ROW_GAP)) {
                                AccordionSettingRow(row.prop, displays.getValue(row.prop), enabled, Modifier.weight(1f))
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    is OptionRow.Pair -> Row(horizontalArrangement = Arrangement.spacedBy(OPTION_ROW_GAP)) {
                        AccordionSettingRow(row.first, displays.getValue(row.first), enabled, Modifier.weight(1f))
                        AccordionSettingRow(row.second, displays.getValue(row.second), enabled, Modifier.weight(1f))
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

@Composable
internal fun displayAlpha(display: Property.Display): Float {
    return if (display == Property.Display.DISABLED) LocalTheme.current.disabledAlpha else 1f
}

@Composable
private fun SettingContent(prop: Property<*>, nested: Boolean = false, compact: Boolean = false, enabled: Boolean = true) {
    val active = enabled && LocalOptionEnabled.current
    CompositionLocalProvider(LocalOptionEnabled provides active) {
        SettingContentBody(prop, nested, compact, active)
    }
}

@Composable
private fun SettingContentBody(prop: Property<*>, nested: Boolean, compact: Boolean, enabled: Boolean) {
    val verticalPadding = if (nested) 0.dp else 12.dp
    val labelModifier = if (enabled) Modifier else Modifier.alpha(LocalTheme.current.disabledAlpha)

    val visualizer = prop[MetaKeys.VISUALIZER]
    if (visualizer == Visualizer.InfoVisualizer::class.java || visualizer is Visualizer.InfoVisualizer) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = verticalPadding)) {
            Option(prop)
        }
        return
    }

    val menu = rememberOptionMenu(prop)
    val rowInteraction = rememberInteractionSource()
    val isRowHovered by rowInteraction.collectIsHoveredAsState()
    val showActionButton = OneConfigConfig.showOptionActionButtons && enabled

    val measurer = rememberTextMeasurer()
    val titleStyle = rememberTitleStyle()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(rowInteraction)
            .optionContextMenu(menu, enabled)
    ) {
        val actionButton = @Composable {
            if (showActionButton) OptionMoreButton(menu, visible = isRowHovered)
        }
        if (isFullWidth(prop)) {
            val fullControl = @Composable { modifier: Modifier ->
                BoxWithConstraints(modifier) {
                    CompositionLocalProvider(LocalOptionWidth provides maxWidth, LocalOptionWidthOrNull provides maxWidth) { Option(prop) }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = verticalPadding, bottom = if (nested) 8.dp else verticalPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (hidesLabel(prop)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        fullControl(Modifier.weight(1f))
                        actionButton()
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingLabel(prop, nested = nested, modifier = Modifier.weight(1f).then(labelModifier))
                        actionButton()
                    }
                    fullControl(Modifier.fillMaxWidth())
                }
            }
        } else {
            val title = prop.localizedTitle().asRenderText()
            val titleWidth = remember(title, titleStyle, measurer) {
                measurer.measure(title, style = titleStyle, softWrap = false, maxLines = 1).size.width
            }
            LabelledOptionRow(
                titleWidth = titleWidth,
                leading = if (prop.nodeIcon() != null) OPTION_ICON_WIDTH else 0.dp,
                stack = compact,
                verticalPadding = verticalPadding,
                bottomPadding = if (nested) 8.dp else verticalPadding,
                modifier = Modifier.fillMaxWidth(),
                label = { SettingLabel(prop, nested = nested, modifier = labelModifier) },
                actionButton = actionButton,
                control = { Option(prop) },
            )
        }
    }
}

/**
 * An option's [label] beside its [control], or above it when the title would not fit on one line next to the
 * control as it is actually laid out, or whenever [stack] asks for it
 *
 * [titleWidth] is the title's width on one line in pixels and [leading] what the label puts before it, such as an icon
 */
@Composable
private fun LabelledOptionRow(
    titleWidth: Int,
    leading: Dp,
    stack: Boolean,
    verticalPadding: Dp,
    bottomPadding: Dp,
    modifier: Modifier,
    label: @Composable () -> Unit,
    actionButton: @Composable () -> Unit,
    control: @Composable () -> Unit,
) {
    Layout(listOf(label, actionButton, control), modifier) { (labels, actions, controls), constraints ->
        val side = 16.dp.roundToPx()
        val top = verticalPadding.roundToPx()
        val width = constraints.maxWidth
        val inner = (width - side * 2).coerceAtLeast(0)
        val loose = Constraints(maxWidth = inner)
        val ctrl = controls.firstOrNull()?.measure(loose)
        val action = actions.firstOrNull()?.measure(loose)
        val ctrlWidth = ctrl?.width ?: 0

        val besideGap = 16.dp.roundToPx()
        val beside = inner - ctrlWidth - besideGap - (action?.let { it.width + besideGap } ?: 0)
        if (!stack && titleWidth + leading.roundToPx() <= beside) {
            val lbl = labels.first().measure(Constraints.fixedWidth(beside.coerceAtLeast(0)))
            // as tall as a dropdown even when the control is a smaller switch or checkbox
            val rowHeight = maxOf(lbl.height, action?.height ?: 0, ctrl?.height ?: 0, OptionControlHeight.roundToPx())
            layout(width, rowHeight + top * 2) {
                fun Placeable.center(x: Int) = place(x, top + (rowHeight - height) / 2)
                lbl.center(side)
                action?.center(side + beside + besideGap)
                ctrl?.center(width - side - ctrlWidth)
            }
        } else {
            val lbl = labels.first().measure(loose)
            val belowGap = 10.dp.roundToPx()
            val controlsGap = 12.dp.roundToPx()
            val rowHeight = maxOf(action?.height ?: 0, ctrl?.height ?: 0)
            val rowTop = top + lbl.height + belowGap
            layout(width, rowTop + rowHeight + bottomPadding.roundToPx()) {
                lbl.place(side, top)
                val ctrlX = width - side - ctrlWidth
                ctrl?.place(ctrlX, rowTop + (rowHeight - ctrl.height) / 2)
                action?.place(ctrlX - (if (ctrl != null) controlsGap else 0) - action.width, rowTop + (rowHeight - action.height) / 2)
            }
        }
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
        prop.nodeIcon()?.let { NodeIcon(it, Modifier.size(32.dp)) }
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

/** Whether [tree] has any custom options besides the built-in ones */
@Composable
internal fun rememberHasHudOptions(tree: Tree?): Boolean {
    val categories = remember(tree) { tree?.let(::buildCategories).orEmpty() }
    val revision = rememberDisplayRevision(categories)
    return remember(categories, revision) { categories.any { filterHiddenNodes(it) != null } }
}

@Composable
fun HudConfigScreen(tree: Tree, searchQuery: String) {
    key(System.identityHashCode(tree)) { HudConfigScreenContent(tree, searchQuery) }
}

@Composable
private fun HudConfigScreenContent(tree: Tree, searchQuery: String) {
    val categories = remember(tree) { buildCategories(tree) }
    val revision = rememberDisplayRevision(categories)
    val visibleCategories = remember(categories, revision) { categories.filter { filterHiddenNodes(it) != null } }
    var chosenCategory by remember(tree) { mutableStateOf<CategoryGroup?>(null) }
    val selectedCategory = chosenCategory?.takeIf { it in visibleCategories } ?: visibleCategories.firstOrNull()
    val entries = remember(visibleCategories, selectedCategory, searchQuery, revision) {
        if (searchQuery.isEmpty()) {
            selectedCategory?.let(::filterHiddenNodes)?.let(::flattenEntries).orEmpty()
        } else {
            flattenSearchEntries(filterCategories(visibleCategories, searchQuery).mapNotNull(::filterHiddenNodes))
        }
    }
    // empty draws nothing but stays composed, so the display listeners above notice hidden options appearing
    if (entries.isEmpty()) return

    CompositionLocalProvider(LocalOptionWidth provides 210.dp, LocalOptionWidthOrNull provides 210.dp) {
    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        if (searchQuery.isEmpty() && visibleCategories.size > 1) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                visibleCategories.forEach { category ->
                    Chip(
                        label = category.name,
                        selected = selectedCategory == category,
                        onClick = { chosenCategory = category }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { entry -> ConfigListRow(entry, ::hasWideControl) }
        }
    }
    }
}

/**
 * The HUD editor's column is narrow so any row holding a wide control stacks its label above it, and an accordion
 * holding one lists its options in a single column where only those rows stack
 */
private fun hasWideControl(node: SettingNode): Boolean = when (node) {
    is SettingNode.Leaf -> isWideControl(node.prop)
    is SettingNode.Accordion -> node.body.any(::isWideControl) || node.head?.let(::isWideControl) == true
    is SettingNode.Submenu -> node.head?.let(::isWideControl) == true
}
