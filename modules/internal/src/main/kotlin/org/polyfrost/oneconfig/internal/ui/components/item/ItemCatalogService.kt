package org.polyfrost.oneconfig.internal.ui.components.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
import java.util.Locale
import java.util.ServiceLoader

/** A stable item registry ID and the localized name shown to the user */
data class ItemDescriptor(val id: String, val displayName: String)

interface ItemCatalogService {
    fun items(): List<ItemDescriptor>

    fun requestIcon(id: String, onLoaded: (Boolean) -> Unit)

    fun drawIcon(
        id: String,
        canvas: Canvas,
        bounds: Rect,
        alpha: Float = 1f,
    ): Boolean
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
    private val iconRevision = mutableStateOf(0L)

    internal val currentIconRevision: Long get() = iconRevision.value

    fun items(): List<ItemDescriptor> = service?.items().orEmpty()

    fun requestIcon(id: String, onLoaded: (Boolean) -> Unit) {
        service?.requestIcon(id, onLoaded) ?: onLoaded(false)
    }

    fun drawIcon(
        id: String,
        canvas: Canvas,
        bounds: Rect,
        alpha: Float = 1f,
    ): Boolean = service?.drawIcon(id, canvas, bounds, alpha) == true

    fun invalidateIcons() {
        Snapshot.withMutableSnapshot {
            iconRevision.value++
        }
    }

    /** Installs an in-memory catalog for desktop previews and tests */
    fun installOverride(service: ItemCatalogService?) {
        overrideService = service
        invalidateIcons()
    }
}

@Composable
fun rememberItemIconReady(id: String): Boolean {
    val revision = ItemCatalog.currentIconRevision
    var ready by remember(id, revision) { mutableStateOf(false) }
    DisposableEffect(id, revision) {
        val lock = Any()
        var active = true
        ItemCatalog.requestIcon(id) { loaded ->
            synchronized(lock) {
                if (active) ready = loaded
            }
        }
        onDispose {
            synchronized(lock) {
                active = false
            }
        }
    }
    return ready
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
