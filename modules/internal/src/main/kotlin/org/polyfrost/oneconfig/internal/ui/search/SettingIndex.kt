package org.polyfrost.oneconfig.internal.ui.search

import java.util.IdentityHashMap
import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.TreeConfigData
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.localizedGroup

/** One row of a settings list: a single property, or an accordion together with the body it expands to. */
internal sealed interface SettingNode {
    data class Leaf(val prop: Property<*>) : SettingNode
    data class Accordion(val tree: Tree, val head: Property<Boolean>?, val body: List<Property<*>>) : SettingNode
}

internal data class CategoryGroup(
    val name: String,
    val subcategories: List<SubcategoryGroup>
)

internal data class SubcategoryGroup(
    val name: String,
    val nodes: List<SettingNode>
)

internal sealed interface ConfigListEntry {
    data class CategoryHeader(val title: String) : ConfigListEntry
    data class SubcategoryHeader(val title: String) : ConfigListEntry
    data class Item(val node: SettingNode) : ConfigListEntry
}

/**
 * Splits [tree] into the rows a settings screen renders, grouped by category and subcategory.
 * [include] can filter the elements.
 */
internal fun buildCategories(tree: Tree, include: (Node) -> Boolean = { true }): List<CategoryGroup> {
    val grouped = LinkedHashMap<String, LinkedHashMap<String, MutableList<SettingNode>>>()
    tree.map.values.forEach { node ->
        if (!include(node)) return@forEach

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

internal fun buildAccordionNode(tree: Tree): SettingNode.Accordion? {
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

internal fun isRenderableProperty(prop: Property<*>): Boolean {
    if (prop.getMetadata<Any?>("hidden") != null) return false
    return (prop.getMetadata<Any?>("visualizer") != null) || prop.canDisplay()
}

internal fun nodeGroup(node: Node, key: String, default: String): String {
    return node.localizedGroup(key, "${key}Key", default)
}

internal fun Property<*>.isHidden(): Boolean = display == Property.Display.HIDDEN

internal fun filterHiddenNodes(category: CategoryGroup): CategoryGroup? {
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

internal fun flattenEntries(category: CategoryGroup): List<ConfigListEntry> {
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

internal fun flattenSearchEntries(categories: List<CategoryGroup>): List<ConfigListEntry> {
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

internal class SearchRow(
    val modTitle: String?,
    val category: String,
    val subcategory: String,
    val node: SettingNode,
)

/**
 * Maps every node the corpus can return back to the row which renders it.
 */
internal fun buildSearchIndex(categories: List<CategoryGroup>, modTitle: String? = null): Map<Node, SearchRow> {
    val owners = IdentityHashMap<Node, SearchRow>()
    categories.forEach { category ->
        category.subcategories.forEach { subcategory ->
            subcategory.nodes.forEach { node ->
                val row = SearchRow(modTitle, category.name, subcategory.name, node)
                when (node) {
                    is SettingNode.Leaf -> owners[node.prop] = row
                    is SettingNode.Accordion -> {
                        owners[node.tree] = row
                        node.head?.let { owners[it] = row }
                        node.body.forEach { owners[it] = row }
                    }
                }
            }
        }
    }
    return owners
}

/**
 * Narrows one row to what its hits matched, handled accordions and hidden options
 */
internal fun searchNode(node: SettingNode, documents: List<SearchDocument<*>>): SettingNode? {
    return when (node) {
        is SettingNode.Leaf -> node.takeUnless { it.prop.isHidden() }
        is SettingNode.Accordion -> {
            val whole = documents.any { it.payload === node.tree || it.payload === node.head }
            val body = if (whole) node.body else documents.mapNotNull { it.payload as? Property<*> }
            val visible = body.any { !it.isHidden() } || (whole && node.head?.isHidden() == false)
            if (visible) node.copy(body = body) else null
        }
    }
}

/**
 * The node -> row index across every searchable config, for screens which search outside a single config.
 */
internal object GlobalSettingIndex {
    @Volatile
    private var rows: Map<Node, SearchRow> = emptyMap()

    /** The row which renders [document]'s payload, or null. */
    fun rowOf(document: SearchDocument<*>): SearchRow? = (document.payload as? Node)?.let(rows::get)

    fun rebuild() {
        val built = IdentityHashMap<Node, SearchRow>()
        ConfigRegistry.configs.toList().forEach { config ->
            if (!ConfigRegistry.shouldShowInSearch(config)) return@forEach
            val tree = (config as? TreeConfigData)?.tree ?: return@forEach
            built += buildSearchIndex(buildCategories(tree), config.title.asRenderText())
        }
        rows = built
    }
}
