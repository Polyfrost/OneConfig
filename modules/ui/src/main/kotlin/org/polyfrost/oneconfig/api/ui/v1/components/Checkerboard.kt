@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
@Composable
public fun Modifier.transparencyCheckerboard(cell: Dp = 6.dp): Modifier {
    val theme = OneConfigTheme.current
    val light = theme.checkerLight
    val dark = theme.checkerDark
    return drawBehind {
        val side = cell.toPx()
        if (side <= 0f) return@drawBehind
        clipRect {
            drawRect(dark)
            val columns = ceil(size.width / side).toInt()
            val rows = ceil(size.height / side).toInt()
            val square = Size(side, side)
            for (row in 0 until rows) {
                for (column in row % 2 until columns step 2) {
                    drawRect(light, Offset(column * side, row * side), square)
                }
            }
        }
    }
}
