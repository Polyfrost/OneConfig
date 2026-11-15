package org.polyfrost.oneconfig.api.ui.v1.internal

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import org.polyfrost.oneconfig.api.ui.v1.components.themedScrollbarStyle
import org.polyfrost.oneconfig.internal.ui.components.backdropBlur as internalBackdropBlur
import org.polyfrost.oneconfig.internal.ui.components.wheelScroll as internalWheelScroll
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.CustomBase
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.LocalThemeTarget
import org.polyfrost.oneconfig.internal.ui.themes.ReduceTransparency
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.UITheme
import org.polyfrost.oneconfig.internal.ui.themes.accentTextOn
import org.polyfrost.oneconfig.internal.ui.themes.animateTheme
import org.polyfrost.oneconfig.internal.ui.themes.baseTheme
import org.polyfrost.oneconfig.internal.ui.themes.customSpec
import org.polyfrost.oneconfig.internal.ui.themes.isCustom
import org.polyfrost.oneconfig.internal.ui.themes.palette
import org.polyfrost.oneconfig.internal.ui.components.animatedFrame as internalAnimatedFrame
import org.polyfrost.oneconfig.internal.ui.components.glassHover as internalGlassHover
import org.polyfrost.oneconfig.internal.ui.components.hoverGlow as internalHoverGlow
import org.polyfrost.oneconfig.internal.ui.components.liquidGlass as internalLiquidGlass

private val LocalVariantBase = compositionLocalOf<UITheme?> { null }

class OneConfigUIGlassBridgeImpl : OneConfigUIGlassBridge {
    @Composable
    override fun glassHover(modifier: Modifier, shape: Shape, pull: Dp): Modifier = modifier.internalGlassHover(shape, pull)

    @Composable
    override fun liquidGlass(modifier: Modifier, shape: Shape): Modifier = modifier.internalLiquidGlass(shape)

    override fun animatedFrame(modifier: Modifier, shape: Shape, fill: () -> Color, accentAlpha: () -> Float, border: () -> Color): Modifier =
        modifier.internalAnimatedFrame(shape, fill, accentAlpha, border = border)

    @Composable
    override fun liquidGlass(modifier: Modifier, shape: Shape, frosted: Boolean): Modifier = modifier.internalLiquidGlass(shape, frosted)

    @Composable
    override fun backdropBlur(modifier: Modifier, shape: Shape): Modifier = modifier.internalBackdropBlur(shape)

    @Composable
    override fun hoverGlow(modifier: Modifier, shape: Shape): Modifier = modifier.internalHoverGlow(shape)

    @Composable
    override fun liquidGlass(modifier: Modifier, shape: Shape, frosted: Boolean, alpha: () -> Float): Modifier =
        modifier.internalLiquidGlass(shape, frosted, alpha)

    override fun wheelScroll(modifier: Modifier, state: ScrollableState, orientation: Orientation): Modifier =
        modifier.internalWheelScroll(state, orientation)

    @Composable
    override fun ProvideThemeVariant(dark: Boolean, keepAccent: Boolean, content: @Composable () -> Unit) {
        val base = LocalVariantBase.current ?: LocalThemeTarget.current ?: LocalTheme.current
        val opaque = ReduceTransparency.enabled
        val accent = Accent
        val target = remember(base, dark, keepAccent, opaque, accent) { variantOf(base, dark, keepAccent, opaque, accent) }
        val shown = animateTheme(target)
        val scrollbar = LocalScrollbarStyle.current
        val scrollbarStyle = remember(scrollbar, shown.textColorSecondary) {
            themedScrollbarStyle(shown.textColorSecondary, scrollbar.shape, scrollbar.thickness, scrollbar.minimalHeight, scrollbar.hoverDurationMillis)
        }
        CompositionLocalProvider(
            LocalVariantBase provides base,
            LocalTheme provides shown,
            LocalThemeTarget provides target,
            LocalScrollbarStyle provides scrollbarStyle,
            content = content,
        )
    }
}

private fun variantOf(base: UITheme, dark: Boolean, keepAccent: Boolean, opaque: Boolean, accent: Color): UITheme {
    if (base.isLight != dark) return base
    val sibling = siblingOf(base, light = !dark)
    val accentText = if (keepAccent) base.accentTextColor else sibling.accentTextOn(accent)
    val shaped = sibling.copy(
        accentTextColor = accentText,
        backgroundShape = base.backgroundShape,
        sideBarNavigationEntryShape = base.sideBarNavigationEntryShape,
        modCardShape = base.modCardShape,
        checkBoxShape = base.checkBoxShape,
        buttonShape = base.buttonShape,
        popupShape = base.popupShape,
        circleShape = base.circleShape,
    ).withControlTrackColor(sibling.controlTrackColor)
    return if (opaque) shaped.withOpaqueSurfaces() else shaped
}

private fun siblingOf(theme: UITheme, light: Boolean): UITheme {
    val themes = ThemeRegistry.registry.toList()
    theme.palette?.group?.let { group ->
        themes.firstOrNull { it.palette?.group == group && it.isLight == light }?.let { return it }
    }
    theme.customSpec?.let { return baseTheme(it.base, light) }
    if (!theme.isCustom) {
        val family = familyName(theme.name)
        themes.firstOrNull { !it.isCustom && familyName(it.name) == family && it.isLight == light }?.let { return it }
    }
    val style = when {
        theme.liquidGlass -> CustomBase.POLYGLASS
        theme.isMinecraftStyle -> CustomBase.MINECRAFT
        else -> CustomBase.POLYFROSTED
    }
    return baseTheme(style, light)
}

private fun familyName(name: String): String = name.removeSuffix(" Light").removeSuffix(" Dark")
