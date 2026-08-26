package org.polyfrost.oneconfig.internal.ui.components.item

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.skia.Rect
import org.polyfrost.oneconfig.internal.ui.api.Tooltip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.containVerticalScroll
import org.polyfrost.oneconfig.internal.ui.components.localizedString
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

val ItemTileShape = RoundedCornerShape(8.dp)
val ItemIconShape = RoundedCornerShape(5.dp)

val ItemTileSize = 44.dp

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
 * Resolving may need the Minecraft render thread
 *
 * [framed] adds the theme chip background
 */
@Composable
fun ItemIcon(
    id: String,
    modifier: Modifier = Modifier,
    framed: Boolean = true,
    alpha: Float = 1f,
    placeholder: String = "layers",
) {
    val theme = LocalTheme.current
    val iconReady = rememberItemIconReady(id)
    val background = if (framed) Modifier.background(theme.chipBackground) else Modifier
    Box(
        modifier = modifier
            .clip(ItemIconShape)
            .then(background),
        contentAlignment = Alignment.Center,
    ) {
        if (iconReady) {
            Canvas(modifier = Modifier.fillMaxSize().padding(if (framed) 2.dp else 1.dp)) {
                drawIntoCanvas { canvas ->
                    ItemCatalog.drawIcon(
                        id,
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
) {
    Column(modifier = modifier) {
        Text(
            item?.displayName ?: id,
            color = nameColor,
            fontSize = nameSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            item?.id ?: localizedString(null, unavailableKey),
            color = idColor,
            fontSize = idSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A square item cell with selection border tick badge and tooltip where a `null` [onClick] is decorative */
@Composable
fun ItemTile(
    item: ItemDescriptor,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    tooltip: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val theme = LocalTheme.current
    val interaction = rememberInteractionSource()
    val hovered by interaction.collectIsHoveredAsState()
    val interactive = enabled && onClick != null
    val background by animateColorAsState(
        when {
            selected -> Accent.copy(alpha = 0.18f).compositeOver(theme.modCardBackground)
            hovered && interactive -> theme.componentBackground
            else -> theme.modCardBackground
        }
    )
    val border by animateColorAsState(if (selected) Accent else theme.borderColor)
    val contentAlpha = if (enabled) 1f else 0.45f

    ItemTooltip(item, enabled = tooltip) {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .clip(ItemTileShape)
                .background(background, ItemTileShape)
                .border(1.dp, border, ItemTileShape)
                .hoverable(interaction)
                .pointerHoverIcon(if (interactive) PointerIcon.Hand else PointerIcon.Default)
                .then(if (interactive) Modifier.onClick(interaction, onClick!!) else Modifier)
                .padding(5.dp),
            contentAlignment = Alignment.Center,
        ) {
            ItemIcon(item.id, Modifier.fillMaxSize(), framed = false, alpha = contentAlpha)
            if (selected) {
                Icon(
                    "tick",
                    modifier = Modifier.align(Alignment.TopEnd).size(11.dp),
                    color = Accent,
                )
            }
        }
    }
}

/** Wraps [content] in the name and ID tooltip or renders it bare when [enabled] is false */
@Composable
fun ItemTooltip(
    item: ItemDescriptor,
    enabled: Boolean = true,
    anchor: Alignment = Alignment.BottomCenter,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }
    val theme = LocalTheme.current
    Tooltip(
        text = {
            Column {
                Text(
                    item.displayName,
                    color = theme.textColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(item.id, color = theme.textColorSecondary, fontSize = 9.sp)
            }
        },
        modifier = Modifier,
        anchor = anchor,
        content = content,
    )
}

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
    onClick: ((ItemDescriptor) -> Unit)? = null,
) {
    val gridState = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(cellSize),
        state = gridState,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .containVerticalScroll {
                gridState.canScrollBackward || gridState.canScrollForward
            },
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        items(items, key = ItemDescriptor::id) { item ->
            ItemTile(
                item,
                modifier = Modifier.fillMaxWidth(),
                selected = selected(item),
                enabled = enabled(item),
                onClick = onClick?.let { click -> { click(item) } },
            )
        }
    }
}

/** Search box for [ItemGrid] where [autoFocus] is off by default for embedded use */
@Composable
fun ItemSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
    placeholderKey: String = ItemStrings.Search,
) {
    val theme = LocalTheme.current
    val interaction = rememberInteractionSource()
    val focused by interaction.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }

    if (autoFocus) {
        LaunchedEffect(Unit) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = theme.textColor, fontSize = 12.sp, fontFamily = theme.typography.family),
        cursorBrush = SolidColor(theme.textColor),
        interactionSource = interaction,
        modifier = modifier.focusRequester(focusRequester),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(ItemTileShape)
                    .background(theme.componentBackground, ItemTileShape)
                    .border(1.dp, if (focused) Accent else theme.borderColor, ItemTileShape)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon("search", modifier = Modifier.size(14.dp), color = theme.textColorSecondary)
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            localizedString(null, placeholderKey),
                            color = theme.textColorSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    inner()
                }
                if (value.isNotEmpty()) {
                    IconButton("close", modifier = Modifier.size(14.dp)) { onValueChange("") }
                }
            }
        },
    )
}

