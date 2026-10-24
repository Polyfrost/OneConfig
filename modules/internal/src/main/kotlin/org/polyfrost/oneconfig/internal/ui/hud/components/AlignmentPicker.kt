package org.polyfrost.oneconfig.internal.ui.hud.components

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.animation.core.tween
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.clickable
import org.polyfrost.compose.foundation.layout.*
import org.polyfrost.compose.foundation.shape.RoundedCornerShape
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.geometry.Size
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val alignmentGrid: Map<Alignment, Pair<Int, Int>> = mapOf(
    Alignment.TopStart     to Pair(0, 0),
    Alignment.TopCenter    to Pair(1, 0),
    Alignment.TopEnd       to Pair(2, 0),
    Alignment.CenterStart  to Pair(0, 1),
    Alignment.Center       to Pair(1, 1),
    Alignment.CenterEnd    to Pair(2, 1),
    Alignment.BottomStart  to Pair(0, 2),
    Alignment.BottomCenter to Pair(1, 2),
    Alignment.BottomEnd    to Pair(2, 2),
)

private val gridAlignment: Map<Pair<Int, Int>, Alignment> =
    alignmentGrid.entries.associate { (k, v) -> v to k }

@Composable
fun AlignmentPicker(
    alignment: Alignment,
    onChange: (Alignment) -> Unit
) {
    val theme = LocalTheme.current
    val accentColor = Accent
    val activeColor = theme.textColor
    val inactiveColor = theme.textColorSecondary

    var hoveredCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val totalSize = 125.dp
    val cellSize = totalSize / 3f

    Box(
        modifier = Modifier
            .size(totalSize)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF192126).copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .border(1.dp, theme.borderColor, RoundedCornerShape(8.dp))
    ) {
        for (row in 0..2) {
            for (col in 0..2) {
                val cell = Pair(col, row)
                val cellAlign = gridAlignment[cell] ?: continue
                val isSelected = alignment == cellAlign
                val isHovered = hoveredCell == cell

                AlignmentCell(
                    col = col,
                    row = row,
                    cellSize = cellSize,
                    isSelected = isSelected,
                    isHovered = isHovered,
                    accentColor = accentColor,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    onHover = { hov ->
                        if (hov) hoveredCell = cell else if (hoveredCell == cell) hoveredCell = null
                    },
                    onClick = {
                        Snapshot.withMutableSnapshot {
                            onChange(cellAlign)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AlignmentCell(
    col: Int,
    row: Int,
    cellSize: Dp,
    isSelected: Boolean,
    isHovered: Boolean,
    accentColor: Color,
    activeColor: Color,
    inactiveColor: Color,
    onHover: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val targetColor = when {
        isSelected -> accentColor
        isHovered  -> activeColor
        else       -> inactiveColor
    }

    val color by animateColorAsState(targetColor)
    val animatedAlpha by animateFloatAsState(
        if (isHovered && !isSelected) 0.08f else 0f,
        tween(150)
    )

    val isCenter = col == 1 && row == 1
    val isCorner = (col == 0 || col == 2) && (row == 0 || row == 2)

    Canvas(
        modifier = Modifier
            .absoluteOffset(x = cellSize * col, y = cellSize * row)
            .size(cellSize)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onHover = onHover) {
                UiSounds.play(UiSoundEvent.CLICK)
                onClick()
            }
    ) { x, y, w, h ->
        if (animatedAlpha > 0f) {
            rect(x, y, w, h, activeColor.copy(alpha = animatedAlpha))
        }

        val s = Size(w, h)
        val strokeWidth = 1.5f
        val len = 8f

        save()
        translate(x, y)
        when {
            isCenter -> drawCenterPlus(color, s, strokeWidth * 1.2f, len * 1.6f)
            isCorner -> drawCornerBracket(col, row, color, s, strokeWidth, len)
            else -> drawEdgeTick(col, row, color, s, strokeWidth, len)
        }
        restore()
    }
}

private fun RenderContext.drawCenterPlus(
    color: Color,
    s: Size,
    strokeWidth: Float,
    armLen: Float
) {
    val cx = s.width / 2f
    val cy = s.height / 2f
    val arm = armLen / 2f

    line(cx - arm, cy, cx + arm, cy, color, strokeWidth)
    line(cx, cy - arm, cx, cy + arm, color, strokeWidth)

    val boxSize = armLen * 1.55f
    rectStroke(
        cx - boxSize / 2f,
        cy - boxSize / 2f,
        boxSize,
        boxSize,
        color.copy(alpha = 0.13f),
        strokeWidth * 0.8f,
        6f,
    )
}

private fun RenderContext.drawCornerBracket(
    col: Int,
    row: Int,
    color: Color,
    s: Size,
    strokeWidth: Float,
    len: Float
) {
    val pad = s.width * 0.22f
    val cx = if (col == 0) pad else s.width - pad
    val cy = if (row == 0) pad else s.height - pad
    val hDir = if (col == 0) 1f else -1f
    val vDir = if (row == 0) 1f else -1f

    line(cx, cy, cx + hDir * len, cy, color, strokeWidth)
    line(cx, cy, cx, cy + vDir * len, color, strokeWidth)
}

private fun RenderContext.drawEdgeTick(
    col: Int,
    row: Int,
    color: Color,
    s: Size,
    strokeWidth: Float,
    len: Float
) {
    val cx = s.width / 2f
    val cy = s.height / 2f
    val isHorizontalEdge = row == 1
    val isTopOrLeft = col == 0 || row == 0

    if (isHorizontalEdge) {
        val tipX = if (isTopOrLeft) cx + len * 0.5f else cx - len * 0.5f
        val baseX = if (isTopOrLeft) cx - len * 0.3f else cx + len * 0.3f

        line(baseX, cy, tipX, cy, color, strokeWidth)
        line(tipX, cy - len * 0.5f, tipX, cy + len * 0.5f, color, strokeWidth)
    } else {
        val tipY = if (isTopOrLeft) cy + len * 0.5f else cy - len * 0.5f
        val baseY = if (isTopOrLeft) cy - len * 0.3f else cy + len * 0.3f

        line(cx, baseY, cx, tipY, color, strokeWidth)
        line(cx - len * 0.5f, tipY, cx + len * 0.5f, tipY, color, strokeWidth)
    }
}
