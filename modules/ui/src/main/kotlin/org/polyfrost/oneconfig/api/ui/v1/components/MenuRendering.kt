@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11y
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.isKeyboardMode
import org.polyfrost.oneconfig.api.ui.v1.a11y.notFocusable
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@Composable
internal fun MenuEntryItems(entries: List<MenuEntry>, onSelect: () -> Unit, size: MenuItemSize) {
    val reserveCheck = entries.any { it is MenuAction && it.checked != null || it is MenuRadioGroup<*> }
    val reserveIcon = entries.any { it is MenuAction && (it.icon != null || it.painter != null || it.leading != null) }
    entries.forEach { entry ->
        when (entry) {
            is MenuAction -> MenuActionRow(entry, onSelect, size, reserveCheck, reserveIcon)
            is MenuSeparator -> Divider(Modifier.padding(vertical = MenuDefaults.itemSpacing))
            is MenuHeading -> MenuHeadingText(entry.text, size)
            is MenuRadioGroup<*> -> MenuRadioRows(entry, onSelect, size, reserveIcon)
            is MenuIconRow -> MenuIconRowItems(entry, onSelect)
            is MenuCustomItem -> entry.content()
        }
    }
}

@Composable
private fun MenuActionRow(action: MenuAction, onSelect: () -> Unit, size: MenuItemSize, reserveCheck: Boolean, reserveIcon: Boolean) {
    val level = LocalMenuLevel.current
    val itemSize = maxOf(action.size, size)
    val submenu: (@Composable (MenuNavItem) -> Unit)? =
        if (action.children.isEmpty() || level == null) null
        else { owner -> SubmenuPopup(level, owner, action.children, onSelect, size) }
    MenuItemRow(
        label = action.label,
        onClick = {
            onSelect()
            action.onClick()
        },
        icon = action.icon,
        painter = action.painter,
        detail = action.detail,
        style = action.style,
        size = itemSize,
        enabled = action.enabled,
        checked = action.checked,
        shortcutHint = action.shortcutHint,
        badge = action.badge,
        tooltip = action.disabledReason?.takeIf { !action.enabled },
        leading = action.leading,
        reserveCheck = reserveCheck,
        reserveIcon = reserveIcon,
        submenu = submenu,
    )
}

@Composable
private fun MenuHeadingText(text: String, size: MenuItemSize) {
    val theme = OneConfigTheme.current
    Text(
        text,
        Modifier
            .fillMaxWidth()
            .padding(start = size.inset, end = size.inset, top = 6.dp, bottom = 2.dp)
            .semantics { heading() },
        theme.mutedText,
        theme.typography.caption.copy(fontWeight = FontWeight.Medium),
        1,
        TextOverflow.Ellipsis,
    )
}

@Composable
private fun <T> MenuRadioRows(group: MenuRadioGroup<T>, onSelect: () -> Unit, size: MenuItemSize, reserveIcon: Boolean) {
    group.options.forEach { option ->
        MenuItemRow(
            label = group.label(option),
            onClick = {
                onSelect()
                group.onSelect(option)
            },
            size = size,
            checked = option == group.selected,
            radio = true,
            reserveIcon = reserveIcon,
        )
    }
}

@Composable
private fun MenuIconRowItems(row: MenuIconRow, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = MenuDefaults.contentPadding, vertical = MenuDefaults.itemSpacing),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        row.items.forEach { MenuIconButton(it, onSelect) }
    }
}

@Composable
private fun MenuIconButton(action: MenuAction, onSelect: () -> Unit) {
    val theme = OneConfigTheme.current
    val level = LocalMenuLevel.current
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val keyboard = isKeyboardMode()
    val click = {
        onSelect()
        action.onClick()
    }
    val nav = rememberMenuNavItem(level, action.label, action.enabled, false) {
        playUiSound(UiSound.Click)
        click()
    }
    MenuNavHover(level, nav, hovered)
    val highlighted = nav != null && level?.highlighted === nav
    val tooltip = action.disabledReason?.takeIf { !action.enabled } ?: action.label
    LaunchedEffect(highlighted, keyboard) {
        if (highlighted && keyboard) A11y.focused(tooltip, A11yRole.Button, null, null)
    }
    val ringShape = if (action.painter != null || action.icon != null) IconButtonDefaults.shape(IconButtonStyle.Ghost) else ButtonDefaults.shape
    val ring = if (highlighted && keyboard) Modifier.border(1.dp, theme.focusRingColor, ringShape) else Modifier
    Box(Modifier.menuNavPosition(nav).then(ring)) {
        val modifier = if (level != null) Modifier.notFocusable() else Modifier
        when {
            action.painter != null -> IconButton(
                action.painter, action.label, click, modifier,
                enabled = action.enabled, size = ControlSize.Compact, danger = action.danger,
                tooltip = tooltip, interactionSource = source,
            )
            action.icon != null -> IconButton(
                action.icon, action.label, click, modifier,
                enabled = action.enabled, size = ControlSize.Compact, danger = action.danger,
                tooltip = tooltip, interactionSource = source,
            )
            else -> TextButton(
                action.label, click, modifier,
                style = if (action.danger) ControlStyle.Danger else ControlStyle.Ghost, size = ControlSize.Compact,
                enabled = action.enabled, leading = action.leading, interactionSource = source,
            )
        }
        if (highlighted && keyboard) TooltipPopup(tooltip, TooltipPlacement.BelowCenter)
    }
}

@Composable
private fun SubmenuPopup(
    parent: MenuNavLevel,
    owner: MenuNavItem,
    entries: List<MenuEntry>,
    onSelect: () -> Unit,
    size: MenuItemSize,
) {
    val controller = parent.controller
    val level = remember(parent, owner) { MenuNavLevel(controller, parent) }
    DisposableEffect(level) {
        parent.child = level
        onDispose {
            if (parent.child === level) parent.child = null
            if (level.contains(controller.active)) controller.active = parent
        }
    }
    EscapeCloses { parent.closeChild() }
    LaunchedEffect(level) {
        if (controller.focusFirstOf !== owner) return@LaunchedEffect
        controller.focusFirstOf = null
        withFrameNanos { }
        level.highlightFirst()
    }
    MenuHoverIntentEffect(level)
    componentsBridge().Popup(rememberAnchoredPosition(Placement.End, MenuDefaults.submenuGap), null, false) {
        CompositionLocalProvider(LocalMenuLevel provides level) {
            MenuPanel(Modifier, popupMaxHeight()) {
                MenuEntryItems(entries, onSelect, size)
            }
        }
    }
}

internal fun Modifier.outset(by: Dp): Modifier = layout { measurable, constraints ->
    val px = by.roundToPx()
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val placeable = measurable.measure(Constraints.fixed(width + px * 2, height + px * 2))
    layout(width, height) { placeable.place(-px, -px) }
}
