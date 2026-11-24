@file:JvmMultifileClass
@file:JvmName("MenuKt")
@file:OptIn(ExperimentalOneConfigUiApi::class, ExperimentalVersionOverloading::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
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
import org.polyfrost.compose.host.controller.Controllers
import org.polyfrost.compose.host.controller.controllerArrows
import org.polyfrost.compose.host.controller.hidesFocusRing
import org.polyfrost.compose.host.controller.isFromController
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11y
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.a11y.focusRing
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

internal val LocalMenuSound = staticCompositionLocalOf<UiSound?> { UiSound.Click }

@ExperimentalOneConfigUiApi
public object MenuDefaults {
    public val size: MenuItemSize = MenuItemSize.Compact

    public val minWidth: Dp = 170.dp

    public val maxWidth: Dp = 280.dp

    public val contentPadding: Dp = 4.dp

    public val itemSpacing: Dp = 2.dp

    public val tooltipMaxWidth: Dp = 320.dp

    public val submenuGap: Dp = 2.dp

    public val anchorGap: Dp = 6.dp

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
    @IntroducedAt("1.8.0") modifier: Modifier = Modifier,
    @IntroducedAt("1.8.0") icon: String? = null,
    @IntroducedAt("1.8.0") painter: Painter? = null,
    @IntroducedAt("1.8.0") detail: String? = null,
    @IntroducedAt("1.8.0") style: ControlStyle = ControlStyle.Ghost,
    @IntroducedAt("1.8.0") size: MenuItemSize = MenuDefaults.size,
    @IntroducedAt("1.8.0") enabled: Boolean = true,
    @IntroducedAt("1.8.0") selected: Boolean = false,
    @IntroducedAt("1.8.0") checked: Boolean? = null,
    @IntroducedAt("1.8.0") shortcutHint: String? = null,
    @IntroducedAt("1.8.0") badge: String? = null,
    @IntroducedAt("1.8.0") tooltip: String? = null,
    @IntroducedAt("1.8.0") danger: Boolean = false,
    @IntroducedAt("1.8.0") leading: (@Composable () -> Unit)? = null,
    @IntroducedAt("1.8.0") trailing: (@Composable () -> Unit)? = null,
    @IntroducedAt("1.8.0") interactionSource: MutableInteractionSource? = null,
    @IntroducedAt("1.8.0") sound: UiSound? = LocalMenuSound.current,
    @IntroducedAt("1.8.0") dangerColor: Color = Color.Unspecified,
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
        sound = sound,
        dangerColor = dangerColor,
    )
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
    sound: UiSound? = LocalMenuSound.current,
    dangerColor: Color = Color.Unspecified,
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
        sound?.let(::playUiSound)
        click()
    }
    navHolder[0] = navItem
    MenuNavHover(level, navItem, hovered)
    val highlighted = navItem != null && level?.highlighted === navItem
    val submenuOpen = navItem != null && level?.openChild === navItem
    val active = if (enabled) hovered || highlighted || submenuOpen else highlighted && keyboard
    val base = when {
        selected -> theme.accentTextColor
        dangerColor.isSpecified && (style == ControlStyle.Danger || style == ControlStyle.DangerFilled) -> dangerColor
        else -> MenuDefaults.contentColor(style)
    }
    val color = if (enabled) base else theme.textColorSecondary
    val background by animateColorAsState(
        when {
            selected -> accent
            active -> base.copy(alpha = MenuDefaults.HOVER_ALPHA)
            else -> base.copy(alpha = 0f)
        },
        Motion.colorSpec(Motion.fast),
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
                .focusRing(shape, highlighted)
                .clip(shape)
                .background(background, shape)
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
                    sound = sound,
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
    surface: (@Composable (Modifier) -> Modifier)? = null,
    tabNavigation: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (surface != null) {
        ProvideMenuSurface(surface) { MenuSurface(modifier, maxHeight, null, tabNavigation, content) }
        return
    }
    val focus = remember { FocusRequester() }
    val controller = remember { MenuNavController(focus) }
    val controllerFocus = remember { MenuControllerFocus(controller, focus) }
    val focusManager = LocalFocusManager.current
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
                .onKeyEvent { event ->
                    controllerFocus.onKey(event, focusManager)?.let { return@onKeyEvent it }
                    (tabNavigation && controller.onTab(event)) || controller.onKey(event, rtl)
                }
                .focusRequester(focus)
                .controllerArrows()
                .hidesFocusRing()
                .onFocusChanged(controllerFocus::onFocusChanged)
                .focusProperties { onExit = { if (controller.probing != null) cancelFocusChange() } }
                .focusable(),
            maxHeight,
            Modifier.focusRequester(controllerFocus.content).focusGroup(),
            content,
        )
    }
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun MenuSurface(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    MenuSurface(modifier, Dp.Unspecified, null, false, content)
}

