package org.polyfrost.compose.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import org.polyfrost.compose.animation.core.InfiniteRepeatableSpec
import org.polyfrost.compose.animation.core.InfiniteTransition
import org.polyfrost.compose.animation.core.animateFloat
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.ui.graphics.lerp

@Composable
fun InfiniteTransition.animateColor(initialValue: PolyColor, targetValue: PolyColor, animationSpec: InfiniteRepeatableSpec, label: String = "ColorAnimation"): State<PolyColor> {
    val t = animateFloat(0f, 1f, animationSpec, label)
    return remember(t, initialValue, targetValue) { derivedStateOf { lerp(initialValue, targetValue, t.value) } }
}
