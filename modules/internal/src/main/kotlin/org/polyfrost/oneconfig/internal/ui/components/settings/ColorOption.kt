package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.Image
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.text.BasicTextField
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.ImageBitmap
import org.polyfrost.compose.ui.graphics.LinearGradient
import org.polyfrost.compose.ui.graphics.SolidColor
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.graphics.toArgb
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.text.style.TextAlign
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.compose.ui.window.Popup
import org.polyfrost.compose.ui.window.PopupProperties
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.reflect.KProperty
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.api.settings.ColorOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.screens.LocalUiClipboard
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.cornerRadiusOrNull
import org.polyfrost.oneconfig.internal.ui.themes.updateAccent

private val PickerShape @Composable get() = LocalTheme.current.popupShape

private val HueGradient = LinearGradient(
    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
)

private val HueDisc: ImageBitmap by lazy {
    val n = 64
    val px = ByteArray(n * n * 4)
    val c = n / 2f
    for (y in 0 until n) for (x in 0 until n) {
        val dx = x + 0.5f - c
        val dy = y + 0.5f - c
        val cover = (c - kotlin.math.hypot(dx, dy) + 0.5f).coerceIn(0f, 1f)
        val deg = (Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble()).toFloat() + 360f) % 360f
        val rgb = hsbToColor(deg, 1f, 1f).toArgb()
        val o = (y * n + x) * 4
        px[o] = (rgb shr 16).toByte(); px[o + 1] = (rgb shr 8).toByte(); px[o + 2] = rgb.toByte()
        px[o + 3] = (cover * 255f).roundToInt().toByte()
    }
    ImageBitmap(n, n, px)
}

private fun colorToHsb(color: Color): FloatArray {
    val r = color.redF
    val g = color.greenF
    val b = color.blueF
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

internal fun Color.luminance(): Float {
    fun linear(c: Float) = if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    return 0.2126f * linear(redF) + 0.7152f * linear(greenF) + 0.0722f * linear(blueF)
}

internal class ColorPickerModel(initialColor: Color, hasAlpha: Boolean = true) {
    private val hsb = colorToHsb(initialColor)
    var hue by mutableFloatStateOf(hsb[0])
    var saturation by mutableFloatStateOf(hsb[1])
    var brightness by mutableFloatStateOf(hsb[2])
    var alpha by mutableFloatStateOf(initialColor.alphaF)
    var chromaEnabled by mutableStateOf(false)
    val alphaEnabled = hasAlpha
    var chromaSpeed by mutableFloatStateOf(1f)
    var gradientEnabled by mutableStateOf(false)
    var gradientColor by mutableStateOf(initialColor)

    fun currentColor(): Color = hsbToColor(hue, saturation, brightness, alpha)

    fun applyFrom(color: Color) {
        val newHsb = colorToHsb(color)
        hue = newHsb[0]
        saturation = newHsb[1]
        brightness = newHsb[2]
        alpha = color.alphaF
    }
}

@Composable
internal fun ChromaColorAnimation(
    model: ColorPickerModel,
    onColorChanged: (Color) -> Unit,
) {
    LaunchedEffect(model, model.chromaEnabled, model.chromaSpeed) {
        if (!model.chromaEnabled) return@LaunchedEffect
        var lastNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameNanos ->
                val dt = (frameNanos - lastNanos) / 1_000_000_000f
                lastNanos = frameNanos
                model.hue = (model.hue + (360f / PolyColor.CHROMA_CYCLE_SECONDS.toFloat()) * model.chromaSpeed * dt) % 360f
                onColorChanged(model.currentColor())
            }
        }
    }
}

// TODO: make this design accurate

