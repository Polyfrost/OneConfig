@file:JvmMultifileClass
@file:JvmName("MenuKt")
@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11y
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.a11y.isKeyboardMode
import org.polyfrost.oneconfig.api.ui.v1.a11y.notFocusable
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.concentric

@ExperimentalOneConfigUiApi
public enum class MenuItemSize(internal val height: Dp, internal val textSize: TextUnit, internal val iconSize: Dp, internal val inset: Dp) {
    Compact(30.dp, 13.sp, 14.dp, 8.dp),
    Regular(38.dp, 14.sp, 18.dp, 10.dp),
}

@ExperimentalOneConfigUiApi
public object MenuDefaults {
    public val size: MenuItemSize = MenuItemSize.Compact

    public val minWidth: Dp = 170.dp

    public val maxWidth: Dp = 280.dp

    public val contentPadding: Dp = 4.dp

    public val itemSpacing: Dp = 2.dp

    public val tooltipMaxWidth: Dp = 320.dp

    public val submenuGap: Dp = 2.dp

    public const val HOVER_ALPHA: Float = 0.10f

    public const val SUBMENU_DELAY_MILLIS: Long = 200L

    public const val TYPE_AHEAD_RESET_MILLIS: Long = 700L

    public val shape: Shape
        @Composable get() = OneConfigTheme.current.popupShape

    public val itemShape: Shape
        @Composable get() = OneConfigTheme.current.popupShape.concentric(contentPadding)

