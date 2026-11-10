package org.polyfrost.compose.runtime

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

public class PolyComposeClock {
    private val clock = BroadcastFrameClock()
    private val scope = CoroutineScope(Dispatchers.Unconfined + clock)

    private val recomposerImpl = Recomposer(scope.coroutineContext).also { r ->
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            r.runRecomposeAndApplyChanges()
        }
    }

    internal val recomposer: CompositionContext get() = recomposerImpl

    private val inFrame = AtomicBoolean(false)

    public fun frame(nanos: Long = System.nanoTime(), notify: Boolean = true): Boolean {
        if (!inFrame.compareAndSet(false, true)) return false
        try {
            if (notify) Snapshot.sendApplyNotifications()
            val appliedBefore = recomposerImpl.changeCount
            clock.sendFrame(nanos)
            appliedChange = recomposerImpl.changeCount != appliedBefore
            return appliedChange
        } finally {
            inFrame.set(false)
        }
    }

    public var appliedChange: Boolean = false
        private set

    public val changeCount: Long get() = recomposerImpl.changeCount
}

public object PolyComposeHost {
    public val huds: PolyComposeClock = PolyComposeClock()

    public val previews: PolyComposeClock = PolyComposeClock()

    public val notifications: PolyComposeClock = PolyComposeClock()

    internal val recomposer: CompositionContext get() = huds.recomposer

    public fun frame(nanos: Long = System.nanoTime()) {
        frameWithReport(nanos)
    }

    /**
     * Runs a frame like [frame] and reports whether composition content may have changed
     */
    public fun frameWithReport(nanos: Long = System.nanoTime()): Boolean = huds.frame(nanos)
}