private fun MenuNavController.onTab(event: KeyEvent): Boolean {
    if (event.key != Key.Tab) return false
    if (event.type != KeyEventType.KeyDown) return true
    if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return false
    val level = active
    val items = level.ordered()
    if (items.isEmpty()) return true
    val step = if (event.isShiftPressed) -1 else 1
    val at = items.indexOfFirst { it === level.highlighted }
    val next = if (at < 0) {
        if (step > 0) 0 else items.lastIndex
    } else {
        (at + step).mod(items.size)
    }
    level.select(items[next])
    return true
}

private val LocalMenuSurface = staticCompositionLocalOf<(@Composable (Modifier) -> Modifier)?> { null }

@ExperimentalOneConfigUiApi
@Composable
public fun ProvideMenuSurface(surface: @Composable (Modifier) -> Modifier, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMenuSurface provides surface, content = content)
}

@Composable
internal fun MenuPanel(modifier: Modifier, maxHeight: Dp, inner: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val padding = MenuDefaults.contentPadding
    val sized = modifier
        .widthIn(min = MenuDefaults.minWidth, max = MenuDefaults.maxWidth)
        .width(IntrinsicSize.Max)
        .then(if (maxHeight.isSpecified) Modifier.heightIn(max = maxHeight) else Modifier)
    val override = LocalMenuSurface.current
    val surfaced = if (override != null) override(sized) else componentsBridge().popupSurface(sized, MenuDefaults.shape)
    Column(
        modifier = surfaced.verticalScroll(rememberScrollState()).padding(padding).then(inner),
        verticalArrangement = Arrangement.spacedBy(MenuDefaults.itemSpacing),
        content = content,
    )
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
    @IntroducedAt("1.8.0") modifier: Modifier = Modifier,
    @IntroducedAt("1.8.0") size: MenuItemSize = MenuItemSize.Compact,
    @IntroducedAt("1.8.0") surface: (@Composable (Modifier) -> Modifier)? = null,
    @IntroducedAt("1.8.0") tabNavigation: Boolean = false,
) {
    MenuSurface(modifier, surface = surface, tabNavigation = tabNavigation) {
        MenuEntryItems(entries, onSelect, size)
    }
}

private class MenuControllerFocus(private val controller: MenuNavController, private val surface: FocusRequester) {
    val content = FocusRequester()
    private var surfaceFocused: Boolean = false

    fun onFocusChanged(state: FocusState) {
        surfaceFocused = state.isFocused
        if (state.hasFocus && !state.isFocused && controller.probing == null && Controllers.isActive) leaveRows(controller.root)
    }

    fun onKey(event: KeyEvent, focusManager: FocusManager): Boolean? {
        if (!event.isFromController) return null
        val direction = when (event.key) {
            Key.DirectionUp -> FocusDirection.Up
            Key.DirectionDown -> FocusDirection.Down
            Key.DirectionLeft -> FocusDirection.Left
            Key.DirectionRight -> FocusDirection.Right
            Key.Enter, Key.NumPadEnter, Key.Spacebar -> return if (surfaceFocused) null else false
            else -> return null
        }
        if (event.type != KeyEventType.KeyDown || event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) {
            return if (surfaceFocused) null else false
        }
        val vertical = direction == FocusDirection.Up || direction == FocusDirection.Down
        val level = controller.root
        if (!surfaceFocused) {
            return probe(level) { focusManager.moveFocus(direction) } || vertical && probe(level) { content.requestFocus(direction) }
        }
        if (!vertical || controller.active !== level) return null
        val from = level.highlighted
        val moved = probe(level) { from != null && from.focus.requestFocus() && focusManager.moveFocus(direction) } ||
            probe(level) { content.requestFocus(direction) }
        return if (moved) true else null
    }

    private fun probe(level: MenuNavLevel, move: () -> Boolean): Boolean {
        controller.probing = level
        controller.probed = null
        try {
            val moved = runCatching(move).getOrDefault(false)
            val row = controller.probed
            if (row != null) {
                runCatching { surface.requestFocus() }
                if (moved) level.select(row)
                return moved
            }
            if (moved) leaveRows(level)
            return moved
        } finally {
            controller.probing = null
            controller.probed = null
        }
    }

    private fun leaveRows(level: MenuNavLevel) {
        controller.active = level
        level.openChild = null
        level.hoverIntent = null
        level.highlighted = null
    }
}

@Composable
internal fun MenuCustomSlot(content: @Composable () -> Unit) {
    if (LocalMenuLevel.current == null) {
        content()
        return
    }
    Column(Modifier.focusGroup(), verticalArrangement = Arrangement.spacedBy(MenuDefaults.itemSpacing)) {
        content()
    }
}
