package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.dp
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
        else Box(modifier, contentAlignment = Alignment.Center) {
            Icon("eye-off", modifier = Modifier.size(TileRowIconSize - 10.dp), color = LocalTheme.current.textColorSecondary)
        }
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
            pinAnchor = true,
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
                width = SelectorWidth,
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
