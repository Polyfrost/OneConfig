@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.page.PageText
import org.polyfrost.oneconfig.api.ui.v1.page.resolve
import org.polyfrost.oneconfig.api.ui.v1.page.translate
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.concentric

private val UNDERLINE_THICKNESS = 2.dp
private const val SEGMENT_SELECTED_ALPHA = 0.1f
private val PILL_PADDING = 14.dp
private val CORNER_BADGE_GAP = 1.dp
private val CORNER_BADGE_RISE = 7.dp

@ExperimentalOneConfigUiApi
public enum class TabStyle {
    Pill,

    Underline,

    Segmented,

    Chips,
}

@ExperimentalOneConfigUiApi
public enum class TabBadgePlacement {
    Inline,

    Corner,
}

@ExperimentalOneConfigUiApi
public enum class TabActivation {
    Automatic,

    Manual,
}

@ExperimentalOneConfigUiApi
public class TabItem<T>(
    public val value: T,
    label: PageText,
    public val icon: String? = null,
    public val enabled: Boolean = true,
    public val badge: String? = null,
    public val iconPainter: Painter? = null,
    public val size: ControlSize? = null,
    public val interactionSource: MutableInteractionSource? = null,
    public val tooltip: String? = null,
    public val modifier: Modifier = Modifier,
    public val badgeDescription: String? = null,
) {
    @JvmOverloads
    public constructor(
        value: T,
        label: String,
        icon: String? = null,
        enabled: Boolean = true,
        badge: String? = null,
        iconPainter: Painter? = null,
        size: ControlSize? = null,
        interactionSource: MutableInteractionSource? = null,
        tooltip: String? = null,
        modifier: Modifier = Modifier,
        badgeDescription: String? = null,
    ) : this(value, PageText.literal(label), icon, enabled, badge, iconPainter, size, interactionSource, tooltip, modifier, badgeDescription)

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
    interactionSource: MutableInteractionSource? = null,
    tooltipPlacement: TooltipPlacement = TooltipDefaults.placement,
    sound: UiSound? = UiSound.Click,
    onDeselect: ((T) -> Unit)? = null,
    track: Boolean = true,
    underlineWidth: Dp? = null,
    badgePlacement: TabBadgePlacement = TabBadgePlacement.Inline,
    activation: TabActivation = TabActivation.Automatic,
    underlineThickness: Dp? = null,
    slide: Boolean = false,
) {
    val theme = OneConfigTheme.current
    val fillModifier = if (fill) Modifier.fillMaxWidth() else Modifier
    val indicator = rememberSlidingIndicatorState(tabs.size)
    val focus = rememberChoiceFocus(tabs.size)
    val selectedIndex = tabs.indexOfFirst { it.value == selected }
    val indicatorAlpha = Modifier.graphicsLayer { alpha = if (enabled && tabs.getOrNull(selectedIndex)?.enabled != false) 1f else theme.disabledAlpha }
    val rowModifier = modifier
        .groupInteractions(interactionSource, enabled)
        .then(fillModifier)
        .choiceGroup(focus, selectedIndex, enabled, { enabled && tabs[it].enabled }, activation) { onSelect(tabs[it].value) }
    val click: (Int) -> Unit = { index ->
        val tab = tabs[index]
        val deselect = onDeselect
        if (index == selectedIndex && deselect != null) deselect(tab.value) else onSelect(tab.value)
    }

    fun item(index: Int, host: Modifier): Modifier = host.then(tabs[index].modifier).slidingIndicatorItem(indicator, index).choiceItem(focus, index)

    when (style) {
        TabStyle.Pill -> Box(rowModifier.slidingIndicatorContainer(indicator)) {
            if (slide) SlidingIndicator(indicator, selectedIndex, indicatorAlpha.choiceIndicator(theme.buttonShape, true))
            Row(fillModifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEachIndexed { index, tab ->
                    TabHost(tab, fill, false, tooltipPlacement) { host ->
                        PillTab(tab, index == selectedIndex, enabled && tab.enabled, tab.size ?: size, theme.buttonShape, sound, badgePlacement, slide, item(index, host)) { click(index) }
                    }
                }
            }
        }
        TabStyle.Underline -> {
            val hairline = theme.borderColor
            val density = LocalDensity.current
            val stroke = with(density) { (underlineThickness ?: UNDERLINE_THICKNESS).roundToPx() }
            val lineWidth = underlineWidth?.let { with(density) { it.roundToPx() } }
            Box(
                rowModifier
                    .then(
                        if (track) {
                            Modifier.drawBehind {
                                val y = this.size.height - 0.5.dp.toPx()
                                drawLine(hairline, Offset(0f, y), Offset(this.size.width, y), 1.dp.toPx())
                            }
                        } else {
                            Modifier
                        },
                    )
                    .slidingIndicatorContainer(indicator),
            ) {
                SlidingIndicator(
                    indicator,
                    selectedIndex,
                    Modifier.background(OneConfigTheme.accent),
                    area = { bounds ->
                        if (lineWidth == null) {
                            IntRect(bounds.left, bounds.bottom - stroke, bounds.right, bounds.bottom)
                        } else {
                            val width = lineWidth.coerceAtMost(bounds.width)
                            val left = bounds.center.x - width / 2
                            IntRect(left, bounds.bottom - stroke, left + width, bounds.bottom)
                        }
                    },
                )
                Row(fillModifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tabs.forEachIndexed { index, tab ->
                        TabHost(tab, fill, false, tooltipPlacement) { host ->
                            UnderlineTab(tab, index == selectedIndex, enabled && tab.enabled, tab.size ?: size, theme.shapes.small, sound, badgePlacement, item(index, host)) { click(index) }
                        }
                    }
                }
            }
        }
        TabStyle.Segmented -> {
            val trackShape = theme.buttonShape
            val itemShape = if (track) trackShape.concentric(ChoiceInset) else trackShape
            val rowHeight = tabs.maxOfOrNull { (it.size ?: size).height } ?: size.height
            Box(
                rowModifier
                    .height(rowHeight)
                    .then(if (track) Modifier.choiceTrack(trackShape).padding(ChoiceInset) else Modifier)
                    .slidingIndicatorContainer(indicator),
            ) {
                if (slide) SlidingIndicator(indicator, selectedIndex, indicatorAlpha.choiceIndicator(itemShape, false))
                Row(fillModifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    tabs.forEachIndexed { index, tab ->
                        TabHost(tab, fill, true, tooltipPlacement) { host ->
                            SegmentTab(tab, index == selectedIndex, enabled && tab.enabled, tab.size ?: size, itemShape, sound, badgePlacement, slide, item(index, host)) { click(index) }
                        }
                    }
                }
            }
        }
        TabStyle.Chips -> {
            FlowRow(
                rowModifier,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tabs.forEachIndexed { index, tab ->
                    TabHost(tab, false, false, tooltipPlacement) { host ->
                        ChipTab(tab, index == selectedIndex, enabled && tab.enabled, tab.size ?: size, ChipDefaults.shape, sound, badgePlacement, host.then(tab.modifier).choiceItem(focus, index)) { click(index) }
                    }
                }
            }
        }
    }
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
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
    interactionSource: MutableInteractionSource? = null,
    tooltipPlacement: TooltipPlacement = TooltipDefaults.placement,
    sound: UiSound? = UiSound.Click,
    onDeselect: ((T) -> Unit)? = null,
    track: Boolean = true,
    underlineWidth: Dp? = null,
    badgePlacement: TabBadgePlacement = TabBadgePlacement.Inline,
    activation: TabActivation = TabActivation.Automatic,
    underlineThickness: Dp? = null,
) {
    TabRow(tabs, selected, onSelect, modifier, style, size, fill, enabled, interactionSource, tooltipPlacement, sound, onDeselect, track, underlineWidth, badgePlacement, activation, underlineThickness, slide = false)
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
    val tabs = options.map { TabItem(it, label(it)) }
    TabRow(tabs, selected, onSelect, modifier, TabStyle.Chips, size, fill = false, enabled = enabled)
}