@Composable
fun ColorOption(data: ColorOptionData) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    var expanded by remember { mutableStateOf(false) }

    val initialValue = remember(data.prop) { data.prop.shown() }
    val initialColor = remember(data.prop, initialValue) {
        when (val v = initialValue) {
            is PolyColor -> Color(v.argb).let {
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
    val textColor = animateColorAsState(
        if (currentColor.luminance() > 0.6f) Color.Black else Color.White
    )

    val borderColor = animateColorAsState(
        if (isHovered || expanded) theme.textColor.copy(0.3f) else theme.borderColor
    )

    fun persistColor(color: Color) {
        currentColor = color
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
        }
    }

    ChromaColorAnimation(pickerModel) {
        currentColor = it
        if (data.prop.type == PolyColor::class.java) updateAccent()
    }

    val showGradient = data.gradient && pickerModel.gradientEnabled && !pickerModel.chromaEnabled
    val swatch = if (showGradient) Brush.horizontalGradient(listOf(currentColor, pickerModel.gradientColor)) else SolidColor(currentColor)

    Box {
        Row(
            modifier = Modifier
                .onClick(interactionSource) { expanded = !expanded }
                .pointerHoverIcon(PointerIcon.Hand)
                .background(swatch, LocalTheme.current.sideBarNavigationEntryShape)
                .border(1.dp, borderColor, LocalTheme.current.sideBarNavigationEntryShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon("paintbrush", color = textColor, modifier = Modifier.size(14.dp))
            Text(
                if (showGradient) "#${colorToHex(currentColor)} → #${colorToHex(pickerModel.gradientColor)}" else "#${colorToHex(currentColor)}",
                color = textColor,
                fontSize = 13.sp,
            )
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = DpOffset(0.dp, 44.dp),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                ColorPickerPopup(
                    model = pickerModel,
                    onColorChanged = ::persistColor,
                    onClose = { expanded = false },
                    chromaCapable = data.prop.type == PolyColor::class.java,
                    gradientCapable = data.gradient && data.prop.type == PolyColor::class.java,
                )
            }
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
) {
    val theme = LocalTheme.current
    val density = LocalDensity.current
    val hue = model.hue
    val saturation = model.saturation
    val brightness = model.brightness
    val alpha = model.alpha
    val chromaEnabled = model.chromaEnabled
    val chromaSpeed = model.chromaSpeed
    val hexField = remember(model) { FieldText(colorToHex(model.currentColor())) }
    val alphaField = remember(model) { FieldText((model.alpha * 100f).roundToInt().toString()) }
    val speedField = remember(model) { FieldText("%.1f".format(model.chromaSpeed)) }
    val gradientField = remember(model) { FieldText(colorToHex(model.gradientColor)) }
    var hexText by hexField
    var alphaText by alphaField
    var speedText by speedField
    var gradientText by gradientField

    LaunchedEffect(model.chromaEnabled, model.hue) {
        if (model.chromaEnabled) hexText = colorToHex(model.currentColor())
    }

    val currentColor = model.currentColor()
    val circleRadius = theme.circleShape.cornerRadiusOrNull()?.value ?: Float.POSITIVE_INFINITY

    Column(
        modifier = Modifier
            .width(280.dp)
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
            Text("Color Picker", color = theme.textColor, fontSize = 14.sp)
            Icon(
                "close",
                color = theme.textColorSecondary,
                modifier = Modifier.size(14.dp)
                    .onClick(rememberInteractionSource()) { onClose() }
                    .pointerHoverIcon(PointerIcon.Hand)
            )
        }

        DragArea(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(LocalTheme.current.sideBarNavigationEntryShape),
            onDrag = { x, y ->
                model.saturation = x
                model.brightness = 1f - y
                val c = model.currentColor()
                hexText = colorToHex(c)
                onColorChanged(c)
            },
        ) { x, y, w, h ->
            rectGradient(x, y, w, h, Brush.horizontalGradient(Color.White, hsbToColor(hue, 1f, 1f)))
            rectGradient(x, y, w, h, Brush.verticalGradient(Color.Transparent, Color.Black))
            val selectorX = x + saturation * w - 7f
            val selectorY = y + (1f - brightness) * h - 7f
            val r = min(circleRadius, 7f)
            rect(selectorX, selectorY, 14f, 14f, currentColor, r)
            rectStroke(selectorX, selectorY, 14f, 14f, Color.White, 2f, r)
        }

        DragArea(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(LocalTheme.current.sideBarNavigationEntryShape),
            onDrag = { x, _ ->
                model.hue = x * 360f
                val c = model.currentColor()
                hexText = colorToHex(c)
                onColorChanged(c)
            },
        ) { x, y, w, h ->
            rectGradient(x, y, w, h, HueGradient)
            thumb(x, y, w, hue / 360f, circleRadius)
        }

        if (model.alphaEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Opacity", color = theme.textColorSecondary, fontSize = 12.sp)
                SliderValueField(
                    field = alphaField,
                    suffix = "%",
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }.take(3)
                        alphaText = filtered
                        filtered.toIntOrNull()?.coerceIn(0, 100)?.let { pct ->
                            model.alpha = pct / 100f
                            val c = model.currentColor()
                            hexText = colorToHex(c)
                            onColorChanged(c)
                        }
                    },
                )
            }
            DragArea(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(LocalTheme.current.sideBarNavigationEntryShape),
                onDrag = { x, _ ->
                    model.alpha = x
                    alphaText = (model.alpha * 100f).roundToInt().toString()
                    val c = model.currentColor()
                    hexText = colorToHex(c)
                    onColorChanged(c)
                },
            ) { x, y, w, h ->
                val cellSize = 6f / density.density
                for (row in 0..(h / cellSize).toInt()) {
                    for (col in 0..(w / cellSize).toInt()) {
                        val isLight = (row + col) % 2 == 0
                        rect(
                            x + col * cellSize,
                            y + row * cellSize,
                            cellSize,
                            cellSize,
                            if (isLight) Color(0xFFCCCCCC) else Color(0xFF999999),
                        )
                    }
                }
                val opaqueColor = hsbToColor(hue, saturation, brightness, 1f)
                rectGradient(x, y, w, h, Brush.horizontalGradient(opaqueColor.copy(alpha = 0f), opaqueColor))
                thumb(x, y, w, alpha, circleRadius)
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
                    Image(HueDisc, null, Modifier.size(14.dp))
                    Text("Chroma", color = theme.textColor, fontSize = 13.sp)
                }
                SwitchControl(chromaEnabled) {
                    model.chromaEnabled = it
                    onColorChanged(model.currentColor())
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
                    Text("Gradient", color = theme.textColor, fontSize = 13.sp)
                }
                SwitchControl(model.gradientEnabled) {
                    model.gradientEnabled = it
                    if (it && model.gradientColor.toArgb() == currentColor.toArgb()) {
                        model.gradientColor = hsbToColor((model.hue + 40f) % 360f, model.saturation, model.brightness, model.alpha)
                        gradientText = colorToHex(model.gradientColor)
                    }
                    onColorChanged(model.currentColor())
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
                gradientField.OnValueChange { input ->
                    val filtered = input.filter { it.isLetterOrDigit() }.take(8)
                    gradientText = filtered
                    hexToColor(filtered)?.let { c ->
                        model.gradientColor = c
                        onColorChanged(model.currentColor())
                    }
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(theme.componentBackground, LocalTheme.current.sideBarNavigationEntryShape)
                        .border(1.dp, theme.borderColor, LocalTheme.current.sideBarNavigationEntryShape)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("#", color = theme.textColorSecondary, fontSize = 13.sp)
                    FieldTextInput(gradientField, 13f, Modifier.weight(1f))
                }
            }
        }

        if (chromaCapable && chromaEnabled) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Speed", color = theme.textColorSecondary, fontSize = 12.sp)
                    SliderValueField(
                        field = speedField,
                        suffix = "x",
                        onValueChange = { input ->
                            val filtered = filterNumberInput(input).take(4)
                            speedText = filtered
                            filtered.toFloatOrNull()?.coerceIn(0.1f, 4f)?.let {
                                model.chromaSpeed = it
                                onColorChanged(model.currentColor())
                            }
                        },
                    )
                }
                DragArea(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(LocalTheme.current.sideBarNavigationEntryShape),
                    onDrag = { x, _ ->
                        model.chromaSpeed = (x * 4f).coerceIn(0.1f, 4f)
                        speedText = "%.1f".format(model.chromaSpeed)
                        onColorChanged(model.currentColor())
                    },
                ) { x, y, w, h ->
                    rectGradient(x, y, w, h, Brush.horizontalGradient(theme.componentBackground, Accent))
                    thumb(x, y, w, (chromaSpeed - 0.1f) / 3.9f, circleRadius)
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

            hexField.OnValueChange { input ->
                val filtered = input.filter { it.isLetterOrDigit() }.take(8)
                hexText = filtered
                hexToColor(filtered)?.let { c ->
                    model.applyFrom(c)
                    onColorChanged(c)
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(theme.componentBackground, LocalTheme.current.sideBarNavigationEntryShape)
                    .border(1.dp, theme.borderColor, LocalTheme.current.sideBarNavigationEntryShape)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("#", color = theme.textColorSecondary, fontSize = 13.sp)
                FieldTextInput(hexField, 13f, Modifier.weight(1f))
            }
        }
    }
}

