package org.polyfrost.compose.animation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember

class InfiniteTransition internal constructor(val label: String)

@Composable
fun rememberInfiniteTransition(label: String = "InfiniteTransition") = remember { InfiniteTransition(label) }

@Composable
fun InfiniteTransition.animateFloat(initialValue: Float, targetValue: Float, animationSpec: InfiniteRepeatableSpec, label: String = "FloatAnimation"): State<Float> {
    val state = remember { mutableFloatStateOf(initialValue) }
    LaunchedEffect(initialValue, targetValue, animationSpec) {
        animate(initialValue, targetValue, 0f, animationSpec) { v, _ -> state.floatValue = v }
    }
    return state
}