private fun RowScope.tabWeight(fill: Boolean): Modifier = if (fill) Modifier.weight(1f) else Modifier

@Composable
private fun RowScope.TabHost(tab: TabItem<*>, fill: Boolean, tall: Boolean, placement: TooltipPlacement, content: @Composable (Modifier) -> Unit) {
    val tooltip = tab.tooltip
    if (tooltip.isNullOrEmpty()) {
        content(tabWeight(fill))
        return
    }
    val outer = tabWeight(fill).then(if (tall) Modifier.fillMaxHeight() else Modifier)
    TooltipBox(tooltip, outer, placement) {
        content(if (fill) Modifier.fillMaxWidth() else Modifier)
    }
}

@Composable
private fun tabLabel(tab: TabItem<*>): String {
    val label = tab.labelText.resolve().ifEmpty { tab.tooltip.orEmpty() }
    val spoken = tab.badgeDescription ?: tab.badge
    return if (spoken.isNullOrEmpty()) label else "$label, $spoken"
}

@Composable
private fun PillTab(tab: TabItem<*>, selected: Boolean, enabled: Boolean, size: ControlSize, shape: Shape, sound: UiSound?, badges: TabBadgePlacement, slide: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val theme = OneConfigTheme.current
    val source = tab.interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val fill by Motion.animateColorAsState(if (selected) Color.Transparent else theme.componentBackground, label = "pillTabFill")
    val accentAlpha by Motion.animateFloatAsState(if (selected && !slide) 1f else 0f, label = "pillTabAccent")
    val border by Motion.animateColorAsState(
        when {
            selected -> Color.Transparent
            hovered && enabled -> theme.textColorSecondary
            else -> theme.borderColor
        },
        label = "pillTabBorder",
    )
    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .heightIn(min = ControlDefaults.metrics(size).height)
            .glassFrame(shape, { fill }, { border }, { accentAlpha }, hover = enabled)
            .oneConfigClickable(tabLabel(tab), onClick, role = A11yRole.Tab, enabled = enabled, selected = selected, shape = shape, indication = !glassAvailable, sound = sound, interactionSource = source)
            .padding(horizontal = PILL_PADDING),
        contentAlignment = Alignment.Center,
    ) {
        TabContent(tab, if (selected) theme.accentTextColor else theme.textColor, size, badges)
    }
}

