package org.polyfrost.oneconfig.internal.ui.components.item

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import org.jetbrains.skia.Rect
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerLabel
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerMessage
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerSearchField
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerTile
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerTileIcon
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerTileShape
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerTileSize
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerTooltip
import org.polyfrost.oneconfig.internal.ui.components.picker.TileGrid
import org.polyfrost.oneconfig.internal.ui.components.picker.TilePicker
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

val ItemTileShape = PickerTileShape
val ItemIconShape = RoundedCornerShape(5.dp)

val ItemTileSize = PickerTileSize

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

/** [rememberItemCatalog] indexed by registry ID for resolving stored IDs to descriptors */
@Composable
fun rememberItemCatalogById(catalog: List<ItemDescriptor>): Map<String, ItemDescriptor> =
    remember(catalog) { catalog.associateBy(ItemDescriptor::id) }

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
) {
    val itemIcon = rememberItemIconHandle(id)
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
                drawIntoCanvas { canvas ->
                    itemIcon.draw(
                        canvas.skiaCanvas,
                        Rect.makeWH(size.width, size.height),
                        alpha,
                    )
                }
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

/** Display name over registry ID where a `null` [item] is not in the catalog so [id] becomes the name */
@Composable
fun ItemLabel(
    id: String,
    item: ItemDescriptor?,
    modifier: Modifier = Modifier,
    nameColor: Color = LocalTheme.current.textColor,
    idColor: Color = LocalTheme.current.textColorSecondary,
    nameSize: TextUnit = 12.sp,
    idSize: TextUnit = 10.sp,
    unavailableKey: String = ItemStrings.Unavailable,
) = PickerLabel(id, item, modifier, nameColor, idColor, nameSize, idSize, unavailableKey)

/** A square item cell with selection border tick badge and tooltip where a `null` [onClick] is decorative */
@Composable
fun ItemTile(
    item: ItemDescriptor,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    tooltip: Boolean = true,
    onClick: (() -> Unit)? = null,
) = PickerTile(item, ItemTileIcon, modifier, selected, enabled, tooltip, onClick)

/** Wraps [content] in the name and ID tooltip or renders it bare when [enabled] is false */
@Composable
fun ItemTooltip(
    item: ItemDescriptor,
    enabled: Boolean = true,
    anchor: Alignment = Alignment.BottomCenter,
    content: @Composable () -> Unit,
) = PickerTooltip(item, enabled, anchor, content)

/** Adaptive grid of [ItemTile]s where [selected] and [enabled] are queried per item so selection rules stay with the caller */
@Composable
fun ItemGrid(
    items: List<ItemDescriptor>,
    modifier: Modifier = Modifier,
    selected: (ItemDescriptor) -> Boolean = { false },
    enabled: (ItemDescriptor) -> Boolean = { true },
    cellSize: Dp = ItemTileSize,
    maxHeight: Dp = 300.dp,
    spacing: Dp = 6.dp,
    gutter: Dp = 12.dp,
    onClick: ((ItemDescriptor) -> Unit)? = null,
) = TileGrid(items, ItemTileIcon, modifier, selected, enabled, cellSize, maxHeight, spacing, gutter, onClick)

/** Search box for [ItemGrid] where [autoFocus] is off by default for embedded use */
@Composable
fun ItemSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
    placeholderKey: String = ItemStrings.Search,
) = PickerSearchField(value, onValueChange, modifier, autoFocus, placeholderKey)

/**
 * Header search field and result grid making up the whole item selector for any HUD or screen
 *
 * The [modifier] supplies the chrome, minus [gutter] side padding, so it fits a dropdown a HUD editor panel
 * or a full screen
 *
 * [selected] and [onToggle] leave the selection with the caller
 *
 * [maxEntries] of `0` or less is unlimited and `1` is radio-style
 */
@Composable
fun ItemPicker(
    items: List<ItemDescriptor>,
    selected: List<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxEntries: Int = 0,
    onClose: (() -> Unit)? = null,
    autoFocusSearch: Boolean = true,
    cellSize: Dp = ItemTileSize,
    gridMaxHeight: Dp = 300.dp,
    gutter: Dp = 12.dp,
    titleKey: String? = ItemStrings.Title,
    searchPlaceholderKey: String = ItemStrings.Search,
    emptyKey: String = ItemStrings.Empty,
    header: @Composable (ColumnScope.() -> Unit)? = null,
) = TilePicker(
    entries = items,
    selected = selected,
    onToggle = onToggle,
    icon = ItemTileIcon,
    modifier = modifier,
    maxEntries = maxEntries,
    onClose = onClose,
    autoFocusSearch = autoFocusSearch,
    cellSize = cellSize,
    gridMaxHeight = gridMaxHeight,
    gutter = gutter,
    titleKey = titleKey,
    searchPlaceholderKey = searchPlaceholderKey,
    emptyKey = emptyKey,
    header = header,
)

@Deprecated("Not item specific", ReplaceWith("PickerMessage(key, modifier, height)", "org.polyfrost.oneconfig.internal.ui.components.picker.PickerMessage"))
@Composable
fun ItemMessage(key: String, modifier: Modifier = Modifier, height: Dp = 120.dp) = PickerMessage(key, modifier, height)

/** Default translation keys where every composable taking one accepts an override */
object ItemStrings {
    const val Title = "oneconfig.itemlist.title"
    const val Search = "oneconfig.itemlist.search"
    const val Empty = "oneconfig.itemlist.empty"
    const val Unavailable = "oneconfig.itemlist.unavailable"
}

@Deprecated("Not item specific", ReplaceWith("canSelect(selected, id, maxEntries)", "org.polyfrost.oneconfig.internal.ui.components.picker.canSelect"))
fun canSelect(selected: List<String>, id: String, maxEntries: Int): Boolean =
    org.polyfrost.oneconfig.internal.ui.components.picker.canSelect(selected, id, maxEntries)
