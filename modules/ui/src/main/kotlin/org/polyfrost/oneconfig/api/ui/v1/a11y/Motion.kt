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
object Motion {
    private const val REDUCED_FADE_MILLIS = 150

    @JvmStatic
    val reduced: Boolean
        get() = runCatching { componentsBridge().reducedMotion() }.getOrDefault(false)

    @JvmStatic
    fun <T> spec(spec: AnimationSpec<T>): AnimationSpec<T> = if (reduced) snap() else spec

    @JvmStatic
    fun <T> finiteSpec(spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> = if (reduced) snap() else spec

    @JvmStatic
    fun enter(enter: EnterTransition): EnterTransition = if (reduced) fadeIn(tween(REDUCED_FADE_MILLIS)) else enter

    @JvmStatic
    fun exit(exit: ExitTransition): ExitTransition = if (reduced) fadeOut(tween(REDUCED_FADE_MILLIS)) else exit

    @JvmStatic
    fun itemPlacementSpec(): FiniteAnimationSpec<IntOffset>? =
        if (reduced) null else spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold)
}
