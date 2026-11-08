@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.a11y
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.concentric
import kotlin.math.roundToInt

@ExperimentalOneConfigUiApi
enum class MenuItemSize(internal val height: Dp, internal val textSize: TextUnit, internal val iconSize: Dp, internal val inset: Dp) {
    Compact(30.dp, 13.sp, 14.dp, 8.dp),
    Regular(38.dp, 14.sp, 18.dp, 10.dp),
}

@ExperimentalOneConfigUiApi
class MenuAction(
    val label: String,
    val icon: String? = null,
    val detail: String? = null,
    val danger: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

private val MENU_INSET = 4.dp
private val MENU_MIN_WIDTH = 170.dp
private val MENU_MAX_WIDTH = 280.dp
private val MENU_TOOLTIP_WIDTH = 320.dp
private const val HOVER_ALPHA = 0.10f

@ExperimentalOneConfigUiApi
@Composable
fun MenuItem(label: String, onClick: () -> Unit) {
    MenuItem(label, null, null, MenuItemSize.Compact, false, false, true, null, Modifier, null, onClick)
}

@ExperimentalOneConfigUiApi
@Composable
fun MenuItem(label: String, icon: String?, onClick: () -> Unit) {
    MenuItem(label, icon, null, MenuItemSize.Compact, false, false, true, null, Modifier, null, onClick)
}

@ExperimentalOneConfigUiApi
@Composable
fun MenuItem(
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
    val theme = OneConfigTheme.current
    val accent = OneConfigTheme.accent
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val base = when {
        selected -> theme.accentTextColor
        danger -> DangerColor
        else -> theme.textColor
    }
    val color = if (enabled) base else theme.textColorSecondary
    val background by animateColorAsState(
        when {
            selected -> accent
            enabled && hovered -> base.copy(alpha = HOVER_ALPHA)
            else -> base.copy(alpha = 0f)
        },
        tween(120),
    )
    val shape = theme.popupShape.concentric(MENU_INSET)
    val spoken = if (detail == null) label else "$label. $detail"
    Box(modifier) {
        if (tooltip != null && hovered) TooltipPopup(tooltip, TooltipPlacement.AboveCenter, MENU_TOOLTIP_WIDTH)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = size.height)
                .clip(shape)
                .background(background, shape)
                .hoverable(source)
                .then(
                    if (enabled) {
                        Modifier.a11y(spoken, A11yRole.Button, selected.takeIf { it }, null, shape, true)
                            .onClick(source, onClick)
                            .pointerHoverIcon(PointerIcon.Hand)
                    } else {
                        Modifier.graphicsLayer { alpha = DISABLED_ALPHA }
                    },
                )
                .padding(horizontal = size.inset, vertical = if (detail == null) 0.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(size.inset),
        ) {
            if (leading != null) leading() else if (icon != null) Icon(icon, color, Modifier.size(size.iconSize))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, Modifier, color, size.textSize, null, BaselineShift.None, FontWeight.Normal, TextAlign.Start, 1, TextOverflow.Ellipsis)
                if (detail != null) {
                    Text(detail, Modifier, theme.textColorSecondary, 11.sp, null, BaselineShift.None, FontWeight.Normal, TextAlign.Start, Int.MAX_VALUE, TextOverflow.Clip)
                }
            }
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
fun MenuSurface(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = componentsBridge().popupSurface(
            modifier.widthIn(min = MENU_MIN_WIDTH, max = MENU_MAX_WIDTH).width(IntrinsicSize.Max),
            OneConfigTheme.current.popupShape,
        ).padding(MENU_INSET),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content,
    )
}

@ExperimentalOneConfigUiApi
@Composable
fun MenuList(items: List<MenuAction>, onPicked: () -> Unit) {
    MenuList(items, MenuItemSize.Compact, onPicked)
}

@ExperimentalOneConfigUiApi
@Composable
fun MenuList(items: List<MenuAction>, size: MenuItemSize, onPicked: () -> Unit) {
    MenuSurface(Modifier) {
        items.forEach { item ->
            MenuItem(item.label, item.icon, item.detail, size, item.danger, false, item.enabled, null, Modifier, null) {
                onPicked()
                item.onClick()
            }
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
fun ContextMenu(expanded: Boolean, offset: IntOffset, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (!expanded) return
    EscapeCloses(onDismiss)
    val provider = remember(offset) { PointPosition(offset, true) }
    componentsBridge().Popup(provider, onDismiss, true) {
        MenuSurface(Modifier, content)
    }
}

@ExperimentalOneConfigUiApi
@Composable
fun ContextMenu(expanded: Boolean, offset: IntOffset, onDismiss: () -> Unit, items: List<MenuAction>) {
    if (!expanded) return
    EscapeCloses(onDismiss)
    val provider = remember(offset) { PointPosition(offset, true) }
    componentsBridge().Popup(provider, onDismiss, true) {
        MenuList(items, onDismiss)
    }
}

private class OpenContextMenu(val at: IntOffset, val items: List<MenuAction>)

private class ContextMenuState {
    var open by mutableStateOf<OpenContextMenu?>(null)
}

private class CoordinatesHolder {
    var coordinates: LayoutCoordinates? = null
}

private val LocalContextMenu = staticCompositionLocalOf<ContextMenuState?> { null }

@ExperimentalOneConfigUiApi
@Composable
fun ContextMenuHost(content: @Composable () -> Unit) {
    val state = remember { ContextMenuState() }
    CompositionLocalProvider(LocalContextMenu provides state, content = content)
    val menu = state.open ?: return
    key(menu) {
        val close = { if (state.open === menu) state.open = null }
        EscapeCloses(close)
        val provider = remember(menu.at) { PointPosition(menu.at, false) }
        componentsBridge().Popup(provider, close, true) {
            MenuList(menu.items, close)
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
fun Modifier.contextMenu(items: () -> List<MenuAction>): Modifier {
    val state = LocalContextMenu.current ?: return this
    val latest by rememberUpdatedState(items)
    val holder = remember { CoordinatesHolder() }
    return this
        .onGloballyPositioned { holder.coordinates = it }
        .pointerInput(state) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (event.type != PointerEventType.Press || !event.secondaryPress()) continue
                    val change = event.changes.firstOrNull()?.takeUnless { it.isConsumed } ?: continue
                    val coordinates = holder.coordinates?.takeIf { it.isAttached } ?: continue
                    val list = latest()
                    if (list.isEmpty()) continue
                    change.consume()
                    val point = coordinates.localToWindow(change.position)
                    state.open = OpenContextMenu(IntOffset(point.x.roundToInt(), point.y.roundToInt()), list)
                }
            }
        }
}

private fun PointerEvent.secondaryPress(): Boolean = buttons.isSecondaryPressed && !buttons.isPrimaryPressed
