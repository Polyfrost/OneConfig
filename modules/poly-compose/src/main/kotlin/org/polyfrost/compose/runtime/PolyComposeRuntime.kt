package org.polyfrost.compose.runtime

import androidx.compose.runtime.*
import org.polyfrost.compose.layout.PolyLayoutEngine
import org.polyfrost.compose.node.RootNode

class PolyComposeRuntime {
    val root = RootNode()

    private val composition = Composition(PolyApplier(root), PolyComposeHost.recomposer)

    fun setContent(content: @Composable () -> Unit) = composition.setContent(content)

    fun frame(parentWidth: Float, parentHeight: Float, nanos: Long = System.nanoTime()) {
        PolyComposeHost.frame(nanos)
        layout(parentWidth, parentHeight)
    }

    fun layout(parentWidth: Float, parentHeight: Float) {
        PolyLayoutEngine.layout(root, parentWidth, parentHeight)
    }

    fun dispose() {
        composition.dispose()
    }
}
