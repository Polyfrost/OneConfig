@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

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
import org.polyfrost.compose.backend.DynamicOp
import org.polyfrost.compose.backend.PolyBlurEffect
import org.polyfrost.compose.backend.PolyCanvas
import org.polyfrost.compose.backend.RecordingCanvas
import org.polyfrost.compose.backend.imageBlur
import org.polyfrost.compose.render.CapturingRenderer
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyShadow

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
    private var animating = false
    private var lastScaleX = 1f
    private var lastScaleY = 1f
    private var cache: LayerCache? = null

    private class LayerCache(val version: Int, val props: List<Any?>, val key: Any, val segs: List<Any>)
    private class ChildRef(val layer: PolyGraphicsLayer, val state: Any)
    private class DynamicRef(val op: DynamicOp, val state: Any)
    private class Capture(var mark: Any) { val segs = ArrayList<Any>() }

    override fun record(density: Density, layoutDirection: LayoutDirection, layer: GraphicsLayer, block: DrawScope.() -> Unit) {
        version++
        recording.ops.clear()
        drawScope.draw(density, layoutDirection, recording, size.toSize(), block)
    }

    override fun draw(canvas: Canvas) {
        if (canvas is RecordingCanvas) { canvas.polyOp(::drawNow); return }
        if (canvas is PolyCanvas) drawNow(canvas)
    }

    private fun drawNow(c: PolyCanvas) {
        animating = (scaleX != 1f || scaleY != 1f) && (scaleX != lastScaleX || scaleY != lastScaleY)
        lastScaleX = scaleX
        lastScaleY = scaleY
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
        val props = props()
        val hit = cache
        if (hit != null && hit.version == version && cr.sameState(hit.key) && hit.props == props && hit.segs.all { it is ChildRef || it is DynamicRef || cr.valid(it) }) {
            capturing += null
            try {
                for (seg in hit.segs) {
                    if (seg !is ChildRef && seg !is DynamicRef) { cr.append(seg); continue }
                    val saved = cr.saveState()
                    try {
                        when (seg) {
                            is ChildRef -> { cr.restoreState(seg.state); seg.layer.drawNow(c) }
                            is DynamicRef -> { cr.restoreState(seg.state); seg.op(c) }
                        }
                    } finally {
                        cr.restoreState(saved)
                    }
                }
            } finally {
                capturing.removeLast()
            }
            return
        }
        val key = cr.stateKey()
        val cap = Capture(cr.mark())
        capturing += cap
        try { drawLive(c) } finally { capturing.removeLast() }
        cap.segs += cr.since(cap.mark)
        cache = LayerCache(version, props, key, cap.segs)
    }

    private fun props(): List<Any?> = listOf(alpha, scaleX, scaleY, animating, translationX, rotationZ, translationY, left, top, size, outline, clip, shadowElevation, spotShadowColor, pivotOffset, renderEffect)

    private fun drawLive(c: PolyCanvas) {
        if (alpha <= 0f) return
        val r = c.renderer
        if (alpha < 1f) r.saveLayerAlpha(alpha) else r.save()
        r.translate(left + translationX, top + translationY)
        if (scaleX != 1f || scaleY != 1f || rotationZ != 0f) {
            val p = if (pivotOffset.isUnspecified) Offset(size.width / 2f, size.height / 2f) else pivotOffset
            r.translate(p.x, p.y)
            if (rotationZ != 0f) r.rotate(rotationZ)
            if (scaleX != 1f || scaleY != 1f) { if (animating) r.animatedScale(scaleX, scaleY) else r.scale(scaleX, scaleY) }
            r.translate(-p.x, -p.y)
        }
        val o = outline
        if (shadowElevation > 0f && o != null) {
            val b = o.bounds
            r.shadow(b.left, b.top + shadowElevation / 2f, b.width, b.height, radiusOf(o), PolyShadow(PolyColor(spotShadowColor.copy(alpha = spotShadowColor.alpha * 0.25f).toArgb()), shadowElevation, 0f, 0f, 0f))
        }
        if (clip) {
            if (o == null) r.clipRect(0f, 0f, size.width.toFloat(), size.height.toFloat())
            else { val b = o.bounds; r.clipRRect(b.left, b.top, b.width, b.height, radiusOf(o)) }
        }
        val cap = capturing.lastOrNull()
        val cr = r as? CapturingRenderer
        val prevBlur = imageBlur
        imageBlur = (renderEffect?.platformRenderEffect as? PolyBlurEffect)?.sigma ?: prevBlur
        try {
            if (cap == null || cr == null) recording.replay(c)
            else recording.replay(c) { op ->
                cap.segs += cr.since(cap.mark)
                cap.segs += DynamicRef(op, cr.saveState())
                try { op(c) } finally { cap.mark = cr.mark() }
            }
        } finally {
            imageBlur = prevBlur
        }
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
