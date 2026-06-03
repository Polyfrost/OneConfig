package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.internal.ui.components.Chip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.Option
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
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
    data class SubcategoryHeader(val title: String) : ConfigListEntry
    data class Item(val node: SettingNode) : ConfigListEntry
}

@Composable
fun ConfigScreen(tree: Tree, initialCategory: String? = null) {
    val categories = remember(tree) { buildCategories(tree) }
    var selectedCategory by remember(tree, initialCategory) {
        mutableStateOf(
            categories.firstOrNull { it.name.equals(initialCategory, ignoreCase = true) }
                ?: categories.firstOrNull()
        )
    }

    LaunchedEffect(categories, initialCategory) {
        if (selectedCategory == null || selectedCategory !in categories) {
            selectedCategory = categories.firstOrNull { it.name.equals(initialCategory, ignoreCase = true) }
                ?: categories.firstOrNull()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        if (categories.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    Chip(
                        label = category.name,
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }
        }

        val entries = remember(selectedCategory) {
            selectedCategory?.let(::flattenEntries).orEmpty()
        }

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No settings available.", color = LocalTheme.current.textColorSecondary)
            }
            return@Column
        }

        val lazyListState = rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                items(entries) { entry ->
                    when (entry) {
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
    return (prop.getMetadata<Any?>("visualizer") != null) || prop.canDisplay()
}

private fun nodeGroup(node: Node, key: String, default: String): String {
    val value = node.getMetadata<String>(key)?.trim()
    return value?.takeUnless(String::isEmpty) ?: default
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
private fun AccordionRow(node: SettingNode.Accordion) {
    val theme = LocalTheme.current
    val shape = theme.modCardShape

    var expanded by remember(node) { mutableStateOf(node.head?.get() != false) }
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
                        node.tree.title ?: node.tree.id ?: "",
                        color = theme.textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    node.tree.description?.let {
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
                    LaunchedEffect(headChecked) {
                        node.head.set(headChecked)
                        expanded = headChecked
                    }
                    SwitchControl(headChecked) { headChecked = it }
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2,
                ) {
                    node.body.forEach { prop ->
                        AccordionSettingRow(prop)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingRow(prop: Property<*>) {
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
        SettingContent(prop)
    }
}

@Composable
private fun FlowRowScope.AccordionSettingRow(prop: Property<*>) {
    val display = rememberDisplay(prop)
    if (display == Property.Display.HIDDEN) {
        return
    }

    Box(Modifier.weight(1f).alpha(displayAlpha(display))) {
        SettingContent(prop, nested = true)
    }
}

@Composable
private fun rememberDisplay(prop: Property<*>): Property.Display {
    var display by remember(prop) {
        mutableStateOf(if (prop.canDisplay()) Property.Display.SHOWN else Property.Display.HIDDEN)
    }

    DisposableEffect(prop) {
        prop.onDisplayChange { display = it }
        onDispose { prop.onDisplayChange(null) }
    }

    return display
}

private fun displayAlpha(display: Property.Display): Float {
    return if (display == Property.Display.DISABLED) 0.65f else 1f
}

@Composable
private fun SettingContent(prop: Property<*>, nested: Boolean = false) {
    val theme = LocalTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            prop.getMetadata<String>("icon")?.let {
                Icon(it, color = theme.textColor, modifier = Modifier.size(32.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    prop.title ?: prop.id ?: "",
                    color = theme.textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (!nested) {
                    prop.description?.let {
                        Text(it, color = theme.textColorSecondary, fontSize = 13.sp)
                    }
                }
            }
        }

        Option(prop)
    }
}

@Composable
fun HudConfigScreen(tree: Tree, initialCategory: String? = null) {
    val filteredTree = remember(tree) {
        tree
    }
    val categories = remember(filteredTree) { buildHudCategories(filteredTree) }
    var selectedCategory by remember(filteredTree, initialCategory) {
        mutableStateOf(
            categories.firstOrNull { it.name.equals(initialCategory, ignoreCase = true) }
                ?: categories.firstOrNull()
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(19.dp)) {
        if (categories.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category ->
                    Chip(
                        label = category.name,
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category }
                    )
                }
            }
        }

        val entries = remember(selectedCategory) {
            selectedCategory?.let(::flattenEntries).orEmpty()
        }

        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No settings available.", color = LocalTheme.current.textColorSecondary)
            }
            return@Column
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { entry ->
                when (entry) {
                    is ConfigListEntry.SubcategoryHeader -> SubcategoryHeader(entry.title)
                    is ConfigListEntry.Item -> when (val node = entry.node) {
                        is SettingNode.Leaf -> SettingRow(node.prop)
                        is SettingNode.Accordion -> AccordionRow(node)
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

