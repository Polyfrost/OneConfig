package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.api.settings.ItemListOptionData
import org.polyfrost.oneconfig.internal.ui.api.Tooltip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.dropdown.SimpleDropdownMenu
import org.polyfrost.oneconfig.internal.ui.components.localizedString
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.item.ItemCatalog
import org.polyfrost.oneconfig.internal.ui.components.settings.item.ItemDescriptor
import org.polyfrost.oneconfig.internal.ui.components.settings.item.ItemIconData
import org.polyfrost.oneconfig.internal.ui.components.settings.item.filterItems
import org.polyfrost.oneconfig.internal.ui.components.settings.item.toggleItem
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.awt.image.BufferedImage

private val ItemRowHeight = 44.dp
private val SelectorWidth = 440.dp
private val SelectorShape = RoundedCornerShape(12.dp)
private val ItemCardShape = RoundedCornerShape(8.dp)

@Composable
fun ItemListOption(data: ItemListOptionData) {
    val catalog = remember(data.prop) {
        ItemCatalog.items()
            .distinctBy(ItemDescriptor::id)
            .sortedWith { first, second ->
                first.displayName.compareTo(second.displayName, ignoreCase = true)
                    .takeIf { it != 0 } ?: first.id.compareTo(second.id)
            }
    }
    val itemById = remember(catalog) { catalog.associateBy(ItemDescriptor::id) }
    val state = rememberListEntries(data.prop, data.ids()) { data.setIds(it) }
    var expanded by remember(data.prop) { mutableStateOf(false) }

    Box {
        ListOptionContainer(
            state = state,
            reorderable = data.reorderable,
            maxEntries = data.maxEntries,
            addText = data.addText,
            onAdd = { expanded = true },
            showAddWhenFull = true,
            entryHeight = ItemRowHeight,
        ) { entry, ctx ->
            SelectedItemRow(entry.value, itemById[entry.value], ctx)
        }

        SimpleDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 8.dp),
            modifier = Modifier.width(SelectorWidth),
        ) {
            ItemSelectorPopup(
                items = catalog,
                selected = state.values,
                maxEntries = data.maxEntries,
                onToggle = { id ->
                    val next = toggleItem(state.values, id, data.maxEntries)
                    if (next != state.values) state.replace(next)
                    if (data.maxEntries == 1) expanded = false
                },
                onClose = { expanded = false },
            )
        }
    }
}

@Composable
private fun RowScope.SelectedItemRow(id: String, item: ItemDescriptor?, ctx: ListRowContext) {
    ItemPreview(id, Modifier.size(30.dp))
    Column(modifier = Modifier.weight(1f)) {
        Text(
            item?.displayName ?: id,
            color = ctx.contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            item?.id ?: localizedString(null, "oneconfig.itemlist.unavailable"),
            color = LocalTheme.current.textColorSecondary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ItemSelectorPopup(
    items: List<ItemDescriptor>,
    selected: List<String>,
    maxEntries: Int,
    onToggle: (String) -> Unit,
    onClose: () -> Unit,
) {
    val theme = LocalTheme.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(items, query) { filterItems(items, query) }

    Column(
        modifier = Modifier
            .clip(SelectorShape)
            .background(theme.popupBackground, SelectorShape)
            .border(1.dp, theme.borderColor, SelectorShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    localizedString(null, "oneconfig.itemlist.title"),
                    color = theme.textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (maxEntries > 0) {
                    Text(
                        "${selected.size}/$maxEntries",
                        color = theme.textColorSecondary,
                        fontSize = 10.sp,
                    )
                }
            }
            IconButton("close", modifier = Modifier.size(18.dp), onClick = onClose)
        }

        ItemSearchField(query, onValueChange = { query = it })

        when {
            items.isEmpty() -> EmptyCatalogMessage("oneconfig.itemlist.catalog_unavailable")
            filtered.isEmpty() -> EmptyCatalogMessage("oneconfig.itemlist.empty")
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(44.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(filtered, key = ItemDescriptor::id) { item ->
                    val isSelected = item.id in selected
                    val enabled = isSelected || maxEntries <= 0 || maxEntries == 1 || selected.size < maxEntries
                    SelectableItemTile(item, isSelected, enabled) { onToggle(item.id) }
                }
            }
        }
    }
}

@Composable
private fun ItemSearchField(value: String, onValueChange: (String) -> Unit) {
    val theme = LocalTheme.current
    val interaction = rememberInteractionSource()
    val focused by interaction.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = theme.textColor, fontSize = 12.sp, fontFamily = theme.typography.family),
        cursorBrush = SolidColor(theme.textColor),
        interactionSource = interaction,
        modifier = Modifier.focusRequester(focusRequester),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(ItemCardShape)
                    .background(theme.componentBackground, ItemCardShape)
                    .border(1.dp, if (focused) Accent else theme.borderColor, ItemCardShape)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon("search", modifier = Modifier.size(14.dp), color = theme.textColorSecondary)
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            localizedString(null, "oneconfig.itemlist.search"),
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

@Composable
private fun SelectableItemTile(
    item: ItemDescriptor,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val theme = LocalTheme.current
    val interaction = rememberInteractionSource()
    val hovered by interaction.collectIsHoveredAsState()
    val background by animateColorAsState(
        when {
            selected -> Accent.copy(alpha = 0.18f).compositeOver(theme.modCardBackground)
            hovered && enabled -> theme.componentBackground
            else -> theme.modCardBackground
        }
    )
    val border by animateColorAsState(if (selected) Accent else theme.borderColor)
    val contentAlpha = if (enabled) 1f else 0.45f
    val clickModifier = if (enabled) Modifier.onClick(interaction, onClick) else Modifier

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
        anchor = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(ItemCardShape)
                .background(background, ItemCardShape)
                .border(1.dp, border, ItemCardShape)
                .hoverable(interaction)
                .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
                .then(clickModifier)
                .padding(5.dp),
            contentAlignment = Alignment.Center,
        ) {
            ItemPreview(item.id, Modifier.fillMaxSize(), framed = false, alpha = contentAlpha)
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

@Composable
private fun EmptyCatalogMessage(key: String) {
    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        Text(
            localizedString(null, key),
            color = LocalTheme.current.textColorSecondary,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ItemPreview(
    id: String,
    modifier: Modifier = Modifier,
    framed: Boolean = true,
    alpha: Float = 1f,
) {
    val theme = LocalTheme.current
    var icon by remember(id) { mutableStateOf(ItemCatalog.icon(id)) }
    LaunchedEffect(id) {
        ItemCatalog.loadIcon(id) { icon = it }
    }
    val bitmap = remember(icon) { icon?.toBitmap() }
    val background = if (framed) Modifier.background(theme.chipBackground) else Modifier
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .then(background),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                painter = BitmapPainter(bitmap, filterQuality = FilterQuality.Low),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(if (framed) 2.dp else 1.dp),
                alpha = alpha,
            )
        } else {
            Icon(
                "layers",
                modifier = Modifier.fillMaxSize().padding(if (framed) 6.dp else 5.dp),
                color = theme.textColorSecondary.copy(alpha = alpha),
            )
        }
    }
}

private fun ItemIconData.toBitmap() = runCatching {
    require(width > 0 && height > 0 && argb.size >= width * height)
    BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also {
        it.setRGB(0, 0, width, height, argb, 0, width)
    }.toComposeImageBitmap()
}.getOrNull()
