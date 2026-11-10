@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.a11y

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.unit.IntOffset
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

@ExperimentalOneConfigUiApi
public object Motion {
    private const val REDUCED_FADE_MILLIS = 150

    @JvmStatic
    public val reduced: Boolean
        get() = runCatching { componentsBridge().reducedMotion() }.getOrDefault(false)

    @JvmStatic
    public fun <T> spec(spec: AnimationSpec<T>): AnimationSpec<T> = if (reduced) snap() else spec

    @JvmStatic
    public fun <T> finiteSpec(spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> = if (reduced) snap() else spec

    @JvmStatic
    public fun enter(enter: EnterTransition): EnterTransition = if (reduced) fadeIn(tween(REDUCED_FADE_MILLIS)) else enter

    @JvmStatic
    public fun exit(exit: ExitTransition): ExitTransition = if (reduced) fadeOut(tween(REDUCED_FADE_MILLIS)) else exit

    @JvmStatic
    public fun itemPlacementSpec(): FiniteAnimationSpec<IntOffset>? =
        if (reduced) null else spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold)
}