/**
 * Header search field and result grid making up the whole item selector for any HUD or screen
 *
 * The [modifier] supplies the chrome so it fits a dropdown a HUD editor panel or a full screen
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
    titleKey: String? = ItemStrings.Title,
    searchPlaceholderKey: String = ItemStrings.Search,
    emptyKey: String = ItemStrings.Empty,
    catalogUnavailableKey: String = ItemStrings.CatalogUnavailable,
    header: @Composable (ColumnScope.() -> Unit)? = null,
) {
    val theme = LocalTheme.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(items, query) { filterItems(items, query) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (header != null) {
            header()
        } else if (titleKey != null || onClose != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (titleKey != null) {
                        Text(
                            localizedString(null, titleKey),
                            color = theme.textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (maxEntries > 0) {
                        Text(
                            "${selected.size}/$maxEntries",
                            color = theme.textColorSecondary,
                            fontSize = 10.sp,
                        )
                    }
                }
                if (onClose != null) {
                    IconButton("close", modifier = Modifier.size(18.dp), onClick = onClose)
                }
            }
        }

        ItemSearchField(
            query,
            onValueChange = { query = it },
            autoFocus = autoFocusSearch,
            placeholderKey = searchPlaceholderKey,
        )

        when {
            items.isEmpty() -> ItemMessage(catalogUnavailableKey)
            filtered.isEmpty() -> ItemMessage(emptyKey)
            else -> ItemGrid(
                items = filtered,
                selected = { it.id in selected },
                enabled = { canSelect(selected, it.id, maxEntries) },
                cellSize = cellSize,
                maxHeight = gridMaxHeight,
                onClick = { onToggle(it.id) },
            )
        }
    }
}

/** Centered message for empty and unavailable states */
@Composable
fun ItemMessage(key: String, modifier: Modifier = Modifier, height: Dp = 120.dp) {
    Box(modifier = modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Text(
            localizedString(null, key),
            color = LocalTheme.current.textColorSecondary,
            fontSize = 12.sp,
        )
    }
}

/** Default translation keys where every composable taking one accepts an override */
object ItemStrings {
    const val Title = "oneconfig.itemlist.title"
    const val Search = "oneconfig.itemlist.search"
    const val Empty = "oneconfig.itemlist.empty"
    const val Unavailable = "oneconfig.itemlist.unavailable"
    const val CatalogUnavailable = "oneconfig.itemlist.catalog_unavailable"
}

/** Whether [id] can be picked where already-selected items stay clickable so they can be deselected */
fun canSelect(selected: List<String>, id: String, maxEntries: Int): Boolean =
    id in selected || maxEntries <= 0 || maxEntries == 1 || selected.size < maxEntries
