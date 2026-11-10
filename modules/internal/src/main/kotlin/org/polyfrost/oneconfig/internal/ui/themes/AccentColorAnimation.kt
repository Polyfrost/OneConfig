package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

private class ColorChange(val from: Color, val progress: Animatable<Float, AnimationVector1D>)

/**
 * [androidx.compose.animation.animateColorAsState] for colours made from [Accent], which a chroma accent changes every frame. Only a change of
 * [key] animates, from wherever the colour was, and otherwise it follows [targetValue] so it never trails the accent
 */
@Composable
fun animateAccentColorAsState(key: Any?, targetValue: Color, animationSpec: AnimationSpec<Float> = spring()): State<Color> {
    val target = remember { mutableStateOf(targetValue) }
    val shown = remember { arrayOfNulls<State<Color>>(1) }
    val change = remember(key) {
        val before = shown[0]?.let { Snapshot.withoutReadObservation { it.value } }
        if (before == null) ColorChange(targetValue, Animatable(1f)) else ColorChange(before, Animatable(0f))
    }
    target.value = targetValue
    LaunchedEffect(change) { change.progress.animateTo(1f, animationSpec) }
    return remember(change) {
        derivedStateOf { lerp(change.from, target.value, change.progress.value) }
    }.also { shown[0] = it }
}
