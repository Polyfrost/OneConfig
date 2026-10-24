package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import org.polyfrost.compose.foundation.shape.RoundedCornerShape
import org.polyfrost.compose.ui.graphics.Shape
import org.polyfrost.compose.ui.graphics.lerp
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.shell.fastOutSlowIn

private const val THEME_ANIM_MS = 350f

private class ThemeTween(var from: UITheme, var to: UITheme)

@Composable
fun animateTheme(target: UITheme): UITheme {
    val tween = remember { ThemeTween(target, target) }
    val t = remember { mutableFloatStateOf(1f) }
    LaunchedEffect(target) {
        if (tween.to == target) return@LaunchedEffect
        tween.from = blendTheme(tween.from, tween.to, fastOutSlowIn(t.floatValue))
        tween.to = target
        t.floatValue = 0f
        val start = withFrameNanos { it }
        while (t.floatValue < 1f) t.floatValue = ((withFrameNanos { it } - start) / 1e6f / THEME_ANIM_MS).coerceIn(0f, 1f)
    }
    val e = fastOutSlowIn(t.floatValue)
    if (e >= 1f) return tween.to
    return blendTheme(tween.from, tween.to, e)
}

private fun blendTheme(from: UITheme, to: UITheme, f: Float) = to.copy(
    pageBackground = lerp(from.pageBackground, to.pageBackground, f),
    sidebarBackground = lerp(from.sidebarBackground, to.sidebarBackground, f),
    chipBackground = lerp(from.chipBackground, to.chipBackground, f),
    modCardBackground = lerp(from.modCardBackground, to.modCardBackground, f),
    componentBackground = lerp(from.componentBackground, to.componentBackground, f),
    popupBackground = lerp(from.popupBackground, to.popupBackground, f),
    borderColor = lerp(from.borderColor, to.borderColor, f),
    textColor = lerp(from.textColor, to.textColor, f),
    textColorSecondary = lerp(from.textColorSecondary, to.textColorSecondary, f),
    accentTextColor = lerp(from.accentTextColor, to.accentTextColor, f),
    shadowColor = lerp(from.shadowColor, to.shadowColor, f),
    controlThumbColor = lerp(from.controlThumbColor, to.controlThumbColor, f),
    favoriteColor = lerp(from.favoriteColor, to.favoriteColor, f),
    backgroundShape = lerpShape(from.backgroundShape, to.backgroundShape, f),
    sideBarNavigationEntryShape = lerpShape(from.sideBarNavigationEntryShape, to.sideBarNavigationEntryShape, f),
    modCardShape = lerpShape(from.modCardShape, to.modCardShape, f),
    checkBoxShape = lerpShape(from.checkBoxShape, to.checkBoxShape, f),
    buttonShape = lerpShape(from.buttonShape, to.buttonShape, f),
    popupShape = lerpShape(from.popupShape, to.popupShape, f),
).withControlTrackColor(lerp(from.controlTrackColor, to.controlTrackColor, f))

private fun lerpShape(from: Shape, to: Shape, f: Float): Shape {
    if (from.percent >= 0f || to.percent >= 0f) return to
    return RoundedCornerShape((from.radius + (to.radius - from.radius) * f).dp)
}
