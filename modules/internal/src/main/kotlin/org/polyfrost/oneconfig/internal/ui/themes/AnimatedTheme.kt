package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.CornerBasedShape

private const val THEME_ANIM_MS = 350

/**
 * Animates every interpolatable property of [target] (colors + corner radii) so a theme
 * switch transitions smoothly instead of snapping. Non-interpolatable properties
 * (fonts, branding, boolean flags, circle shapes) switch instantly.
 */
@Composable
fun animateTheme(target: UITheme): UITheme {
    val spec = tween<Float>(THEME_ANIM_MS)
    val colorSpec = tween<Color>(THEME_ANIM_MS)

    val pageBackground by animateColorAsState(target.pageBackground, colorSpec, label = "pageBackground")
    val sidebarBackground by animateColorAsState(target.sidebarBackground, colorSpec, label = "sidebarBackground")
    val chipBackground by animateColorAsState(target.chipBackground, colorSpec, label = "chipBackground")
    val modCardBackground by animateColorAsState(target.modCardBackground, colorSpec, label = "modCardBackground")
    val componentBackground by animateColorAsState(target.componentBackground, colorSpec, label = "componentBackground")
    val popupBackground by animateColorAsState(target.popupBackground, colorSpec, label = "popupBackground")

    val borderColor by animateColorAsState(target.borderColor, colorSpec, label = "borderColor")
    val textColor by animateColorAsState(target.textColor, colorSpec, label = "textColor")
    val textColorSecondary by animateColorAsState(target.textColorSecondary, colorSpec, label = "textColorSecondary")
    val accentTextColor by animateColorAsState(target.accentTextColor, colorSpec, label = "accentTextColor")

    val shadowColor by animateColorAsState(target.shadowColor, colorSpec, label = "shadowColor")
    val controlThumbColor by animateColorAsState(target.controlThumbColor, colorSpec, label = "controlThumbColor")

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
        backgroundShape = backgroundShape,
        sideBarNavigationEntryShape = sideBarNavigationEntryShape,
        modCardShape = modCardShape,
        checkBoxShape = checkBoxShape,
        buttonShape = buttonShape,
        popupShape = popupShape,
    )
}

// large reference size so dp-based corners resolve independently of layout size
private val REF_SIZE = Size(1_000_000f, 1_000_000f)

/**
 * Animates the corner radius (in px) of a [RoundedCornerShape]-style target. Percent-based
 * corners (e.g. CircleShape) resolve to large values at [REF_SIZE]; those are left untouched
 * so circles stay circular.
 */
@Composable
private fun animateCornerShape(target: Shape, spec: androidx.compose.animation.core.AnimationSpec<Float>): Shape {
    if (target !is CornerBasedShape) return target
    val density = LocalDensity.current
    val targetPx = remember(target, density) {
        runCatching { target.topStart.toPx(REF_SIZE, density) }.getOrDefault(Float.NaN)
    }
    // percent/circle corners scale with size -> huge at REF_SIZE; don't animate, keep as-is
    if (targetPx.isNaN() || targetPx >= REF_SIZE.minDimension / 2f) return target
    val animatedPx by animateFloatAsState(targetPx, spec, label = "cornerRadius")
    return RoundedCornerShape(animatedPx)
}
