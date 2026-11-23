@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Announce
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.internal.glassBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigUITheme

private const val TINT_SELECTED_ALPHA = 0.2f
private const val TRANSLUCENT_SELECTED_ALPHA = 0.12f
private const val SECONDARY_ON_ACCENT_ALPHA = 0.75f

@ExperimentalOneConfigUiApi
public enum class ItemSelectionStyle {
    Accent,

    Tint,

    Translucent,
}

@ExperimentalOneConfigUiApi
public enum class SelectionRingTone {
    Accent,

    Text,
}

@ExperimentalOneConfigUiApi
@Immutable
public class CardSelectionStyle(
    public val ringWidth: Dp = 2.dp,
    public val fillAlpha: Float = 0.08f,
    public val gap: Dp = 0.dp,
    public val ringTone: SelectionRingTone = SelectionRingTone.Accent,
) {
    public fun copy(
        ringWidth: Dp = this.ringWidth,
        fillAlpha: Float = this.fillAlpha,
        gap: Dp = this.gap,
        ringTone: SelectionRingTone = this.ringTone,
    ): CardSelectionStyle = CardSelectionStyle(ringWidth, fillAlpha, gap, ringTone)
}

@ExperimentalOneConfigUiApi
public object SelectionDefaults {
    public val Card: CardSelectionStyle = CardSelectionStyle()

    public val Swatch: CardSelectionStyle = CardSelectionStyle(ringWidth = 2.dp, fillAlpha = 0f, gap = 2.dp, ringTone = SelectionRingTone.Text)

    public val itemStyle: ItemSelectionStyle
        @Composable get() = ItemSelectionStyle.Tint

    @Composable
    public fun itemContentColor(
        selected: Boolean,
        style: ItemSelectionStyle = itemStyle,
        rest: Color = OneConfigTheme.current.textColor,
    ): Color {
        val theme = OneConfigTheme.current
        return when {
            selected && style == ItemSelectionStyle.Accent -> theme.accentTextColor
            selected -> theme.textColor
            else -> rest
        }
    }

    @Composable
    public fun itemSecondaryColor(
        selected: Boolean,
        style: ItemSelectionStyle = itemStyle,
        rest: Color = OneConfigTheme.current.textColorSecondary,
    ): Color {
        val theme = OneConfigTheme.current
        return if (selected && style == ItemSelectionStyle.Accent) theme.accentTextColor.copy(alpha = theme.accentTextColor.alpha * SECONDARY_ON_ACCENT_ALPHA) else rest
    }
}

@ExperimentalOneConfigUiApi
public fun Modifier.selectableRow(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    shape: Shape? = null,
    enabled: Boolean = true,
    style: ItemSelectionStyle? = null,
    role: A11yRole = A11yRole.Option,
    sound: UiSound? = UiSound.Click,
    recipe: ControlIndication = if (style == null) ControlDefaults.Underlay else ControlDefaults.PressOnly,
    hoverBorder: Boolean = style != null,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val theme = OneConfigTheme.current
    val outline = shape ?: theme.shapes.medium
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val selection = if (style == null && !hoverBorder) {
        Modifier.rowTint(selected, outline)
    } else {
        Modifier.itemSelection(selected, style ?: SelectionDefaults.itemStyle, outline, hovered && enabled && hoverBorder)
    }
    alpha(if (enabled) 1f else theme.disabledAlpha)
        .then(selection)
        .controlInteraction(source, outline, recipe, enabled, focusRing = false, pointerIcon = null)
        .oneConfigClickable(label, onClick, role, enabled, selected = selected, shape = outline, indication = false, sound = sound, interactionSource = source)
}

@ExperimentalOneConfigUiApi
public fun Modifier.selectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    shape: Shape? = null,
    enabled: Boolean = true,
    style: CardSelectionStyle = SelectionDefaults.Card,
    role: A11yRole = A11yRole.Option,
    sound: UiSound? = UiSound.Click,
    recipe: ControlIndication? = ControlDefaults.Underlay,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val theme = OneConfigTheme.current
    val outline = shape ?: theme.shapes.large
    val source = interactionSource ?: remember { MutableInteractionSource() }
    alpha(if (enabled) 1f else theme.disabledAlpha)
        .cardSelection(selected, outline, style)
        .then(if (recipe != null) Modifier.controlInteraction(source, outline, recipe, enabled, focusRing = false, pointerIcon = null) else Modifier)
        .oneConfigClickable(label, onClick, role, enabled, selected = selected, shape = outline, indication = false, sound = sound, interactionSource = source)
        .padding(style.gap + if (style.gap > 0.dp) style.ringWidth else 0.dp)
}

@ExperimentalOneConfigUiApi
public fun Modifier.selectableSwatch(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    circle: Boolean = true,
    enabled: Boolean = true,
    style: CardSelectionStyle = SelectionDefaults.Swatch,
    role: A11yRole = A11yRole.RadioButton,
    sound: UiSound? = UiSound.Click,
): Modifier = composed {
    val shapes = OneConfigTheme.current.shapes
    selectableCard(selected, onClick, label, if (circle) shapes.circle else shapes.small, enabled, style, role, sound, recipe = null)
}

