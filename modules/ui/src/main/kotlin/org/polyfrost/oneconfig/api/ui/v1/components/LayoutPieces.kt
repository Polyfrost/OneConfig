@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigUITheme
import org.polyfrost.oneconfig.api.ui.v1.theme.contrastAgainst
import org.polyfrost.oneconfig.api.ui.v1.theme.legibleOn

@ExperimentalOneConfigUiApi
public enum class BadgeTone {
    Neutral,

    Accent,

    Success,

    Warning,

    Danger,
}

private const val BADGE_FILL_ALPHA = 0.16f
private const val HAIRLINE_MEDIA_ALPHA = 0.3f
private const val HAIRLINE_TONE_ALPHA = 0.5f
private const val SOLID_WHITE_MIN_CONTRAST = 3f
private const val NEUTRAL_FILL_ALPHA = 0.08f
private const val CHEVRON_COLLAPSED = 90f
private const val CHEVRON_EXPANDED = 180f
private val DotGlowSpread = 2.dp
private val DotGlowBlur = 7.dp

@ExperimentalOneConfigUiApi
public enum class BadgeStyle {
    Tint,

    Solid,

    Hairline,

    Tab,
}

@ExperimentalOneConfigUiApi
public object BadgeDefaults {
    public val tabHeight: Dp = 21.dp

    public val iconSize: Dp = 12.dp

    public val hairlineWidth: Dp = 1.dp
}

@ExperimentalOneConfigUiApi
public class BadgeColors(
    public val container: Color = Color.Unspecified,
    public val content: Color = Color.Unspecified,
    public val border: Color = Color.Unspecified,
) {
    override fun equals(other: Any?): Boolean =
        other is BadgeColors && other.container == container && other.content == content && other.border == border

    override fun hashCode(): Int = (container.hashCode() * 31 + content.hashCode()) * 31 + border.hashCode()
}

@ExperimentalOneConfigUiApi
public object StatusDotDefaults {
    public val size: Dp = 6.dp
}

@ExperimentalOneConfigUiApi
public object SectionHeaderDefaults {
    public val iconSize: Dp = 16.dp

    public val chevronSize: Dp = 12.dp

    public val labelStyle: TextStyle
        @Composable get() = OneConfigTheme.current.typography.eyebrow.copy(letterSpacing = 0.sp)
}

