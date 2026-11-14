@file:OptIn(ExperimentalOneConfigUiApi::class, InternalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private val SWATCH_SIZE = 24.dp

private const val SWATCH_BORDER_MILLIS = 120

@ExperimentalOneConfigUiApi
public open class ColorPickerModel private constructor(private val delegate: ColorPickerModel?, @Suppress("UNUSED_PARAMETER") marker: Unit) {
    protected constructor() : this(null, Unit)

    public constructor(initialColor: Color, hasAlpha: Boolean) :
        this(componentsBridge().createColorPicker(initialColor.opaqueUnless(hasAlpha), hasAlpha, false, 1f, null), Unit)

    public constructor(initialColor: Color) : this(initialColor, true)

    public open val color: Color get() = requireNotNull(delegate).color

    public open val chroma: Boolean get() = requireNotNull(delegate).chroma

    public open val chromaSpeed: Float get() = requireNotNull(delegate).chromaSpeed

    public open val gradient: Color? get() = requireNotNull(delegate).gradient

    public open fun set(color: Color) {
        requireNotNull(delegate).set(color)
    }

    @InternalOneConfigUiApi
    public val implementation: ColorPickerModel get() = delegate ?: this
}

@ExperimentalOneConfigUiApi
@Composable
public fun rememberColorPickerModel(initial: Color, hasAlpha: Boolean = true): ColorPickerModel =
    remember(hasAlpha) { componentsBridge().createColorPicker(initial.opaqueUnless(hasAlpha), hasAlpha, false, 1f, null) }

@ExperimentalOneConfigUiApi
@Composable
public fun rememberColorPickerModel(initial: PolyColor, hasAlpha: Boolean = true): ColorPickerModel =
    remember(hasAlpha) {
        componentsBridge().createColorPicker(
            Color(initial.rawArgb).opaqueUnless(hasAlpha),
            hasAlpha,
            initial.chroma,
            initial.chromaSpeed,
            if (initial.gradientEnabled) Color(initial.gradientArgb) else null,
        )
    }

@ExperimentalOneConfigUiApi
public fun ColorPickerModel.toPolyColor(): PolyColor {
    val argb = color.toArgb()
    return PolyColor(argb, chroma, chromaSpeed).withGradient(gradient != null, gradient?.toArgb() ?: argb)
}

@ExperimentalOneConfigUiApi
@Composable
public fun ColorPickerPopup(
    model: ColorPickerModel,
    onColorChanged: (Color) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    placement: Placement = Placement.BelowStart,
    chromaCapable: Boolean = false,
    gradientCapable: Boolean = false,
) {
    TriggeredColorPickerPopup(model, onColorChanged, onDismiss, null, modifier, placement, chromaCapable, gradientCapable)
}

@Composable
private fun TriggeredColorPickerPopup(
    model: ColorPickerModel,
    onColorChanged: (Color) -> Unit,
    onDismiss: () -> Unit,
    trigger: PopupTrigger?,
    modifier: Modifier = Modifier,
    placement: Placement = Placement.BelowStart,
    chromaCapable: Boolean = false,
    gradientCapable: Boolean = false,
) {
    AnchoredPopup(true, onDismiss, placement, trigger = trigger) {
        componentsBridge().ColorPickerPanel(model, onColorChanged, onDismiss, modifier, chromaCapable, gradientCapable)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun ColorSwatchButton(
    color: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    hasAlpha: Boolean = true,
    chromaCapable: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val model = rememberColorPickerModel(color, hasAlpha)
    LaunchedEffect(model, color) {
        if (model.color.toArgb() != color.toArgb()) model.set(color.opaqueUnless(hasAlpha))
    }
    SwatchButton(model, onColorChanged, modifier, label, false, false, enabled, interactionSource)
}

@ExperimentalOneConfigUiApi
@Composable
public fun ColorSwatchButton(
    color: PolyColor,
    onColorChanged: (PolyColor) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    hasAlpha: Boolean = true,
    chromaCapable: Boolean = true,
    gradientCapable: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val model = rememberColorPickerModel(color, hasAlpha)
    val gradient = if (color.gradientEnabled) color.gradientArgb else null
    LaunchedEffect(model, color.rawArgb, color.chroma, color.chromaSpeed, gradient) {
        val current = model.toPolyColor()
        val gradientChanged = (if (current.gradientEnabled) current.gradientArgb else null) != gradient
        if (current.rawArgb != color.rawArgb || current.chroma != color.chroma || current.chromaSpeed != color.chromaSpeed || gradientChanged) {
            componentsBridge().updateColorPicker(
                model,
                Color(color.rawArgb).opaqueUnless(hasAlpha),
                color.chroma,
                color.chromaSpeed,
                gradient?.let(::Color),
            )
        }
    }
    SwatchButton(model, { onColorChanged(model.toPolyColor()) }, modifier, label, chromaCapable, gradientCapable, enabled, interactionSource)
}

@Composable
private fun SwatchButton(
    model: ColorPickerModel,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier,
    label: String?,
    chromaCapable: Boolean,
    gradientCapable: Boolean,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
) {
    val theme = OneConfigTheme.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) { if (!enabled) open = false }
    val trigger = rememberPopupTrigger()
    val shape = theme.shapes.small
    val shown = rememberShownColor(model)
    val gradient = model.gradient
    val fill = if (gradient != null && !model.chroma && gradientCapable) Brush.horizontalGradient(listOf(shown, gradient)) else SolidColor(shown)
    val border by animateColorAsState(
        if (enabled && (hovered || open)) theme.textColor.copy(alpha = 0.3f) else theme.borderColor,
        Motion.spec(tween(SWATCH_BORDER_MILLIS)),
        label = "swatchBorder",
    )
    val spoken = label ?: "${kitString("oneconfig.colour", "Colour")} #${colorToHex(model.color)}"
    Box(modifier) {
        Row(
            Modifier
                .popupTrigger(trigger)
                .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
                .oneConfigClickable(
                    label = spoken,
                    onClick = { open = !open },
                    enabled = enabled,
                    shape = if (label == null) shape else theme.shapes.medium,
                    indication = label != null,
                    interactionSource = source,
                )
                .then(if (label != null) Modifier.heightIn(min = ControlSize.Compact.height).padding(horizontal = 6.dp) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(SWATCH_SIZE)
                    .clip(shape)
                    .drawBehind {
                        val cell = 6.dp.toPx()
                        val columns = (size.width / cell).toInt()
                        val rows = (size.height / cell).toInt()
                        for (row in 0..rows) {
                            for (column in 0..columns) {
                                drawRect(
                                    if ((row + column) % 2 == 0) theme.checkerLight else theme.checkerDark,
                                    Offset(column * cell, row * cell),
                                    Size(cell, cell),
                                )
                            }
                        }
                    }
                    .background(fill, shape)
                    .border(1.dp, border, shape),
            )
            if (label != null) Text(label, color = theme.textColor, style = theme.typography.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (open) {
            TriggeredColorPickerPopup(model, onColorChanged, { open = false }, trigger, chromaCapable = chromaCapable, gradientCapable = gradientCapable)
        }
    }
}

@Composable
private fun rememberShownColor(model: ColorPickerModel): Color {
    var shown by remember(model) { mutableStateOf(model.color) }
    val chroma = model.chroma
    val base = model.color
    val argb = base.toArgb()
    val speed = model.chromaSpeed
    LaunchedEffect(model, chroma, argb, speed) {
        if (!chroma) return@LaunchedEffect
        val turning = PolyColor(argb, true, speed)
        while (true) {
            withInfiniteAnimationFrameNanos { shown = Color(turning.argb) }
        }
    }
    return if (chroma) shown else base
}

private fun Color.opaqueUnless(hasAlpha: Boolean): Color = if (hasAlpha) this else copy(alpha = 1f)

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun ColorPickerPopup(model: ColorPickerModel, onColorChanged: (Color) -> Unit, onClose: () -> Unit) {
    componentsBridge().ColorPickerPanel(model, onColorChanged, onClose, Modifier, true, false)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun ColorPickerPopup(model: ColorPickerModel, onColorChanged: (Color) -> Unit, onClose: () -> Unit, chromaCapable: Boolean) {
    componentsBridge().ColorPickerPanel(model, onColorChanged, onClose, Modifier, chromaCapable, false)
}

@ExperimentalOneConfigUiApi
public fun colorToHex(color: Color): String = componentsBridge().colorToHex(color)
