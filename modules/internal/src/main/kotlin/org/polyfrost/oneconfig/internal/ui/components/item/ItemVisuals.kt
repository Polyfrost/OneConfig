package org.polyfrost.oneconfig.internal.ui.components.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.ceil
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.layout.onPlaced
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerTileIcon
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.components.sizeDp

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
    val density = LocalDensity.current.density
    val background = if (framed) Modifier.background(theme.chipBackground, ItemIconShape) else Modifier
    Box(
        modifier = modifier
            .clip(ItemIconShape)
            .then(background),
        contentAlignment = Alignment.Center,
    ) {
        if (itemIcon != null) {
            val inset = if (framed) 2f else 1f
            // Register at placement so icons revealed by a scroll are in the atlas before this frame draws.
            Canvas(modifier = Modifier.fillMaxSize().onPlaced { node ->
                itemIcon.setRenderSizePx(ceil((maxOf(node.width, node.height) - 2 * inset) * density).toInt())
            }) { x, y, w, h ->
                itemIcon.draw(this, x + inset, y + inset, w - 2 * inset, h - 2 * inset, alpha)
            }
        } else {
            val padding = if (framed) 6.dp else 5.dp
            Icon(
                placeholder,
                modifier = Modifier.size(modifier.sizeDp().dp - padding * 2),
                color = theme.textColorSecondary.copy(alpha = theme.textColorSecondary.alphaF * alpha),
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
