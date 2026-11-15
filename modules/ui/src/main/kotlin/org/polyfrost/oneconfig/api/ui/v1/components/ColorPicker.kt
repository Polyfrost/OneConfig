@file:OptIn(ExperimentalOneConfigUiApi::class, InternalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.utils.v1.ClipboardHelper

private val SWATCH_SIZE = 24.dp

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
    onCommit: ((Color) -> Unit)? = null,
) {
    TriggeredColorPickerPopup(model, onColorChanged, onDismiss, null, modifier, placement, chromaCapable, gradientCapable, onCommit)
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
    onCommit: ((Color) -> Unit)? = null,
) {
    if (onCommit == null) {
        AnchoredPopup(true, onDismiss, placement, trigger = trigger) {
            componentsBridge().ColorPickerPanel(model, onColorChanged, onDismiss, modifier, chromaCapable, gradientCapable)
        }
        return
    }
    val latestCommit by rememberUpdatedState(onCommit)
    val latestChanged by rememberUpdatedState(onColorChanged)
    val latestDismiss by rememberUpdatedState(onDismiss)
    val pending = remember(model) { booleanArrayOf(false) }
    val commit = remember(model) {
        {
            if (pending[0]) {
                pending[0] = false
                latestCommit(model.color)
            }
        }
    }
    val changed: (Color) -> Unit = remember(model) {
        { color ->
            pending[0] = true
            latestChanged(color)
        }
    }
    val dismiss: () -> Unit = remember(commit) {
        {
            commit()
            latestDismiss()
        }
    }
    DisposableEffect(commit) { onDispose { commit() } }
    AnchoredPopup(true, dismiss, placement, trigger = trigger) {
        componentsBridge().ColorPickerPanel(model, changed, dismiss, modifier.commitOnRelease(commit).commitOnEnter(commit), chromaCapable, gradientCapable)
    }
}

private fun Modifier.commitOnEnter(commit: () -> Unit): Modifier = onPreviewKeyEvent { event ->
    if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) commit()
    false
}

private fun Modifier.commitOnRelease(commit: () -> Unit): Modifier = pointerInput(commit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
        do {
            val event = awaitPointerEvent(PointerEventPass.Final)
        } while (event.changes.any { it.pressed })
        commit()
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
    onReset: (() -> Unit)? = null,
    menu: (List<MenuEntry>) -> List<MenuEntry> = { it },
) {
    val model = rememberColorPickerModel(color, hasAlpha)
    LaunchedEffect(model, color) {
        if (model.color.toArgb() != color.toArgb()) model.set(color.opaqueUnless(hasAlpha))
    }
    SwatchButton(model, onColorChanged, modifier, label, hasAlpha, false, false, enabled, interactionSource, onReset, menu)
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
    onReset: (() -> Unit)? = null,
    menu: (List<MenuEntry>) -> List<MenuEntry> = { it },
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
    SwatchButton(model, { onColorChanged(model.toPolyColor()) }, modifier, label, hasAlpha, chromaCapable, gradientCapable, enabled, interactionSource, onReset, menu)
}

@Composable
private fun SwatchButton(
    model: ColorPickerModel,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier,
    label: String?,
    hasAlpha: Boolean,
    chromaCapable: Boolean,
    gradientCapable: Boolean,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    onReset: (() -> Unit)?,
    menu: (List<MenuEntry>) -> List<MenuEntry>,
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
    val border by Motion.animateColorAsState(
        if (enabled && (hovered || open)) theme.textColor.copy(alpha = 0.3f) else theme.borderColor,
        Motion.colorSpec(Motion.fast),
        "swatchBorder",
    )
    val latestChanged by rememberUpdatedState(onColorChanged)
    val latestReset by rememberUpdatedState(onReset)
    val latestMenu by rememberUpdatedState(menu)
    val spoken = label ?: "${kitString("oneconfig.colour", "Colour")} #${colorToHex(model.color)}"
    Box(modifier) {
        Row(
            Modifier
                .contextMenu(enabled) {
                    latestMenu(
                        colorSwatchMenu(
                            model,
                            hasAlpha,
                            onPaste = { pasted ->
                                model.set(pasted)
                                latestChanged(model.color)
                            },
                            onReset = latestReset,
                        ),
                    )
                }
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
                    .transparencyCheckerboard()
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

@ExperimentalOneConfigUiApi
public fun colorSwatchMenu(
    model: ColorPickerModel,
    hasAlpha: Boolean,
    onPaste: (Color) -> Unit,
    onReset: (() -> Unit)? = null,
    color: Color = model.color,
): List<MenuEntry> {
    val pasted = ClipboardHelper.getString()?.trim()?.let { text ->
        runCatching { PolyColor.hex(text) }.getOrNull()?.let { Color(it.rawArgb).opaqueUnless(hasAlpha) }
    }
    return buildList {
        add(MenuAction(kitString("oneconfig.menu.option.copy_hex", "Copy hex"), { ClipboardHelper.setString("#${colorToHex(color)}") }, icon = "copy"))
        add(
            MenuAction(
                kitString("oneconfig.menu.option.paste_hex", "Paste hex"),
                { if (pasted != null) onPaste(pasted) },
                icon = "clipboard",
                enabled = pasted != null && pasted.toArgb() != color.toArgb(),
            ),
        )
        if (onReset != null) {
            add(MenuSeparator)
            add(MenuAction(kitString("oneconfig.menu.option.reset", "Reset to default"), onReset, icon = "refresh"))
        }
    }
}

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
