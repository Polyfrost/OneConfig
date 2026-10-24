package org.polyfrost.compose.runtime

import androidx.compose.runtime.*
import org.polyfrost.compose.layout.PolyLayoutEngine
import org.polyfrost.compose.node.RootNode
import org.polyfrost.compose.render.FontManager

class PolyComposeRuntime(private val host: PolyComposeClock = PolyComposeHost.huds) {
    val root = RootNode()

    private val composition = Composition(PolyApplier(root), host.recomposer)

    fun setContent(content: @Composable () -> Unit) {
        laidOutChanges = -1L
        composition.setContent(content)
    }

    fun frame(parentWidth: Float, parentHeight: Float, nanos: Long = System.nanoTime()) {
        host.frame(nanos)
        layout(parentWidth, parentHeight)
    }

    fun update(parentWidth: Float, parentHeight: Float, nanos: Long = System.nanoTime()): Boolean {
        val changed = host.frame(nanos)
        return layoutIfChanged(parentWidth, parentHeight) || changed
    }

    var pixelSnap = 0f

    private var laidOutChanges = -1L
    private var laidOutW = Float.NaN
    private var laidOutH = Float.NaN
    private var laidOutSnap = Float.NaN
    private var laidOutFonts = -1

    fun layout(parentWidth: Float, parentHeight: Float) {
        laidOutChanges = host.changeCount
        laidOutW = parentWidth
        laidOutH = parentHeight
        laidOutSnap = pixelSnap
        laidOutFonts = FontManager.revision
        PolyLayoutEngine.layout(root, parentWidth, parentHeight, pixelSnap)
    }

    fun layoutIfChanged(parentWidth: Float, parentHeight: Float): Boolean {
        if (laidOutChanges == host.changeCount && laidOutW == parentWidth && laidOutH == parentHeight &&
            laidOutSnap == pixelSnap && laidOutFonts == FontManager.revision
        ) return false
        layout(parentWidth, parentHeight)
        return true
    }

    fun dispose() {
        composition.dispose()
    }
}
