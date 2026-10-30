@file:OptIn(InternalComposeUiApi::class)

package org.polyfrost.compose.backend

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.SingleComposeSceneRenderingScope
import androidx.compose.ui.text.PlatformParagraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.polyfrost.compose.render.CapturingRenderer
import org.polyfrost.compose.render.FilterQuality
import org.polyfrost.compose.render.FontManager
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.render.PolyGradient
import org.polyfrost.compose.render.PolyPath
import org.polyfrost.compose.render.PolyRenderer
import org.polyfrost.compose.render.PolyShadow

class ComposeBackendTest {
    private class LogRenderer : PolyRenderer {
        val calls = ArrayList<String>()
        private var tx = 0f
        private var ty = 0f
        private val stack = ArrayDeque<Pair<Float, Float>>()
        private fun log(s: String) { calls += s }
        override fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float) = log("rect ${x + tx},${y + ty} ${w}x$h #${Integer.toHexString(color)} r=$radius")
        override fun rectWithHole(x: Float, y: Float, w: Float, h: Float, holeX: Float, holeY: Float, holeW: Float, holeH: Float, holeRadius: Float, color: Int) = log("hole")
        override fun rectGradient(x: Float, y: Float, w: Float, h: Float, gradient: PolyGradient, radius: Float) = log("gradient ${x + tx},${y + ty} ${w}x$h")
        override fun shadow(x: Float, y: Float, w: Float, h: Float, radius: Float, shadow: PolyShadow) = log("shadow ${x + tx},${y + ty} ${w}x$h r=$radius blur=${shadow.blur}")
        override fun rectStroke(x: Float, y: Float, w: Float, h: Float, color: Int, strokeWidth: Float, radius: Float) = log("stroke")
        override fun rectStrokeGradient(x: Float, y: Float, w: Float, h: Float, gradient: PolyGradient, strokeWidth: Float, radius: Float) = log("strokeGradient")
        override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, strokeWidth: Float) = log("line")
        override fun fillPath(path: PolyPath, color: Int) = log("path #${Integer.toHexString(color)}")
        override fun text(text: String, x: Float, y: Float, color: Int, font: PolyFont) = log("text '$text' ${x + tx},${y + ty}")
        override fun textOutline(text: String, x: Float, y: Float, color: Int, font: PolyFont, width: Float) = log("textOutline")
        override fun mcText(text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float) = log("mcText")
        override fun mcTextOutline(text: String, x: Float, y: Float, color: Int, outlineColor: Int, scale: Float) = log("mcTextOutline")
        override fun image(bitmap: PolyBitmap, x: Float, y: Float, w: Float, h: Float, u0: Float, v0: Float, u1: Float, v1: Float, tint: Int, filterQuality: FilterQuality) = log("image")
        override fun live(draw: (PolyRenderer) -> Unit) = draw(this)
        override fun save() { stack.addLast(tx to ty) }
        override fun restore() { val (x, y) = stack.removeLast(); tx = x; ty = y }
        override fun saveLayerAlpha(alpha: Float) = save()
        override fun translate(dx: Float, dy: Float) { tx += dx; ty += dy }
        override fun scale(sx: Float, sy: Float) {}
        override fun clipRect(x: Float, y: Float, w: Float, h: Float) = log("clip ${x + tx},${y + ty} ${w}x$h")
        override fun clipRRect(x: Float, y: Float, w: Float, h: Float, radius: Float) = log("clipR ${x + tx},${y + ty} ${w}x$h r=$radius")
    }

    private class CapturingLogRenderer : CapturingRenderer, PolyRenderer by LogRenderer() {
        val calls = ArrayList<String>()
        private var tx = 0f
        private var ty = 0f
        private val stack = ArrayDeque<Pair<Float, Float>>()
        override fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float) { calls += "rect ${x + tx},${y + ty} #${Integer.toHexString(color)}" }
        override fun save() { stack.addLast(tx to ty) }
        override fun restore() { val (x, y) = stack.removeLast(); tx = x; ty = y }
        override fun saveLayerAlpha(alpha: Float) { calls += "layer $alpha"; save() }
        override fun translate(dx: Float, dy: Float) { tx += dx; ty += dy }
        override fun mark(): Any = calls.size
        override fun since(mark: Any): Any = ArrayList(calls.subList(mark as Int, calls.size))
        override fun valid(captured: Any) = true
        override fun append(captured: Any) { @Suppress("UNCHECKED_CAST") (captured as List<String>).forEach { calls += "cached $it" } }
        override fun stateKey(): Any = tx to ty
        override fun sameState(key: Any) = key == stateKey()
        override fun saveState(): Any = Triple(tx, ty, ArrayList(stack))
        override fun restoreState(saved: Any) {
            @Suppress("UNCHECKED_CAST") val (x, y, s) = saved as Triple<Float, Float, List<Pair<Float, Float>>>
            tx = x; ty = y; stack.clear(); stack.addAll(s)
        }
    }

    @Test
    fun layerCacheReplaysStaticOpsAndRerunsPolyOps() {
        PolyCompose.install()
        val rec = FrameRecomposer(Dispatchers.Unconfined) {}
        val scope = SingleComposeSceneRenderingScope {}
        val scene = CanvasLayersComposeScene(frameRecomposer = rec, density = Density(1f), size = IntSize(400, 300))
        var polyRuns = 0
        scene.setContent {
            Box(Modifier.padding(10.dp).graphicsLayer {}.size(50.dp).background(Color.Red).drawBehind {
                drawPoly { polyRuns++; it.renderer.rect(5f, 5f, 1f, 1f, 0xFF00FF00.toInt(), 0f) }
            })
        }
        val frames = List(2) { CapturingLogRenderer().also { r -> with(scope) { scene.render(rec, PolyCanvas(r), System.nanoTime()) } }.calls }
        scene.close()
        rec.close()
        assertEquals(2, polyRuns)
        assertEquals(listOf("rect 10.0,10.0 #ffff0000", "rect 15.0,15.0 #ff00ff00"), frames[0])
        assertEquals(listOf("cached rect 10.0,10.0 #ffff0000", "rect 15.0,15.0 #ff00ff00"), frames[1])
    }

    @Test
    fun translucentLayerCacheReplaysInsideLayer() {
        PolyCompose.install()
        val rec = FrameRecomposer(Dispatchers.Unconfined) {}
        val scope = SingleComposeSceneRenderingScope {}
        val scene = CanvasLayersComposeScene(frameRecomposer = rec, density = Density(1f), size = IntSize(400, 300))
        scene.setContent {
            Box(Modifier.padding(10.dp).graphicsLayer { alpha = 0.5f }.size(50.dp).background(Color.Red))
        }
        val frames = List(2) { CapturingLogRenderer().also { r -> with(scope) { scene.render(rec, PolyCanvas(r), System.nanoTime()) } }.calls }
        scene.close()
        rec.close()
        assertEquals(listOf("layer 0.5", "rect 10.0,10.0 #ffff0000"), frames[0])
        assertEquals(listOf("layer 0.5", "cached rect 10.0,10.0 #ffff0000"), frames[1])
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

    private fun paragraph(text: String, style: TextStyle = TextStyle(), maxWidth: Int = 100): PlatformParagraph {
        PolyFont.engine = object : PolyFont.Engine {
            override fun measureTextWidth(font: PolyFont, text: String) = text.codePointCount(0, text.length) * font.size * 0.5f
            override fun metrics(font: PolyFont) = PolyFont.Metrics(-font.size * 0.8f, font.size * 0.2f, 0f, font.size * 0.1f, 1f)
        }
        return PolyText.createParagraph(text, style, emptyList(), emptyList(), 0, TextOverflow.Clip, Constraints(maxWidth = maxWidth), Density(1f), PolyText.createFontFamilyResolver())
    }

    private fun drawnText(p: PlatformParagraph): List<String> {
        val r = LogRenderer()
        p.paint(PolyCanvas(r), Color.Black, null, null)
        return r.calls.filter { it.startsWith("text") }
    }

    @Test
    fun plainLatinLayoutIsUnchanged() {
        val p = paragraph("hello world")
        assertEquals(listOf("text 'hello world' 0.0,11.2"), drawnText(p))
        assertEquals(0f, p.getLineLeft(0))
        assertEquals(21f, p.getHorizontalPosition(3, true))
        assertEquals(3, p.getOffsetForPosition(Offset(22f, 5f)))
        assertEquals(ResolvedTextDirection.Ltr, p.getParagraphDirection(0))
        assertEquals(ResolvedTextDirection.Ltr, p.getBidiRunDirection(0))
    }

    @Test
    fun hebrewLaysOutRightToLeft() {
        val p = paragraph("אבג")
        assertEquals(ResolvedTextDirection.Rtl, p.getParagraphDirection(0))
        assertEquals(ResolvedTextDirection.Rtl, p.getBidiRunDirection(1))
        assertEquals(79f, p.getLineLeft(0))
        assertEquals(100f, p.getHorizontalPosition(0, true))
        assertEquals(79f, p.getHorizontalPosition(3, true))
        assertEquals(listOf("text 'גבא' 79.0,11.2"), drawnText(p))
    }

    @Test
    fun mixedTextDrawsRunsInVisualOrder() {
        val p = paragraph("abc אבג def")
        assertEquals(ResolvedTextDirection.Ltr, p.getParagraphDirection(0))
        assertEquals(ResolvedTextDirection.Rtl, p.getBidiRunDirection(5))
        assertEquals(listOf("text 'abc ' 0.0,11.2", "text 'גבא' 28.0,11.2", "text ' def' 49.0,11.2"), drawnText(p))
        assertEquals(28f, p.getHorizontalPosition(4, true))
        assertEquals(49f, p.getHorizontalPosition(4, false))
        assertEquals(42f, p.getHorizontalPosition(5, true))
        assertEquals(49f, p.getHorizontalPosition(7, true))
        fun span(s: Int, e: Int) = p.getPathForRange(s, e).getBounds().let { it.left to it.right }
        assertEquals(21f to 28f, span(3, 4))
        assertEquals(35f to 49f, span(4, 6))
        assertEquals(21f to 49f, span(3, 6))
    }

    @Test
    fun paintColorReplacesBrushAlpha() {
        PolyCompose.install()
        val r = LogRenderer()
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, PolyCanvas(r), Size(10f, 10f)) {
            drawRect(Brush.horizontalGradient(listOf(Color.Red, Color.Blue)), alpha = 0.25f)
            drawRect(Color.Red)
        }
        assertEquals("rect 0.0,0.0 10.0x10.0 #ffff0000 r=0.0", r.calls.last())
    }

    @Test
    fun translucentTextColorAppliesOnce() {
        var argb = 0
        val r = object : PolyRenderer by LogRenderer() {
            override fun text(text: String, x: Float, y: Float, color: Int, font: PolyFont, letterSpacing: Float, skewX: Float) { argb = color }
        }
        paragraph("a", TextStyle(color = Color.Black.copy(alpha = 0.5f))).paint(PolyCanvas(r), Color.Unspecified, null, null)
        assertEquals(0x80, argb ushr 24)
    }

    @Test
    fun italicWithoutItalicFaceIsSlanted() {
        fun skewOf(style: TextStyle): Float {
            var skew = Float.NaN
            val r = object : PolyRenderer by LogRenderer() {
                override fun text(text: String, x: Float, y: Float, color: Int, font: PolyFont, letterSpacing: Float, skewX: Float) { skew = skewX }
            }
            paragraph("a", style).paint(PolyCanvas(r), Color.Unspecified, null, null)
            return skew
        }
        assertEquals(0.25f, skewOf(TextStyle(fontStyle = FontStyle.Italic)))
        assertEquals(0f, skewOf(TextStyle(fontStyle = FontStyle.Italic, fontSynthesis = FontSynthesis.None)))
        assertEquals(0f, skewOf(TextStyle()))
    }

    @Test
    fun advancesRefreshWhenFontsChange() {
        var unit = 0.5f
        PolyFont.engine = object : PolyFont.Engine {
            override fun measureTextWidth(font: PolyFont, text: String) = text.codePointCount(0, text.length) * font.size * unit
            override fun metrics(font: PolyFont) = PolyFont.Metrics(-font.size * 0.8f, font.size * 0.2f, 0f, font.size * 0.1f, 1f)
        }
        fun width() = PolyText.createParagraph("ab", TextStyle(), emptyList(), emptyList(), 0, TextOverflow.Clip, Constraints(), Density(1f), PolyText.createFontFamilyResolver()).maxIntrinsicWidth
        assertEquals(14f, width())
        unit = 1f
        FontManager.invalidateMetrics()
        assertEquals(28f, width())
    }

    @Test
    fun selectionEndingAtWrapCoversLine() {
        val p = paragraph("abcdefgh", maxWidth = 28)
        assertEquals(2, p.lineCount)
        assertEquals(0f to 28f, p.getPathForRange(0, 4).getBounds().let { it.left to it.right })
    }

    @Test
    fun rtlRunsKeepMarksOnTheirBase() {
        assertEquals(listOf("text 'באָ' 79.0,11.2"), drawnText(paragraph("אָב")))
    }

    @Test
    fun arabicIsDrawnJoined() {
        assertEquals(listOf("text 'ﺖﻴﺑ' 79.0,11.2"), drawnText(paragraph("بيت")))
    }

    @Test
    fun arabicLigaturesLeaveNoGap() {
        assertEquals(listOf("text '\uFEE1\uFEFC\uFEB3' 79.0,11.2"), drawnText(paragraph("\u0633\u0644\u0627\u0645")))
    }

    @Test
    fun caretAndHitTestRoundTrip() {
        for (text in listOf("abc אבג def", "אבג", "אבג abc דהו", "abc")) {
            val p = paragraph(text)
            for (o in 0..text.length) {
                val x = p.getHorizontalPosition(o, true)
                assertEquals(o, p.getOffsetForPosition(Offset(x, 5f)), "'$text' offset $o at x=$x")
            }
        }
    }

    @Test
    fun rtlDirectionRightAlignsLatinAtStart() {
        val p = paragraph("abc", TextStyle(textDirection = TextDirection.Rtl, textAlign = TextAlign.Start))
        assertEquals(ResolvedTextDirection.Rtl, p.getParagraphDirection(0))
        assertEquals(ResolvedTextDirection.Ltr, p.getBidiRunDirection(0))
        assertEquals(79f, p.getLineLeft(0))
        assertEquals(listOf("text 'abc' 79.0,11.2"), drawnText(p))
        val end = paragraph("abc", TextStyle(textDirection = TextDirection.Rtl, textAlign = TextAlign.End))
        assertEquals(0f, end.getLineLeft(0))
    }

    @Test
    fun directionIsPerParagraph() {
        assertEquals(100f, paragraph("", TextStyle(textDirection = TextDirection.Rtl)).getHorizontalPosition(0, true))
        val p = paragraph("אב\nab\n")
        assertEquals(ResolvedTextDirection.Rtl, p.getParagraphDirection(0))
        assertEquals(ResolvedTextDirection.Ltr, p.getParagraphDirection(3))
        assertEquals(86f, p.getLineLeft(0))
        assertEquals(0f, p.getLineLeft(1))
        assertEquals(3, p.lineCount)
    }

    @Test
    fun rtlEllipsisSitsAtVisualEnd() {
        PolyFont.engine = object : PolyFont.Engine {
            override fun measureTextWidth(font: PolyFont, text: String) = text.codePointCount(0, text.length) * font.size * 0.5f
            override fun metrics(font: PolyFont) = PolyFont.Metrics(-font.size * 0.8f, font.size * 0.2f, 0f, font.size * 0.1f, 1f)
        }
        val p = PolyText.createParagraph("אבגד הוזח טיכל", TextStyle(), emptyList(), emptyList(), 1, TextOverflow.Ellipsis, Constraints(maxWidth = 50), Density(1f), PolyText.createFontFamilyResolver())
        val calls = drawnText(p)
        assertEquals(listOf("text 'דגבא' 22.0,11.2", "text '…' 15.0,11.2"), calls)
    }
}
