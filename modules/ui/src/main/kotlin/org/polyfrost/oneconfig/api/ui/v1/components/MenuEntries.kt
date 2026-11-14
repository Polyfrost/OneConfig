@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.page.PageText
import org.polyfrost.oneconfig.api.ui.v1.page.translate

@ExperimentalOneConfigUiApi
public sealed interface MenuEntry

@ExperimentalOneConfigUiApi
public class MenuAction(
    label: PageText,
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
    public constructor(
        label: String,
        onClick: () -> Unit,
        icon: String? = null,
        painter: Painter? = null,
        detail: String? = null,
        style: ControlStyle = ControlStyle.Ghost,
        enabled: Boolean = true,
        disabledReason: String? = null,
        checked: Boolean? = null,
        shortcutHint: String? = null,
        badge: String? = null,
        size: MenuItemSize = MenuItemSize.Compact,
        leading: (@Composable () -> Unit)? = null,
        children: List<MenuEntry> = emptyList(),
        danger: Boolean = false,
    ) : this(
        PageText.literal(label), onClick, icon, painter, detail, style, enabled, disabledReason, checked,
        shortcutHint, badge, size, leading, children, danger,
    )

    public val labelText: PageText = label

    public val label: String get() = labelText.translate()

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
