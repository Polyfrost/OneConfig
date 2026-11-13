package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.ColorPickerModel as KitColorPickerModel
import org.polyfrost.oneconfig.api.ui.v1.components.ColorPickerPopup as KitColorPickerPopup
import org.polyfrost.oneconfig.api.ui.v1.components.MenuAction
import org.polyfrost.oneconfig.api.ui.v1.components.MenuEntry
import org.polyfrost.oneconfig.api.ui.v1.components.MenuSeparator
import org.polyfrost.oneconfig.api.ui.v1.components.Placement
import org.polyfrost.oneconfig.api.ui.v1.components.TextField
import org.polyfrost.oneconfig.api.ui.v1.components.TextFieldDefaults
import org.polyfrost.oneconfig.api.ui.v1.components.contextMenu
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.api.ui.v1.components.transparencyCheckerboard
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.internal.ui.api.settings.ColorOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.animatedFrame
import org.polyfrost.oneconfig.internal.ui.components.liquidGlass
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.shellTransition
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.readableOn
import org.polyfrost.oneconfig.internal.ui.themes.updateAccent
import org.polyfrost.oneconfig.utils.v1.ClipboardHelper

private val PickerShape @Composable get() = LocalTheme.current.popupShape

private fun colorToHsb(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val brightness = max
    val saturation = if (max == 0f) 0f else delta / max
    val hue = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta).mod(6f) * 60f
        max == g -> ((b - r) / delta + 2f) * 60f
        else -> ((r - g) / delta + 4f) * 60f
    }
    return floatArrayOf(hue, saturation, brightness)
}

private fun hsbToColor(hue: Float, saturation: Float, brightness: Float, alpha: Float = 1f): Color {
    val h = hue / 60f
    val c = brightness * saturation
    val x = c * (1f - abs(h.mod(2f) - 1f))
    val m = brightness - c
    val (r, g, b) = when {
        h < 1f -> Triple(c, x, 0f)
        h < 2f -> Triple(x, c, 0f)
        h < 3f -> Triple(0f, c, x)
        h < 4f -> Triple(0f, x, c)
        h < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m, alpha)
}

internal fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    val a = (argb shr 24) and 0xFF
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return if (a == 255) "%02X%02X%02X".format(r, g, b)
    else "%02X%02X%02X%02X".format(a, r, g, b)
}

