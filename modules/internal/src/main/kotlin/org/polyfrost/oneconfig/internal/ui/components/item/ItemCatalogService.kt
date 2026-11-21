package org.polyfrost.oneconfig.internal.ui.components.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.oneconfig.api.hud.v1.LocalHud
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerEntry
import java.util.ServiceLoader

/** A stable item registry ID and the localized name shown to the user */
data class ItemDescriptor(override val id: String, override val displayName: String) : PickerEntry

/** An item icon registered with the renderer until [close] is called. */
interface ItemIconHandle : AutoCloseable {
    /** Returns whether the icon was rendered. */
    fun draw(ctx: RenderContext, x: Float, y: Float, w: Float, h: Float, alpha: Float = 1f): Boolean

    /** Unregisters this icon from the renderer. */
    override fun close()
}

interface ItemCatalogService {
    fun items(): List<ItemDescriptor>

    val iconsAvailable: Boolean

    fun requestIcons() = Unit

    fun openIcon(id: String): ItemIconHandle?
}

object ItemCatalog {
    private val service: ItemCatalogService? by lazy {
        runCatching {
            ServiceLoader.load(ItemCatalogService::class.java, ItemCatalogService::class.java.classLoader)
                .iterator()
                .let { if (it.hasNext()) it.next() else null }
        }.getOrNull()
    }

    val iconsAvailable: Boolean get() = service?.iconsAvailable == true

    fun items(): List<ItemDescriptor> = service?.items().orEmpty()

    fun requestIcons() {
        if (!iconsAvailable) service?.requestIcons()
    }

    fun openIcon(id: String): ItemIconHandle? = if (iconsAvailable) service?.openIcon(id) else null
}

@Composable
fun rememberItemIconHandle(id: String): ItemIconHandle? =
    rememberItemIconHandle(id) { ItemCatalog.openIcon(id) }

/** Holds an icon handle open while icons are available and the enclosing HUD, if any, is visible. */
@Composable
fun <T : ItemIconHandle> rememberItemIconHandle(key: Any?, open: () -> T?): T? {
    val hud = LocalHud.current
    val visible = hud == null || hud.isVisible.value
    val available = ItemCatalog.iconsAvailable
    SideEffect {
        if (visible && !available) ItemCatalog.requestIcons()
    }
    val icon = remember(key, visible, available, hud) {
        if (visible && available) open() else null
    }
    DisposableEffect(icon) {
        onDispose { icon?.close() }
    }
    return icon
}