@ExperimentalOneConfigUiApi
@Composable
public fun StatusDot(
    tone: BadgeTone = BadgeTone.Accent,
    modifier: Modifier = Modifier,
    size: Dp = StatusDotDefaults.size,
    glow: Boolean = false,
    onAccent: Boolean = false,
    contentDescription: String? = null,
) {
    val theme = OneConfigTheme.current
    val circle = theme.shapes.circle
    val color = if (onAccent) theme.accentTextColor else dotToneColor(theme, tone, OneConfigTheme.accent)
    val described = if (contentDescription == null) modifier else modifier.semantics { this.contentDescription = contentDescription }
    if (!glow) {
        Box(described.size(size).background(color, circle))
        return
    }
    Box(described, contentAlignment = Alignment.Center) {
        Box(Modifier.size(size + DotGlowSpread).background(color, circle).blur(DotGlowBlur))
        Box(Modifier.size(size).background(color, circle))
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.Neutral,
    overImage: Boolean = false,
    style: BadgeStyle = BadgeStyle.Tint,
    icon: String? = null,
    dot: Boolean = overImage && tone != BadgeTone.Neutral && style == BadgeStyle.Tint,
    contentDescription: String? = null,
    colors: BadgeColors? = null,
    shape: Shape? = null,
    labelStyle: TextStyle? = null,
    contentPadding: PaddingValues? = null,
) {
    val theme = OneConfigTheme.current
    val inkOverride = colors?.content ?: Color.Unspecified
    if (text.isEmpty() && icon == null) {
        if (inkOverride.isSpecified) {
            val described = if (contentDescription == null) modifier else modifier.semantics { this.contentDescription = contentDescription }
            Box(described.size(StatusDotDefaults.size).background(inkOverride, theme.shapes.circle))
        } else {
            StatusDot(tone, modifier, contentDescription = contentDescription)
        }
        return
    }
    val spacing = theme.spacing
    val accent = OneConfigTheme.accent
    val tab = style == BadgeStyle.Tab
    val solid = style == BadgeStyle.Solid || tab
    val hairline = style == BadgeStyle.Hairline
    val small = theme.shapes.small
    val badgeShape = shape ?: if (tab) remember(small) { HangingTabShape(small) } else theme.shapes.pill
    val toneColor = badgeToneColor(theme, tone, accent)
    val mediaSurface = theme.onMediaShadow.compositeOver(Color.Black)
    val fillOverride = colors?.container ?: Color.Unspecified
    val fill = when {
        fillOverride.isSpecified -> fillOverride
        solid -> solidBadgeFill(theme, tone, accent)
        overImage -> theme.onMediaShadow
        hairline -> Color.Transparent
        tone == BadgeTone.Neutral -> theme.textColor.copy(alpha = NEUTRAL_FILL_ALPHA)
        else -> toneColor.copy(alpha = BADGE_FILL_ALPHA)
    }
    val content = when {
        inkOverride.isSpecified -> inkOverride
        solid && tone == BadgeTone.Accent && fillOverride.isUnspecified -> theme.accentTextColor
        solid && tone == BadgeTone.Neutral && fillOverride.isUnspecified -> theme.textColor
        solid -> if (Color.White.contrastAgainst(fill) >= SOLID_WHITE_MIN_CONTRAST) Color.White else Color.Black
        overImage && hairline && tone != BadgeTone.Neutral -> dotToneColor(theme, tone, accent).legibleOn(mediaSurface)
        overImage -> theme.onMedia
        else -> toneColor
    }
    val edgeOverride = colors?.border ?: Color.Unspecified
    val edge = when {
        edgeOverride.isSpecified -> edgeOverride
        !hairline -> null
        overImage && tone == BadgeTone.Neutral -> theme.onMedia.copy(alpha = HAIRLINE_MEDIA_ALPHA)
        tone == BadgeTone.Neutral -> theme.borderColor
        else -> content.copy(alpha = HAIRLINE_TONE_ALPHA)
    }
    val painted =
        if (solid && tone == BadgeTone.Accent && fillOverride.isUnspecified) Modifier.background(theme.accentBrush, badgeShape)
        else Modifier.background(fill, badgeShape)
    val padding = contentPadding ?: PaddingValues(horizontal = if (tab) spacing.m else spacing.s, vertical = spacing.xxs)
    val textStyle = if (labelStyle == null) theme.typography.captionMedium else theme.typography.captionMedium.merge(labelStyle)
    Row(
        modifier
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .then(if (tab) Modifier.heightIn(min = BadgeDefaults.tabHeight) else Modifier)
            .clip(badgeShape)
            .then(painted)
            .then(if (edge != null) Modifier.border(BadgeDefaults.hairlineWidth, edge, badgeShape) else Modifier)
            .padding(padding),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) {
            if (solid || inkOverride.isSpecified) Box(Modifier.size(StatusDotDefaults.size).background(content, theme.shapes.circle)) else StatusDot(tone)
        }
        if (icon != null) Icon(icon, null, Modifier.size(BadgeDefaults.iconSize), tint = content)
        if (text.isNotEmpty()) LayoutText(text, textStyle, content, maxLines = 1)
    }
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.Neutral,
    overImage: Boolean = false,
    style: BadgeStyle = BadgeStyle.Tint,
    icon: String? = null,
    dot: Boolean = overImage && tone != BadgeTone.Neutral && style == BadgeStyle.Tint,
    contentDescription: String? = null,
) {
    Badge(text, modifier, tone, overImage, style, icon, dot, contentDescription, null, null, null, null)
}

private fun solidBadgeFill(theme: OneConfigUITheme, tone: BadgeTone, accent: Color): Color = when (tone) {
    BadgeTone.Neutral -> theme.chipBackground
    BadgeTone.Accent -> accent
    BadgeTone.Success -> theme.success
    BadgeTone.Warning -> theme.warning
    BadgeTone.Danger -> theme.error
}

