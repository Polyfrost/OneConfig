package org.polyfrost.compose.runtime

import androidx.compose.runtime.*
import org.polyfrost.compose.layout.PolyLayoutEngine
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.node.RootNode
import org.polyfrost.compose.node.requestLayout
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
        var changed = host.frame(nanos)
        if (stepScrolls(root, nanos)) {
            layout(parentWidth, parentHeight)
            changed = true
        } else if (layoutIfChanged(parentWidth, parentHeight)) changed = true
        return changed
    }

    private fun stepScrolls(node: PolyNode, nanos: Long): Boolean {
        var moving = node.style.scroll?.let { s -> s.step(nanos); s.value != s.laidOutValue || s.animating } ?: false
        for (c in node.children) if (stepScrolls(c, nanos)) moving = true
        return moving
    }

    var pixelSnap = 0f

    private var laidOutChanges = -1L
    private var laidOutW = Float.NaN
    private var laidOutH = Float.NaN
    private var laidOutSnap = Float.NaN
    private var laidOutFonts = -1

    fun layout(parentWidth: Float, parentHeight: Float) {
        root.layoutRequested = false
        laidOutChanges = host.changeCount
        laidOutW = parentWidth
        laidOutH = parentHeight
        laidOutSnap = pixelSnap
        laidOutFonts = FontManager.revision
        PolyLayoutEngine.layout(root, parentWidth, parentHeight, pixelSnap)
    }

    fun layoutIfChanged(parentWidth: Float, parentHeight: Float): Boolean {
        if (!root.layoutRequested && laidOutChanges == host.changeCount && laidOutW == parentWidth && laidOutH == parentHeight &&
            laidOutSnap == pixelSnap && laidOutFonts == FontManager.revision
        ) return false
        layout(parentWidth, parentHeight)
        return true
    }

    fun dispose() {
        composition.dispose()
    }
}
