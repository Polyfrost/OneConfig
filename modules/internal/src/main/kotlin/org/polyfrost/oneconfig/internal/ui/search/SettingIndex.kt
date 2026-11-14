package org.polyfrost.oneconfig.internal.ui.search

import java.util.IdentityHashMap
import org.polyfrost.oneconfig.api.config.v1.MetaKeys
import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.enableToggle
import org.polyfrost.oneconfig.api.config.v1.get
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.MASTER_TOGGLE
import org.polyfrost.oneconfig.internal.ui.api.TreeConfigData
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.localizedGroup
import org.polyfrost.oneconfig.internal.ui.components.localizedTitle

internal const val PRIMARY = "primary"
internal const val SUBMENU = "submenu"

/**
 * One row of a settings list being a single property, an accordion with the body it expands to or a submenu
 * opening its options on a page of their own
 *
 * [Submenu.path] is the ids of the submenus down to it joined by "/"
 */
internal sealed interface SettingNode {
    data class Leaf(val prop: Property<*>) : SettingNode
    data class Accordion(val tree: Tree, val head: Property<*>?, val body: List<Property<*>>) : SettingNode
    data class Submenu(val tree: Tree, val head: Property<*>?, val path: String, val content: List<CategoryGroup>) : SettingNode
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
 * Splits [tree] into the rows a settings screen renders grouped by category and subcategory
 *
 * [path] is where [tree] sits among the submenus and [include] can filter the elements
 */
internal fun buildCategories(tree: Tree, path: String? = null, include: (Node) -> Boolean = { true }): List<CategoryGroup> {
    val grouped = LinkedHashMap<String, LinkedHashMap<String, MutableList<SettingNode>>>()
    tree.map.values.forEach { node ->
        if (!include(node)) return@forEach

        val category = nodeGroup(node, MetaKeys.CATEGORY.name, ConfigVisualizer.DEFAULT_CATEGORY)
        val subcategory = nodeGroup(node, MetaKeys.SUBCATEGORY.name, ConfigVisualizer.DEFAULT_SUBCATEGORY)
        val bucket = grouped.getOrPut(category) { LinkedHashMap() }.getOrPut(subcategory) { ArrayList() }

        when (node) {
            is Property<*> -> {
                if (isRenderableProperty(node)) {
                    bucket += SettingNode.Leaf(node)
                }
            }
            is Tree -> {
                val row = if (node.isSubmenu()) buildSubmenuNode(node, path) else buildAccordionNode(node)
                row?.let(bucket::add)
            }
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
    val head = groupHead(tree)
    val body = tree.map.values
        .filterIsInstance<Property<*>>()
        .filter { it !== head }
        .filter(::isRenderableProperty)

    if (body.isEmpty() && head == null) {
        return null
    }

    return SettingNode.Accordion(tree, head, body)
}

private fun buildSubmenuNode(tree: Tree, parentPath: String?): SettingNode.Submenu? {
    val path = if (parentPath == null) tree.id else "$parentPath/${tree.id}"
    val content = submenuContent(tree, path)
    val head = groupHead(tree)
    if (content.isEmpty() && head == null) {
        return null
    }

    return SettingNode.Submenu(tree, head, path, content)
}

/** The option shown on the header of [tree] which is the marked primary or else the toggle of an old style accordion */
internal fun groupHead(tree: Tree): Property<*>? {
    tree.enableToggle?.let { return it }
    val properties = tree.map.values.filterIsInstance<Property<*>>()
    return properties.firstOrNull { it.getMetadata<Boolean>(PRIMARY) == true }
        ?: properties.firstOrNull(::isAccordionToggle)
}

private fun isAccordionToggle(prop: Property<*>): Boolean {
    return prop.isBooleanHead() && !prop.hasVisualizer() && prop.getMetadata<Any?>(MASTER_TOGGLE) == null
}

internal fun Property<*>.isBooleanHead(): Boolean = type == Boolean::class.java || type == Boolean::class.javaPrimitiveType

internal fun Property<*>.hasVisualizer(): Boolean = this[MetaKeys.VISUALIZER] != null

internal fun Tree.isSubmenu(): Boolean = getMetadata<Boolean>(SUBMENU) == true

/** The submenus from this tree down to the one at [path] or null when a part of it is missing or no submenu */
internal fun Tree.submenuTrail(path: String): List<Tree>? {
    val trail = ArrayList<Tree>()
    var current = this
    for (id in path.split('/')) {
        current = (current.get(id) as? Tree)?.takeIf { it.isSubmenu() } ?: return null
        trail += current
    }
    return trail
}

/** The rows of the page of the submenu [tree] without its own head which sits on the row opening it */
internal fun submenuContent(tree: Tree, path: String?): List<CategoryGroup> {
    val head = groupHead(tree)
    return buildCategories(tree, path) { it !== head }
}

internal fun isRenderableProperty(prop: Property<*>): Boolean {
    if (prop.getMetadata<Any?>(MetaKeys.HIDDEN.name).let { it != null && it != false }) return false
    return prop.hasVisualizer()
}

internal fun nodeGroup(node: Node, key: String, default: String): String {
    return node.localizedGroup(key, "${key}Key", default)
}

internal fun Property<*>.isHidden(): Boolean = display == Property.Display.HIDDEN

private fun Property<*>.isUnlisted(): Boolean = isHidden() || getMetadata<Any?>("keybindsPageOnly") == true

internal fun filterHiddenNodes(category: CategoryGroup): CategoryGroup? {
    val subcategories = category.subcategories.mapNotNull { subcategory ->
        val nodes = subcategory.nodes.filter { node ->
            when (node) {
                is SettingNode.Leaf -> !node.prop.isUnlisted()
                is SettingNode.Accordion -> when {
                    node.head != null && !node.head.hasVisualizer() -> !node.head.isUnlisted()
                    else -> node.head?.isUnlisted() == false || node.body.any { !it.isUnlisted() }
                }
                is SettingNode.Submenu -> node.head?.isUnlisted() == false || node.content.any { filterHiddenNodes(it) != null }
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
    /**
     * How this row is headed inside its mod
     *
     * The titles of the submenus holding it or else the category or the subcategory when the config has one
     * category or null when it has one of each
     */
    val groupLabel: String?,
    val node: SettingNode,
    val mod: ConfigData? = null,
)

/**
 * Maps every node the corpus can return back to the row which renders it
 *
 * [trail] is the titles of the submenus [categories] sit in and labels the options inside them
 */
internal fun buildSearchIndex(
    categories: List<CategoryGroup>,
    modTitle: String? = null,
    mod: ConfigData? = null,
    trail: String? = null,
): Map<Node, SearchRow> {
    val owners = IdentityHashMap<Node, SearchRow>()
    val singleCategory = categories.size == 1
    categories.forEach { category ->
        category.subcategories.forEach { subcategory ->
            val groupLabel = when {
                trail != null -> trail
                !singleCategory -> category.name
                category.subcategories.size > 1 -> subcategory.name
                else -> null
            }
            val subcategoryLabel = when {
                trail == null -> subcategory.name
                subcategory.name == ConfigVisualizer.DEFAULT_SUBCATEGORY -> trail
                else -> "$trail / ${subcategory.name}"
            }
            subcategory.nodes.forEach { node ->
                val row = SearchRow(modTitle, category.name, subcategoryLabel, groupLabel, node, mod)
                when (node) {
                    is SettingNode.Leaf -> owners[node.prop] = row
                    is SettingNode.Accordion -> {
                        owners[node.tree] = row
                        node.head?.let { owners[it] = row }
                        node.body.forEach { owners[it] = row }
                    }
                    is SettingNode.Submenu -> {
                        owners[node.tree] = row
                        node.head?.let { owners[it] = row }
                        // the options inside are listed under the category of the row opening them
                        val inside = node.content.map { it.copy(name = category.name) }
                        val insideTrail = listOfNotNull(trail, node.tree.localizedTitle().asRenderText()).joinToString(" / ")
                        owners.putAll(buildSearchIndex(inside, modTitle, mod, insideTrail))
                    }
                }
            }
        }
    }
    return owners
}

/** Narrows one row to what its hits matched handling accordions and hidden options, a hidden switch head hiding its accordion */
internal fun searchNode(node: SettingNode, documents: List<SearchDocument<*>>): SettingNode? {
    return when (node) {
        is SettingNode.Leaf -> node.takeUnless { it.prop.isHidden() }
        is SettingNode.Accordion -> {
            if (node.head?.let { !it.hasVisualizer() && it.isHidden() } == true) return null
            val whole = documents.any { it.payload === node.tree || it.payload === node.head }
            val body = if (whole) node.body else documents.mapNotNull { it.payload as? Property<*> }
            val visible = body.any { !it.isHidden() } || (whole && node.head?.isHidden() == false)
            if (visible) node.copy(body = body) else null
        }
        is SettingNode.Submenu -> node.takeIf { documents.isNotEmpty() }
    }
}

/** The node to row index across every searchable config for screens searching outside a single config */
internal object GlobalSettingIndex {
    @Volatile
    private var rows: Map<Node, SearchRow> = emptyMap()

    /** The row which renders [document]'s payload or null */
    fun rowOf(document: SearchDocument<*>): SearchRow? = (document.payload as? Node)?.let(rows::get)

    fun rebuild() {
        val built = IdentityHashMap<Node, SearchRow>()
        ConfigRegistry.configs.toList().forEach { config ->
            if (!ConfigRegistry.shouldShowInSearch(config)) return@forEach
            val tree = (config as? TreeConfigData)?.tree ?: return@forEach
            built += buildSearchIndex(buildCategories(tree), config.title.asRenderText(), config)
        }
        rows = built
    }
}