private class HangingTabShape(private val base: Shape) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val outline = base.createOutline(size, layoutDirection, density)
        if (outline is Outline.Generic) return squareTop(size, layoutDirection, density)
        if (outline !is Outline.Rounded) return outline
        val bottomStart = outline.roundRect.bottomLeftCornerRadius
        val bottomEnd = outline.roundRect.bottomRightCornerRadius
        return Outline.Rounded(
            RoundRect(
                Rect(0f, 0f, size.width, size.height),
                topLeft = CornerRadius.Zero,
                topRight = CornerRadius.Zero,
                bottomRight = bottomEnd,
                bottomLeft = bottomStart,
            ),
        )
    }

    private fun squareTop(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val tall = Path().apply {
            addOutline(base.createOutline(Size(size.width, size.height * 2), layoutDirection, density))
            translate(Offset(0f, -size.height))
        }
        val bounds = Path().apply { addRect(Rect(0f, 0f, size.width, size.height)) }
        return Outline.Generic(Path.combine(PathOperation.Intersect, tall, bounds))
    }

    override fun equals(other: Any?): Boolean = other is HangingTabShape && other.base == base

    override fun hashCode(): Int = base.hashCode()
}

private fun badgeToneColor(theme: OneConfigUITheme, tone: BadgeTone, accent: Color): Color = when (tone) {
    BadgeTone.Neutral -> theme.mutedText
    BadgeTone.Accent -> accent.legibleOn(theme.pageSurface)
    BadgeTone.Success -> theme.success
    BadgeTone.Warning -> theme.warning
    BadgeTone.Danger -> theme.error
}

private fun dotToneColor(theme: OneConfigUITheme, tone: BadgeTone, accent: Color): Color =
    if (tone == BadgeTone.Accent) accent else badgeToneColor(theme, tone, accent)

@ExperimentalOneConfigUiApi
@Composable
public fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    eyebrow: Boolean = true,
    icon: String? = null,
    count: Int? = null,
    expanded: Boolean? = null,
    onToggle: (() -> Unit)? = null,
    rule: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(vertical = OneConfigTheme.current.spacing.xs),
    interactionSource: MutableInteractionSource? = null,
    textStyle: TextStyle? = null,
    color: Color = Color.Unspecified,
    hoverColor: Color = Color.Unspecified,
    uppercase: Boolean = eyebrow,
    iconSize: Dp = SectionHeaderDefaults.iconSize,
    title: (@Composable RowScope.(style: TextStyle, color: Color) -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val theme = OneConfigTheme.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val style = textStyle ?: if (eyebrow) theme.typography.eyebrow else theme.typography.heading
    val rest = if (color.isSpecified) color else if (eyebrow) theme.mutedText else theme.textColor
    val tint by Motion.animateColorAsState(if (onToggle != null && hovered) (if (hoverColor.isSpecified) hoverColor else theme.textColor) else rest, Motion.colorSpec(), "sectionHeader")
    val toggle = if (onToggle == null) {
        Modifier.semantics(mergeDescendants = true) { heading() }
    } else {
        Modifier.semantics { heading() }.oneConfigClickable(text, onToggle, indication = false, interactionSource = source)
    }
    Row(
        modifier.fillMaxWidth().then(toggle).padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(iconSize), tint = tint)
        if (title != null) {
            title(style, tint)
        } else {
            LayoutText(if (uppercase) text.uppercase() else text, style, tint, if (rule) Modifier else Modifier.weight(1f), maxLines = 1)
        }
        if (count != null) Badge(count.toString())
        if (rule) Divider(Modifier.weight(1f))
        if (expanded != null) SectionChevron(expanded)
        trailing?.invoke(this)
    }
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    eyebrow: Boolean = true,
    icon: String? = null,
    count: Int? = null,
    expanded: Boolean? = null,
    onToggle: (() -> Unit)? = null,
    rule: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(vertical = OneConfigTheme.current.spacing.xs),
    interactionSource: MutableInteractionSource? = null,
    title: (@Composable RowScope.(style: TextStyle, color: Color) -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    SectionHeader(text, modifier, eyebrow, icon, count, expanded, onToggle, rule, contentPadding, interactionSource, title = title, trailing = trailing)
}

@ExperimentalOneConfigUiApi
@Composable
public fun SectionChevron(expanded: Boolean, modifier: Modifier = Modifier) {
    val rotation by Motion.animateFloatAsState(if (expanded) CHEVRON_EXPANDED else CHEVRON_COLLAPSED, Motion.tweenSpec(), "sectionHeaderChevron")
    Icon(
        "up",
        null,
        modifier.size(SectionHeaderDefaults.chevronSize).graphicsLayer { rotationZ = rotation },
        tint = OneConfigTheme.current.textColorSecondary,
    )
}

