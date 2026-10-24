package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.platform.PlatformGraphicsContext
import androidx.compose.ui.graphics.platform.PlatformGraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import org.polyfrost.compose.backend.PolyCanvas
import org.polyfrost.compose.backend.RecordingCanvas
import org.polyfrost.compose.render.CapturingRenderer
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.ui.graphics.shadow.Shadow
import org.polyfrost.compose.ui.unit.Dp

@Suppress("UNUSED_PARAMETER")
class SkiaGraphicsContext(measureDrawBounds: Boolean = false, snapshotCache: Boolean = true) : PlatformGraphicsContext() {
    override fun createPlatformGraphicsLayer(): PlatformGraphicsLayer = PolyGraphicsLayer()
}

class PolyGraphicsLayer : PlatformGraphicsLayer {
    override var compositingStrategy = CompositingStrategy.Auto
    override var pivotOffset = Offset.Unspecified
    override var alpha = 1f
    override var scaleX = 1f
    override var scaleY = 1f
    override var translationX = 0f
    override var translationY = 0f
    override var shadowElevation = 0f
    override var ambientShadowColor = Color.Black
    override var spotShadowColor = Color.Black
    override var blendMode = BlendMode.SrcOver
    override var colorFilter: ColorFilter? = null
    override var rotationX = 0f
    override var rotationY = 0f
    override var rotationZ = 0f
    override var cameraDistance = 8f
    override var renderEffect: RenderEffect? = null

    private var left = 0f
    private var top = 0f
    private var size = IntSize.Zero
    private var outline: Outline? = null
    private var clip = false
    private val recording = RecordingCanvas()
    private val drawScope = CanvasDrawScope()

    override fun setBounds(topLeft: IntOffset, size: IntSize) {
        left = topLeft.x.toFloat()
        top = topLeft.y.toFloat()
        this.size = size
    }

    override fun setOutline(outline: Outline?, clip: Boolean) {
        this.outline = outline
        this.clip = clip
    }

    private var version = 0
    private var cache: LayerCache? = null

    private class LayerCache(val version: Int, val props: List<Any?>, val key: Any, val segs: List<Any>)
    private class ChildRef(val layer: PolyGraphicsLayer, val state: Any)
    private class Capture(var mark: Any) { val segs = ArrayList<Any>() }

    override fun record(density: Density, layoutDirection: LayoutDirection, layer: GraphicsLayer, block: DrawScope.() -> Unit) {
        version++
        recording.ops.clear()
        recording.dynamic = false
        drawScope.draw(density, layoutDirection, recording, size.toSize(), block)
    }

    override fun draw(canvas: Canvas) {
        if (canvas is RecordingCanvas) { canvas.polyOp(::drawNow); return }
        if (canvas is PolyCanvas) drawNow(canvas)
    }

    private fun drawNow(c: PolyCanvas) {
        val cr = c.renderer as? CapturingRenderer
        if (cr == null) return drawLive(c)
        val parent = capturing.lastOrNull()
        if (parent != null) {
            parent.segs += cr.since(parent.mark)
            parent.segs += ChildRef(this, cr.saveState())
        }
        try {
            drawCached(c, cr)
        } finally {
            if (parent != null) parent.mark = cr.mark()
        }
    }

    private fun drawCached(c: PolyCanvas, cr: CapturingRenderer) {
        if (recording.dynamic) {
            cache = null
            capturing += null
            try { drawLive(c) } finally { capturing.removeLast() }
            return
        }
        val key = cr.stateKey()
        val props = props()
        val hit = cache
        if (hit != null && hit.version == version && hit.key == key && hit.props == props && hit.segs.all { it is ChildRef || cr.valid(it) }) {
            capturing += null
            try {
                for (seg in hit.segs) {
                    if (seg !is ChildRef) { cr.append(seg); continue }
                    val saved = cr.saveState()
                    cr.restoreState(seg.state)
                    try { seg.layer.drawNow(c) } finally { cr.restoreState(saved) }
                }
            } finally {
                capturing.removeLast()
            }
            return
        }
        val cap = Capture(cr.mark())
        capturing += cap
        try { drawLive(c) } finally { capturing.removeLast() }
        cap.segs += cr.since(cap.mark)
        cache = LayerCache(version, props, key, cap.segs)
    }

    private fun props(): List<Any?> = listOf(alpha, scaleX, scaleY, translationX, translationY, left, top, size, outline, clip, shadowElevation, spotShadowColor, pivotOffset)

    private fun drawLive(c: PolyCanvas) {
        if (alpha <= 0f) return
        val r = c.renderer
        if (alpha < 1f) r.saveLayerAlpha(alpha) else r.save()
        r.translate(left + translationX, top + translationY)
        if (scaleX != 1f || scaleY != 1f) {
            val p = if (pivotOffset.isUnspecified) Offset(size.width / 2f, size.height / 2f) else pivotOffset
            r.translate(p.x, p.y)
            r.scale(scaleX, scaleY)
            r.translate(-p.x, -p.y)
        }
        val o = outline
        if (shadowElevation > 0f && o != null) {
            val b = o.bounds
            r.shadow(b.left, b.top + shadowElevation / 2f, b.width, b.height, radiusOf(o), Shadow(Dp(shadowElevation), PolyColor(spotShadowColor.copy(alpha = spotShadowColor.alpha * 0.25f).toArgb())))
        }
        if (clip) {
            if (o == null) r.clipRect(0f, 0f, size.width.toFloat(), size.height.toFloat())
            else { val b = o.bounds; r.clipRRect(b.left, b.top, b.width, b.height, radiusOf(o)) }
        }
        recording.replay(c)
        r.restore()
    }

    private fun radiusOf(o: Outline) = (o as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: 0f

    override fun discardDisplayList() {
        version++
        cache = null
        recording.ops.clear()
    }
    override fun setOutsets(left: Int, top: Int, right: Int, bottom: Int) {}

    private companion object {
        val capturing = ArrayList<Capture?>()
    }
}
