@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeStyle

private val DEFAULT_ICON_SIZE = 18.dp

private const val PIXEL_ICON_CELLS = 12

@ExperimentalOneConfigUiApi
@Composable
public fun Icon(
    name: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    tintProducer: (() -> Color)? = null,
    fitAspectRatio: Boolean = false,
) {
    val pixel = !fitAspectRatio && OneConfigTheme.current.style == ThemeStyle.Pixel &&
        (OneConfigTheme.icon(name) != name || "/minecraft/" in name)
    val sized = modifier.describedAs(contentDescription).then(if (pixel) Modifier.snapToCells(PIXEL_ICON_CELLS) else Modifier)
    componentsBridge().Icon(name, tint, sized, fitAspectRatio, tintProducer)
}

@ExperimentalOneConfigUiApi
@Composable
public fun Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val theme = OneConfigTheme.current
    val cells = if (theme.style != ThemeStyle.Pixel) null
    else (painter as? BitmapPainter)?.intrinsicSize?.width?.takeIf { it.isFinite() && it >= 1f }?.roundToInt()
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier
            .then(if (cells != null) Modifier.snapToCells(cells) else Modifier)
            .size(DEFAULT_ICON_SIZE),
        colorFilter = ColorFilter.tint(if (tint.isSpecified) tint else theme.textColor),
    )
}

@ExperimentalOneConfigUiApi
@Composable
public fun Icon(iconName: String, color: Color, modifier: Modifier) {
    Icon(iconName, null, modifier, color)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun Icon(iconName: String, color: Color, modifier: Modifier, fitAspectRatio: Boolean) {
    Icon(iconName, null, modifier, color, fitAspectRatio = fitAspectRatio)
}

private fun Modifier.describedAs(contentDescription: String?): Modifier =
    if (contentDescription == null) this
    else semantics {
        this.contentDescription = contentDescription
        role = Role.Image
    }

private fun Modifier.snapToCells(cells: Int): Modifier = layout { measurable, constraints ->
    val limit = minOf(constraints.maxWidth, constraints.maxHeight)
    val requested = if (constraints.hasFixedWidth && constraints.hasFixedHeight) limit else DEFAULT_ICON_SIZE.roundToPx().coerceAtMost(limit)
    var steps = (requested.toFloat() / cells).roundToInt()
    while (steps > 0 && steps * cells > limit) steps--
    val side = if (steps > 0) steps * cells else requested
    val placeable = measurable.measure(Constraints.fixed(side, side))
    val width = constraints.constrainWidth(side)
    val height = constraints.constrainHeight(side)
    layout(width, height) { placeable.place((width - side) / 2, (height - side) / 2) }
}
