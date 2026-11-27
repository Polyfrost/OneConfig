package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.apache.logging.log4j.LogManager
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
import org.polyfrost.oneconfig.api.ui.v1.components.CardBorder
import org.polyfrost.oneconfig.api.ui.v1.components.Confirm
import org.polyfrost.oneconfig.api.ui.v1.components.ControlDefaults
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.EmptyState
import org.polyfrost.oneconfig.api.ui.v1.components.LoadState
import org.polyfrost.oneconfig.api.ui.v1.components.MenuAction
import org.polyfrost.oneconfig.api.ui.v1.components.MenuEntry
import org.polyfrost.oneconfig.api.ui.v1.components.ScrollContainer
import org.polyfrost.oneconfig.api.ui.v1.components.SectionChevron
import org.polyfrost.oneconfig.api.ui.v1.components.SectionHeader
import org.polyfrost.oneconfig.api.ui.v1.components.SectionHeaderDefaults
import org.polyfrost.oneconfig.api.ui.v1.components.Switch
import org.polyfrost.oneconfig.api.ui.v1.components.Text
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipBox
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipDefaults
import org.polyfrost.oneconfig.api.ui.v1.components.cardSurface
import org.polyfrost.oneconfig.api.ui.v1.components.contextMenu
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.api.PropertyModToggle
import org.polyfrost.oneconfig.internal.ui.api.isLockExempt
import org.polyfrost.oneconfig.internal.ui.api.isMarkedSwitch
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.ContentSlide
import org.polyfrost.oneconfig.internal.ui.components.ContentSlide.tabSlide
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.StripTab
import org.polyfrost.oneconfig.internal.ui.components.TabStrip
import org.polyfrost.oneconfig.internal.ui.components.isEmptyText
import org.polyfrost.oneconfig.internal.ui.components.localizedDescription
import org.polyfrost.oneconfig.internal.ui.components.localizedTitle
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.scrollFade
import org.polyfrost.oneconfig.internal.ui.components.settings.LocalOptionEnabled
import org.polyfrost.oneconfig.internal.ui.components.settings.LocalOptionWidth
import org.polyfrost.oneconfig.internal.ui.components.settings.LocalOptionWidthOrNull
import org.polyfrost.oneconfig.internal.ui.components.settings.Option
import org.polyfrost.oneconfig.internal.ui.components.settings.OptionControlHeight
import org.polyfrost.oneconfig.internal.ui.components.settings.OptionMenuState
import org.polyfrost.oneconfig.internal.ui.components.settings.OptionMoreButton
import org.polyfrost.oneconfig.internal.ui.components.settings.asPlainLabel
import org.polyfrost.oneconfig.internal.ui.components.settings.asStyledLabel
import org.polyfrost.oneconfig.internal.ui.components.settings.identitySet
import org.polyfrost.oneconfig.internal.ui.components.settings.optionContextMenu
import org.polyfrost.oneconfig.internal.ui.components.settings.rememberChangedFromDefault
import org.polyfrost.oneconfig.internal.ui.components.settings.rememberOptionMenu
import org.polyfrost.oneconfig.internal.ui.components.settings.resetTree
import org.polyfrost.oneconfig.internal.ui.components.settings.treeHasDefault
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
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.function.Predicate
import javax.imageio.ImageIO
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min
import kotlin.math.roundToInt

/** The id of the config whose page is showing or null where a submenu row has no page of its config to open */
internal val LocalConfigId = staticCompositionLocalOf<String?> { null }

