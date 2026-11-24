@file:OptIn(ExperimentalOneConfigUiApi::class, InternalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.internal.glassBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigUITheme
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeStyle

@ExperimentalOneConfigUiApi
public enum class SurfaceKind {
    Panel,

    Popup,

    Card,

    Field,
}

@ExperimentalOneConfigUiApi
public enum class CardBorder {
    Gradient,

    Solid,

    None,
}

private val PanelElevation = 16.dp

@ExperimentalOneConfigUiApi
@Composable
public fun Modifier.themedSurface(
    kind: SurfaceKind = SurfaceKind.Panel,
    shape: Shape? = null,
    selected: Boolean = false,
    hoverGlow: Boolean = false,
    border: CardBorder = CardBorder.Solid,
    borderColor: Color? = null,
    glass: Boolean = true,
): Modifier {
    if (kind != SurfaceKind.Popup) return surfaceLayers(kind, shape, selected, hoverGlow, border, borderColor, glass = glass)
    val theme = OneConfigTheme.current
    val surfaceShape = shape ?: theme.popupShape
    val popup = componentsBridge().popupSurface(this, surfaceShape, true, glass)
    return if (selected) popup.border(1.dp, theme.accentBrush, surfaceShape) else popup
}

@ExperimentalOneConfigUiApi
@Composable
public fun Modifier.popupSurface(shape: Shape? = null, blur: Boolean = true): Modifier =
    componentsBridge().popupSurface(this, shape ?: OneConfigTheme.current.popupShape, blur, true)

@InternalOneConfigUiApi
@ExperimentalOneConfigUiApi
@Composable
public fun Modifier.surfaceLayers(
    kind: SurfaceKind,
    shape: Shape?,
    selected: Boolean,
    hoverGlow: Boolean = false,
    border: CardBorder = CardBorder.Solid,
    borderColor: Color? = null,
    blur: Boolean = true,
    glass: Boolean = true,
): Modifier {
    val theme = OneConfigTheme.current
    val surfaceShape = shape ?: theme.surfaceShape(kind)
    val fill = theme.surfaceFill(kind)
    val floating = kind == SurfaceKind.Panel || kind == SurfaceKind.Popup
    val bridge = glassBridge()
    val shadowed = if (kind == SurfaceKind.Panel && theme.shadowEnabled) {
        Modifier.shadow(
            PanelElevation,
            surfaceShape,
            clip = false,
            ambientColor = theme.shadowColor,
            spotColor = theme.shadowColor,
        )
    } else {
        Modifier
    }
    val glowing = if (hoverGlow && bridge != null) bridge.hoverGlow(this, surfaceShape) else this
    val treated = when {
        bridge == null -> glowing.then(shadowed)
        glass && theme.style == ThemeStyle.Glass -> bridge.liquidGlass(glowing.then(shadowed), surfaceShape, floating && blur)
        blur && floating && fill.alpha < 1f -> bridge.backdropBlur(glowing.then(shadowed), surfaceShape)
        else -> glowing.then(shadowed)
    }
    val surface = treated.clip(surfaceShape).background(fill, surfaceShape)
    if (selected) return surface.border(1.dp, theme.accentBrush, surfaceShape)
    val edge = borderColor ?: theme.borderColor
    return when (border) {
        CardBorder.Gradient -> surface.border(1.dp, remember(edge) { Brush.verticalGradient(listOf(edge, edge.copy(alpha = 0f))) }, surfaceShape)
        CardBorder.Solid -> surface.border(1.dp, edge, surfaceShape)
        CardBorder.None -> surface
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun Modifier.cardSurface(
    shape: Shape? = null,
    hoverGlow: Boolean = true,
    border: CardBorder = CardBorder.Gradient,
    borderColor: Color? = null,
): Modifier = themedSurface(SurfaceKind.Card, shape, false, hoverGlow, border, borderColor)

@ExperimentalOneConfigUiApi
@Composable
public fun Modifier.liquidGlass(shape: Shape, frosted: Boolean = true): Modifier =
    glassBridge()?.liquidGlass(this, shape, frosted) ?: this

@ExperimentalOneConfigUiApi
@Composable
public fun ProvideThemeVariant(dark: Boolean, keepAccent: Boolean = true, content: @Composable () -> Unit) {
    val bridge = glassBridge()
    if (bridge == null) {
        content()
        return
    }
    bridge.ProvideThemeVariant(dark, keepAccent, content)
}

private fun OneConfigUITheme.surfaceShape(kind: SurfaceKind): Shape = when (kind) {
    SurfaceKind.Panel -> backgroundShape
    SurfaceKind.Popup -> popupShape
    SurfaceKind.Card -> shapes.large
    SurfaceKind.Field -> shapes.field
}

private fun OneConfigUITheme.surfaceFill(kind: SurfaceKind): Color = when (kind) {
    SurfaceKind.Panel -> pageBackground
    SurfaceKind.Popup -> popupBackground
    SurfaceKind.Card -> modCardBackground
    SurfaceKind.Field -> componentBackground
}
