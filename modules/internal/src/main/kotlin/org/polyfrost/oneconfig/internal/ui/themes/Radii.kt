package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.runtime.Composable
import org.polyfrost.compose.foundation.shape.RoundedCornerShape
import org.polyfrost.compose.ui.graphics.Shape
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.coerceAtLeast
import org.polyfrost.compose.ui.unit.dp

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

@Composable
fun Shape.cornerRadiusOrNull(): Dp? = if (percent >= 0f) null else radius.dp

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