internal val LocalSearchExpands = staticCompositionLocalOf { false }

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
    val locksAll = locked && lockExempt?.isMarkedSwitch() != false && !tree.isLockExempt()

    // an option jumped to from search opens on its own tab, kept once the jump is done
    val jump = navigation.pendingJump
    val jumpCategory = remember(jump, categories) {
        jump?.let { target -> categories.firstOrNull { c -> c.subcategories.any { s -> s.nodes.any { it.holds(target) } } } }
    }
    SideEffect { jumpCategory?.let { navigation.rememberCategory(pageKey, it.name) } }
    val savedCategory = jumpCategory?.name ?: navigation.selectedCategory(pageKey) ?: initialCategory
    val selectedCategory = categories.firstOrNull { it.name.equals(savedCategory, ignoreCase = true) }
        ?: categories.firstOrNull()

    fun selectCategory(category: CategoryGroup) {
        navigation.selectCategory(pageKey, category.name)
    }

    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        if (localSearchQuery.isBlank() && categories.size > 1) {
            CategoryTabs(categories, selectedCategory, Modifier.padding(horizontal = PageGutter), ::selectCategory)
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
            val rows = remember(entries) { groupSections(entries, "$pageKey/${category?.name.orEmpty()}") }

            // each category keeps its own place in the list
            val lazyListState = rememberRestorableLazyListState(
                category?.let { "$pageKey/${it.name}" } ?: pageKey,
                localSearchQuery,
                search.query,
            )

            val jumpHighlight = remember { JumpHighlight() }
            val highlightScope = rememberCoroutineScope()
            LaunchedEffect(jump, entries) {
                val target = jump ?: return@LaunchedEffect
                val at = rows.indexOfFirst { it is PageRow.Section && it.items.any { item -> item.node.holds(target) } }
                if (at < 0) return@LaunchedEffect
                val section = rows[at] as PageRow.Section
                SectionCollapse.expand(section.id)
                val itemIndex = section.items.indexOfFirst { it.node.holds(target) }
                val listKeys = pageListKeys(rows, header != null, false, localSearchQuery.isNotBlank())
                val listIndex = listKeys.indexOf(sectionItemKey(at, itemIndex))
                // clearing the jump restarts this effect, so the scroll and highlight run in a scope that outlives it
                highlightScope.launch {
                    try {
                        // the list has no size until its first layout, and the row ends a third of the way down
                        // rather than at the top
                        val viewport = snapshotFlow { lazyListState.layoutInfo.viewportSize.height }.first { it > 0 }
                        val laidOut = withTimeoutOrNull(SECTION_EXPAND_WAIT_MS) {
                            snapshotFlow { lazyListState.layoutInfo.totalItemsCount }.first { it >= listKeys.size }
                        } != null
                        if (laidOut) {
                            lazyListState.scrollToItem(listIndex.coerceAtLeast(0))
                            lazyListState.scrollBy(-viewport / 3f)
                        }
                    } catch (e: CancellationException) {
                        // scrolling by hand stops the jump, which should still leave the highlight
                        if (!isActive) throw e
                    } catch (t: Throwable) {
                        LOGGER.warn("Failed to scroll to the option jumped to on {}", pageKey, t)
                    }
                    jumpHighlight.play(target)
                }
                navigation.consumeJump(target)
            }

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

            ScrollContainer(
                lazyListState,
                Modifier.fillMaxSize().then(if (leaving) Modifier.graphicsLayer { alpha = progress.value } else Modifier),
                gutter = PageGutter,
            ) {
                CompositionLocalProvider(LocalJumpHighlight provides jumpHighlight) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.scrollFade(lazyListState),
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
                            Box(Modifier.fillMaxWidth().padding(top = PAGE_ROW_GAP).then(slide), contentAlignment = Alignment.Center) {
                                EmptyState(empty.title, icon = empty.icon, state = empty.state)
                            }
                        }
                    }
                    val searching = localSearchQuery.isNotBlank()
                    rows.forEachIndexed { rowIndex, row ->
                        val topGap = if (rowIndex == 0 && header == null) 0.dp else PAGE_ROW_GAP
                        val decorate: @Composable (@Composable () -> Unit) -> Unit = { content ->
                            CompositionLocalProvider(LocalSearchExpands provides searching) {
                                Box(slide) { content() }
                            }
                        }
                        when (row) {
                            is PageRow.Heading -> item(key = headingKey(rowIndex)) {
                                decorate {
                                    Box(Modifier.padding(top = topGap)) {
                                        CompositionLocalProvider(LocalOptionEnabled provides !(locksAll || groupOff)) {
                                            ConfigListRow(row.entry)
                                        }
                                    }
                                }
                            }
                            is PageRow.Section -> groupedSection(
                                key = sectionKey(rowIndex),
                                title = row.title,
                                items = row.items,
                                collapsed = !searching && SectionCollapse.isCollapsed(row.id),
                                collapsible = !searching,
                                first = rowIndex == 0,
                                topGap = topGap,
                                dimmed = locksAll || groupOff,
                                onToggle = { SectionCollapse.toggle(row.id) },
                                decorate = decorate,
                            ) { item ->
                                val lock = locksAll && !item.isLockExempt(lockExempt)
                                CompositionLocalProvider(LocalOptionEnabled provides !(lock || groupOff)) {
                                    ConfigListRow(item)
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

private val LOGGER = LogManager.getLogger("OneConfig/UI")

private const val JUMP_HIGHLIGHT_IN_MS = 150
private const val JUMP_HIGHLIGHT_HOLD_MS = 700
private const val JUMP_HIGHLIGHT_OUT_MS = 900

/** The option or group a page was jumped to from search, and how strongly it is highlighted right now */
private class JumpHighlight {
    var target: Any? by mutableStateOf(null)
        private set
    val strength = Animatable(0f)

    suspend fun play(target: Any) {
        this.target = target
        try {
            strength.snapTo(0f)
            strength.animateTo(1f, tween(JUMP_HIGHLIGHT_IN_MS))
            strength.animateTo(0f, tween(JUMP_HIGHLIGHT_OUT_MS, delayMillis = JUMP_HIGHLIGHT_HOLD_MS))
        } finally {
            this.target = null
        }
    }
}

private val LocalJumpHighlight = staticCompositionLocalOf<JumpHighlight?> { null }

/** Whether this node's row is [target] or holds it, such as an accordion holding an option */
private fun SettingNode.holds(target: Any): Boolean = when (this) {
    is SettingNode.Leaf -> prop === target
    is SettingNode.Accordion -> tree === target || head === target || body.any { it === target }
    is SettingNode.Submenu -> tree === target || head === target
}

/** An accent tint and outline in the card's own shape over a row that was jumped to and stands for one of [anchors] */
@Composable
private fun Modifier.jumpHighlightOf(vararg anchors: Any?): Modifier {
    val jump = LocalJumpHighlight.current ?: return this
    val target = jump.target ?: return this
    if (anchors.none { it === target }) return this
    val shape = LocalTheme.current.modCardShape
    return drawWithContent {
        drawContent()
        val a = jump.strength.value
        if (a <= 0f) return@drawWithContent
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(outline, Accent.copy(alpha = 0.14f * a))
        drawOutline(outline, Accent.copy(alpha = 0.7f * a), style = Stroke(1.5.dp.toPx()))
    }
}

@Composable
private fun CategoryTabs(
    categories: List<CategoryGroup>,
    selected: CategoryGroup?,
    modifier: Modifier,
    onSelect: (CategoryGroup) -> Unit,
) {
    val tabs = remember(categories) { categories.map { StripTab(it.name, it.name.asPlainLabel()) } }
    TabStrip(tabs, selected?.name.orEmpty(), { name -> categories.firstOrNull { it.name == name }?.let(onSelect) }, modifier)
}

private class ConfigEmpty(val title: String, val icon: String?, val state: LoadState = LoadState.Idle)

private sealed interface PageRow {
    class Heading(val entry: ConfigListEntry.CategoryHeader) : PageRow

    class Section(val id: String, val title: String?, val items: List<ConfigListEntry.Item>) : PageRow
}

private fun groupSections(entries: List<ConfigListEntry>, scope: String): List<PageRow> {
    val rows = ArrayList<PageRow>()
    var category = scope
    var title: String? = null
    var items = ArrayList<ConfigListEntry.Item>()
    fun flush() {
        if (items.isNotEmpty()) rows += PageRow.Section("$category/${title.orEmpty()}", title, items)
        items = ArrayList()
        title = null
    }
    for (entry in entries) {
        when (entry) {
            is ConfigListEntry.CategoryHeader -> {
                flush()
                category = "$scope/${entry.title}"
                rows += PageRow.Heading(entry)
            }
            is ConfigListEntry.SubcategoryHeader -> {
                flush()
                title = entry.title
            }
            is ConfigListEntry.Item -> items += entry
        }
    }
    flush()
    return rows
}

private object SectionCollapse {
    private val collapsed = mutableStateMapOf<String, Boolean>()

    fun isCollapsed(id: String): Boolean = collapsed[id] == true

    fun toggle(id: String) {
        collapsed[id] = !isCollapsed(id)
    }

    fun expand(id: String) {
        collapsed.remove(id)
    }
}

private val SECTION_GAP = 14.dp
private val SECTION_HEADING_GAP = 8.dp
private const val SECTION_DIVIDER_ALPHA = 0.7f

internal val LocalGroupedRows = staticCompositionLocalOf { false }

@Composable
private fun SectionBlock(
    section: PageRow.Section,
    first: Boolean,
    collapsible: Boolean,
    dimmed: Boolean,
    item: @Composable (ConfigListEntry.Item) -> Unit,
) {
    val collapsed = collapsible && SectionCollapse.isCollapsed(section.id)
    Column(Modifier.fillMaxWidth()) {
        section.title?.let {
            SectionHeading(
                it,
                collapsed = collapsed,
                count = optionCountLabel(section.items.size),
                collapsible = collapsible,
                icon = null,
                modifier = Modifier
                    .padding(top = if (first) 0.dp else SECTION_GAP)
                    .then(if (dimmed) Modifier.alpha(LocalTheme.current.disabledAlpha) else Modifier),
                onToggle = { SectionCollapse.toggle(section.id) },
            )
        }
        AnimatedVisibility(visible = !collapsed, enter = foldEnter(), exit = foldExit()) {
            Column(Modifier.padding(top = if (section.title != null) SECTION_HEADING_GAP else 0.dp).optionCard()) {
                CompositionLocalProvider(LocalGroupedRows provides true) {
                    section.items.forEachIndexed { index, entry ->
                        Box(if (index == 0) Modifier else Modifier.rowDivider()) { item(entry) }
                    }
                }
            }
        }
    }
}

private val PAGE_ROW_GAP = 8.dp

private fun headingKey(rowIndex: Int) = "heading:$rowIndex"

private fun sectionKey(rowIndex: Int) = "section:$rowIndex"

private fun sectionItemKey(rowIndex: Int, itemIndex: Int) = "${sectionKey(rowIndex)}:$itemIndex"

private fun pageListKeys(rows: List<PageRow>, hasHeader: Boolean, hasEmpty: Boolean, searching: Boolean): List<String> = buildList {
    if (hasHeader) add("header")
    if (hasEmpty) add("empty")
    rows.forEachIndexed { rowIndex, row ->
        when (row) {
            is PageRow.Heading -> add(headingKey(rowIndex))
            is PageRow.Section -> {
                if (row.title != null) add("${sectionKey(rowIndex)}:title")
                if (searching || !SectionCollapse.isCollapsed(row.id)) row.items.indices.forEach { add(sectionItemKey(rowIndex, it)) }
            }
        }
    }
}

internal fun <T> LazyListScope.groupedSection(
    key: String,
    title: Any?,
    items: List<T>,
    collapsed: Boolean,
    collapsible: Boolean,
    first: Boolean,
    topGap: Dp,
    dimmed: Boolean = false,
    icon: String? = null,
    countLabel: (Int) -> String = ::optionCountLabel,
    onToggle: () -> Unit,
    decorate: @Composable (@Composable () -> Unit) -> Unit = { it() },
    item: @Composable (T) -> Unit,
) {
    if (title != null) {
        item(key = "$key:title") {
            decorate {
                SectionHeading(
                    title,
                    collapsed = collapsed,
                    count = countLabel(items.size),
                    collapsible = collapsible,
                    icon = icon,
                    modifier = Modifier
                        .padding(top = topGap + if (first) 0.dp else SECTION_GAP, bottom = if (collapsed) 0.dp else SECTION_HEADING_GAP)
                        .then(if (dimmed) Modifier.alpha(LocalTheme.current.disabledAlpha) else Modifier),
                    onToggle = onToggle,
                )
            }
        }
    }
    if (collapsed) return
    items.forEachIndexed { index, entry ->
        item(key = "$key:$index") {
            val fold = Modifier.animateItem(fadeInSpec = tween(SECTION_FOLD_MS), placementSpec = null, fadeOutSpec = tween(SECTION_FOLD_MS))
            decorate {
                Box(
                    fold
                        .padding(top = if (index == 0 && title == null) topGap else 0.dp)
                        .sectionSegment(top = index == 0, bottom = index == items.lastIndex)
                        .then(if (index == 0) Modifier else Modifier.rowDivider()),
                ) {
                    CompositionLocalProvider(LocalGroupedRows provides true) { item(entry) }
                }
            }
        }
    }
}

private const val SECTION_FOLD_MS = 150
private const val SECTION_EXPAND_WAIT_MS = 1000L

@Composable
private fun Modifier.sectionSegment(top: Boolean, bottom: Boolean): Modifier {
    val theme = LocalTheme.current
    val shape = segmentShape(theme.modCardShape, top, bottom)
    val border = theme.borderColor
    return fillMaxWidth()
        .cardSurface(shape, border = CardBorder.None)
        .drawWithContent {
            drawContent()
            val stroke = 1.dp.toPx()
            val above = if (top) 0f else stroke
            val below = if (bottom) 0f else stroke
            val outline = shape.createOutline(Size(size.width - stroke, size.height - stroke + above + below), layoutDirection, this)
            clipRect {
                translate(stroke / 2f, stroke / 2f - above) { drawOutline(outline, border, style = Stroke(stroke)) }
            }
        }
}

private fun segmentShape(base: Shape, top: Boolean, bottom: Boolean): Shape {
    if (top && bottom) return base
    val corners = base as? CornerBasedShape ?: return RectangleShape
    val square = CornerSize(0)
    return when {
        top -> corners.copy(bottomStart = square, bottomEnd = square)
        bottom -> corners.copy(topStart = square, topEnd = square)
        else -> RectangleShape
    }
}

private fun optionCountLabel(count: Int): String =
    if (count == 1) translated("oneconfig.config.section.count.one", "1 option")
    else translated("oneconfig.config.section.count", "%s options", count)

@Composable
private fun SectionHeading(
    title: Any,
    collapsed: Boolean,
    count: String,
    collapsible: Boolean,
    icon: String?,
    modifier: Modifier,
    onToggle: () -> Unit,
) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val style = theme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (collapsible) {
                    Modifier.oneConfigClickable(title.asPlainLabel(), onToggle, indication = false, interactionSource = source)
                } else {
                    Modifier
                },
            )
            .padding(start = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, color = theme.textColor, modifier = Modifier.size(18.dp)) }
        Text(title.asStyledLabel(), Modifier.weight(1f, fill = false), theme.textColor, style, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (collapsible) SectionChevron(!collapsed)
        if (collapsed) Text(count, color = theme.textColorSecondary, style = descriptionStyle())
    }
}

@Composable
private fun Modifier.rowDivider(): Modifier {
    val border = LocalTheme.current.borderColor
    val color = border.copy(alpha = border.alpha * SECTION_DIVIDER_ALPHA)
    return drawBehind {
        if (size.height <= 0f) return@drawBehind
        val start = 16.dp.toPx()
        val end = OPTION_ROW_END_PADDING.toPx()
        drawRect(color, Offset(start, 0f), Size(size.width - start - end, 1.dp.toPx()))
    }
}

private fun ConfigListEntry.isLockExempt(ownSwitch: Property<*>?): Boolean {
    val node = (this as? ConfigListEntry.Item)?.node ?: return false
    return when (node) {
        is SettingNode.Leaf -> node.prop === ownSwitch || node.prop.isLockExempt()
        is SettingNode.Accordion -> node.tree.isLockExempt()
        is SettingNode.Submenu -> node.tree.isLockExempt()
    }
}

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
        .any { searchMatches(it.asPlainLabel(), query) }
}

