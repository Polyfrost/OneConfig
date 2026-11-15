@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.a11y

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.OneConfigPrefs

@ExperimentalOneConfigUiApi
public object Motion {
    public const val fast: Int = 120

    public const val standard: Int = 150

    @JvmStatic
    public val reduced: Boolean
        get() = OneConfigPrefs.reducedMotion

    @JvmStatic
    public fun <T> spec(spec: AnimationSpec<T>): AnimationSpec<T> = if (reduced) snap() else spec

    @JvmStatic
    public fun <T> finiteSpec(spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> = if (reduced) snap() else spec

    @JvmStatic
    public fun enter(enter: EnterTransition): EnterTransition = if (reduced) fadeIn(tween(standard)) else enter

    @JvmStatic
    public fun exit(exit: ExitTransition): ExitTransition = if (reduced) fadeOut(tween(standard)) else exit

    @JvmStatic
    public fun <T> tweenSpec(durationMillis: Int = standard): FiniteAnimationSpec<T> = finiteSpec(tween(durationMillis))

    @JvmStatic
    public fun colorSpec(durationMillis: Int = standard): FiniteAnimationSpec<Color> = tweenSpec(durationMillis)

    @JvmStatic
    public fun itemPlacementSpec(): FiniteAnimationSpec<IntOffset>? =
        if (reduced) null else spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold)

    @Composable
    public fun animateColorAsState(target: Color, spec: AnimationSpec<Color> = tween(standard), label: String = "ColorAnimation"): State<Color> =
        androidx.compose.animation.animateColorAsState(target, spec(spec), label)

    @Composable
    public fun animateFloatAsState(target: Float, spec: AnimationSpec<Float> = tween(standard), label: String = "FloatAnimation"): State<Float> =
        androidx.compose.animation.core.animateFloatAsState(target, spec(spec), label = label)

    @Composable
    public fun animateDpAsState(target: Dp, spec: AnimationSpec<Dp> = tween(standard), label: String = "DpAnimation"): State<Dp> =
        androidx.compose.animation.core.animateDpAsState(target, spec(spec), label = label)

    @Composable
    public fun animateInfiniteFloat(
        initial: Float,
        target: Float,
        spec: InfiniteRepeatableSpec<Float>,
        label: String = "InfiniteAnimation",
    ): State<Float> {
        if (reduced) return rememberUpdatedState(initial)
        return rememberInfiniteTransition(label).animateFloat(initial, target, spec, label)
    }
}
