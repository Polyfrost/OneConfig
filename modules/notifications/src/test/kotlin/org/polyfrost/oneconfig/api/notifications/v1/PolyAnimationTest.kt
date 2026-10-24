package org.polyfrost.oneconfig.api.notifications.v1

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.polyfrost.compose.animation.AnimatedVisibility
import org.polyfrost.compose.animation.Crossfade
import org.polyfrost.compose.animation.core.Animatable
import org.polyfrost.compose.animation.core.FastOutSlowInEasing
import org.polyfrost.compose.animation.core.LinearEasing
import org.polyfrost.compose.animation.core.RepeatMode
import org.polyfrost.compose.animation.core.animateFloat
import org.polyfrost.compose.animation.core.infiniteRepeatable
import org.polyfrost.compose.animation.core.rememberInfiniteTransition
import org.polyfrost.compose.animation.core.tween
import org.polyfrost.compose.animation.expandVertically
import org.polyfrost.compose.animation.fadeIn
import org.polyfrost.compose.animation.fadeOut
import org.polyfrost.compose.animation.shrinkVertically
import org.polyfrost.compose.animation.slideInHorizontally
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.runtime.PolyComposeClock
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.unit.dp

class PolyAnimationTest {
    private class Driver {
        val clock = PolyComposeClock()
        val runtime = PolyComposeRuntime(clock)
        var nanos = 0L

        fun frames(ms: Int, step: Int = 10) {
            repeat(ms / step) {
                nanos += step * 1_000_000L
                runtime.frame(200f, 200f, nanos)
            }
        }
    }

    @Test
    fun easingsHitTheirEnds() {
        assertEquals(0f, FastOutSlowInEasing.transform(0f))
        assertEquals(1f, FastOutSlowInEasing.transform(1f))
        assertTrue(FastOutSlowInEasing.transform(0.5f) > 0.7f)
        var last = 0f
        for (i in 1..20) {
            val v = FastOutSlowInEasing.transform(i / 20f)
            assertTrue(v >= last)
            last = v
        }
    }

    @Test
    fun animatedVisibilityExpandsAndRemoves() {
        val d = Driver()
        val visible = mutableStateOf(false)
        d.runtime.setContent {
            Column {
                AnimatedVisibility(visible.value, enter = expandVertically(tween(100, easing = LinearEasing)), exit = shrinkVertically(tween(100, easing = LinearEasing))) {
                    Box(Modifier.size(10.dp, 40.dp))
                }
            }
        }
        d.frames(20)
        val column = d.runtime.root.children.single()
        assertEquals(0, column.children.size)

        visible.value = true
        d.frames(60)
        val mid = column.height
        assertTrue(mid > 0f && mid < 40f, "mid $mid")
        assertTrue(column.children.single().style.clipToBounds)
        d.frames(100)
        assertEquals(40f, column.height)
        assertTrue(!column.children.single().style.clipToBounds)

        visible.value = false
        d.frames(60)
        assertTrue(column.height in 1f..39f, "shrinking ${column.height}")
        d.frames(100)
        assertEquals(0, column.children.size)
        assertEquals(0f, column.height)
    }

    @Test
    fun enterEffectsCombine() {
        val d = Driver()
        val visible = mutableStateOf(false)
        d.runtime.setContent {
            AnimatedVisibility(visible.value, enter = fadeIn(tween(100, easing = LinearEasing)) + slideInHorizontally(tween(100, easing = LinearEasing)) { -it }) {
                Box(Modifier.size(50.dp, 10.dp))
            }
        }
        d.frames(20)
        visible.value = true
        d.frames(60)
        val node = d.runtime.root.children.single()
        assertTrue(node.style.alpha > 0f && node.style.alpha < 1f, "alpha ${node.style.alpha}")
        assertTrue(node.style.translateX < 0f && node.style.translateX > -50f, "slide ${node.style.translateX}")
        d.frames(100)
        assertEquals(1f, node.style.alpha)
        assertEquals(0f, node.style.translateX)
    }

    @Test
    fun crossfadeKeepsBothWhileSwapping() {
        val d = Driver()
        val page = mutableStateOf(1)
        d.runtime.setContent {
            Crossfade(page.value, animationSpec = tween(100, easing = LinearEasing)) { Box(Modifier.size((it * 10).dp, 10.dp)) }
        }
        d.frames(20)
        val box = d.runtime.root.children.single()
        assertEquals(1, box.children.size)
        page.value = 2
        d.frames(50)
        assertEquals(2, box.children.size)
        val (old, new) = box.children
        assertTrue(old.style.alpha < 1f && new.style.alpha < 1f)
        assertEquals(20f, new.children.single().width)
        d.frames(150)
        assertEquals(1, box.children.size)
        assertEquals(20f, box.width)
    }

    @Test
    fun infiniteTransitionReverses() {
        val d = Driver()
        val seen = ArrayList<Float>()
        d.runtime.setContent {
            val t = rememberInfiniteTransition()
            val v = t.animateFloat(0f, 1f, infiniteRepeatable(tween(100, easing = LinearEasing), RepeatMode.Reverse))
            seen += v.value
        }
        d.frames(300)
        assertTrue(seen.max() > 0.9f && seen.drop(seen.indexOfFirst { it > 0.9f }).any { it < 0.5f }, seen.toString())
    }

    @Test
    fun animatableRetargetsAndSnaps() {
        val d = Driver()
        val a = Animatable(0f)
        val target = mutableStateOf(10f)
        d.runtime.setContent {
            LaunchedEffect(target.value) { a.animateTo(target.value, tween(100, easing = LinearEasing)) }
        }
        d.frames(60)
        assertTrue(a.isRunning && a.value > 0f && a.value < 10f)
        target.value = -10f
        d.frames(200)
        assertEquals(-10f, a.value)
        assertTrue(!a.isRunning)
    }
}
