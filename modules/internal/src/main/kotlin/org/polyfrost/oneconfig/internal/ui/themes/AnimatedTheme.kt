package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion

private const val THEME_ANIM_MS = 350

/**
 * Animates every interpolatable property of [target] such as colors and corner radii so a theme switch
 * transitions smoothly
 *
 * Fonts branding boolean flags and circle shapes switch instantly
 */
@Composable
fun animateTheme(target: UITheme): UITheme {
    val last = remember { arrayOf(target) }
    val editing = target.isCustom && last[0].previewImage == target.previewImage && last[0] != target
    SideEffect { last[0] = target }
    val spec: AnimationSpec<Float> = if (editing) snap() else Motion.spec(tween<Float>(THEME_ANIM_MS))
    val colorSpec: AnimationSpec<Color> = if (editing) snap() else Motion.spec(tween<Color>(THEME_ANIM_MS))

    val pageBackground by animateColorAsState(target.pageBackground, colorSpec, label = "pageBackground")
    val sidebarBackground by animateColorAsState(target.sidebarBackground, colorSpec, label = "sidebarBackground")
    val chipBackground by animateColorAsState(target.chipBackground, colorSpec, label = "chipBackground")
    val controlTrackColor by animateColorAsState(target.controlTrackColor, colorSpec, label = "controlTrackColor")
    val modCardBackground by animateColorAsState(target.modCardBackground, colorSpec, label = "modCardBackground")
    val componentBackground by animateColorAsState(target.componentBackground, colorSpec, label = "componentBackground")
    val popupBackground by animateColorAsState(target.popupBackground, colorSpec, label = "popupBackground")

    val borderColor by animateColorAsState(target.borderColor, colorSpec, label = "borderColor")
    val textColor by animateColorAsState(target.textColor, colorSpec, label = "textColor")
    val textColorSecondary by animateColorAsState(target.textColorSecondary, colorSpec, label = "textColorSecondary")
    val accentTextColor by animateColorAsState(target.accentTextColor, colorSpec, label = "accentTextColor")

    val shadowColor by animateColorAsState(target.shadowColor, colorSpec, label = "shadowColor")
    val controlThumbColor by animateColorAsState(target.controlThumbColor, colorSpec, label = "controlThumbColor")
    val favoriteColor by animateColorAsState(target.favoriteColor, colorSpec, label = "favoriteColor")

    val backgroundShape = animateCornerShape(target.backgroundShape, spec)
    val sideBarNavigationEntryShape = animateCornerShape(target.sideBarNavigationEntryShape, spec)
    val modCardShape = animateCornerShape(target.modCardShape, spec)
    val checkBoxShape = animateCornerShape(target.checkBoxShape, spec)
    val buttonShape = animateCornerShape(target.buttonShape, spec)
    val popupShape = animateCornerShape(target.popupShape, spec)

    return target.copy(
        pageBackground = pageBackground,
        sidebarBackground = sidebarBackground,
        chipBackground = chipBackground,
        modCardBackground = modCardBackground,
        componentBackground = componentBackground,
        popupBackground = popupBackground,
        borderColor = borderColor,
        textColor = textColor,
        textColorSecondary = textColorSecondary,
        accentTextColor = accentTextColor,
        shadowColor = shadowColor,
        controlThumbColor = controlThumbColor,
        favoriteColor = favoriteColor,
        backgroundShape = backgroundShape,
        sideBarNavigationEntryShape = sideBarNavigationEntryShape,
        modCardShape = modCardShape,
        checkBoxShape = checkBoxShape,
        buttonShape = buttonShape,
        popupShape = popupShape,
    ).withControlTrackColor(controlTrackColor)
}

/**
 * Animates the corner radius of a [RoundedCornerShape]-style target
 *
 * Shapes without a fixed radius such as `CircleShape` are left untouched so circles stay circular
 */
@Composable
private fun animateCornerShape(target: Shape, spec: AnimationSpec<Float>): Shape {
    val targetRadius = target.cornerRadiusOrNull()
    val animated by animateFloatAsState(targetRadius?.value ?: 0f, if (targetRadius == null) snap() else spec, label = "cornerRadius")
    return if (targetRadius == null) target else RoundedCornerShape(animated.dp)
}
