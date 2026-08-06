package org.polyfrost.oneconfig.internal.ui.search

import net.kyori.adventure.text.ComponentLike
import org.polyfrost.oneconfig.api.config.v1.Node
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.TreeConfigData
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.localizedDescription
import org.polyfrost.oneconfig.internal.ui.components.localizedGroup
import org.polyfrost.oneconfig.internal.ui.components.localizedLabel
import org.polyfrost.oneconfig.internal.ui.components.localizedTitle
import org.polyfrost.oneconfig.internal.ui.hud.HudModCardData
import org.polyfrost.oneconfig.internal.ui.hud.configForHud
import org.polyfrost.oneconfig.internal.ui.hud.hudModCardConfigs
import org.polyfrost.oneconfig.internal.ui.hud.modNameFor
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindProviderRegistry
import org.polyfrost.oneconfig.internal.ui.keybind.isKeybindProperty

private const val ID_SEPARATOR = "::"

/**
 * Add all properties of a mod, as well as the mod itself.
 */
object ConfigDocumentSource : SearchDocumentSource {
    override fun documents(): List<SearchDocument<*>> {
        val documents = ArrayList<SearchDocument<*>>()
        ConfigRegistry.configs.toList().forEach { config ->
            val searchable = ConfigRegistry.shouldShowInSearch(config)
            if (searchable && ConfigRegistry.shouldShowModCard(config)) {
                documents += modDocument(config)
            }

            val tree = (config as? TreeConfigData)?.tree ?: return@forEach
            val scopes = mutableSetOf<SearchScope>(SearchScope.Config(config.id))
            if (searchable) scopes += SearchScope.Options // Global search

            documents += treeDocuments(
                tree = tree,
                ownerId = config.id,
                modTitle = config.title.asRenderText(),
                modDescription = config.description?.asRenderText(),
                scopes = scopes,
            )
        }
        return documents
    }
}

/**
 * Keybinds from outside OneConfig, like MC keybinds
 */
object KeybindDocumentSource : SearchDocumentSource {
    override fun documents(): List<SearchDocument<Property<*>>> {

        return KeybindProviderRegistry.groups().flatMap { group ->
            val modTitle = group.modTitle.asRenderText()
            group.entries.map { entry ->
                SearchDocument(
                    id = "${group.modId}$ID_SEPARATOR${entry.path}",
                    scopes = setOf(SearchScope.Keybinds),
                    metadata = SearchMetadata(
                        title = entry.prop.localizedTitle().asRenderText().takeIf { it.isNotBlank() },
                        id = entry.prop.id?.takeIf { it.isNotBlank() },
                        description = entry.prop.localizedDescription()?.asRenderText()?.takeIf { it.isNotBlank() },
                        section = null,
                        category = entry.category.asRenderText().takeIf { it.isNotBlank() },
                        subcategory = entry.subcategory.asRenderText().takeIf { it.isNotBlank() },
                        modTitle = modTitle.takeIf { it.isNotBlank() },
                        path = entry.path.takeIf { it.isNotBlank() },
                    ),
                    payload = entry.prop,
                )
            }
        }
    }
}

private fun modDocument(config: ConfigData): SearchDocument<ConfigData> {
    val tree = (config as? TreeConfigData)?.tree
    return SearchDocument(
        id = "mod$ID_SEPARATOR${config.id}",
        scopes = setOf(SearchScope.Mods),
        metadata = SearchMetadata(
            title = config.title.asRenderText().takeIf { it.isNotBlank() },
            id = config.id,
            description = config.description?.asRenderText()?.takeIf { it.isNotBlank() },
            category = localizedLabel(config.category.name)?.takeIf { it.isNotBlank() },
            subcategory = tree?.subcategory?.asRenderText()?.takeIf { it.isNotBlank() },
        ),
        payload = config,
    )
}

