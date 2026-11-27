package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ModSortMode
import org.polyfrost.oneconfig.api.ui.v1.ModSortModes
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.AnchoredConfirm
import org.polyfrost.oneconfig.api.ui.v1.components.ControlStyle
import org.polyfrost.oneconfig.api.ui.v1.components.DropdownMenu
import org.polyfrost.oneconfig.api.ui.v1.components.Placement
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.internal.ui.api.ModSortStore
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val customMode = ModSortMode(ModSortStore.CUSTOM, "dots-grid", "Custom order") {}
private val alphabeticalMode = ModSortMode(ModSortStore.ALPHABETICAL, "capitalized", "Name, A to Z") {}
private val enabledFirstMode = ModSortMode(ModSortStore.ENABLED_FIRST, "check-circle", "Enabled first") {}
private val disabledFirstMode = ModSortMode(ModSortStore.DISABLED_FIRST, "x-circle", "Disabled first") {}

private val SortButtonHeight = 32.dp
private val SortMenuWidth = 220.dp

private fun ModSortMode.label(): String = when (id) {
    ModSortStore.CUSTOM -> translated("oneconfig.mods.sort.custom", title)
    ModSortStore.ALPHABETICAL -> translated("oneconfig.mods.sort.alphabetical", title)
    ModSortStore.ENABLED_FIRST -> translated("oneconfig.mods.sort.enabled_first", title)
    ModSortStore.DISABLED_FIRST -> translated("oneconfig.mods.sort.disabled_first", title)
    else -> title
}

private fun ModSortMode.hint(): String? = when (id) {
    ModSortStore.CUSTOM -> translated("oneconfig.mods.sort.custom.hint", "Drag cards to reorder")
    else -> null
}

@Composable
fun ModSortControl(modifier: Modifier = Modifier) {
    val modes = listOf(customMode, alphabeticalMode, enabledFirstMode, disabledFirstMode) + ModSortModes.all()
    val selected = modes.firstOrNull { it.id == ModSortStore.selectedId } ?: customMode

    fun apply(mode: ModSortMode) {
        ModSortStore.select(mode.id)
        mode.onSelect()
    }

    var open by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(customMode) }

    fun pick(mode: ModSortMode) {
        open = false
        if (mode.id == selected.id) return
        if (mode.confirmation == null) {
            apply(mode)
            return
        }
        pending = mode
        confirming = true
    }

    Box(modifier) {
        SortButton(selected.label(), open) { open = !open }
        DropdownMenu(
            expanded = open,
            onDismiss = { open = false },
            modifier = Modifier.width(SortMenuWidth),
            placement = Placement.BelowEnd,
        ) {
            modes.forEach { mode -> SortMenuRow(mode, mode.id == selected.id) { pick(mode) } }
        }
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

@Composable
private fun SortButton(label: String, open: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    val ring by Motion.animateColorAsState(
        when {
            open -> Accent
            hovered -> theme.textColorSecondary
            else -> theme.borderColor
        }
    )
    val shape = theme.buttonShape
    val description = translated("oneconfig.mods.sort.button", "Sort mods: %s", label)
    Row(
        Modifier
            .height(SortButtonHeight)
            .clip(shape)
            .background(theme.componentBackground, shape)
            .border(1.dp, ring, shape)
            .oneConfigClickable(description, onClick, role = A11yRole.Button, indication = false, interactionSource = source)
            .padding(start = 12.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon("sort", color = theme.textColorSecondary, modifier = Modifier.size(15.dp))
        Text(label, color = theme.textColor, fontSize = 13.sp, maxLines = 1)
        Icon("down", color = theme.textColorSecondary, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun SortMenuRow(mode: ModSortMode, checked: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    val fill by Motion.animateColorAsState(if (hovered) theme.textColor.copy(alpha = 0.07f) else theme.textColor.copy(alpha = 0f))
    val shape = theme.buttonShape
    val label = mode.label()
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .clip(shape)
            .background(fill, shape)
            .oneConfigClickable(label, onClick, role = A11yRole.RadioButton, selected = checked, indication = false, interactionSource = source)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
            if (checked) Icon("tick", color = Accent, modifier = Modifier.size(16.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, color = theme.textColor, fontSize = 13.sp, maxLines = 1)
            mode.hint()?.let { Text(it, color = theme.textColorSecondary, fontSize = 11.sp, maxLines = 1) }
        }
    }
}