    @Composable
    public fun contentColor(style: ControlStyle): Color {
        val theme = OneConfigTheme.current
        return when (style) {
            ControlStyle.Danger, ControlStyle.DangerFilled -> theme.error
            else -> theme.textColor
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun MenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    painter: Painter? = null,
    detail: String? = null,
    style: ControlStyle = ControlStyle.Ghost,
    size: MenuItemSize = MenuDefaults.size,
    enabled: Boolean = true,
    selected: Boolean = false,
    checked: Boolean? = null,
    shortcutHint: String? = null,
    badge: String? = null,
    tooltip: String? = null,
    danger: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    MenuItemRow(
        label = label,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        painter = painter,
        detail = detail,
        style = if (danger) ControlStyle.Danger else style,
        size = size,
        enabled = enabled,
        selected = selected,
        checked = checked,
        shortcutHint = shortcutHint,
        badge = badge,
        tooltip = tooltip,
        leading = leading,
        trailing = trailing,
        interactionSource = interactionSource,
    )
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun MenuItem(label: String, onClick: () -> Unit) {
    MenuItem(label, onClick, Modifier)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun MenuItem(label: String, icon: String?, onClick: () -> Unit) {
    MenuItem(label, onClick, Modifier, icon = icon)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun MenuItem(
    label: String,
    icon: String?,
    detail: String?,
    size: MenuItemSize,
    danger: Boolean,
    selected: Boolean,
    enabled: Boolean,
    tooltip: String?,
    modifier: Modifier,
    leading: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    MenuItem(
        label = label,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        detail = detail,
        size = size,
        enabled = enabled,
        selected = selected,
        tooltip = tooltip,
        danger = danger,
        leading = leading,
    )
}

@Composable
internal fun MenuItemRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    painter: Painter? = null,
    detail: String? = null,
    style: ControlStyle = ControlStyle.Ghost,
    size: MenuItemSize = MenuDefaults.size,
    enabled: Boolean = true,
    selected: Boolean = false,
    checked: Boolean? = null,
    shortcutHint: String? = null,
    badge: String? = null,
    tooltip: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    radio: Boolean = false,
    reserveCheck: Boolean = false,
    reserveIcon: Boolean = false,
    submenu: (@Composable (MenuNavItem) -> Unit)? = null,
    styledLabel: AnnotatedString? = null,
) {
    val theme = OneConfigTheme.current
    val accent = OneConfigTheme.accent
    val level = LocalMenuLevel.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val keyboard = isKeyboardMode()
    val submenuLevel = if (submenu != null) level else null
    val opensSubmenu = submenuLevel != null
    val navHolder = remember { arrayOfNulls<MenuNavItem>(1) }
    val click: () -> Unit = {
        val item = navHolder[0]
        if (submenuLevel != null && item != null) submenuLevel.toggle(item, false) else onClick()
    }
    val navItem = rememberMenuNavItem(level, label, enabled, opensSubmenu) {
        playUiSound(UiSound.Click)
        click()
    }
    navHolder[0] = navItem
    MenuNavHover(level, navItem, hovered)
    val highlighted = navItem != null && level?.highlighted === navItem
    val submenuOpen = navItem != null && level?.openChild === navItem
    val active = if (enabled) hovered || highlighted || submenuOpen else highlighted && keyboard
    val base = when {
        selected -> theme.accentTextColor
        else -> MenuDefaults.contentColor(style)
    }
    val color = if (enabled) base else theme.textColorSecondary
    val background by animateColorAsState(
        when {
            selected -> accent
            active -> base.copy(alpha = MenuDefaults.HOVER_ALPHA)
            else -> base.copy(alpha = 0f)
        },
        Motion.spec(tween(120)),
    )
    val shape = MenuDefaults.itemShape
    val spoken = if (detail == null) label else "$label. $detail"
    val role = if (radio) A11yRole.RadioButton else A11yRole.MenuItem
    LaunchedEffect(highlighted, keyboard) {
        if (highlighted && keyboard) A11y.focused(spoken, role, selected.takeIf { it }, checked)
    }
    val wantTooltip = tooltip != null && (hovered || highlighted && keyboard)
    var tooltipShown by remember { mutableStateOf(false) }
    LaunchedEffect(wantTooltip) {
        if (wantTooltip) {
            delay(TooltipDefaults.delayMillis)
            tooltipShown = true
        } else {
            tooltipShown = false
        }
    }
    Box(modifier.menuNavPosition(navItem)) {
        if (tooltipShown && tooltip != null) TooltipPopup(tooltip, Modifier, TooltipPlacement.AboveCenter, MenuDefaults.tooltipMaxWidth)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = size.height)
                .clip(shape)
                .background(background, shape)
                .then(if (highlighted && keyboard) Modifier.border(1.dp, theme.focusRingColor, shape) else Modifier)
                .hoverable(source)
                .then(if (level != null) Modifier.notFocusable() else Modifier)
                .oneConfigClickable(
                    label = spoken,
                    onClick = click,
                    role = role,
                    enabled = enabled,
                    selected = selected.takeIf { it },
                    checked = checked,
                    shape = shape,
                    indication = false,
                    interactionSource = source,
                )
                .then(if (enabled) Modifier else Modifier.graphicsLayer { alpha = theme.disabledAlpha })
                .padding(horizontal = size.inset, vertical = if (detail == null) 0.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(size.inset),
        ) {
            if (checked != null || reserveCheck) {
                Box(Modifier.size(size.iconSize), contentAlignment = Alignment.Center) {
                    if (checked == true) {
                        if (radio) Box(Modifier.size(size.iconSize / 2.5f).background(color, theme.shapes.circle))
                        else Icon("tick", null, Modifier.size(size.iconSize), color)
                    }
                }
            }
            when {
                leading != null -> leading()
                icon != null -> Icon(icon, null, Modifier.size(size.iconSize), color)
                painter != null -> Icon(painter, null, Modifier.size(size.iconSize), color)
                reserveIcon -> Box(Modifier.size(size.iconSize))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val labelStyle = theme.typography.body.copy(fontSize = size.textSize, fontWeight = FontWeight.Normal)
                if (styledLabel != null) Text(styledLabel, Modifier, color, labelStyle, 1, TextOverflow.Ellipsis)
                else Text(label, Modifier, color, labelStyle, 1, TextOverflow.Ellipsis)
                if (detail != null) {
                    Text(detail, Modifier, theme.textColorSecondary, theme.typography.caption)
                }
            }
            if (badge != null) Badge(badge)
            if (shortcutHint != null) {
                Text(shortcutHint, Modifier, theme.textColorSecondary, theme.typography.caption, 1, TextOverflow.Clip, softWrap = false)
            }
            trailing?.invoke()
            if (opensSubmenu) {
                val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                Icon(if (rtl) "left-arrow" else "right-arrow", null, Modifier.size(size.iconSize), theme.textColorSecondary)
            }
        }
        if (submenu != null && navItem != null && level?.openChild === navItem) {
            Box(Modifier.matchParentSize().outset(MenuDefaults.contentPadding)) { submenu(navItem) }
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun MenuSurface(
    modifier: Modifier = Modifier,
    maxHeight: Dp = Dp.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    val controller = remember { MenuNavController() }
    val focus = remember { FocusRequester() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    LaunchedEffect(controller) {
        runCatching { focus.requestFocus() }
        if (A11y.keyboardMode) {
            withFrameNanos { }
            controller.root.highlightFirst()
        }
    }
    MenuHoverIntentEffect(controller.root)
    CompositionLocalProvider(LocalMenuLevel provides controller.root) {
        MenuPanel(
            modifier
                .onKeyEvent { controller.onKey(it, rtl) }
                .focusRequester(focus)
                .focusable(),
            maxHeight,
            content,
        )
    }
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun MenuSurface(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    MenuSurface(modifier, Dp.Unspecified, content)
}

@Composable
internal fun MenuPanel(modifier: Modifier, maxHeight: Dp, content: @Composable ColumnScope.() -> Unit) {
    val padding = MenuDefaults.contentPadding
    Column(
        modifier = componentsBridge().popupSurface(
            modifier
                .widthIn(min = MenuDefaults.minWidth, max = MenuDefaults.maxWidth)
                .width(IntrinsicSize.Max)
                .then(if (maxHeight.isSpecified) Modifier.heightIn(max = maxHeight) else Modifier),
            MenuDefaults.shape,
        ).verticalScroll(rememberScrollState()).padding(padding),
        verticalArrangement = Arrangement.spacedBy(MenuDefaults.itemSpacing),
        content = content,
    )
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun MenuList(items: List<MenuAction>, onPicked: () -> Unit) {
    MenuList(items, onPicked, Modifier, MenuItemSize.Compact)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun MenuList(items: List<MenuAction>, size: MenuItemSize, onPicked: () -> Unit) {
    MenuList(items, onPicked, Modifier, size)
}

@ExperimentalOneConfigUiApi
@Composable
public fun MenuList(
    entries: List<MenuEntry>,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    size: MenuItemSize = MenuItemSize.Compact,
) {
    MenuSurface(modifier) {
        MenuEntryItems(entries, onSelect, size)
    }
}