private fun treeDocuments(
    tree: Tree,
    ownerId: String,
    modTitle: String?,
    modDescription: String?,
    scopes: Set<SearchScope>,
    include: (Node) -> Boolean = { true },
): List<SearchDocument<Node>> {
    val documents = ArrayList<SearchDocument<Node>>()

    fun walk(node: Node, path: String, category: String, subcategory: String, section: String?) {
        if (!include(node)) return
        if (node.getMetadata<Any?>("hidden") != null) return

        val documentScopes = if (node is Property<*> && node.isKeybindProperty()) scopes + SearchScope.Keybinds
        else scopes

        val title = node.localizedTitle().asRenderText().takeIf { it.isNotBlank() }
        val nodeCategory = node.localizedGroup("category", "categoryKey", category)
        val nodeSubcategory = node.localizedGroup("subcategory", "subcategoryKey", subcategory)
        val searchTags = node.metadata?.get("searchTags")?.let {
            if (it is Iterable<*>) it.mapNotNull {
                if (it !is String && it !is ComponentLike) return@mapNotNull null
                it.asRenderText()
            } else if (it is String) listOf(it) else listOf()
        } ?: emptyList()
        documents += SearchDocument(
            id = "$ownerId$ID_SEPARATOR$path",
            scopes = documentScopes,
            metadata = SearchMetadata(
                title = title,
                id = node.id?.takeIf { it.isNotBlank() },
                description = node.localizedDescription()?.asRenderText()?.takeIf { it.isNotBlank() },
                section = section,
                category = nodeCategory.takeIf { it.isNotBlank() },
                subcategory = nodeSubcategory.takeIf { it.isNotBlank() },
                modTitle = modTitle?.takeIf { it.isNotBlank() },
                modDescription = modDescription?.takeIf { it.isNotBlank() },
                tags = searchTags,
            ),
            payload = node,
        )

        if (node is Tree) {
            node.map.forEach { (id, child) ->
                walk(child, "$path.$id", nodeCategory, nodeSubcategory, title)
            }
        }
    }

    tree.map.forEach { (id, node) ->
        walk(node, id, ConfigVisualizer.DEFAULT_CATEGORY, ConfigVisualizer.DEFAULT_SUBCATEGORY, null)
    }
    return documents
}

object HudDocumentSource : SearchDocumentSource {
    /** Hud documents use the title and description of the config owning the hud */
    override val dependencies = setOf<SearchDocumentSource>(ConfigDocumentSource)

    init {
        HudManager.addRegistrationListener { SearchCorpus.invalidate(HudDocumentSource) }
    }

    override fun documents(): List<SearchDocument<Hud>> = HudManager.providers().toList().map { hud ->
        val config = hud.configId?.let(::configForHud)
        return@map SearchDocument(
            id = "hud$ID_SEPARATOR${hud::class.java.name}$ID_SEPARATOR${hud.id}",
            scopes = setOf(SearchScope.Huds),
            metadata = SearchMetadata(
                title = localizedLabel(hud.title)?.takeIf { it.isNotBlank() },
                description = localizedLabel(hud.description)?.takeIf { it.isNotBlank() },
                id = hud.id.takeIf { it.isNotBlank() },
                category = localizedLabel(hud.category.name)?.takeIf { it.isNotBlank() },
                tags = hud.searchTags.mapNotNull { localizedLabel(it)?.takeIf { l -> l.isNotEmpty() } },
                modTitle = hud.configId?.let { modNameFor(it) ?: it }?.takeIf { it.isNotBlank() },
                modDescription = config?.description?.asRenderText()?.takeIf { it.isNotBlank() },
            ),
            payload = hud,
        )
    }
}

/**
 * The HUDs shown as mod cards on the config screen
 */
object HudModCardDocumentSource : SearchDocumentSource {
    override val dependencies = setOf<SearchDocumentSource>(ConfigDocumentSource)

    init {
        HudManager.addRegistrationListener { SearchCorpus.invalidate(HudModCardDocumentSource) }
    }

    override fun documents(): List<SearchDocument<ConfigData>> = hudModCardConfigs().mapNotNull { card ->
        val hud = (card as? HudModCardData)?.hud ?: return@mapNotNull null
        SearchDocument(
            id = "mod$ID_SEPARATOR${card.id}",
            scopes = setOf(SearchScope.Mods),
            metadata = SearchMetadata(
                title = card.title.asRenderText().takeIf { it.isNotBlank() },
                id = hud.id.takeIf { it.isNotBlank() },
                description = localizedLabel(hud.description)?.takeIf { it.isNotBlank() },
                category = localizedLabel(hud.category.name)?.takeIf { it.isNotBlank() },
                tags = hud.searchTags.mapNotNull { localizedLabel(it)?.takeIf { l -> l.isNotBlank() } },
                modTitle = hud.configId?.let { modNameFor(it) ?: it }?.takeIf { it.isNotBlank() },
                modDescription = card.owner?.description?.asRenderText()?.takeIf { it.isNotBlank() },
            ),
            payload = card,
        )
    }
}