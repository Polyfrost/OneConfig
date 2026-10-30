package org.polyfrost.oneconfig.internal.ui.components.item

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import org.polyfrost.compose.backend.drawPoly
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerTileIcon
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

val ItemIconShape @Composable get() = LocalTheme.current.checkBoxShape

internal val ItemTileIcon: PickerTileIcon<ItemDescriptor> = { item, modifier, alpha ->
    ItemIcon(item.id, modifier, framed = false, alpha = alpha)
}

fun polyItemRenderSizePx(size: Float, hud: Hud?): Int =
    ceil(size * Platform.compatibility().options().guiScale * (hud?.effectiveScale ?: 1f)).toInt()

/** The catalog deduplicated and sorted by display name so grid positions never jump */
@Composable
fun rememberItemCatalog(key: Any? = Unit): List<ItemDescriptor> = remember(key) {
    ItemCatalog.items()
        .distinctBy(ItemDescriptor::id)
        .sortedWith { first, second ->
            first.displayName.compareTo(second.displayName, ignoreCase = true)
                .takeIf { it != 0 } ?: first.id.compareTo(second.id)
        }
}

/**
 * The icon for [id] showing [placeholder] until the catalog resolves it or if the item is unknown
 *
 * [framed] adds the theme chip background
 */
@Composable
fun ItemIcon(
    id: String,
    modifier: Modifier = Modifier,
    framed: Boolean = true,
    alpha: Float = 1f,
    placeholder: String = "eye-off",
) = ItemIcon(rememberItemIconHandle(id), modifier, framed, alpha, placeholder)

/** [itemIcon] drawn like [ItemIcon], or [placeholder] while it is `null` */
@Composable
fun ItemIcon(
    itemIcon: ItemIconHandle?,
    modifier: Modifier = Modifier,
    framed: Boolean = true,
    alpha: Float = 1f,
    placeholder: String = "eye-off",
) {
    val theme = LocalTheme.current
    val background = if (framed) Modifier.background(theme.chipBackground) else Modifier
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(ItemIconShape)
            .then(background),
        contentAlignment = Alignment.Center,
    ) {
        if (itemIcon != null) {
            // Register at placement so icons revealed by a scroll are in the atlas before this frame draws.
            Canvas(modifier = Modifier.fillMaxSize().padding(if (framed) 2.dp else 1.dp).onPlaced { coords ->
                val surfaceScale = Platform.screen().surfaceRatio().coerceAtLeast(0.0001f)
                itemIcon.setRenderSizePx(ceil(maxOf(coords.size.width, coords.size.height) * surfaceScale).toInt())
            }) {
                val w = size.width
                val h = size.height
                drawPoly { itemIcon.draw(it, 0f, 0f, w, h, alpha) }
            }
        } else {
            Icon(
                placeholder,
                modifier = Modifier.fillMaxSize().padding(if (framed) 6.dp else 5.dp),
                color = theme.textColorSecondary.copy(alpha = alpha),
            )
        }
    }
}

/** Default translation keys where every composable taking one accepts an override */
object ItemStrings {
    const val Title = "oneconfig.itemlist.title"
    const val Search = "oneconfig.itemlist.search"
    const val Empty = "oneconfig.itemlist.empty"
    const val Unavailable = "oneconfig.itemlist.unavailable"
}
