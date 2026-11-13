@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.a11y.a11y
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeStyle

@ExperimentalOneConfigUiApi
public object ProgressBarDefaults {
    public const val INDETERMINATE_MILLIS: Int = 1200

    public fun thickness(size: ControlSize): Dp = when (size) {
        ControlSize.Compact -> 4.dp
        ControlSize.Regular -> 6.dp
        ControlSize.Large -> 8.dp
    }
}

private const val TRACK_ALPHA = 0.35f
private const val SEGMENT_FRACTION = 0.4f
private const val REDUCED_FILL_ALPHA = 0.5f
private const val PROGRESS_MILLIS = 250

@ExperimentalOneConfigUiApi
@Composable
public fun ProgressBar(progress: Float?, modifier: Modifier = Modifier, size: ControlSize = ControlSize.Compact) {
    val theme = OneConfigTheme.current
    val shape = theme.shapes.pill
    val rounded = theme.style != ThemeStyle.Pixel
    val track = theme.controlTrackColor.let { it.copy(alpha = it.alpha * TRACK_ALPHA) }
    val brush = theme.accentBrush
    val value = progress?.coerceIn(0f, 1f)
    val shown by animateFloatAsState(value ?: 0f, Motion.spec(tween(PROGRESS_MILLIS)), label = "progress")
    val reduced = Motion.reduced
    val sweep: State<Float>? = if (value == null && !reduced) {
        rememberInfiniteTransition(label = "progressSweep").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(ProgressBarDefaults.INDETERMINATE_MILLIS, easing = LinearEasing)),
            label = "progressSweep",
        )
    } else {
        null
    }
    val label = if (value == null) {
        layoutString("oneconfig.ui.loading", "Loading")
    } else {
        "${(value * 100f).roundToInt()}%"
    }
    Box(
        Modifier
            .then(modifier)
            .fillMaxWidth()
            .height(ProgressBarDefaults.thickness(size))
            .a11y(label, A11yRole.ProgressBar, null, null, shape, false)
            .semantics {
                progressBarRangeInfo = if (value == null) ProgressBarRangeInfo.Indeterminate else ProgressBarRangeInfo(value, 0f..1f)
            }
            .clip(shape)
            .drawBehind {
                val bar = this.size
                val corner = if (rounded) CornerRadius(bar.height / 2f) else CornerRadius.Zero
                drawRoundRect(track, cornerRadius = corner)
                when {
                    value != null -> if (shown > 0f) {
                        drawRoundRect(brush, size = Size(bar.width * shown, bar.height), cornerRadius = corner)
                    }
                    sweep == null -> drawRoundRect(brush, cornerRadius = corner, alpha = REDUCED_FILL_ALPHA)
                    else -> {
                        val segment = bar.width * SEGMENT_FRACTION
                        val start = sweep.value * (bar.width + segment) - segment
                        drawRoundRect(brush, Offset(start, 0f), Size(segment, bar.height), corner)
                    }
                }
            },
    )
}
