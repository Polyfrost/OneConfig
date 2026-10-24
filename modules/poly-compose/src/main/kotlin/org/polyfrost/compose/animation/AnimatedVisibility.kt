package org.polyfrost.compose.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.polyfrost.compose.animation.core.AnimationSpec
import org.polyfrost.compose.animation.core.Animatable
import org.polyfrost.compose.animation.core.tween
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.ColumnScope
import org.polyfrost.compose.foundation.layout.RowScope
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.layoutElement
import org.polyfrost.compose.ui.unit.IntSize
import kotlin.math.roundToInt

interface AnimatedVisibilityScope

interface AnimatedContentScope : AnimatedVisibilityScope

private object AnimatedContentScopeInstance : AnimatedContentScope

internal class EnterExitState(shown: Boolean) {
    private val initial = if (shown) 1f else 0f
    val fade = Animatable(initial)
    val slide = Animatable(initial)
    val size = Animatable(initial)
    val scale = Animatable(initial)

    var exited by mutableStateOf(!shown)

    suspend fun run(visible: Boolean, enter: EnterTransition, exit: ExitTransition) {
        if (visible) exited = false
        val data = if (visible) enter.data else exit.data
        val target = if (visible) 1f else 0f
        coroutineScope {
            fun go(anim: Animatable, spec: AnimationSpec?) {
                if (spec == null) launch { anim.snapTo(target) }
                else if (anim.value != target || anim.isRunning) launch { anim.animateTo(target, spec) }
            }
            go(fade, data.fade?.spec)
            go(slide, data.slide?.spec)
            go(size, data.changeSize?.spec)
            go(scale, data.scale?.spec)
        }
        if (!visible) exited = true
    }
}

@Composable
internal fun EnterExitBox(state: EnterExitState, visible: Boolean, enter: EnterTransition, exit: ExitTransition, modifier: Modifier, content: @Composable () -> Unit) {
    val data = if (visible) enter.data else exit.data
    val alpha = data.fade?.let { it.alpha + (1f - it.alpha) * state.fade.value } ?: 1f
    val scale = data.scale?.let { it.scale + (1f - it.scale) * state.scale.value } ?: 1f
    val slide = data.slide?.takeIf { state.slide.value < 1f }
    val slideLeft = 1f - state.slide.value
    val change = data.changeSize?.takeIf { state.size.value < 1f }
    val sizeT = state.size.value
    Box(
        modifier.layoutElement {
            this.alpha *= alpha
            scaleX *= scale
            scaleY *= scale
            if (change?.clip == true) clipToBounds = true
            if (slide != null || change != null) {
                val tx = translateX
                val ty = translateY
                onMeasured = { node ->
                    val full = IntSize(node.width.roundToInt(), node.height.roundToInt())
                    if (slide != null) {
                        val o = slide.offset(full)
                        node.style.translateX = tx + o.x * slideLeft
                        node.style.translateY = ty + o.y * slideLeft
                    }
                    if (change != null) {
                        val s = change.size(full)
                        node.width = s.width + (node.width - s.width) * sizeT
                        node.height = s.height + (node.height - s.height) * sizeT
                    }
                }
            }
        },
        contentAlignment = change?.alignment ?: Alignment.TopStart,
    ) { content() }
}

@Composable
fun AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandIn(),
    exit: ExitTransition = shrinkOut() + fadeOut(),
    label: String = "AnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) = AnimatedVisibilityImpl(visible, modifier, enter, exit, content)

@Composable
private fun AnimatedVisibilityImpl(visible: Boolean, modifier: Modifier, enter: EnterTransition, exit: ExitTransition, content: @Composable AnimatedVisibilityScope.() -> Unit) {
    val state = remember { EnterExitState(visible) }
    LaunchedEffect(visible) { state.run(visible, enter, exit) }
    if (visible || !state.exited) EnterExitBox(state, visible, enter, exit, modifier) { AnimatedContentScopeInstance.content() }
}

@Composable
fun ColumnScope.AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandVertically(),
    exit: ExitTransition = fadeOut() + shrinkVertically(),
    label: String = "AnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) = AnimatedVisibilityImpl(visible, modifier, enter, exit, content)

@Composable
fun RowScope.AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = fadeIn() + expandHorizontally(),
    exit: ExitTransition = fadeOut() + shrinkHorizontally(),
    label: String = "AnimatedVisibility",
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) = AnimatedVisibilityImpl(visible, modifier, enter, exit, content)

class ContentTransform(val targetContentEnter: EnterTransition, val initialContentExit: ExitTransition)

infix fun EnterTransition.togetherWith(exit: ExitTransition) = ContentTransform(this, exit)

class AnimatedContentTransitionScope<S> internal constructor(val initialState: S, val targetState: S)

private class ContentEntry<S>(val state: S, shown: Boolean) {
    val anim = EnterExitState(shown)
    var enter = EnterTransition.None
    var exit = ExitTransition.None
}

@Composable
fun <S> AnimatedContent(
    targetState: S,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        (fadeIn(tween(220, delayMillis = 90)) + scaleIn(tween(220, delayMillis = 90), initialScale = 0.92f)) togetherWith fadeOut(tween(90))
    },
    contentAlignment: Alignment = Alignment.TopStart,
    label: String = "AnimatedContent",
    content: @Composable AnimatedContentScope.(targetState: S) -> Unit,
) {
    val entries = remember { mutableStateListOf(ContentEntry(targetState, true)) }
    val last = remember { arrayOf<Any?>(targetState) }
    @Suppress("UNCHECKED_CAST")
    val previous = last[0] as S
    if (previous != targetState) {
        val transform = AnimatedContentTransitionScope(previous, targetState).transitionSpec()
        entries.firstOrNull { it.state == previous }?.exit = transform.initialContentExit
        val entry = entries.firstOrNull { it.state == targetState } ?: ContentEntry(targetState, false)
        entry.enter = transform.targetContentEnter
        entries.remove(entry)
        entries.add(entry)
        last[0] = targetState
    }
    Box(modifier, contentAlignment = contentAlignment) {
        for (entry in entries.toList()) key(entry.state) {
            val visible = entry.state == targetState
            LaunchedEffect(visible) {
                entry.anim.run(visible, entry.enter, entry.exit)
                if (!visible) entries.remove(entry)
            }
            EnterExitBox(entry.anim, visible, entry.enter, entry.exit, Modifier) { AnimatedContentScopeInstance.content(entry.state) }
        }
    }
}

@Composable
fun <T> Crossfade(targetState: T, modifier: Modifier = Modifier, animationSpec: AnimationSpec = tween(), label: String = "Crossfade", content: @Composable (T) -> Unit) =
    AnimatedContent(targetState, modifier, { fadeIn(animationSpec) togetherWith fadeOut(animationSpec) }, label = label) { content(it) }
