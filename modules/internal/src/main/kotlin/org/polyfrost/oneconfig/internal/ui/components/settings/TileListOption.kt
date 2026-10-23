package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.api.settings.TileListOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.dropdown.SimpleDropdownMenu
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerEntry
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerLabel
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerStrings
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerTileIcon
import org.polyfrost.oneconfig.internal.ui.components.picker.TilePicker
import org.polyfrost.oneconfig.internal.ui.components.picker.toggleEntry
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val TileRowHeight = 44.dp
private val TileRowIconSize = 30.dp
private val SelectorWidth = 436.dp
private val SelectorShape @Composable get() = LocalTheme.current.popupShape

@Composable
fun <T : PickerEntry> TileListOption(
    data: TileListOptionData,
    entries: List<T>,
    icon: PickerTileIcon<T>,
    titleKey: String? = PickerStrings.Title,
    searchPlaceholderKey: String = PickerStrings.Search,
    emptyKey: String = PickerStrings.Empty,
    unavailableKey: String = PickerStrings.Unavailable,
    rowIcon: @Composable (id: String, entry: T?, modifier: Modifier) -> Unit = { _, entry, modifier ->
        if (entry != null) icon(entry, modifier, 1f)
        else Icon("eye-off", modifier = modifier.padding(5.dp), color = LocalTheme.current.textColorSecondary)
    },
) {
    val theme = LocalTheme.current
    val maxEntries = data.maxEntries
    val byId = remember(entries) { entries.distinctBy(PickerEntry::id).associateBy(PickerEntry::id) }
    val state = rememberListEntries(data.prop, { data.ids() }) { data.setIds(it) }
    var expanded by remember(data.prop) { mutableStateOf(false) }

    Box {
        ListOptionContainer(
            state = state,
            reorderable = data.reorderable,
            maxEntries = maxEntries,
            addText = data.addText,
            onAdd = { expanded = true },
            showAddWhenFull = true,
            entryHeight = TileRowHeight,
            containScroll = true,
        ) { entry, ctx ->
            val resolved = byId[entry.value]
            rowIcon(entry.value, resolved, Modifier.size(TileRowIconSize))
            PickerLabel(
                entry.value,
                resolved,
                modifier = Modifier.weight(1f),
                nameColor = ctx.contentColor,
                unavailableKey = unavailableKey,
            )
        }

        SimpleDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset((-16).dp, 8.dp),
            modifier = Modifier.width(SelectorWidth),
        ) {
            TilePicker(
                entries = entries,
                selected = state.values,
                onToggle = { id ->
                    val next = toggleEntry(state.values, id, maxEntries)
                    if (next != state.values) state.replace(next)
                    if (maxEntries == 1) expanded = false
                },
                icon = icon,
                modifier = Modifier
                    .clip(SelectorShape)
                    .background(theme.popupBackground, SelectorShape)
                    .border(1.dp, theme.borderColor, SelectorShape)
                    .padding(vertical = 12.dp),
                maxEntries = maxEntries,
                onClose = { expanded = false },
                titleKey = titleKey,
                searchPlaceholderKey = searchPlaceholderKey,
                emptyKey = emptyKey,
            )
        }
    }
}
