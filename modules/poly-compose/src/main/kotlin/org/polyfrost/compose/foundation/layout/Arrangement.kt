package org.polyfrost.compose.foundation.layout

import org.polyfrost.compose.layout.PolyArrangement
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.dp

object Arrangement {
    interface Horizontal {
        val spacing: Dp
        val mode: PolyArrangement
    }

    interface Vertical {
        val spacing: Dp
        val mode: PolyArrangement
    }

    interface HorizontalOrVertical : Horizontal, Vertical

    private data class Spec(override val mode: PolyArrangement, override val spacing: Dp = 0.dp) : HorizontalOrVertical

    val Start: Horizontal = Spec(PolyArrangement.Start)
    val End: Horizontal = Spec(PolyArrangement.End)
    val Top: Vertical = Spec(PolyArrangement.Start)
    val Bottom: Vertical = Spec(PolyArrangement.End)
    val Center: HorizontalOrVertical = Spec(PolyArrangement.Center)
    val SpaceBetween: HorizontalOrVertical = Spec(PolyArrangement.SpaceBetween)
    val SpaceEvenly: HorizontalOrVertical = Spec(PolyArrangement.SpaceEvenly)
    val SpaceAround: HorizontalOrVertical = Spec(PolyArrangement.SpaceAround)

    fun spacedBy(space: Dp): HorizontalOrVertical = Spec(PolyArrangement.Start, space)

    fun spacedBy(space: Dp, alignment: Alignment.Horizontal): Horizontal = Spec(mode(alignment.bias), space)

    fun spacedBy(space: Dp, alignment: Alignment.Vertical): Vertical = Spec(mode(alignment.bias), space)

    private fun mode(bias: Float) = when {
        bias < 0f -> PolyArrangement.Start
        bias > 0f -> PolyArrangement.End
        else -> PolyArrangement.Center
    }
}
