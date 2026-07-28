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

    internal val recomposer: CompositionContext = Recomposer(scope.coroutineContext).also { r ->
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            r.runRecomposeAndApplyChanges()
        }
    }

    fun frame(nanos: Long = System.nanoTime()) {
        Snapshot.sendApplyNotifications()
        clock.sendFrame(nanos)
    }
}
