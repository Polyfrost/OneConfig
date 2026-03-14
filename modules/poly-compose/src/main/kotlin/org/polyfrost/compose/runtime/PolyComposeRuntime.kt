package org.polyfrost.compose.runtime

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import org.polyfrost.compose.layout.PolyLayoutEngine
import org.polyfrost.compose.node.RootNode
import androidx.compose.runtime.snapshots.Snapshot

class PolyComposeRuntime {
    val root = RootNode()

    private val clock = BroadcastFrameClock()
    private val scope = CoroutineScope(Dispatchers.Unconfined + clock)
    private val recomposer = Recomposer(scope.coroutineContext)
    private val composition = Composition(PolyApplier(root), recomposer)

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }
    }

    fun setContent(content: @Composable () -> Unit) = composition.setContent(content)

    fun frame(parentWidth: Float, parentHeight: Float, nanos: Long = System.nanoTime()) {
        Snapshot.sendApplyNotifications()
        scope.launch(start = CoroutineStart.UNDISPATCHED) { clock.sendFrame(nanos) }
        PolyLayoutEngine.layout(root, parentWidth, parentHeight)
    }

    fun dispose() {
        composition.dispose()
        recomposer.cancel()
        scope.cancel()
    }
}