@ExperimentalOneConfigUiApi
@Composable
public fun Divider(modifier: Modifier = Modifier, vertical: Boolean = false) {
    val color = OneConfigTheme.current.borderColor
    Box(
        if (vertical) modifier.fillMaxHeight().width(1.dp).background(color)
        else modifier.fillMaxWidth().height(1.dp).background(color),
    )
}

@ExperimentalOneConfigUiApi
@Composable
public fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    maxValueLines: Int = Int.MAX_VALUE,
    stacked: Boolean = false,
) {
    val theme = OneConfigTheme.current
    val style = theme.typography.small
    if (stacked) {
        Column(
            modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(theme.spacing.xxs),
        ) {
            LayoutText(label, style, theme.mutedText)
            LayoutText(value, style, theme.textColor, Modifier.fillMaxWidth(), maxLines = maxValueLines)
        }
        return
    }
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.l),
        verticalAlignment = Alignment.Top,
    ) {
        LayoutText(label, style, theme.mutedText, Modifier.widthIn(max = KEY_VALUE_LABEL_MAX))
        LayoutText(value, style, theme.textColor, Modifier.weight(1f), maxLines = maxValueLines, align = TextAlign.End)
    }
}

private val KEY_VALUE_LABEL_MAX = 160.dp

@ExperimentalOneConfigUiApi
@Composable
public fun PageScaffold(
    modifier: Modifier = Modifier,
    toolbar: (@Composable RowScope.() -> Unit)? = null,
    sidePanel: (@Composable () -> Unit)? = null,
    sidePanelWidth: Dp = 320.dp,
    content: @Composable () -> Unit,
) {
    val spacing = OneConfigTheme.current.spacing
    Column(modifier.fillMaxSize()) {
        if (toolbar != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = ControlSize.Large.height)
                    .padding(horizontal = spacing.pageGutter, vertical = spacing.m),
                horizontalArrangement = Arrangement.spacedBy(spacing.m),
                verticalAlignment = Alignment.CenterVertically,
                content = toolbar,
            )
        }
        Row(Modifier.fillMaxWidth().weight(1f)) {
            Box(Modifier.weight(1f).fillMaxHeight()) { content() }
            if (sidePanel != null) {
                Divider(vertical = true)
                Box(Modifier.width(sidePanelWidth).fillMaxHeight()) { sidePanel() }
            }
        }
    }
}

@ExperimentalOneConfigUiApi
public enum class CenteredPanelSurface {
    Popup,

    Card,

    Sidebar,
}

@ExperimentalOneConfigUiApi
@Composable
public fun CenteredPanel(
    modifier: Modifier = Modifier,
    width: Dp = 650.dp,
    scrollable: Boolean = true,
    maxHeight: Dp = Dp.Unspecified,
    surface: CenteredPanelSurface = CenteredPanelSurface.Popup,
    contentPadding: Dp = Dp.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    val shape = if (surface == CenteredPanelSurface.Sidebar) theme.backgroundShape else theme.shapes.large
    val fill = when (surface) {
        CenteredPanelSurface.Popup -> theme.popupBackground
        CenteredPanelSurface.Card -> theme.modCardBackground
        CenteredPanelSurface.Sidebar -> theme.sidebarBackground
    }
    val inset = if (contentPadding.isSpecified) contentPadding else spacing.xxl
    Box(modifier.fillMaxSize().padding(spacing.xl), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .widthIn(max = width)
                .then(if (maxHeight.isSpecified) Modifier.heightIn(max = maxHeight) else Modifier)
                .fillMaxWidth()
                .clip(shape)
                .background(fill, shape)
                .border(1.dp, theme.borderColor, shape),
        ) {
            val scroll = rememberScrollState()
            Column(
                Modifier
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.verticalScroll(scroll) else Modifier)
                    .padding(inset),
                verticalArrangement = Arrangement.spacedBy(spacing.l),
                content = content,
            )
            if (scrollable) {
                Box(Modifier.matchParentSize()) {
                    VerticalScrollbar(
                        rememberScrollbarAdapter(scroll),
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = spacing.m, horizontal = spacing.xxs),
                    )
                }
            }
        }
    }
}