private fun Tree.matchesLocalSearch(category: String, subcategory: String, query: String): Boolean {
    return listOfNotNull(localizedTitle(), id, localizedDescription(), category, subcategory)
        .any { searchMatches(it.asPlainLabel(), query) }
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
    SectionHeader(
        title.asPlainLabel(),
        modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 2.dp),
        textStyle = SectionHeaderDefaults.labelStyle,
        color = OneConfigTheme.current.textColorSecondary,
        uppercase = false,
    )
}

@Composable
private fun SubcategoryHeader(title: String, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    Text(
        title,
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp),
        color = theme.textColor,
        style = theme.typography.titleLarge,
        legacyFormatting = true,
    )
}

@Composable
private fun AccordionRow(node: SettingNode.Accordion, compact: Boolean = false) {
    val head = node.head
    val toggle = head?.takeIf { it === node.tree.enableToggle }
    val searching = LocalSearchExpands.current
    var expanded by remember(node, searching) {
        mutableStateOf(searching || (node.tree[MetaKeys.COLLAPSED] != true && node.head?.shown() != false))
    }
    var headOn by remember(node) { mutableStateOf(head?.shown() == true) }
    if (head != null && head.isBooleanHead()) {
        DisposableEffect(head, searching) {
            val boolHead = Property.recast<Boolean>(head)
            val listener = Predicate<Boolean> { value ->
                headOn = value == true
                if (head.hasVisualizer() || toggle != null) expanded = value == true || searching
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
    val jumpTarget = LocalJumpHighlight.current?.target
    LaunchedEffect(jumpTarget) {
        if (jumpTarget != null && node.body.any { it === jumpTarget }) expanded = true
    }

    GroupCard(Modifier.jumpHighlightOf(node.tree, node.head)) {
        GroupHeader(
            tree = node.tree,
            head = head,
            expanded = expanded,
            compact = compact,
            onHeadChange = { expanded = it || searching; headOn = it },
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
    mixed: Boolean = false,
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
            if (checked != null) Switch(checked, onCheckedChange, enabled = headEnabled, stateLabel = false, mixed = mixed)
        }
        AnimatedVisibility(visible = expanded, enter = foldEnter(), exit = foldExit()) {
            Box(Modifier.fillMaxWidth().padding(bottom = 12.dp)) { body((checked != false || mixed) && headEnabled) }
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
    SectionHeader(
        title.asPlainLabel(),
        modifier,
        eyebrow = false,
        onToggle = onToggle,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        title = { _, _ ->
            HeaderLabel(title, description, Modifier.weight(1f).alpha(labelAlpha), titleActions) {
                icon?.let { Icon(it, color = LocalTheme.current.textColor, modifier = Modifier.size(32.dp)) }
            }
        },
        trailing = {
            Row(
                Modifier.padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                trailing()
                if (onToggle != null) SectionChevron(expanded)
            }
        },
    )
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
                Text(title.asStyledLabel(), color = theme.textColor, style = rememberTitleStyle())
                titleActions()
            }
            description?.let { Text(it.asStyledLabel(), color = theme.textColorSecondary, style = descriptionStyle()) }
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
    GroupCard(Modifier.jumpHighlightOf(node.tree, node.head)) {
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
    val searching = LocalSearchExpands.current
    var expanded by remember(node, searching) { mutableStateOf(searching || node.head?.shown() != false) }
    val groupOff = rememberSwitchValue(toggle) == false
    val revision = rememberDisplayRevision(node.content)
    val entries = remember(node, revision) {
        node.content.mapNotNull(::filterHiddenNodes).flatMap(::flattenEntries)
    }

    GroupCard(Modifier.jumpHighlightOf(node.tree, node.head)) {
        GroupHeader(
            tree = node.tree,
            head = node.head,
            expanded = expanded,
            compact = compact,
            onHeadChange = { if (toggle != null) expanded = it || searching },
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
private fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.optionCard().then(modifier), content = content)
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
    val showActionButton = OneConfigConfig.showOptionActionButtons
    val menu = head?.let { rememberOptionMenu(it) }
    val configId = LocalConfigId.current
    menu?.extraEntries = { listOfNotNull(groupResetEntry(tree, false, configId, null)) }
    val changed = head?.let { rememberChangedFromDefault(it) } == true

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interaction)
            .contextMenu { groupMenuEntries(tree, menu.takeUnless { headLocked }, !groupEnabled, configId, head.takeIf { headDisplay == Property.Display.DISABLED }) }
            .oneConfigClickable(tree.localizedTitle().asPlainLabel(), onClick, indication = false, interactionSource = interaction)
    ) {
        val switch = head != null && isSwitchHead(tree, head)
        val control = head?.takeIf { switch || headDisplay != Property.Display.HIDDEN }
        val stacked = control != null && !switch && (compact || isFullWidth(control) || run {
            val iconWidth = if (tree.nodeIcon() != null) OPTION_ICON_WIDTH else 0.dp
            val reserved = optionReservedWidth(control, optionWidth) + iconWidth + GROUP_CHEVRON_WIDTH
            measurer.titleWidth(tree, titleStyle, density) + reserved > maxWidth
        })
        val headControl = @Composable {
            if (control != null) HeadControl(control, !headLocked, switch, onHeadChange)
        }
        val actionMenu = menu?.takeIf { showActionButton && control != null }
        val endPadding = if (actionMenu != null) OPTION_ROW_END_PADDING else 16.dp
        val actionButton = @Composable {
            actionMenu?.let { OptionMoreButton(it, hovered = isHovered, changed = changed, enabled = !headLocked) }
        }

        if (stacked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = endPadding, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    GroupLabel(tree, Modifier.weight(1f).then(labelModifier))
                    SectionChevron(expanded)
                    actionButton()
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { headControl() }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = endPadding, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GroupLabel(tree, Modifier.weight(1f).then(labelModifier))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    headControl()
                    SectionChevron(expanded)
                    actionButton()
                }
            }
        }
    }
}

private fun groupMenuEntries(tree: Tree, headMenu: OptionMenuState?, locked: Boolean, configId: String?, lockedHead: Property<*>?): List<MenuEntry> =
    headMenu?.entries() ?: listOfNotNull(groupResetEntry(tree, locked, configId, lockedHead))

private fun groupResetEntry(tree: Tree, locked: Boolean, configId: String?, lockedHead: Property<*>?): MenuEntry? {
    if (!isNativeConfig(configId)) return null
    val skipped = listOfNotNull(ownSwitchOf(configId), lockedHead)
    val resettable = treeHasDefault(tree, identitySet().apply { addAll(skipped) })
    return MenuAction(
        translated("oneconfig.group.menu.reset", "Reset to defaults"),
        { confirmGroupReset(tree, skipped) },
        icon = "refresh",
        danger = true,
        enabled = !locked && resettable,
        disabledReason = if (locked) {
            translated("oneconfig.group.menu.reset.locked", "These settings can't be reset while they are switched off.")
        } else {
            translated("oneconfig.group.menu.reset.disabled", "Nothing in this group has a default to restore.")
        },
    )
}

private fun isNativeConfig(configId: String?): Boolean {
    val data = configId?.let(ConfigRegistry::findById) ?: return false
    return data.source == ConfigSource.OC && ConfigRegistry.findTree(data.id)?.getMetadata<Boolean>("ui_only") != true
}

private fun ownSwitchOf(configId: String?): Property<*>? =
    (configId?.let { ConfigRegistry.findById(it)?.toggle } as? PropertyModToggle)?.property

private fun confirmGroupReset(tree: Tree, skipped: List<Property<*>>) {
    Confirm.ask(
        title = translated("oneconfig.group.menu.reset.title", "Reset %s?", tree.localizedTitle().asPlainLabel()),
        text = translated("oneconfig.group.menu.reset.text", "Every setting in this group goes back to its default. This can't be undone."),
        confirmLabel = translated("oneconfig.group.menu.reset.confirm", "Reset settings"),
        onConfirm = { resetTree(tree, identitySet().apply { addAll(skipped) }) },
        icon = "refresh",
    )
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
    Switch(
        checked,
        {
            checked = it
            head.set(it)
            onHeadChange(it)
        },
        enabled = enabled,
        stateLabel = false,
    )
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

    val requester = remember { BringIntoViewRequester() }
    val jumpTarget = LocalJumpHighlight.current?.target
    LaunchedEffect(jumpTarget) {
        if (jumpTarget === prop) requester.bringIntoView()
    }
    Box(Modifier.optionCard().bringIntoViewRequester(requester).jumpHighlightOf(prop)) {
        SettingContent(prop, compact = compact, enabled = display != Property.Display.DISABLED)
    }
}

/** The card behind an option or accordion */
@Composable
private fun Modifier.optionCard(): Modifier =
    if (LocalGroupedRows.current) fillMaxWidth() else fillMaxWidth().cardSurface(LocalTheme.current.modCardShape)

@Composable
private fun AccordionSettingRow(
    prop: Property<*>,
    display: Property.Display,
    groupEnabled: Boolean,
    modifier: Modifier,
    compact: Boolean = false,
) {
    val requester = remember { BringIntoViewRequester() }
    val jumpTarget = LocalJumpHighlight.current?.target
    LaunchedEffect(jumpTarget) {
        if (jumpTarget === prop) requester.bringIntoView()
    }
    Box(modifier.bringIntoViewRequester(requester).jumpHighlightOf(prop)) {
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

private val OPTION_ROW_END_PADDING = 8.dp

private val OPTION_CHROME_WIDTH = 108.dp
private val OPTION_ICON_WIDTH = 44.dp
private val OPTION_ACTION_WIDTH = ControlDefaults.metrics(ControlSize.Compact).height + 16.dp

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
private fun rememberTitleStyle(): TextStyle = LocalTheme.current.typography.headingMedium

@Composable
private fun descriptionStyle(): TextStyle = LocalTheme.current.typography.labelRegular

private fun Node.rowTitle(): Any =
    if (title == null && getMetadata<String>("titleKey").isNullOrBlank()) "" else localizedTitle()

private fun TextMeasurer.titleWidth(node: Node, style: TextStyle, density: Density): Dp {
    return with(density) {
        measure(node.rowTitle().asStyledLabel(), style = style, softWrap = false, maxLines = 1).size.width.toDp()
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
    val showActionButton = OneConfigConfig.showOptionActionButtons
    val changed = rememberChangedFromDefault(prop)

    val measurer = rememberTextMeasurer()
    val titleStyle = rememberTitleStyle()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(rowInteraction)
            .optionContextMenu(menu, enabled)
    ) {
        val actionButton = @Composable {
            if (showActionButton) OptionMoreButton(menu, hovered = isRowHovered, changed = changed, enabled = enabled)
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
                    .padding(
                        start = 16.dp,
                        end = if (showActionButton) OPTION_ROW_END_PADDING else 16.dp,
                        top = verticalPadding,
                        bottom = if (nested) 8.dp else verticalPadding,
                    ),
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
            val title = prop.rowTitle().asStyledLabel()
            val titleWidth = remember(title, titleStyle, measurer) {
                measurer.measure(title, style = titleStyle, softWrap = false, maxLines = 1).size.width
            }
            LabelledOptionRow(
                titleWidth = titleWidth,
                leading = if (prop.nodeIcon() != null) OPTION_ICON_WIDTH else 0.dp,
                stack = compact,
                endPadding = if (showActionButton) OPTION_ROW_END_PADDING else 16.dp,
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
    endPadding: Dp,
    verticalPadding: Dp,
    bottomPadding: Dp,
    modifier: Modifier,
    label: @Composable () -> Unit,
    actionButton: @Composable () -> Unit,
    control: @Composable () -> Unit,
) {
    Layout(listOf(label, actionButton, control), modifier) { (labels, actions, controls), constraints ->
        val side = 16.dp.roundToPx()
        val end = endPadding.roundToPx()
        val top = verticalPadding.roundToPx()
        val width = constraints.maxWidth
        val inner = (width - side - end).coerceAtLeast(0)
        val loose = Constraints(maxWidth = inner)
        val action = actions.firstOrNull()?.measure(loose)
        val actionGap = 12.dp.roundToPx()
        val actionSpace = action?.let { it.width + actionGap } ?: 0
        val ctrl = controls.firstOrNull()?.measure(Constraints(maxWidth = (inner - actionSpace).coerceAtLeast(0)))
        val ctrlWidth = ctrl?.width ?: 0
        val actionX = width - end - (action?.width ?: 0)
        val ctrlX = width - end - actionSpace - ctrlWidth

        val besideGap = 16.dp.roundToPx()
        val beside = inner - ctrlWidth - besideGap - actionSpace
        if (!stack && titleWidth + leading.roundToPx() <= beside) {
            val lbl = labels.first().measure(Constraints.fixedWidth(beside.coerceAtLeast(0)))
            // as tall as a dropdown even when the control is a smaller switch or checkbox
            val rowHeight = maxOf(lbl.height, action?.height ?: 0, ctrl?.height ?: 0, OptionControlHeight.roundToPx())
            layout(width, rowHeight + top * 2) {
                fun Placeable.center(x: Int) = place(x, top + (rowHeight - height) / 2)
                lbl.center(side)
                action?.center(actionX)
                ctrl?.center(ctrlX)
            }
        } else {
            val lbl = labels.first().measure(loose)
            val belowGap = 10.dp.roundToPx()
            val rowHeight = maxOf(action?.height ?: 0, ctrl?.height ?: 0)
            val rowTop = top + lbl.height + belowGap
            layout(width, rowTop + rowHeight + bottomPadding.roundToPx()) {
                lbl.place(side, top)
                ctrl?.place(ctrlX, rowTop + (rowHeight - ctrl.height) / 2)
                action?.place(actionX, rowTop + (rowHeight - action.height) / 2)
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
            val titleStyle = rememberTitleStyle()
            val title = @Composable {
                Text(prop.rowTitle().asStyledLabel(), color = theme.textColor, style = titleStyle)
            }
            val description = prop.localizedDescription()?.takeUnless { it.isEmptyText() }

            if (nested && description != null) {
                TooltipBox(description.asStyledLabel(), maxWidth = TooltipDefaults.descriptionMaxWidth) {
                    title()
                }
            } else {
                title()
            }

            if (!nested) {
                description?.let { Text(it.asStyledLabel(), color = theme.textColorSecondary, style = descriptionStyle()) }
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

/** One option laid out the way [HudConfigScreen] lays out the options of a HUD */
@Composable
fun HudOptionRow(prop: Property<*>) {
    CompositionLocalProvider(LocalOptionWidth provides 220.dp) {
        SettingRow(prop, compact = isWideControl(prop))
    }
}

@Composable
private fun HudConfigScreenContent(tree: Tree, searchQuery: String) {
    val categories = remember(tree) { buildCategories(tree) }
    HudCategoryList(categories, searchQuery)
}

/**
 * The category chips and rows of [categories], laid out the way the HUD editor lays out the options of a HUD
 *
 * [row] draws one entry, which a caller replaces to draw its own options
 */
@Composable
internal fun HudCategoryList(
    categories: List<CategoryGroup>,
    searchQuery: String,
    row: @Composable (ConfigListEntry) -> Unit = { ConfigListRow(it, ::hasWideControl) },
) {
    val revision = rememberDisplayRevision(categories)
    val visibleCategories = remember(categories, revision) { categories.filter { filterHiddenNodes(it) != null } }
    var chosenCategory by remember(categories) { mutableStateOf<CategoryGroup?>(null) }
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

    CompositionLocalProvider(
        LocalOptionWidth provides 210.dp,
        LocalOptionWidthOrNull provides 210.dp,
        LocalSearchExpands provides searchQuery.isNotEmpty(),
    ) {
    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        if (searchQuery.isEmpty() && visibleCategories.size > 1) {
            CategoryTabs(visibleCategories, selectedCategory, Modifier) { chosenCategory = it }
        }

        val rows = remember(entries) { groupSections(entries, "hud/${selectedCategory?.name.orEmpty()}") }
        val searching = searchQuery.isNotEmpty()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEachIndexed { index, pageRow ->
                when (pageRow) {
                    is PageRow.Heading -> row(pageRow.entry)
                    is PageRow.Section -> SectionBlock(pageRow, first = index == 0, collapsible = !searching, dimmed = false) { row(it) }
                }
            }
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
