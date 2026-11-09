package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ReducedMotion {
    private const val FADE_MILLIS = 150

    @JvmStatic
    var enabled by mutableStateOf(false)

    fun <T> spec(spec: AnimationSpec<T>): AnimationSpec<T> = if (enabled) snap() else spec

    fun <T> finiteSpec(spec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> = if (enabled) snap() else spec

    fun enter(enter: EnterTransition): EnterTransition = if (enabled) fadeIn(tween(FADE_MILLIS)) else enter

    fun exit(exit: ExitTransition): ExitTransition = if (enabled) fadeOut(tween(FADE_MILLIS)) else exit
}
