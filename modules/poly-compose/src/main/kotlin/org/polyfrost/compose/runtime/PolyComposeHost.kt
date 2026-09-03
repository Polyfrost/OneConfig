package org.polyfrost.compose.runtime

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PolyComposeHost {
    private val clock = BroadcastFrameClock()
    private val scope = CoroutineScope(Dispatchers.Unconfined + clock)

    private val recomposerImpl = Recomposer(scope.coroutineContext).also { r ->
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            r.runRecomposeAndApplyChanges()
        }
    }

    internal val recomposer: CompositionContext get() = recomposerImpl

    fun frame(nanos: Long = System.nanoTime()) {
        frameWithReport(nanos)
    }

    /**
     * Runs a frame like [frame] and reports whether composition content may have changed
     */
    fun frameWithReport(nanos: Long = System.nanoTime()): Boolean {
        Snapshot.sendApplyNotifications()
        val appliedBefore = recomposerImpl.changeCount
        clock.sendFrame(nanos)
        return recomposerImpl.changeCount != appliedBefore || recomposerImpl.hasPendingWork
    }
}
