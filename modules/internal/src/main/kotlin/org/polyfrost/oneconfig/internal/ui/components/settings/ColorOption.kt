package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.api.settings.ColorOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.updateAccent
import kotlin.math.roundToInt

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
    val x = c * (1f - kotlin.math.abs(h.mod(2f) - 1f))
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

private fun colorToHex(color: Color): String {
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
    } catch (_: Exception) { null }
}

internal class ColorPickerModel(initialColor: Color) {
    private val hsb = colorToHsb(initialColor)
    var hue by mutableFloatStateOf(hsb[0])
    var saturation by mutableFloatStateOf(hsb[1])
    var brightness by mutableFloatStateOf(hsb[2])
    var alpha by mutableFloatStateOf(initialColor.alpha)
    var chromaEnabled by mutableStateOf(false)
    var chromaSpeed by mutableFloatStateOf(1f)

    fun currentColor(): Color = hsbToColor(hue, saturation, brightness, alpha)

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
    LaunchedEffect(model.chromaEnabled, model.chromaSpeed) {
        if (!model.chromaEnabled) return@LaunchedEffect
        var lastNanos = withInfiniteAnimationFrameNanos { it }
        while (true) {
            withInfiniteAnimationFrameNanos { frameNanos ->
                val dt = (frameNanos - lastNanos) / 1_000_000_000f
                lastNanos = frameNanos
                model.hue = (model.hue + 360f * model.chromaSpeed * dt) % 360f
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

    val initialValue = remember(data.prop) { data.prop.get() }
    val initialColor = remember(data.prop, initialValue) {
        when (val v = initialValue) {
            is Color -> v
            is PolyColor -> Color(v.argb)
            is Int -> Color(v)
            is java.awt.Color -> Color(v.red / 255f, v.green / 255f, v.blue / 255f, v.alpha / 255f)
            else -> Color.White
        }
    }
    var currentColor by remember(data.prop) { mutableStateOf(initialColor) }
    val pickerModel = remember(data.prop, initialValue) {
        ColorPickerModel(initialColor).apply {
            val color = initialValue as? PolyColor
            chromaEnabled = color?.chroma == true
            chromaSpeed = color?.chromaSpeed ?: 1f
        }
    }
    val textColor by animateColorAsState(
        if (currentColor.luminance() > 0.6f) Color.Black else Color.White
    )

    val borderColor by animateColorAsState(
        if (isHovered || expanded) theme.textColor.copy(0.3f) else theme.borderColor
    )

    fun persistColor(color: Color) {
        currentColor = color
        @Suppress("UNCHECKED_CAST")
        when {
            data.prop.type == Int::class.java || data.prop.type == Int::class.javaPrimitiveType ->
                (data.prop as Property<Any>).set(color.toArgb())
            data.prop.type == PolyColor::class.java -> {
                (data.prop as Property<Any>).set(
                    PolyColor(color.toArgb(), pickerModel.chromaEnabled, pickerModel.chromaSpeed)
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

    ChromaColorAnimation(pickerModel) { persistColor(it) }

    Box {
        Row(
            modifier = Modifier
                .onClick(interactionSource) { expanded = !expanded }
                .pointerHoverIcon(PointerIcon.Hand)
                .background(currentColor, LocalTheme.current.sideBarNavigationEntryShape)
                .border(1.dp, borderColor, LocalTheme.current.sideBarNavigationEntryShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon("paintbrush", color = textColor, modifier = Modifier.size(14.dp))
            Text("#${colorToHex(currentColor)}", color = textColor, fontSize = 13.sp)
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, 44),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                ColorPickerPopup(
                    model = pickerModel,
                    onColorChanged = ::persistColor,
                    onClose = { expanded = false },
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
) {
    val theme = LocalTheme.current
    val hue = model.hue
    val saturation = model.saturation
    val brightness = model.brightness
    val alpha = model.alpha
    val chromaEnabled = model.chromaEnabled
    val chromaSpeed = model.chromaSpeed
    var hexText by remember(model) { mutableStateOf(colorToHex(model.currentColor())) }

    LaunchedEffect(model.chromaEnabled, model.hue) {
        if (model.chromaEnabled) hexText = colorToHex(model.currentColor())
    }

    val currentColor = model.currentColor()

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
                            onColorChanged(c)
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
            // selector circle
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

        // hue bar
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
                            onColorChanged(c)
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
                    .offset { IntOffset(((hue / 360f) * hueBarWidth - 5.dp.toPx()).roundToInt().coerceAtLeast(0), 0) }
                    .size(10.dp, 10.dp)
                    .clip(LocalTheme.current.circleShape)
                    .background(Color.White)
                    .border(1.dp, Color.White.copy(0.5f), LocalTheme.current.checkBoxShape)
            )
        }

        // alpha bar
        Text("Opacity", color = theme.textColorSecondary, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(LocalTheme.current.sideBarNavigationEntryShape)
                .drawWithCache {
                    val opaqueColor = hsbToColor(hue, saturation, brightness, 1f)
                    val alphaGradient = Brush.horizontalGradient(
                        listOf(opaqueColor.copy(alpha = 0f), opaqueColor)
                    )
                    onDrawBehind {
                        // checkerboard background
                        val cellSize = 6f
                        for (row in 0..(size.height / cellSize).toInt()) {
                            for (col in 0..(size.width / cellSize).toInt()) {
                                val isLight = (row + col) % 2 == 0
                                drawRect(
                                    if (isLight) Color(0xFFCCCCCC) else Color(0xFF999999),
                                    topLeft = Offset(col * cellSize, row * cellSize),
                                    size = Size(cellSize, cellSize)
                                )
                            }
                        }
                        drawRect(alphaGradient)
                    }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        fun update(x: Float) {
                            model.alpha = (x / size.width).coerceIn(0f, 1f)
                            val c = model.currentColor()
                            hexText = colorToHex(c)
                            onColorChanged(c)
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
                    .offset { IntOffset(((alpha) * alphaBarWidth - 5.dp.toPx()).roundToInt().coerceAtLeast(0), 0) }
                    .size(10.dp, 10.dp)
                    .clip(LocalTheme.current.circleShape)
                    .background(Color.White)
                    .border(1.dp, Color.White.copy(0.5f), LocalTheme.current.checkBoxShape)
            )
        }

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
                Text("Chroma", color = theme.textColor, fontSize = 13.sp)
            }
            SwitchControl(chromaEnabled) {
                model.chromaEnabled = it
                onColorChanged(model.currentColor())
            }
        }

        if (chromaEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Speed", color = theme.textColorSecondary, fontSize = 12.sp)
                    Text("%.1fx".format(chromaSpeed), color = theme.textColorSecondary, fontSize = 12.sp)
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
                                    onColorChanged(model.currentColor())
                                }
                                update(down.position.x)
                                do {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { ch ->
                                        if (ch.pressed) { update(ch.position.x); ch.consume() }
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

            BasicTextField(
                value = hexText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isLetterOrDigit() }.take(8)
                    hexText = filtered
                    hexToColor(filtered)?.let { c ->
                        model.applyFrom(c)
                        onColorChanged(c)
                    }
                },
                singleLine = true,
                textStyle = TextStyle(
                    color = theme.textColor,
                    fontSize = 13.sp,
                    fontFamily = theme.typography.family,
                ),
                cursorBrush = SolidColor(theme.textColor),
                modifier = Modifier
                    .weight(1f)
                    .background(theme.componentBackground, LocalTheme.current.sideBarNavigationEntryShape)
                    .border(1.dp, theme.borderColor, LocalTheme.current.sideBarNavigationEntryShape)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("#", color = theme.textColorSecondary, fontSize = 13.sp)
                        innerTextField()
                    }
                },
            )
        }
    }
}
