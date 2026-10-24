package org.polyfrost.compose.animation

import org.polyfrost.compose.animation.core.AnimationSpec
import org.polyfrost.compose.animation.core.Spring
import org.polyfrost.compose.animation.core.spring
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.plus
import org.polyfrost.compose.ui.unit.IntOffset
import org.polyfrost.compose.ui.unit.IntSize

internal class Fade(val alpha: Float, val spec: AnimationSpec)
internal class Slide(val offset: (IntSize) -> IntOffset, val spec: AnimationSpec)
internal class ChangeSize(val alignment: Alignment, val size: (IntSize) -> IntSize, val clip: Boolean, val spec: AnimationSpec)
internal class Scale(val scale: Float, val spec: AnimationSpec)

internal data class TransitionData(val fade: Fade? = null, val slide: Slide? = null, val changeSize: ChangeSize? = null, val scale: Scale? = null) {
    operator fun plus(o: TransitionData) = TransitionData(fade ?: o.fade, slide ?: o.slide, changeSize ?: o.changeSize, scale ?: o.scale)
}

class EnterTransition internal constructor(internal val data: TransitionData) {
    operator fun plus(enter: EnterTransition) = EnterTransition(data + enter.data)

    companion object {
        val None = EnterTransition(TransitionData())
    }
}

class ExitTransition internal constructor(internal val data: TransitionData) {
    operator fun plus(exit: ExitTransition) = ExitTransition(data + exit.data)

    companion object {
        val None = ExitTransition(TransitionData())
    }
}

private val DefaultSpec = spring(Spring.StiffnessMediumLow, visibilityThreshold = 0.001f)

fun fadeIn(animationSpec: AnimationSpec = DefaultSpec, initialAlpha: Float = 0f) = EnterTransition(TransitionData(fade = Fade(initialAlpha, animationSpec)))
fun fadeOut(animationSpec: AnimationSpec = DefaultSpec, targetAlpha: Float = 0f) = ExitTransition(TransitionData(fade = Fade(targetAlpha, animationSpec)))

fun scaleIn(animationSpec: AnimationSpec = DefaultSpec, initialScale: Float = 0f) = EnterTransition(TransitionData(scale = Scale(initialScale, animationSpec)))
fun scaleOut(animationSpec: AnimationSpec = DefaultSpec, targetScale: Float = 0f) = ExitTransition(TransitionData(scale = Scale(targetScale, animationSpec)))

fun slideIn(animationSpec: AnimationSpec = DefaultSpec, initialOffset: (fullSize: IntSize) -> IntOffset) =
    EnterTransition(TransitionData(slide = Slide(initialOffset, animationSpec)))

fun slideOut(animationSpec: AnimationSpec = DefaultSpec, targetOffset: (fullSize: IntSize) -> IntOffset) =
    ExitTransition(TransitionData(slide = Slide(targetOffset, animationSpec)))

fun slideInHorizontally(animationSpec: AnimationSpec = DefaultSpec, initialOffsetX: (fullWidth: Int) -> Int = { -it / 2 }) =
    slideIn(animationSpec) { IntOffset(initialOffsetX(it.width), 0) }

fun slideInVertically(animationSpec: AnimationSpec = DefaultSpec, initialOffsetY: (fullHeight: Int) -> Int = { -it / 2 }) =
    slideIn(animationSpec) { IntOffset(0, initialOffsetY(it.height)) }

fun slideOutHorizontally(animationSpec: AnimationSpec = DefaultSpec, targetOffsetX: (fullWidth: Int) -> Int = { -it / 2 }) =
    slideOut(animationSpec) { IntOffset(targetOffsetX(it.width), 0) }

fun slideOutVertically(animationSpec: AnimationSpec = DefaultSpec, targetOffsetY: (fullHeight: Int) -> Int = { -it / 2 }) =
    slideOut(animationSpec) { IntOffset(0, targetOffsetY(it.height)) }

fun expandIn(
    animationSpec: AnimationSpec = DefaultSpec, expandFrom: Alignment = Alignment.BottomEnd, clip: Boolean = true,
    initialSize: (fullSize: IntSize) -> IntSize = { IntSize.Zero },
) = EnterTransition(TransitionData(changeSize = ChangeSize(expandFrom, initialSize, clip, animationSpec)))

fun shrinkOut(
    animationSpec: AnimationSpec = DefaultSpec, shrinkTowards: Alignment = Alignment.BottomEnd, clip: Boolean = true,
    targetSize: (fullSize: IntSize) -> IntSize = { IntSize.Zero },
) = ExitTransition(TransitionData(changeSize = ChangeSize(shrinkTowards, targetSize, clip, animationSpec)))

fun expandHorizontally(
    animationSpec: AnimationSpec = DefaultSpec, expandFrom: Alignment.Horizontal = Alignment.End, clip: Boolean = true,
    initialWidth: (fullWidth: Int) -> Int = { 0 },
) = expandIn(animationSpec, expandFrom + Alignment.CenterVertically, clip) { IntSize(initialWidth(it.width), it.height) }

fun expandVertically(
    animationSpec: AnimationSpec = DefaultSpec, expandFrom: Alignment.Vertical = Alignment.Bottom, clip: Boolean = true,
    initialHeight: (fullHeight: Int) -> Int = { 0 },
) = expandIn(animationSpec, expandFrom + Alignment.CenterHorizontally, clip) { IntSize(it.width, initialHeight(it.height)) }

fun shrinkHorizontally(
    animationSpec: AnimationSpec = DefaultSpec, shrinkTowards: Alignment.Horizontal = Alignment.End, clip: Boolean = true,
    targetWidth: (fullWidth: Int) -> Int = { 0 },
) = shrinkOut(animationSpec, shrinkTowards + Alignment.CenterVertically, clip) { IntSize(targetWidth(it.width), it.height) }

fun shrinkVertically(
    animationSpec: AnimationSpec = DefaultSpec, shrinkTowards: Alignment.Vertical = Alignment.Bottom, clip: Boolean = true,
    targetHeight: (fullHeight: Int) -> Int = { 0 },
) = shrinkOut(animationSpec, shrinkTowards + Alignment.CenterHorizontally, clip) { IntSize(it.width, targetHeight(it.height)) }
