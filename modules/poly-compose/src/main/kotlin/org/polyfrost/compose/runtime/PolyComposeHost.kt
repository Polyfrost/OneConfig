package org.polyfrost.compose.runtime

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// a composition only does work on a frame, so one attached to a clock nobody ticks costs nothing.
// that is what lets a composition stay alive while nothing is looking at it
class PolyComposeClock {
    private val clock = BroadcastFrameClock()
    private val scope = CoroutineScope(Dispatchers.Unconfined + clock)

    private val recomposerImpl = Recomposer(scope.coroutineContext).also { r ->
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            r.runRecomposeAndApplyChanges()
        }
    }

    internal val recomposer: CompositionContext get() = recomposerImpl

    /**
     * runs a frame and reports whether the content may have changed
     *
     * a second clock ticked in the same frame must pass false: sendApplyNotifications is global,
     * and firing it again mid frame hands new state to trees already composed against the old.
     */
    fun frame(nanos: Long = System.nanoTime(), notify: Boolean = true): Boolean {
        if (notify) Snapshot.sendApplyNotifications()
        val appliedBefore = recomposerImpl.changeCount
        clock.sendFrame(nanos)
        return recomposerImpl.changeCount != appliedBefore || recomposerImpl.hasPendingWork
    }
}

object PolyComposeHost {
    /** Drives the HUDs actually on screen, ticked every frame */
    val huds = PolyComposeClock()

    // drives HUD previews, ticked only while a UI showing them draws. they are expensive to build
    // so they are kept for the process and simply left alone the rest of the time
    val previews = PolyComposeClock()

    internal val recomposer: CompositionContext get() = huds.recomposer

    fun frame(nanos: Long = System.nanoTime()) {
        frameWithReport(nanos)
    }

    /**
     * Runs a frame like [frame] and reports whether composition content may have changed
     */
    fun frameWithReport(nanos: Long = System.nanoTime()): Boolean = huds.frame(nanos)
}
