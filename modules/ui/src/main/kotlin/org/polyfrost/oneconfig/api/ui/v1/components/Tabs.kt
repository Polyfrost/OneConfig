@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.page.PageText
import org.polyfrost.oneconfig.api.ui.v1.page.resolve
import org.polyfrost.oneconfig.api.ui.v1.page.translate
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private const val TAB_FADE_MILLIS = 150
private const val SEGMENT_SELECTED_ALPHA = 0.1f
private val SEGMENT_INSET = 3.dp
private val UNDERLINE_WIDTH = 2.dp

@ExperimentalOneConfigUiApi
public enum class TabStyle {
    Pill,

    Underline,

    Segmented,
}

@ExperimentalOneConfigUiApi
public class TabItem<T>(
    public val value: T,
    label: PageText,
    public val icon: String? = null,
    public val enabled: Boolean = true,
    public val badge: String? = null,
    public val iconPainter: Painter? = null,
) {
    public constructor(
        value: T,
        label: String,
        icon: String? = null,
        enabled: Boolean = true,
        badge: String? = null,
        iconPainter: Painter? = null,
    ) : this(value, PageText.literal(label), icon, enabled, badge, iconPainter)

    @Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
    public constructor(value: T, label: String) : this(value, PageText.literal(label), null, true, null, null)

    public val labelText: PageText = label

    public val label: String get() = labelText.translate()
}

