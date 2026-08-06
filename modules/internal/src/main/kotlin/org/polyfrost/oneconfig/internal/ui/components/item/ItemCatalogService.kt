package org.polyfrost.oneconfig.internal.ui.components.item

import java.util.Locale
import java.util.ServiceLoader

/** A stable item registry ID and the localized name shown to the user. */
data class ItemDescriptor(val id: String, val displayName: String)

/** A small ARGB image supplied by the Minecraft integration for an item preview. */
data class ItemIconData(val width: Int, val height: Int, val argb: IntArray)

/** Supplies item registry data without exposing Minecraft classes to the UI module. */
interface ItemCatalogService {
    fun items(): List<ItemDescriptor>

    fun icon(id: String): ItemIconData?

    /** Resolves an icon that may require rendering on the Minecraft render thread. */
    fun loadIcon(id: String, onLoaded: (ItemIconData?) -> Unit) {
        onLoaded(icon(id))
    }
}

object ItemCatalog {
    @Volatile
    private var overrideService: ItemCatalogService? = null

    private val loadedService: ItemCatalogService? by lazy {
        runCatching {
            ServiceLoader.load(ItemCatalogService::class.java, ItemCatalogService::class.java.classLoader)
                .iterator()
                .let { if (it.hasNext()) it.next() else null }
        }.getOrNull()
    }

    private val service: ItemCatalogService? get() = overrideService ?: loadedService

    fun items(): List<ItemDescriptor> = service?.items().orEmpty()

    fun icon(id: String): ItemIconData? = service?.icon(id)

    fun loadIcon(id: String, onLoaded: (ItemIconData?) -> Unit) {
        service?.loadIcon(id, onLoaded) ?: onLoaded(null)
    }

    /** Installs an in-memory catalog for desktop previews and tests. */
    fun installOverride(service: ItemCatalogService?) {
        overrideService = service
    }
}

fun normalizeItemIds(ids: Iterable<String>): List<String> = ids
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinct()

fun filterItems(items: List<ItemDescriptor>, query: String): List<ItemDescriptor> {
    val terms = query.trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter(String::isNotEmpty)
    if (terms.isEmpty()) return items
    return items.filter { item ->
        val haystack = "${item.displayName.lowercase(Locale.ROOT)} ${item.id.lowercase(Locale.ROOT)}"
        terms.all(haystack::contains)
    }
}

fun toggleItem(
    selected: List<String>,
    id: String,
    maxEntries: Int,
): List<String> {
    val normalized = normalizeItemIds(selected)
    if (id in normalized) return normalized - id
    if (maxEntries == 1) return listOf(id)
    if (maxEntries > 0 && normalized.size >= maxEntries) return normalized
    return normalized + id
}
