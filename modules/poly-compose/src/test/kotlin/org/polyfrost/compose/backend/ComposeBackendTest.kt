package org.polyfrost.compose.backend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.SingleComposeSceneRenderingScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.PolyPath
import org.polyfrost.compose.render.PolyRenderer
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.FilterQuality
import org.polyfrost.compose.ui.graphics.ImageBitmap
import org.polyfrost.compose.ui.graphics.shadow.Shadow

class ComposeBackendTest {
    private class LogRenderer : PolyRenderer {
        val calls = ArrayList<String>()
        private var tx = 0f
        private var ty = 0f
        private val stack = ArrayDeque<Pair<Float, Float>>()
        private fun log(s: String) { calls += s }
        override fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float) = log("rect ${x + tx},${y + ty} ${w}x$h #${Integer.toHexString(color)} r=$radius")
        override fun rectWithHole(x: Float, y: Float, w: Float, h: Float, holeX: Float, holeY: Float, holeW: Float, holeH: Float, holeRadius: Float, color: Int) = log("hole")
        override fun rectGradient(x: Float, y: Float, w: Float, h: Float, gradient: Brush, radius: Float) = log("gradient ${x + tx},${y + ty} ${w}x$h")
        override fun shadow(x: Float, y: Float, w: Float, h: Float, radius: Float, shadow: Shadow) = log("shadow ${x + tx},${y + ty} ${w}x$h r=$radius blur=${shadow.radius.value}")
        override fun rectStroke(x: Float, y: Float, w: Float, h: Float, color: Int, strokeWidth: Float, radius: Float) = log("stroke")
        override fun rectStrokeGradient(x: Float, y: Float, w: Float, h: Float, top: Int, bottom: Int, strokeWidth: Float, radius: Float) = log("strokeGradient")
        override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, strokeWidth: Float) = log("line")
        override fun fillPath(path: PolyPath, color: Int) = log("path #${Integer.toHexString(color)}")
        override fun text(text: String, x: Float, y: Float, color: Int, font: PolyFont) = log("text '$text' ${x + tx},${y + ty}")
        override fun textOutline(text: String, x: Float, y: Float, color: Int, font: PolyFont, width: Float) = log("textOutline")
        override fun mcText(text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float) = log("mcText")
        override fun mcTextOutline(text: String, x: Float, y: Float, color: Int, outlineColor: Int, scale: Float) = log("mcTextOutline")
        override fun image(bitmap: ImageBitmap, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, tint: Int, filterQuality: FilterQuality) = log("image")
        override fun live(draw: (PolyRenderer) -> Unit) = draw(this)
        override fun save() { stack.addLast(tx to ty) }
        override fun restore() { val (x, y) = stack.removeLast(); tx = x; ty = y }
        override fun saveLayerAlpha(alpha: Float) = save()
        override fun translate(dx: Float, dy: Float) { tx += dx; ty += dy }
        override fun scale(sx: Float, sy: Float) {}
        override fun clipRect(x: Float, y: Float, w: Float, h: Float) = log("clip ${x + tx},${y + ty} ${w}x$h")
        override fun clipRRect(x: Float, y: Float, w: Float, h: Float, radius: Float) = log("clipR ${x + tx},${y + ty} ${w}x$h r=$radius")
    }

    private fun render(content: @Composable () -> Unit): List<String> {
        PolyCompose.install()
        PolyFont.engine = object : PolyFont.Engine {
            override fun measureTextWidth(font: PolyFont, text: String) = text.length * font.size * 0.5f
            override fun metrics(font: PolyFont) = PolyFont.Metrics(-font.size * 0.8f, font.size * 0.2f, 0f, font.size * 0.1f, 1f)
        }
        val rec = FrameRecomposer(Dispatchers.Unconfined) {}
        val scope = SingleComposeSceneRenderingScope {}
        val scene = CanvasLayersComposeScene(frameRecomposer = rec, density = Density(1f), size = IntSize(400, 300))
        scene.setContent { content() }
        val r = LogRenderer()
        with(scope) { scene.render(rec, PolyCanvas(r), System.nanoTime()) }
        scene.close()
        rec.close()
        return r.calls
    }

    @Test
    fun drawsBackgroundShadowAndText() {
        val calls = render {
            Box(Modifier.fillMaxSize().background(Color(0x99000000.toInt())).padding(10.dp)) {
                Box(Modifier.size(100.dp).shadow(8.dp, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp)).background(Color.Red)) {
                    BasicText("hi")
                }
            }
        }
        println(calls.joinToString("\n"))
        assertTrue(calls.any { it.startsWith("rect 0.0,0.0 400.0x300.0") }, "full-size background")
        assertTrue(calls.any { it.startsWith("shadow") }, "shadow")
        assertEquals(1, calls.count { it.startsWith("text 'hi'") })
    }
}
