package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.api.ui.v1.components.EscapeCloses
import org.polyfrost.oneconfig.api.ui.v1.components.Placement
import org.polyfrost.oneconfig.api.ui.v1.components.SurfaceKind
import org.polyfrost.oneconfig.api.ui.v1.components.rememberAnchoredPosition
import org.polyfrost.oneconfig.api.ui.v1.components.themedSurface
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.internal.ui.api.settings.TileListOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.ShellPopup
import org.polyfrost.oneconfig.internal.ui.components.blockInteraction
import org.polyfrost.oneconfig.internal.ui.components.shellTransition
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
private val SelectorGap = 8.dp
private val SelectorOutset = 16.dp
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
    rowIcon: @Composable (id: String, entry: T?, modifier: Modifier) -> Unit = { id, entry, modifier -> DefaultTileRowIcon(icon, id, entry, modifier) },
) {
    TileListOption(data, entries, icon, LocalOptionEnabled.current, titleKey, searchPlaceholderKey, emptyKey, unavailableKey, rowIcon)
}

@Composable
fun <T : PickerEntry> TileListOption(
    data: TileListOptionData,
    entries: List<T>,
    icon: PickerTileIcon<T>,
    enabled: Boolean,
    titleKey: String? = PickerStrings.Title,
    searchPlaceholderKey: String = PickerStrings.Search,
    emptyKey: String = PickerStrings.Empty,
    unavailableKey: String = PickerStrings.Unavailable,
    rowIcon: @Composable (id: String, entry: T?, modifier: Modifier) -> Unit = { id, entry, modifier -> DefaultTileRowIcon(icon, id, entry, modifier) },
) {
    val maxEntries = data.maxEntries
    val byId = remember(entries) { entries.distinctBy(PickerEntry::id).associateBy(PickerEntry::id) }
    val state = rememberListEntries(data.prop, { data.ids() }) { data.setIds(it) }
    var expanded by remember(data.prop) { mutableStateOf(false) }
    val disabledAlpha = OneConfigTheme.current.disabledAlpha

    LaunchedEffect(enabled) { if (!enabled) expanded = false }

    Box(
        Modifier
            .graphicsLayer { alpha = if (enabled) 1f else disabledAlpha }
            .blockInteraction(!enabled, keyboard = true),
    ) {
        ListOptionContainer(
            state = state,
            reorderable = data.reorderable,
            maxEntries = maxEntries,
            addText = data.addText,
            onAdd = { if (enabled) expanded = true },
            showAddWhenFull = true,
            rowInsertActions = false,
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

        if (expanded && enabled) {
            EscapeCloses { expanded = false }
            val anchored = rememberAnchoredPosition(Placement.BelowStart, SelectorGap)
            val outset = with(LocalDensity.current) { SelectorOutset.roundToPx() }
            val pinned = remember { mutableStateOf<IntRect?>(null) }
            val position = remember(anchored, outset) { PinnedAnchorPosition(anchored, pinned, outset) }
            ShellPopup(position, { expanded = false }, PopupProperties(focusable = true)) {
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
                        .width(SelectorWidth)
                        .shellTransition()
                        .themedSurface(SurfaceKind.Popup, SelectorShape)
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
}

@Composable
private fun <T : PickerEntry> DefaultTileRowIcon(icon: PickerTileIcon<T>, id: String, entry: T?, modifier: Modifier) {
    if (entry != null) icon(entry, modifier, 1f)
    else Icon("eye-off", modifier = modifier.padding(5.dp), color = LocalTheme.current.textColorSecondary)
}

private class PinnedAnchorPosition(
    private val delegate: PopupPositionProvider,
    private val pinned: MutableState<IntRect?>,
    private val outset: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchor = pinned.value ?: anchorBounds.also { pinned.value = it }
        val widened = IntRect(anchor.left - outset, anchor.top, anchor.right + outset, anchor.bottom)
        return delegate.calculatePosition(widened, windowSize, layoutDirection, popupContentSize)
    }
}
