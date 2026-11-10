package org.polyfrost.compose.runtime

import androidx.compose.runtime.*
import org.polyfrost.compose.layout.PolyLayoutEngine
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.node.RootNode
import org.polyfrost.compose.render.FontManager

public class PolyComposeRuntime(private val host: PolyComposeClock = PolyComposeHost.huds) {
    public val root: RootNode = RootNode()

    private val applier = PolyApplier(root)
    private val composition = Composition(applier, host.recomposer)

    public var revision: Long = 0L
        private set

    public fun setContent(content: @Composable () -> Unit) {
        laidOutChanges = -1L
        composition.setContent(content)
    }

    public fun frame(parentWidth: Float, parentHeight: Float, nanos: Long = System.nanoTime()) {
        host.frame(nanos)
        layout(parentWidth, parentHeight)
    }

    @Volatile private var laidOutChanges = -1L
    private var laidOutW = Float.NaN
    private var laidOutH = Float.NaN
    private var laidOutFonts = -1

    @Volatile private var contentStale = false

    private val changes: Long get() = applier.changes

    public fun layout(parentWidth: Float, parentHeight: Float) {
        revision++
        contentStale = false
        laidOutChanges = changes
        laidOutW = parentWidth
        laidOutH = parentHeight
        laidOutFonts = FontManager.revision
        PolyLayoutEngine.layout(root, parentWidth, parentHeight)
    }

    public fun layoutIfChanged(parentWidth: Float, parentHeight: Float): Boolean {
        if (laidOutChanges == changes && laidOutW == parentWidth && laidOutH == parentHeight &&
            laidOutFonts == FontManager.revision
        ) {
            if (!contentStale) return false
            contentStale = false
            if (remeasureContent(root)) layout(parentWidth, parentHeight) else revision++
            return true
        }
        layout(parentWidth, parentHeight)
        return true
    }

    public fun contentChanged() {
        contentStale = true
    }

    private fun remeasureContent(node: PolyNode): Boolean {
        val w = node.style.width
        val h = node.style.height
        node.measureContent()
        var changed = node.style.width != w || node.style.height != h
        for (child in node.children) if (remeasureContent(child)) changed = true
        return changed
    }

    public fun invalidateLayout() {
        laidOutChanges = -1L
    }

    public fun dispose() {
        composition.dispose()
    }
}

