package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.polyfrost.oneconfig.api.ui.v1.ModSortMode
import org.polyfrost.oneconfig.api.ui.v1.ModSortModes
import org.polyfrost.oneconfig.api.ui.v1.components.Confirm
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.ControlStyle
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

    fun pick(mode: ModSortMode) {
        if (mode.id == selected.id) return
        val confirmation = mode.confirmation
        if (confirmation == null) {
            apply(mode)
            return
        }
        Confirm.ask(
            title = translated("oneconfig.mods.sort.confirm.title", "Sort by %s?", mode.label()),
            text = confirmation,
            confirmLabel = translated("oneconfig.mods.sort.confirm", "Switch"),
            onConfirm = { apply(mode) },
            style = ControlStyle.Primary,
            icon = mode.icon,
        )
    }

    TabRow(
        tabs = modes.map { TabItem(it, "", icon = it.icon, tooltip = it.label()) },
        selected = selected,
        onSelect = ::pick,
        modifier = modifier.padding(end = PageGutter),
        style = TabStyle.Segmented,
        size = ControlSize.Large,
        fill = false,
        tooltipPlacement = TooltipPlacement.BelowCenter,
    )
}
