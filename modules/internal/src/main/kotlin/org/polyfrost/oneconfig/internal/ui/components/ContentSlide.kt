package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.internal.ThemeConfig

/**
 * The slide and fade between a content area's pages, categories or tabs: a fifth of its width, from the right going
 * [forward] and the left going back, over the page animation duration, fading only under reduced motion
 */
object ContentSlide {
    private const val SHIFT_DIVISOR = 5

    fun millis(): Int = (ThemeConfig.pageAnimationDuration * 1000f).toInt().coerceAtLeast(1)

    fun enter(forward: Boolean): EnterTransition {
        val fade = fadeIn(tween(millis()))
        return if (Motion.reduced) fade
        else slideInHorizontally(tween(millis())) { if (forward) it / SHIFT_DIVISOR else -it / SHIFT_DIVISOR } + fade
    }

    fun exit(forward: Boolean): ExitTransition {
        val fade = fadeOut(tween(millis()))
        return if (Motion.reduced) fade
        else slideOutHorizontally(tween(millis())) { if (forward) -it / SHIFT_DIVISOR else it / SHIFT_DIVISOR } + fade
    }

    /** A switch between tabs, instant when "Show Page Animations" is off */
    fun tabSwitch(forward: Boolean): ContentTransform =
        if (ThemeConfig.showPageAnimations) enter(forward) togetherWith exit(forward)
        else EnterTransition.None togetherWith ExitTransition.None

    /** How a hand-driven tab switch runs its progress, so it matches [tabSwitch] */
    fun tabProgressSpec(animate: Boolean): FiniteAnimationSpec<Float> =
        if (animate && ThemeConfig.showPageAnimations) tween(millis()) else snap()

    /**
     * Moves content by hand the way [enter] and [exit] would, for when only part of it should move
     *
     * [progress] runs from 0 to 1 as it arrives and from 1 to 0 as it leaves, [direction] is 1 going forward, -1 going
     * back and 0 for no slide. [fade] can be left to a parent that fades the whole of the leaving content
     */
    fun Modifier.tabSlide(progress: () -> Float, leaving: Boolean, direction: Int, fade: Boolean = true): Modifier = graphicsLayer {
        val p = progress()
        if (fade) alpha = p
        if (!Motion.reduced && direction != 0) {
            val shift = size.width / SHIFT_DIVISOR * (1f - p) * direction
            translationX = if (leaving) -shift else shift
        }
    }
}