@ExperimentalOneConfigUiApi
public fun Modifier.cardSelection(
    selected: Boolean,
    shape: Shape? = null,
    style: CardSelectionStyle = SelectionDefaults.Card,
): Modifier = composed {
    val theme = OneConfigTheme.current
    val outline = shape ?: theme.shapes.large
    val accent = theme.accentBrush
    val ringColor = theme.textColor
    val amount = Motion.animateFloatAsState(if (selected) 1f else 0f, Motion.tweenSpec(Motion.standard), "cardSelection")
    drawWithContent {
        val a = amount.value
        if (a > 0f && style.fillAlpha > 0f) drawOutline(outline.createOutline(size, layoutDirection, this), accent, a * style.fillAlpha)
        drawContent()
        val ring = style.ringWidth.toPx()
        if (a > 0f && ring > 0f && size.minDimension >= ring) {
            inset(ring / 2f) {
                val path = outline.createOutline(size, layoutDirection, this)
                if (style.ringTone == SelectionRingTone.Text) {
                    drawOutline(path, ringColor.copy(alpha = ringColor.alpha * a), style = Stroke(ring))
                } else {
                    drawOutline(path, accent, a, Stroke(ring))
                }
            }
        }
    }
}

@ExperimentalOneConfigUiApi
public fun Modifier.itemSelection(
    selected: Boolean,
    style: ItemSelectionStyle,
    shape: Shape? = null,
    hovered: Boolean = false,
): Modifier = composed {
    val theme = OneConfigTheme.current
    val outline = shape ?: theme.shapes.medium
    val amount = Motion.animateFloatAsState(if (selected) 1f else 0f, Motion.tweenSpec(Motion.standard), "itemSelection")
    val border = Motion.animateColorAsState(itemBorder(theme, selected, hovered, style), Motion.colorSpec(), "itemSelectionBorder")
    val tint = theme.textColor
    val translucent = style == ItemSelectionStyle.Translucent
    val accentShare = when (style) {
        ItemSelectionStyle.Accent -> 1f
        ItemSelectionStyle.Tint -> TINT_SELECTED_ALPHA
        ItemSelectionStyle.Translucent -> 0f
    }
    val framed = Modifier.selectionFrame(
        outline,
        fill = { if (translucent) tint.copy(alpha = tint.alpha * TRANSLUCENT_SELECTED_ALPHA * amount.value) else Color.Transparent },
        accentAlpha = { amount.value * accentShare },
        border = { border.value },
    )
    val bridge = glassBridge()
    if (translucent && bridge != null) bridge.liquidGlass(framed, outline, false) { amount.value } else framed
}

private fun Modifier.rowTint(selected: Boolean, shape: Shape): Modifier = composed {
    val brush = OneConfigTheme.current.accentBrush
    val amount = Motion.animateFloatAsState(if (selected) 1f else 0f, Motion.tweenSpec(Motion.standard), "selectableRow")
    drawWithContent {
        val a = amount.value
        if (a > 0f) drawOutline(shape.createOutline(size, layoutDirection, this), brush, a * TINT_SELECTED_ALPHA)
        drawContent()
    }
}

private fun itemBorder(theme: OneConfigUITheme, selected: Boolean, hovered: Boolean, style: ItemSelectionStyle): Color = when {
    selected && style == ItemSelectionStyle.Translucent -> theme.borderColor.copy(alpha = 0f)
    hovered || selected -> theme.borderColor
    else -> theme.borderColor.copy(alpha = 0f)
}

@Composable
internal fun Modifier.selectionFrame(shape: Shape, fill: () -> Color, accentAlpha: () -> Float, border: () -> Color): Modifier {
    val bridge = glassBridge()
    if (bridge != null) return bridge.animatedFrame(this, shape, fill, accentAlpha, border)
    val accent = OneConfigTheme.current.accentBrush
    return drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val stroke = Stroke(1.dp.toPx())
        onDrawWithContent {
            val f = fill()
            if (f.alpha > 0f) drawOutline(outline, f)
            val a = accentAlpha()
            if (a > 0f) drawOutline(outline, accent, a)
            drawContent()
            val b = border()
            if (b.alpha > 0f) drawOutline(outline, b, style = stroke)
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun SelectionBar(
    count: Int,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    val shape = theme.shapes.large
    val lastCount = remember { IntArray(1) }
    if (count > 0) lastCount[0] = count
    val label = layoutString("oneconfig.ui.selection.count", "%s selected", lastCount[0])
    if (count > 0) {
        EscapeCloses(onClear)
        Announce(label)
    }
    AnimatedVisibility(
        visible = count > 0,
        modifier = modifier,
        enter = Motion.enter(fadeIn(Motion.tweenSpec()) + slideInVertically(Motion.tweenSpec()) { it / 2 }),
        exit = Motion.exit(fadeOut(Motion.tweenSpec()) + slideOutVertically(Motion.tweenSpec()) { it / 2 }),
        label = "selectionBar",
    ) {
        Row(
            Modifier
                .heightIn(min = ControlSize.Large.height + spacing.m * 2)
                .clip(shape)
                .background(theme.popupBackground, shape)
                .border(1.dp, theme.borderColor, shape)
                .padding(start = spacing.xl, end = spacing.s, top = spacing.s, bottom = spacing.s),
            horizontalArrangement = Arrangement.spacedBy(spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LayoutText(label, theme.typography.label, theme.textColor, Modifier.padding(end = spacing.m), maxLines = 1)
            actions()
            Divider(Modifier.height(ControlSize.Compact.height), vertical = true)
            IconButton("close", layoutString("oneconfig.ui.selection.clear", "Clear selection"), onClear)
        }
    }
}
