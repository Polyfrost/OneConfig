@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.min
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

internal fun formControlString(key: String, fallback: String): String =
    runCatching { Platform.i18n().translateString(key) }.getOrNull()?.takeIf { it.isNotBlank() && it != key } ?: fallback

internal fun formOnLabel(): String = formControlString("oneconfig.control.on", "On")

internal fun formOffLabel(): String = formControlString("oneconfig.control.off", "Off")

internal fun Modifier.formFrame(
    shape: Shape,
    accent: Brush,
    fill: () -> Color = { Color.Transparent },
    accentAlpha: () -> Float = { 0f },
    borderWidth: Dp = 1.dp,
    border: () -> Color,
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val strokeWidth = min(ceil(borderWidth.toPx()), ceil(size.minDimension / 2f))
    val half = strokeWidth / 2f
    val stroke = Stroke(strokeWidth)
    val inset = Size(size.width - strokeWidth, size.height - strokeWidth)
    onDrawWithContent {
        val f = fill()
        if (f.alpha > 0f) drawOutline(outline, f)
        val a = accentAlpha()
        if (a > 0f) drawOutline(outline, accent, a)
        drawContent()
        val b = border()
        if (b.alpha <= 0f) return@onDrawWithContent
        val rr = (outline as? Outline.Rounded)?.roundRect
        when {
            outline is Outline.Rectangle -> drawRect(b, Offset(half, half), inset, style = stroke)
            rr != null && rr.isSimple && rr.topLeftCornerRadius.x >= half ->
                drawRoundRect(b, Offset(half, half), inset, CornerRadius(rr.topLeftCornerRadius.x - half, rr.topLeftCornerRadius.y - half), stroke)
            else -> drawOutline(outline, b, style = stroke)
        }
    }
}

@Composable
internal fun Modifier.groupInteractions(source: MutableInteractionSource?, enabled: Boolean): Modifier {
    if (source == null) return this
    val focus = remember(source) { arrayOfNulls<FocusInteraction.Focus>(1) }
    DisposableEffect(source) {
        onDispose {
            focus[0]?.let { source.tryEmit(FocusInteraction.Unfocus(it)) }
            focus[0] = null
        }
    }
    return hoverable(source, enabled).onFocusChanged { state ->
        val current = focus[0]
        if (state.hasFocus && current == null) {
            val next = FocusInteraction.Focus()
            focus[0] = next
            source.tryEmit(next)
        } else if (!state.hasFocus && current != null) {
            source.tryEmit(FocusInteraction.Unfocus(current))
            focus[0] = null
        }
    }
}
