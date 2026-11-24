package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.polyfrost.oneconfig.api.ui.v1.ModSortMode
import org.polyfrost.oneconfig.api.ui.v1.ModSortModes
import org.polyfrost.oneconfig.api.ui.v1.components.AnchoredConfirm
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.ControlStyle
import org.polyfrost.oneconfig.api.ui.v1.components.Placement
import org.polyfrost.oneconfig.api.ui.v1.components.TabItem
import org.polyfrost.oneconfig.api.ui.v1.components.TabRow
import org.polyfrost.oneconfig.api.ui.v1.components.TabStyle
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipPlacement
import org.polyfrost.oneconfig.internal.ui.api.ModSortStore
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter

private val customMode = ModSortMode(ModSortStore.CUSTOM, "dots-grid", "Custom order") {}
private val alphabeticalMode = ModSortMode(ModSortStore.ALPHABETICAL, "capitalized", "A to Z") {}

private fun ModSortMode.label(): String = when (id) {
    ModSortStore.CUSTOM -> translated("oneconfig.mods.sort.custom", title)
    ModSortStore.ALPHABETICAL -> translated("oneconfig.mods.sort.alphabetical", title)
    else -> title
}

@Composable
fun ModSortControl(modifier: Modifier = Modifier) {
    val modes = listOf(customMode, alphabeticalMode) + ModSortModes.all()
    val selected = modes.firstOrNull { it.id == ModSortStore.selectedId } ?: customMode

    fun apply(mode: ModSortMode) {
        ModSortStore.select(mode.id)
        mode.onSelect()
    }

    var confirming by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(customMode) }

    fun pick(mode: ModSortMode) {
        if (mode.id == selected.id) return
        if (mode.confirmation == null) {
            apply(mode)
            return
        }
        pending = mode
        confirming = true
    }

    Box(modifier.padding(end = PageGutter)) {
        TabRow(
            tabs = modes.map { TabItem(it, "", icon = it.icon, tooltip = it.label()) },
            selected = selected,
            onSelect = ::pick,
            style = TabStyle.Segmented,
            size = ControlSize.Large,
            fill = false,
            tooltipPlacement = TooltipPlacement.BelowCenter,
        )
        val mode = pending
        AnchoredConfirm(
            expanded = confirming,
            onDismiss = { confirming = false },
            text = translated("oneconfig.mods.sort.confirm.title", "Sort by %s?", mode.label()) +
                " " + mode.confirmation.orEmpty(),
            confirmLabel = mode.confirmLabel ?: translated("oneconfig.mods.sort.confirm", "Switch"),
            onConfirm = { apply(mode) },
            style = ControlStyle.Primary,
            placement = Placement.BelowEnd,
            popupSounds = false,
        )
    }
}