@ExperimentalOneConfigUiApi
@Composable
public fun <T> TabRow(
    tabs: List<TabItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    style: TabStyle = TabStyle.Pill,
    size: ControlSize = ControlSize.Regular,
    fill: Boolean = true,
    enabled: Boolean = true,
) {
    val rowModifier = modifier.then(if (fill) Modifier.fillMaxWidth() else Modifier)
    when (style) {
        TabStyle.Pill -> Row(rowModifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEach { tab ->
                PillTab(tab, tab.value == selected, enabled && tab.enabled, size, tabWeight(fill)) { onSelect(tab.value) }
            }
        }
        TabStyle.Underline -> {
            val hairline = OneConfigTheme.current.borderColor
            Row(
                rowModifier.drawBehind {
                    val y = this.size.height - 0.5.dp.toPx()
                    drawLine(hairline, Offset(0f, y), Offset(this.size.width, y), 1.dp.toPx())
                },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                tabs.forEach { tab ->
                    UnderlineTab(tab, tab.value == selected, enabled && tab.enabled, size, tabWeight(fill)) { onSelect(tab.value) }
                }
            }
        }
        TabStyle.Segmented -> {
            val theme = OneConfigTheme.current
            val track = theme.componentBackground
            val border = theme.borderColor
            Row(
                rowModifier
                    .height(size.height)
                    .glassFrame(theme.buttonShape, { track }, { border }, hover = false)
                    .padding(SEGMENT_INSET),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                tabs.forEach { tab ->
                    SegmentTab(tab, tab.value == selected, enabled && tab.enabled, size, tabWeight(fill)) { onSelect(tab.value) }
                }
            }
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun <T> SegmentedChips(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
    enabled: Boolean = true,
    size: ControlSize = ChipDefaults.size,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        options.forEach { option ->
            Chip(label(option), option == selected, { onSelect(option) }, enabled = enabled, size = size, role = A11yRole.Tab)
        }
    }
}

private fun RowScope.tabWeight(fill: Boolean): Modifier = if (fill) Modifier.weight(1f) else Modifier

@Composable
private fun tabLabel(tab: TabItem<*>): String {
    val label = tab.labelText.resolve()
    return if (tab.badge.isNullOrEmpty()) label else "$label, ${tab.badge}"
}

@Composable
private fun PillTab(tab: TabItem<*>, selected: Boolean, enabled: Boolean, size: ControlSize, modifier: Modifier, onClick: () -> Unit) {
    val theme = OneConfigTheme.current
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val active = hovered && enabled
    val fill by animateColorAsState(if (selected) Color.Transparent else theme.componentBackground, Motion.spec(tween(TAB_FADE_MILLIS)))
    val accentAlpha by animateFloatAsState(if (selected) 1f else 0f, Motion.spec(tween(TAB_FADE_MILLIS)))
    val border by animateColorAsState(
        when {
            selected -> Color.Transparent
            active -> theme.textColorSecondary
            else -> theme.borderColor
        },
        Motion.spec(tween(TAB_FADE_MILLIS)),
    )
    val shape = theme.buttonShape
    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .heightIn(min = size.height)
            .glassFrame(shape, { fill }, { border }, { accentAlpha }, hover = enabled)
            .oneConfigClickable(tabLabel(tab), onClick, role = A11yRole.Tab, enabled = enabled, selected = selected, shape = shape, indication = !glassAvailable, interactionSource = source)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        TabContent(tab, if (selected) theme.accentTextColor else theme.textColor, size)
    }
}

@Composable
private fun UnderlineTab(tab: TabItem<*>, selected: Boolean, enabled: Boolean, size: ControlSize, modifier: Modifier, onClick: () -> Unit) {
    val theme = OneConfigTheme.current
    val accent = OneConfigTheme.accent
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val content by animateColorAsState(
        if (selected || hovered && enabled) theme.textColor else theme.textColorSecondary,
        Motion.spec(tween(TAB_FADE_MILLIS)),
    )
    val indicator by animateFloatAsState(if (selected) 1f else 0f, Motion.spec(tween(TAB_FADE_MILLIS)))
    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .height(size.height)
            .oneConfigClickable(tabLabel(tab), onClick, role = A11yRole.Tab, enabled = enabled, selected = selected, shape = theme.shapes.small, indication = false, interactionSource = source)
            .drawBehind {
                if (indicator <= 0f) return@drawBehind
                val stroke = UNDERLINE_WIDTH.toPx()
                val y = this.size.height - stroke / 2f
                drawLine(accent.copy(alpha = accent.alpha * indicator), Offset(0f, y), Offset(this.size.width, y), stroke)
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        TabContent(tab, content, size)
    }
}

@Composable
private fun SegmentTab(tab: TabItem<*>, selected: Boolean, enabled: Boolean, size: ControlSize, modifier: Modifier, onClick: () -> Unit) {
    val theme = OneConfigTheme.current
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val fill by animateColorAsState(
        if (selected) theme.textColor.copy(alpha = SEGMENT_SELECTED_ALPHA) else Color.Transparent,
        Motion.spec(tween(TAB_FADE_MILLIS)),
    )
    val content by animateColorAsState(
        if (selected || hovered && enabled) theme.textColor else theme.textColorSecondary,
        Motion.spec(tween(TAB_FADE_MILLIS)),
    )
    val shape = componentsBridge().concentric(theme.buttonShape, SEGMENT_INSET, 0.dp)
    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .glassFrame(shape, { fill }, { Color.Transparent }, hover = enabled, glass = false, pull = 0.75.dp)
            .oneConfigClickable(tabLabel(tab), onClick, role = A11yRole.Tab, enabled = enabled, selected = selected, shape = shape, indication = !glassAvailable, interactionSource = source)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        TabContent(tab, content, size)
    }
}

@Composable
private fun TabContent(tab: TabItem<*>, color: Color, size: ControlSize) {
    val iconSize: Dp = if (size == ControlSize.Compact) 14.dp else 16.dp
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        val painter = tab.iconPainter
        when {
            painter != null -> Image(painter, null, Modifier.size(iconSize), colorFilter = ColorFilter.tint(color))
            tab.icon != null -> Icon(tab.icon, null, Modifier.size(iconSize), tint = color)
        }
        val label = tab.labelText.resolve()
        if (label.isNotEmpty()) {
            Text(label, Modifier, color, OneConfigTheme.typography.body.copy(fontSize = if (size == ControlSize.Compact) 13.sp else 14.sp, fontWeight = FontWeight.Normal), 1, TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
        tab.badge?.let { TabBadge(it) }
    }
}

@Composable
private fun TabBadge(text: String) {
    val theme = OneConfigTheme.current
    val accent = OneConfigTheme.accent
    if (text.isEmpty()) {
        Box(Modifier.size(8.dp).background(accent, theme.shapes.circle))
        return
    }
    Box(
        Modifier
            .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
            .background(accent, theme.shapes.pill)
            .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, Modifier, theme.accentTextColor, OneConfigTheme.typography.body.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium), 1, TextOverflow.Clip, textAlign = TextAlign.Center)
    }
}
