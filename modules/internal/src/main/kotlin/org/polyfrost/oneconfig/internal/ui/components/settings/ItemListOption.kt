package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.api.settings.ItemListOptionData
import org.polyfrost.oneconfig.internal.ui.components.dropdown.SimpleDropdownMenu
import org.polyfrost.oneconfig.internal.ui.components.item.ItemDescriptor
import org.polyfrost.oneconfig.internal.ui.components.item.ItemIcon
import org.polyfrost.oneconfig.internal.ui.components.item.ItemLabel
import org.polyfrost.oneconfig.internal.ui.components.item.ItemPicker
import org.polyfrost.oneconfig.internal.ui.components.item.rememberItemCatalog
import org.polyfrost.oneconfig.internal.ui.components.item.rememberItemCatalogById
import org.polyfrost.oneconfig.internal.ui.components.item.toggleItem
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val ItemRowHeight = 44.dp
private val SelectorWidth = 440.dp
private val SelectorShape = RoundedCornerShape(12.dp)

@Composable
fun ItemListOption(data: ItemListOptionData) {
    val theme = LocalTheme.current
    val catalog = rememberItemCatalog(data.prop)
    val itemById = rememberItemCatalogById(catalog)
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
            containScroll = true,
        ) { entry, ctx ->
            SelectedItemRow(entry.value, itemById[entry.value], ctx)
        }

        SimpleDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 8.dp),
            modifier = Modifier.width(SelectorWidth),
        ) {
            ItemPicker(
                items = catalog,
                selected = state.values,
                maxEntries = data.maxEntries,
                modifier = Modifier
                    .clip(SelectorShape)
                    .background(theme.popupBackground, SelectorShape)
                    .border(1.dp, theme.borderColor, SelectorShape)
                    .padding(12.dp),
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
    ItemIcon(id, Modifier.size(30.dp))
    ItemLabel(id, item, modifier = Modifier.weight(1f), nameColor = ctx.contentColor)
}
