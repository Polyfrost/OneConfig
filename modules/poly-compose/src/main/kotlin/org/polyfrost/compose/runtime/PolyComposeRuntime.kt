package org.polyfrost.compose.runtime

import androidx.compose.runtime.*
import org.polyfrost.compose.layout.PolyLayoutEngine
import org.polyfrost.compose.node.RootNode

class PolyComposeRuntime(private val host: PolyComposeClock = PolyComposeHost.huds) {
    val root = RootNode()

    private val composition = Composition(PolyApplier(root), host.recomposer)

    fun setContent(content: @Composable () -> Unit) = composition.setContent(content)

    fun frame(parentWidth: Float, parentHeight: Float, nanos: Long = System.nanoTime()) {
        host.frame(nanos)
        layout(parentWidth, parentHeight)
    }

    fun layout(parentWidth: Float, parentHeight: Float) {
        PolyLayoutEngine.layout(root, parentWidth, parentHeight)
    }

    fun dispose() {
        composition.dispose()
    }
}
