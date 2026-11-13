@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public sealed interface MenuEntry

@ExperimentalOneConfigUiApi
public class MenuAction(
    public val label: String,
    public val onClick: () -> Unit,
    public val icon: String? = null,
    public val painter: Painter? = null,
    public val detail: String? = null,
    style: ControlStyle = ControlStyle.Ghost,
    public val enabled: Boolean = true,
    public val disabledReason: String? = null,
    public val checked: Boolean? = null,
    public val shortcutHint: String? = null,
    public val badge: String? = null,
    public val size: MenuItemSize = MenuItemSize.Compact,
    public val leading: (@Composable () -> Unit)? = null,
    public val children: List<MenuEntry> = emptyList(),
    danger: Boolean = false,
) : MenuEntry {
    public val style: ControlStyle = if (danger) ControlStyle.Danger else style

    public val danger: Boolean get() = style == ControlStyle.Danger || style == ControlStyle.DangerFilled

    @Deprecated(
        "Use the constructor that takes onClick second",
        ReplaceWith("MenuAction(label, onClick, icon = icon, detail = detail, enabled = enabled, danger = danger)"),
    )
    public constructor(
        label: String,
        icon: String? = null,
        detail: String? = null,
        danger: Boolean = false,
        enabled: Boolean = true,
        onClick: () -> Unit,
    ) : this(label, onClick, icon = icon, detail = detail, enabled = enabled, danger = danger)
}

@ExperimentalOneConfigUiApi
public data object MenuSeparator : MenuEntry

@ExperimentalOneConfigUiApi
public class MenuHeading(public val text: String) : MenuEntry

@ExperimentalOneConfigUiApi
public class MenuRadioGroup<T>(
    public val options: List<T>,
    public val selected: T,
    public val label: (T) -> String,
    public val onSelect: (T) -> Unit,
) : MenuEntry

@ExperimentalOneConfigUiApi
public class MenuIconRow(public val items: List<MenuAction>) : MenuEntry

@ExperimentalOneConfigUiApi
public class MenuCustomItem(public val content: @Composable () -> Unit) : MenuEntry