@Composable
private fun ChipTab(tab: TabItem<*>, selected: Boolean, enabled: Boolean, size: ControlSize, shape: Shape, sound: UiSound?, badges: TabBadgePlacement, modifier: Modifier, onClick: () -> Unit) {
    val theme = OneConfigTheme.current
    val source = tab.interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val rest = ChipDefaults.fill
    val hoverFill = OneConfigTheme.accent.copy(alpha = ChipDefaults.HOVER_ACCENT_ALPHA).compositeOver(rest)
    val fill by Motion.animateColorAsState(if (hovered && enabled) hoverFill else rest, label = "chipTabFill")
    val accentAlpha by Motion.animateFloatAsState(if (selected) 1f else 0f, label = "chipTabAccent")
    val content by Motion.animateColorAsState(ChipDefaults.contentColor(selected), label = "chipTabContent")
    val border = theme.borderColor
    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .heightIn(min = ControlDefaults.metrics(size).height)
            .glassFrame(shape, { fill }, { border }, { accentAlpha }, hover = enabled)
            .oneConfigClickable(tabLabel(tab), onClick, role = A11yRole.Tab, enabled = enabled, selected = selected, shape = shape, indication = !glassAvailable, sound = sound, interactionSource = source)
            .padding(horizontal = ChipDefaults.horizontalPadding(size)),
        contentAlignment = Alignment.Center,
    ) {
        TabContent(tab, content, size, badges, ChipDefaults.contentSpacing(size))
    }
}