private fun hexToColor(hex: String): Color? {
    val h = hex.removePrefix("#").trim()
    return try {
        when (h.length) {
            6 -> Color(
                Integer.parseInt(h.substring(0, 2), 16) / 255f,
                Integer.parseInt(h.substring(2, 4), 16) / 255f,
                Integer.parseInt(h.substring(4, 6), 16) / 255f,
            )

            8 -> Color(
                Integer.parseInt(h.substring(2, 4), 16) / 255f,
                Integer.parseInt(h.substring(4, 6), 16) / 255f,
                Integer.parseInt(h.substring(6, 8), 16) / 255f,
                Integer.parseInt(h.substring(0, 2), 16) / 255f,
            )

            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

internal class ColorPickerModel(initialColor: Color, hasAlpha: Boolean = true) : KitColorPickerModel {
    private val hsb = colorToHsb(initialColor)
    var hue by mutableFloatStateOf(hsb[0])
    var saturation by mutableFloatStateOf(hsb[1])
    var brightness by mutableFloatStateOf(hsb[2])
    var alpha by mutableFloatStateOf(initialColor.alpha)
    private var chromaState by mutableStateOf(false)
    var chromaEnabled: Boolean
        get() = chromaState
        set(value) {
            if (!value) {
                hue = savedHue()
                chromaTurn = 0f
            }
            chromaState = value
        }
    val alphaEnabled = hasAlpha
    override var chromaSpeed by mutableFloatStateOf(1f)
    var gradientEnabled by mutableStateOf(false)
    var gradientColor by mutableStateOf(initialColor)

    override val color: Color get() = savedColor()

    override val chroma: Boolean get() = chromaEnabled

    override val gradient: Color? get() = if (gradientEnabled) gradientColor else null

    override fun set(color: Color) {
        chromaTurn = 0f
        applyFrom(color)
    }

    /** How far [hue] is turned past the saved hue, so the picker matches every other chroma colour right now */
    private var chromaTurn = 0f

    fun currentColor(): Color = hsbToColor(hue, saturation, brightness, alpha)

    /** The colour to save, which is [currentColor] with any chroma turn taken back out */
    fun savedColor(): Color = hsbToColor(savedHue(), saturation, brightness, alpha)

    private fun savedHue(): Float = (hue - chromaTurn).mod(360f)

    fun turnChroma() {
        val turn = PolyColor.chromaTurn(chromaSpeed) * 360f
        hue = (savedHue() + turn) % 360f
        chromaTurn = turn
    }

    fun applyFrom(color: Color) {
        val newHsb = colorToHsb(color)
        hue = newHsb[0]
        saturation = newHsb[1]
        brightness = newHsb[2]
        alpha = color.alpha
    }
}

@Composable
internal fun ChromaColorAnimation(
    model: ColorPickerModel,
    onColorChanged: (Color) -> Unit,
) {
    LaunchedEffect(model.chromaEnabled) {
        if (!model.chromaEnabled) return@LaunchedEffect
        while (true) {
            withInfiniteAnimationFrameNanos {
                model.turnChroma()
                onColorChanged(model.currentColor())
            }
        }
    }
}

private val SWATCH_SIZE = 24.dp

// TODO: make this design accurate

@Composable
fun ColorOption(data: ColorOptionData) {
    ColorOption(data, true)
}

@Composable
fun ColorOption(data: ColorOptionData, enabled: Boolean) {
    val initialValue = remember(data.prop) { data.prop.shown() }
    val initialColor = remember(data.prop, initialValue) {
        when (val v = initialValue) {
            is Color -> v
            is PolyColor -> Color(v.rawArgb).let {
                if (data.alpha) it else it.copy(alpha = 1f)
            }

            is Int -> Color(v).let { if (data.alpha) it else it.copy(alpha = 1f) }
            is java.awt.Color -> Color(
                v.red / 255f,
                v.green / 255f,
                v.blue / 255f,
                if (data.alpha) v.alpha / 255f else 1f
            )

            else -> Color.White
        }
    }
    var currentColor by remember(data.prop) { mutableStateOf(initialColor) }
    val pickerModel = remember(data.prop, initialValue) {
        ColorPickerModel(initialColor, data.alpha).apply {
            val color = initialValue as? PolyColor
            chromaEnabled = color?.chroma == true
            chromaSpeed = color?.chromaSpeed ?: 1f
            gradientEnabled = color?.gradientEnabled == true
            if (color != null) gradientColor = Color(color.gradientArgb)
        }
    }
    fun persistColor(color: Color) {
        currentColor = pickerModel.currentColor()
        @Suppress("UNCHECKED_CAST")
        when {
            data.prop.type == Int::class.javaObjectType || data.prop.type == Int::class.javaPrimitiveType ->
                (data.prop as Property<Any>).set(color.toArgb())

            data.prop.type == PolyColor::class.java -> {
                (data.prop as Property<Any>).set(
                    PolyColor(color.toArgb(), pickerModel.chromaEnabled, pickerModel.chromaSpeed)
                        .withGradient(data.gradient && pickerModel.gradientEnabled, pickerModel.gradientColor.toArgb())
                )
                updateAccent()
            }

            data.prop.type == java.awt.Color::class.java -> {
                val argb = color.toArgb()
                (data.prop as Property<Any>).set(java.awt.Color(argb, true))
            }

            else -> (data.prop as Property<Any>).set(color)
        }
    }

    ChromaColorAnimation(pickerModel) {
        currentColor = it
        if (data.prop.type == PolyColor::class.java) updateAccent()
    }

    ColorPicker(
        pickerModel,
        currentColor,
        onColorChanged = ::persistColor,
        swatch = data.swatch,
        chromaCapable = data.prop.type == PolyColor::class.java,
        gradientCapable = data.gradient && data.prop.type == PolyColor::class.java,
        enabled = enabled,
        onReset = if (optionHasDefault(data.prop)) ({ resetOption(data.prop) }) else null,
    )
}

/** [swatch] draws a small square instead of the `#hex` pill */
@Composable
internal fun ColorPicker(
    model: ColorPickerModel,
    color: Color,
    onColorChanged: (Color) -> Unit,
    swatch: Boolean = false,
    chromaCapable: Boolean = true,
    gradientCapable: Boolean = false,
) {
    ColorPicker(model, color, onColorChanged, swatch, chromaCapable, gradientCapable, true, null)
}

@Composable
internal fun ColorPicker(
    model: ColorPickerModel,
    color: Color,
    onColorChanged: (Color) -> Unit,
    swatch: Boolean,
    chromaCapable: Boolean,
    gradientCapable: Boolean,
    enabled: Boolean,
    onReset: (() -> Unit)?,
) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    var expanded by remember { mutableStateOf(false) }
    val latestOnColorChanged by rememberUpdatedState(onColorChanged)
    val latestOnReset by rememberUpdatedState(onReset)

    val textColor = Motion.animateColorAsState(readableOn(color))

    val borderColor = Motion.animateColorAsState(
        if (enabled && (isHovered || expanded)) theme.textColor.copy(0.3f) else theme.borderColor
    )

    val showGradient = gradientCapable && model.gradientEnabled && !model.chromaEnabled
    val fill = if (showGradient) Brush.horizontalGradient(listOf(color, model.gradientColor)) else SolidColor(color)
    val hex = colorToHex(color)
    val spoken = "${translated("oneconfig.colour", "Colour")} #$hex"
    val disabledAlpha = OneConfigTheme.current.disabledAlpha

    Box(
        Modifier
            .graphicsLayer { alpha = if (enabled) 1f else disabledAlpha }
            .contextMenu(enabled) {
                swatchMenu(
                    model,
                    copy = { ClipboardHelper.setString("#${colorToHex(model.savedColor())}") },
                    paste = { pasted ->
                        model.set(if (model.alphaEnabled) pasted else pasted.copy(alpha = 1f))
                        latestOnColorChanged(model.savedColor())
                    },
                    reset = latestOnReset,
                )
            },
    ) {
        if (swatch) {
            Box(
                modifier = Modifier
                    .size(SWATCH_SIZE)
                    .oneConfigClickable(
                        label = spoken,
                        onClick = { expanded = !expanded },
                        enabled = enabled,
                        shape = theme.checkBoxShape,
                        indication = false,
                        interactionSource = interactionSource,
                    )
                    .graphicsLayer {}
                    .clip(theme.checkBoxShape)
                    .transparencyCheckerboard()
                    .background(fill, theme.checkBoxShape)
                    .animatedFrame(theme.checkBoxShape, border = { borderColor.value }),
            )
        } else {
            Row(
                modifier = Modifier
                    .heightIn(min = OptionControlHeight)
                    .oneConfigClickable(
                        label = spoken,
                        onClick = { expanded = !expanded },
                        enabled = enabled,
                        shape = LocalTheme.current.sideBarNavigationEntryShape,
                        indication = false,
                        interactionSource = interactionSource,
                    )
                    .graphicsLayer {}
                    .background(fill, LocalTheme.current.sideBarNavigationEntryShape)
                    .animatedFrame(LocalTheme.current.sideBarNavigationEntryShape, border = { borderColor.value })
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon("paintbrush", tint = { textColor.value }, modifier = Modifier.size(14.dp))
                Text(
                    if (showGradient) "#$hex → #${colorToHex(model.gradientColor)}" else "#$hex",
                    color = Color.Unspecified,
                    fontSize = 13.sp,
                    colorProducer = { textColor.value },
                )
            }
        }

        if (expanded && enabled) {
            KitColorPickerPopup(
                model,
                onColorChanged,
                { expanded = false },
                placement = Placement.BelowEnd,
                chromaCapable = chromaCapable,
                gradientCapable = gradientCapable,
            )
        }
    }
}

internal fun swatchMenu(
    model: ColorPickerModel,
    copy: () -> Unit,
    paste: (Color) -> Unit,
    reset: (() -> Unit)?,
): List<MenuEntry> {
    val pasted = ClipboardHelper.getString()?.trim()?.let(::hexToColor)
    return buildList<MenuEntry> {
        add(MenuAction(translated("oneconfig.menu.option.copy_hex", "Copy hex"), copy, icon = "copy"))
        add(
            MenuAction(
                translated("oneconfig.menu.option.paste_hex", "Paste hex"),
                { if (pasted != null) paste(pasted) },
                icon = "clipboard",
                enabled = pasted != null && pasted.toArgb() != model.savedColor().toArgb(),
            ),
        )
        if (reset != null) {
            add(MenuSeparator)
            add(MenuAction(translated("oneconfig.menu.option.reset", "Reset to default"), reset, icon = "refresh"))
        }
    }
}

@Composable
internal fun ColorPickerPopup(
    model: ColorPickerModel,
    onColorChanged: (Color) -> Unit,
    onClose: () -> Unit,
    chromaCapable: Boolean = true,
    gradientCapable: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val theme = LocalTheme.current
    val currentOnColorChanged by rememberUpdatedState(onColorChanged)
    fun save() = currentOnColorChanged(model.savedColor())
    val hue = model.hue
    val saturation = model.saturation
    val brightness = model.brightness
    val alpha = model.alpha
    val chromaEnabled = model.chromaEnabled
    val chromaSpeed = model.chromaSpeed
    var hexText by remember(model) { mutableStateOf(colorToHex(model.currentColor())) }
    var hexFocused by remember { mutableStateOf(false) }
    var alphaText by remember(model) { mutableStateOf((model.alpha * 100f).roundToInt().toString()) }
    var speedText by remember(model) { mutableStateOf("%.1f".format(model.chromaSpeed)) }
    var gradientText by remember(model) { mutableStateOf(colorToHex(model.gradientColor)) }

    val currentColor = model.currentColor()

    Column(
        modifier = modifier
            .width(280.dp)
            .shellTransition()
            .liquidGlass(PickerShape, frosted = true)
            .background(theme.popupBackground, PickerShape)
            .border(1.dp, theme.borderColor, PickerShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(translated("oneconfig.colour.picker", "Colour picker"), color = theme.textColor, fontSize = 14.sp)
            Icon(
                "close",
                color = theme.textColorSecondary,
                modifier = Modifier.size(14.dp)
                    .oneConfigClickable(
                        label = translated("oneconfig.colour.picker.close", "Close"),
                        onClick = onClose,
                        shape = LocalTheme.current.circleShape,
                        indication = false,
                    )
            )
        }

        var sbPaneSize by remember { mutableStateOf(Size.Zero) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(LocalTheme.current.sideBarNavigationEntryShape)
                .onSizeChanged { sbPaneSize = Size(it.width.toFloat(), it.height.toFloat()) }
                .drawWithCache {
                    val hueColor = hsbToColor(hue, 1f, 1f)
                    val horizontalGradient = Brush.horizontalGradient(listOf(Color.White, hueColor))
                    val verticalGradient = Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
                    onDrawBehind {
                        drawRect(horizontalGradient)
                        drawRect(verticalGradient)
                    }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun update(x: Float, y: Float) {
                            model.saturation = (x / sbPaneSize.width).coerceIn(0f, 1f)
                            model.brightness = 1f - (y / sbPaneSize.height).coerceIn(0f, 1f)
                            val c = model.currentColor()
                            hexText = colorToHex(c)
                            save()
                        }
                        update(down.position.x, down.position.y)
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { ch ->
                                if (ch.pressed) {
                                    update(ch.position.x, ch.position.y)
                                    ch.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            val selectorX = (saturation * sbPaneSize.width).coerceIn(0f, sbPaneSize.width)
            val selectorY = ((1f - brightness) * sbPaneSize.height).coerceIn(0f, sbPaneSize.height)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (selectorX - 7.dp.toPx()).roundToInt(),
                            (selectorY - 7.dp.toPx()).roundToInt()
                        )
                    }
                    .size(14.dp)
                    .border(2.dp, Color.White, LocalTheme.current.circleShape)
                    .background(currentColor, LocalTheme.current.circleShape)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(LocalTheme.current.sideBarNavigationEntryShape)
                .drawWithCache {
                    val hueGradient = Brush.horizontalGradient(
                        listOf(
                            Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                            Color.Blue, Color.Magenta, Color.Red
                        )
                    )
                    onDrawBehind { drawRect(hueGradient) }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun update(x: Float) {
                            model.hue = ((x / size.width) * 360f).coerceIn(0f, 360f)
                            val c = model.currentColor()
                            hexText = colorToHex(c)
                            save()
                        }
                        update(down.position.x)
                        do {
                            val event = awaitPointerEvent()
                            event.changes.forEach { ch ->
                                if (ch.pressed) {
                                    update(ch.position.x)
                                    ch.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            var hueBarWidth by remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .onSizeChanged { hueBarWidth = it.width.toFloat() }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(((hue / 360f) * hueBarWidth - 5.dp.toPx()).roundToInt().coerceAtLeast(0), 0) }
                    .size(10.dp, 10.dp)
                    .clip(LocalTheme.current.circleShape)
                    .background(Color.White)
                    .border(1.dp, Color.White.copy(0.5f), LocalTheme.current.checkBoxShape)
            )
        }

        if (model.alphaEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(translated("oneconfig.colour.opacity", "Opacity"), color = theme.textColorSecondary, fontSize = 12.sp)
                SliderValueField(
                    text = alphaText,
                    unit = "%",
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }.take(3)
                        alphaText = filtered
                        filtered.toIntOrNull()?.coerceIn(0, 100)?.let { pct ->
                            model.alpha = pct / 100f
                            val c = model.currentColor()
                            hexText = colorToHex(c)
                            save()
                        }
                    },
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(LocalTheme.current.sideBarNavigationEntryShape)
                    .transparencyCheckerboard()
                    .drawWithCache {
                        val opaqueColor = hsbToColor(hue, saturation, brightness, 1f)
                        val alphaGradient = Brush.horizontalGradient(
                            listOf(opaqueColor.copy(alpha = 0f), opaqueColor)
                        )
                        onDrawBehind {
                            drawRect(alphaGradient)
                        }
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            fun update(x: Float) {
                                model.alpha = (x / size.width).coerceIn(0f, 1f)
                                alphaText = (model.alpha * 100f).roundToInt().toString()
                                val c = model.currentColor()
                                hexText = colorToHex(c)
                                save()
                            }
                            update(down.position.x)
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { ch ->
                                    if (ch.pressed) {
                                        update(ch.position.x)
                                        ch.consume()
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                var alphaBarWidth by remember { mutableStateOf(0f) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .onSizeChanged { alphaBarWidth = it.width.toFloat() }
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset { IntOffset(((alpha) * alphaBarWidth - 5.dp.toPx()).roundToInt().coerceAtLeast(0), 0) }
                        .size(10.dp, 10.dp)
                        .clip(LocalTheme.current.circleShape)
                        .background(Color.White)
                        .border(1.dp, Color.White.copy(0.5f), LocalTheme.current.checkBoxShape)
                )
            }
        }

        // only PolyColor properties can persist the chroma flag so Int and java.awt.Color ones would
        // appear to work until the screen closed
        if (chromaCapable) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(LocalTheme.current.circleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        Color.Red, Color.Yellow, Color.Green,
                                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                    )
                                )
                            )
                    )
                    Text(translated("oneconfig.colour.chroma", "Chroma"), color = theme.textColor, fontSize = 13.sp)
                }
                SwitchControl(chromaEnabled) {
                    model.chromaEnabled = it
                    hexText = colorToHex(model.currentColor())
                    save()
                }
            }
        }

        if (gradientCapable) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(LocalTheme.current.circleShape)
                            .background(Brush.horizontalGradient(listOf(currentColor, model.gradientColor)))
                    )
                    Text(translated("oneconfig.colour.gradient", "Gradient"), color = theme.textColor, fontSize = 13.sp)
                }
                SwitchControl(model.gradientEnabled) {
                    model.gradientEnabled = it
                    if (it && model.gradientColor.toArgb() == currentColor.toArgb()) {
                        model.gradientColor = hsbToColor((model.hue + 40f) % 360f, model.saturation, model.brightness, model.alpha)
                        gradientText = colorToHex(model.gradientColor)
                    }
                    save()
                }
            }
        }

        if (gradientCapable && model.gradientEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(LocalTheme.current.sideBarNavigationEntryShape)
                        .background(model.gradientColor)
                        .border(1.dp, theme.borderColor, LocalTheme.current.sideBarNavigationEntryShape)
                )
                PickerTextField(
                    gradientText,
                    { input ->
                        val filtered = input.filter { it.isLetterOrDigit() }.take(8)
                        gradientText = filtered
                        hexToColor(filtered)?.let { c ->
                            model.gradientColor = c
                            save()
                        }
                    },
                    Modifier.weight(1f),
                    prefix = "#",
                    label = translated("oneconfig.colour.gradient", "Gradient"),
                )
            }
        }

        if (chromaCapable && chromaEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(translated("oneconfig.colour.speed", "Speed"), color = theme.textColorSecondary, fontSize = 12.sp)
                    SliderValueField(
                        text = speedText,
                        unit = "x",
                        onValueChange = { input ->
                            val filtered = filterNumberInput(input).take(4)
                            speedText = filtered
                            filtered.toFloatOrNull()?.coerceIn(0.1f, 4f)?.let {
                                model.chromaSpeed = it
                                save()
                            }
                        },
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(LocalTheme.current.sideBarNavigationEntryShape)
                        .drawWithCache {
                            val gradient = Brush.horizontalGradient(
                                listOf(theme.componentBackground, Accent)
                            )
                            onDrawBehind { drawRect(gradient) }
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                fun update(x: Float) {
                                    model.chromaSpeed = ((x / size.width) * 4f).coerceIn(0.1f, 4f)
                                    speedText = "%.1f".format(model.chromaSpeed)
                                    save()
                                }
                                update(down.position.x)
                                do {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { ch ->
                                        if (ch.pressed) {
                                            update(ch.position.x); ch.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    var speedBarWidth by remember { mutableStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .onSizeChanged { speedBarWidth = it.width.toFloat() }
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset {
                                IntOffset(
                                    (((chromaSpeed - 0.1f) / 3.9f) * speedBarWidth - 5.dp.toPx())
                                        .roundToInt().coerceAtLeast(0), 0
                                )
                            }
                            .size(10.dp, 10.dp)
                            .clip(LocalTheme.current.circleShape)
                            .background(Color.White)
                            .border(1.dp, Color.White.copy(0.5f), LocalTheme.current.checkBoxShape)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(LocalTheme.current.sideBarNavigationEntryShape)
                    .background(currentColor)
                    .border(1.dp, theme.borderColor, LocalTheme.current.sideBarNavigationEntryShape)
            )

            PickerTextField(
                if (chromaEnabled && !hexFocused) colorToHex(currentColor) else hexText,
                { input ->
                    val filtered = input.filter { it.isLetterOrDigit() }.take(8)
                    hexText = filtered
                    hexToColor(filtered)?.let { c ->
                        model.applyFrom(c)
                        save()
                    }
                },
                Modifier.weight(1f),
                prefix = "#",
                label = translated("oneconfig.colour", "Colour"),
                onFocusChange = { focused ->
                    if (focused && !hexFocused) hexText = colorToHex(model.currentColor())
                    hexFocused = focused
                },
            )
        }
    }
}

@Composable
private fun SliderValueField(
    text: String,
    onValueChange: (String) -> Unit,
    unit: String? = null,
    width: Dp = 56.dp,
) {
    PickerTextField(
        text,
        onValueChange,
        Modifier.width(width),
        suffix = unit,
        fontSize = 12.sp,
        horizontalPadding = 8.dp,
        verticalPadding = 4.dp,
    )
}

@Composable
private fun PickerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    prefix: String? = null,
    suffix: String? = null,
    label: String? = null,
    fontSize: TextUnit = 13.sp,
    horizontalPadding: Dp = 10.dp,
    verticalPadding: Dp = 7.dp,
    onFocusChange: ((Boolean) -> Unit)? = null,
) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val focused by source.collectIsFocusedAsState()
    val latestOnFocusChange by rememberUpdatedState(onFocusChange)
    LaunchedEffect(focused) { latestOnFocusChange?.invoke(focused) }
    val shape = TextFieldDefaults.shape
    val border = Motion.animateColorAsState(if (focused) TextFieldDefaults.focusBorder else TextFieldDefaults.border)
    Box(
        modifier
            .background(theme.componentBackground, shape)
            .border(1.dp, border.value, shape)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        TextField(
            value,
            onValueChange,
            Modifier.fillMaxWidth(),
            label = label,
            framed = false,
            fontSize = fontSize,
            leading = prefix?.let { text -> @Composable { Text(text, color = theme.textColorSecondary, fontSize = fontSize) } },
            trailing = suffix?.let { text -> @Composable { Text(text, color = theme.textColorSecondary, fontSize = fontSize) } },
            interactionSource = source,
        )
    }
}
