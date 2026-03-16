package org.polyfrost.oneconfig.internal.ui.components.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FlexibleLayout(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val hSpacing = horizontalSpacing.roundToPx()
        val vSpacing = verticalSpacing.roundToPx()
        val maxWidth = constraints.maxWidth.takeIf { it != Int.MAX_VALUE } ?: Int.MAX_VALUE

        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }

        // greedy row packing ~ fit as many items per row as possible
        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0

        for (placeable in placeables) {
            val neededWidth = if (currentRow.isEmpty()) placeable.width
                              else currentRowWidth + hSpacing + placeable.width
            if (currentRow.isNotEmpty() && neededWidth > maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf(placeable)
                currentRowWidth = placeable.width
            } else {
                currentRow.add(placeable)
                currentRowWidth = neededWidth
            }
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)

        val rowHeights = rows.map { row -> row.maxOfOrNull { it.height } ?: 0 }
        val totalHeight = if (rowHeights.isEmpty()) 0
                          else rowHeights.sum() + vSpacing * (rows.size - 1)

        layout(maxWidth.coerceAtLeast(constraints.minWidth), totalHeight.coerceAtLeast(constraints.minHeight)) {
            var y = 0
            rows.forEachIndexed { i, row ->
                var x = 0
                row.forEach { placeable ->
                    placeable.place(x, y)
                    x += placeable.width + hSpacing
                }
                y += rowHeights[i] + vSpacing
            }
        }
    }
}
