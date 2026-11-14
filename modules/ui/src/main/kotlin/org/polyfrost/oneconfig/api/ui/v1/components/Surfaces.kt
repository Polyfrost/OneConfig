@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
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

private val PanelElevation = 16.dp
private val PopupElevation = 12.dp

@ExperimentalOneConfigUiApi
@Composable
public fun Modifier.themedSurface(kind: SurfaceKind = SurfaceKind.Panel, shape: Shape? = null, selected: Boolean = false): Modifier {
    val theme = OneConfigTheme.current
    val surfaceShape = shape ?: theme.surfaceShape(kind)
    val fill = theme.surfaceFill(kind)
    val floating = kind == SurfaceKind.Panel || kind == SurfaceKind.Popup
    val bridge = glassBridge()
    val shadowed = if (floating && theme.shadowEnabled) {
        Modifier.shadow(
            if (kind == SurfaceKind.Panel) PanelElevation else PopupElevation,
            surfaceShape,
            clip = false,
            ambientColor = theme.shadowColor,
            spotColor = theme.shadowColor,
        )
    } else {
        Modifier
    }
    val treated = when {
        bridge == null -> this.then(shadowed)
        theme.style == ThemeStyle.Glass -> bridge.liquidGlass(this.then(shadowed), surfaceShape, floating)
        floating && fill.alpha < 1f -> bridge.backdropBlur(this.then(shadowed), surfaceShape)
        else -> this.then(shadowed)
    }
    val surface = treated.clip(surfaceShape).background(fill, surfaceShape)
    return if (selected) {
        surface.border(1.dp, theme.accentBrush, surfaceShape)
    } else {
        surface.border(1.dp, theme.borderColor, surfaceShape)
    }
}

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
