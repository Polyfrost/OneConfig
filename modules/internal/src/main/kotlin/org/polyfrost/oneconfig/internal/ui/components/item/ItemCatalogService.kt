package org.polyfrost.oneconfig.internal.ui.components.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import java.util.Locale
import java.util.ServiceLoader
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Rect
import org.polyfrost.oneconfig.api.hud.v1.LocalHud

/** A stable item registry ID and the localized name shown to the user */
data class ItemDescriptor(val id: String, val displayName: String)

/** An item icon registered with the renderer until [close] is called. */
interface ItemIconHandle : AutoCloseable {
    /** Sets the pixel size of this icon's square atlas entry */
    fun setRenderSizePx(size: Int)

    /** Returns whether the icon was rendered. */
    fun draw(canvas: Canvas, bounds: Rect, alpha: Float = 1f): Boolean

    /** Unregisters this icon from the renderer. */
    override fun close()
}

interface ItemCatalogService {
    fun items(): List<ItemDescriptor>

    fun requestIcons() = Unit

    /** HUD consumers request a HUD redraw when their icon's cached pixels change. */
    fun openIcon(id: String, forHud: Boolean = false): ItemIconHandle?

    /** Renders icons needed by screen consumers and returns whether their pixels changed this frame. */
    fun renderIcons(): Boolean = false

    /** Renders icons needed by HUD consumers before the HUD dirty gate. */
    fun renderHudIcons() = Unit
}

object ItemCatalog {
    private val service: ItemCatalogService? by lazy {
        runCatching {
            ServiceLoader.load(ItemCatalogService::class.java, ItemCatalogService::class.java.classLoader)
                .iterator()
                .let { if (it.hasNext()) it.next() else null }
        }.getOrNull()
    }

    private val _iconsAvailable = mutableStateOf(false)

    val iconsAvailable: Boolean get() = _iconsAvailable.value

    fun platformService(): ItemCatalogService? = service

    fun items(): List<ItemDescriptor> = service?.items().orEmpty()

    fun requestIcons() {
        if (!iconsAvailable) service?.requestIcons()
    }

    fun openIcon(id: String, forHud: Boolean = false): ItemIconHandle? =
        if (iconsAvailable) service?.openIcon(id, forHud) else null

    fun renderIcons(): Boolean = iconsAvailable && service?.renderIcons() == true

    fun renderHudIcons() {
        if (iconsAvailable) service?.renderHudIcons()
    }

    fun markIconsAvailable() {
        Snapshot.withMutableSnapshot { _iconsAvailable.value = true }
    }
}

@Composable
fun rememberItemIconHandle(id: String): ItemIconHandle? =
    rememberItemIconHandle(id) { forHud -> ItemCatalog.openIcon(id, forHud) }

/** Holds an icon handle open while icons are available and the enclosing HUD, if any, is visible. */
@Composable
fun <T : ItemIconHandle> rememberItemIconHandle(key: Any?, open: (forHud: Boolean) -> T?): T? {
    val hud = LocalHud.current
    val visible = hud == null || hud.isVisible.value
    val available = ItemCatalog.iconsAvailable
    SideEffect {
        if (visible && !available) ItemCatalog.requestIcons()
    }
    val icon = remember(key, visible, available, hud) {
        if (visible && available) open(hud != null) else null
    }
    DisposableEffect(icon) {
        onDispose { icon?.close() }
    }
    return icon
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