private class FieldText(initial: String) {
    val state = TextFieldState(initial)
    var last = initial
        private set

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String = last

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        last = value
        if (state.text != value) state.replaceText(value)
    }
}

@Composable
private fun FieldText.OnValueChange(onValueChange: (String) -> Unit) {
    val text = state.text
    LaunchedEffect(this, text) {
        if (text != last) onValueChange(text)
    }
}

@Composable
private fun FieldTextInput(field: FieldText, fontSize: Float, modifier: Modifier, textAlign: TextAlign = TextAlign.Start) {
    val theme = LocalTheme.current
    val clipboard = LocalUiClipboard.current
    BasicTextField(
        field.state,
        PolyFont(theme.typography.family.resolve(FontWeight.Normal), fontSize),
        color = theme.textColor,
        caretColor = theme.textColor,
        selectionColor = Accent.withAlpha(0.4f),
        readClipboard = clipboard.read,
        writeClipboard = clipboard.write,
        modifier = modifier,
        align = textAlign,
    )
}

@Composable
private fun DragArea(
    modifier: Modifier,
    onDrag: (x: Float, y: Float) -> Unit,
    onDraw: RenderContext.(x: Float, y: Float, width: Float, height: Float) -> Unit,
) {
    val dragging = remember { BooleanArray(1) }
    Box(
        modifier = modifier.onPointerEvent { e ->
            val n = e.node
            fun send() = onDrag(((e.x - n.x) / n.width).coerceIn(0f, 1f), ((e.y - n.y) / n.height).coerceIn(0f, 1f))
            when (e.type) {
                PointerEventType.Press -> e.button == 0 && run { dragging[0] = true; send(); true }
                PointerEventType.Move -> dragging[0] && run { send(); true }
                PointerEventType.Release -> dragging[0].also { dragging[0] = false }
                else -> false
            }
        }
    ) {
        Canvas(Modifier.fillMaxSize(), onDraw = onDraw)
    }
}

private fun RenderContext.thumb(x: Float, y: Float, w: Float, fraction: Float, circleRadius: Float) =
    rect(x + (fraction * w - 5f).coerceAtLeast(0f), y + 3f, 10f, 10f, Color.White, min(circleRadius, 5f))

@Composable
private fun SliderValueField(
    field: FieldText,
    onValueChange: (String) -> Unit,
    suffix: String = "",
    width: Dp = 56.dp,
) {
    val theme = LocalTheme.current
    field.OnValueChange(onValueChange)
    Row(
        modifier = Modifier
            .width(width)
            .background(theme.componentBackground, theme.sideBarNavigationEntryShape)
            .border(1.dp, theme.borderColor, theme.sideBarNavigationEntryShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FieldTextInput(field, 12f, Modifier.weight(1f), TextAlign.End)
        if (suffix.isNotEmpty()) {
            Text(
                suffix,
                color = theme.textColorSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
