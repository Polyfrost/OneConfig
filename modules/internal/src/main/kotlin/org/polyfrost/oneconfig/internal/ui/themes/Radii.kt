package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The corner radius scale
 *
 * Every rounded surface picks a step from here rather than a one-off number so nested shapes read as one
 * family
 *
 * Steps are spaced so a container one step up from its content leaves room for the padding between them
 * see [concentric]
 */
object Radii {
    /** Inline controls such as a checkbox slider thumb or swatch */
    val XS = 6.dp

    /** Small chrome sitting on a card such as buttons chips and list entries */
    val SM = 8.dp

    /** Setting rows and mod or changelog cards */
    val MD = 16.dp

    /** Anything wrapping padded content such as popups dropdown menus list containers and the window */
    val LG = 20.dp
}

// large reference size so dp-based corners resolve independently of layout size
private val REF_SIZE = Size(1_000_000f, 1_000_000f)

/**
 * Returns the radius of this shape in [Dp] or `null` if it is not a fixed-radius rounded rectangle
 *
 * Percent-based corners such as `CircleShape` resolve to huge values at [REF_SIZE]
 */
@Composable
fun Shape.cornerRadiusOrNull(): Dp? {
    if (this !is CornerBasedShape) return null
    val density = LocalDensity.current
    val px = remember(this, density) {
        runCatching { topStart.toPx(REF_SIZE, density) }.getOrDefault(Float.NaN)
    }
    if (px.isNaN() || px >= REF_SIZE.minDimension / 2f) return null
    return with(density) { px.toDp() }
}

/**
 * The radius a nested shape should use so the two stay concentric
 *
 * The gap between the outer and inner curve is then the same all the way around the corner instead of
 * pinching at the diagonal
 *
 * Follows the concentricity rule `inner = outer - padding` clamped at zero so an over-padded container
 * degrades to a sharp inner corner rather than a negative radius
 *
 * Shapes without a fixed radius are returned unchanged since a circle nested in a circle is already
 * concentric
 */
@Composable
fun Shape.concentric(inset: Dp, minRadius: Dp = 0.dp): Shape {
    val outer = cornerRadiusOrNull() ?: return this
    val inner = (outer - inset).coerceAtLeast(minRadius)
    return RoundedCornerShape(inner)
}