@Composable
private fun UnderlineTab(tab: TabItem<*>, selected: Boolean, enabled: Boolean, size: ControlSize, shape: Shape, sound: UiSound?, badges: TabBadgePlacement, modifier: Modifier, onClick: () -> Unit) {
    val theme = OneConfigTheme.current
    val source = tab.interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val content by Motion.animateColorAsState(if (selected || hovered && enabled) theme.textColor else theme.textColorSecondary, label = "underlineTabContent")
    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .height(ControlDefaults.metrics(size).height)
            .oneConfigClickable(tabLabel(tab), onClick, role = A11yRole.Tab, enabled = enabled, selected = selected, shape = shape, indication = false, sound = sound, interactionSource = source)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        TabContent(tab, content, size, badges)
    }
}

@Composable
private fun SegmentTab(tab: TabItem<*>, selected: Boolean, enabled: Boolean, size: ControlSize, shape: Shape, sound: UiSound?, badges: TabBadgePlacement, slide: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val theme = OneConfigTheme.current
    val source = tab.interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val fill by Motion.animateColorAsState(if (selected && !slide) theme.textColor.copy(alpha = SEGMENT_SELECTED_ALPHA) else Color.Transparent, label = "segmentTabFill")
    val content by Motion.animateColorAsState(
        when {
            selected && slide -> theme.accentTextColor
            selected || hovered && enabled -> theme.textColor
            else -> theme.textColorSecondary
        },
        label = "segmentTabContent",
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .glassFrame(shape, { fill }, { Color.Transparent }, hover = enabled, glass = false, pull = 0.75.dp)
            .oneConfigClickable(tabLabel(tab), onClick, role = A11yRole.Tab, enabled = enabled, selected = selected, shape = shape, indication = !glassAvailable, sound = sound, interactionSource = source, recipe = ControlDefaults.Underlay)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        TabContent(tab, content, size, badges)
    }
}

@Composable
private fun TabContent(tab: TabItem<*>, color: Color, size: ControlSize, badges: TabBadgePlacement, spacing: Dp = 6.dp) {
    val badge = tab.badge
    if (badges == TabBadgePlacement.Corner && badge != null) {
        Box(contentAlignment = Alignment.CenterEnd) {
            TabContentRow(tab, color, size, spacing, null)
            Badge(badge, Modifier.cornerBadge(), tone = BadgeTone.Accent)
        }
    } else {
        TabContentRow(tab, color, size, spacing, badge)
    }
}

private fun Modifier.cornerBadge(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0, maxWidth = Constraints.Infinity))
    layout(0, 0) { placeable.placeRelative(CORNER_BADGE_GAP.roundToPx(), -CORNER_BADGE_RISE.roundToPx() - placeable.height / 2) }
}

@Composable
private fun TabContentRow(tab: TabItem<*>, color: Color, size: ControlSize, spacing: Dp, badge: String?) {
    val compact = size == ControlSize.Compact
    val iconSize = if (compact) 14.dp else 16.dp
    Row(horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically) {
        val painter = tab.iconPainter
        when {
            painter != null -> Image(painter, null, Modifier.size(iconSize), colorFilter = ColorFilter.tint(color))
            tab.icon != null -> Icon(tab.icon, null, Modifier.size(iconSize), tint = color)
        }
        val label = tab.labelText.resolve()
        if (label.isNotEmpty()) {
            val style = OneConfigTheme.typography.body.copy(fontSize = if (compact) 13.sp else 14.sp, fontWeight = FontWeight.Normal)
            Text(label, Modifier, color, style, 1, TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
        badge?.let { Badge(it, tone = BadgeTone.Accent) }
    }
}
