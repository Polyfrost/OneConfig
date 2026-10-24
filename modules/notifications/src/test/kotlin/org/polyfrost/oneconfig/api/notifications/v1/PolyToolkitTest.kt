package org.polyfrost.oneconfig.api.notifications.v1

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.polyfrost.compose.animation.core.springStep
import org.polyfrost.compose.foundation.clickable
import org.polyfrost.compose.foundation.focusable
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.layout.align
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.widthIn
import org.polyfrost.compose.input.PolyInput
import org.polyfrost.compose.layout.LayoutType
import org.polyfrost.compose.layout.PolyArrangement
import org.polyfrost.compose.layout.PolyLayoutEngine
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.node.TextNode
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.runtime.PolyComposeClock
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.focus.FocusRequester
import org.polyfrost.compose.ui.focus.onFocusChanged
import org.polyfrost.compose.ui.focus.focusRequester
import org.polyfrost.compose.ui.input.key.KeyEvent
import org.polyfrost.compose.ui.input.key.KeyEventType
import org.polyfrost.compose.ui.input.key.onKeyDown
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.inert
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.unit.dp
import kotlin.math.abs

class PolyToolkitTest {
    init {
        PolyFont.engine = object : PolyFont.Engine {
            override fun measureTextWidth(font: PolyFont, text: String) = text.length * font.size * 0.5f
            override fun metrics(font: PolyFont) = PolyFont.Metrics(-font.size * 0.8f, font.size * 0.2f, 0f, 1f, 1f)
        }
    }

    private fun node(type: LayoutType, modifier: Modifier, vararg children: PolyNode) = PolyNode().also { n ->
        n.style.layoutType = type
        modifier.applyTo(n.style)
        children.forEach { it.parent = n; n.children += it }
    }

    private fun box(modifier: Modifier, vararg children: PolyNode) = node(LayoutType.Box, modifier, *children)

    @Test
    fun weightArrangementAndBounds() {
        val fixed = box(Modifier.size(40.dp, 10.dp))
        val weighted = box(Modifier.weight(1f).height(30.dp))
        val capped = box(Modifier.size(500.dp, 20.dp).widthIn(max = 100.dp))
        val row = node(LayoutType.Row, Modifier.size(400.dp, 50.dp), fixed, weighted, capped)
        row.style.crossAlignment = 0f
        row.style.gap = 10f
        PolyLayoutEngine.layout(row, 400f, 50f)
        assertEquals(100f, capped.width)
        assertEquals(240f, weighted.width)
        assertEquals(300f, capped.x)
        assertEquals(20f, fixed.y)
        assertEquals(15f, capped.y)

        val a = box(Modifier.size(50.dp, 10.dp))
        val b = box(Modifier.size(50.dp, 10.dp))
        val spread = node(LayoutType.Row, Modifier.size(300.dp, 10.dp), a, b)
        spread.style.arrangement = PolyArrangement.SpaceBetween
        PolyLayoutEngine.layout(spread, 300f, 10f)
        assertEquals(0f, a.x)
        assertEquals(250f, b.x)
    }

    @Test
    fun paddingBeforeSizeIsOuterAfterIsInner() {
        val inner = box(Modifier.size(10.dp))
        val outer = box(Modifier.padding(4.dp).size(30.dp).padding(start = 3.dp).padding(start = 2.dp), inner)
        val root = box(Modifier.size(100.dp), outer)
        PolyLayoutEngine.layout(root, 100f, 100f)
        assertEquals(4f, outer.x)
        assertEquals(30f, outer.width)
        assertEquals(9f, inner.x)
    }

    @Test
    fun alignmentBiasesCountMargins() {
        val child = box(Modifier.padding(start = 10.dp).size(20.dp).align(Alignment.Center))
        val root = box(Modifier.size(100.dp), child)
        PolyLayoutEngine.layout(root, 100f, 100f)
        assertEquals(45f, child.x)
        assertEquals(40f, child.y)

        val a = box(Modifier.size(10.dp))
        val b = box(Modifier.size(10.dp).align(Alignment.Bottom))
        val row = node(LayoutType.Row, Modifier.size(100.dp, 50.dp), a, b)
        row.style.crossAlignment = Alignment.CenterVertically.bias
        PolyLayoutEngine.layout(row, 100f, 50f)
        assertEquals(20f, a.y)
        assertEquals(40f, b.y)
    }

    @Test
    fun weightWithoutFillShrinksOnlyWhenNarrow() {
        fun slider() = box(Modifier.size(300.dp, 10.dp).weight(1f, fill = false))
        val spinner = box(Modifier.size(80.dp, 10.dp))
        val wide = slider()
        PolyLayoutEngine.layout(node(LayoutType.Row, Modifier.size(500.dp, 10.dp), wide, spinner), 500f, 10f)
        assertEquals(300f, wide.width)
        val narrow = slider()
        PolyLayoutEngine.layout(node(LayoutType.Row, Modifier.size(220.dp, 10.dp), narrow, box(Modifier.size(80.dp, 10.dp))), 220f, 10f)
        assertEquals(140f, narrow.width)
    }

    @Test
    fun colourAnimationMixesInOklab() {
        val mid = org.polyfrost.compose.ui.graphics.lerp(org.polyfrost.compose.render.PolyColor(0xFF000000.toInt()), org.polyfrost.compose.render.PolyColor(-1), 0.5f)
        assertTrue(mid.red in 95..104, "red ${mid.red}")
        assertEquals(mid.red, mid.blue)
    }

    @Test
    fun wrappedTextAndEllipsis() {
        val text = TextNode().also { it.fontSize = 10f; it.wrap = true; it.text = "alpha beta gamma delta"; it.remeasure() }
        val column = node(LayoutType.Column, Modifier.size(60.dp, 200.dp), text)
        PolyLayoutEngine.layout(column, 60f, 200f)
        assertEquals(20f, text.height)
        assertTrue(text.width <= 60f)

        val cut = TextNode().also { it.fontSize = 10f; it.ellipsis = true; it.text = "a long single line"; it.remeasure() }
        val narrow = node(LayoutType.Column, Modifier.size(40.dp, 20.dp), cut)
        PolyLayoutEngine.layout(narrow, 40f, 20f)
        assertEquals(10f, cut.height)
        assertTrue(cut.width <= 40f)

        val long = TextNode().also { it.fontSize = 10f; it.wrap = true; it.text = "abcdefghijkl"; it.remeasure() }
        PolyLayoutEngine.layout(node(LayoutType.Column, Modifier.size(30.dp, 100.dp), long), 30f, 100f)
        assertEquals(20f, long.height)
    }

    private fun press(key: Int, mods: Int = 0) = KeyEvent(KeyEventType.KeyDown, key, mods)

    @Test
    fun inertSubtreeTakesNoFocusKeysOrPointer() {
        var keys = 0
        var clicks = 0
        val field = box(Modifier.size(10.dp, 10.dp).focusable().onKeyDown { _, _ -> keys++; true }.onPointerEvent { clicks++; true })
        val cover = box(Modifier.size(50.dp, 50.dp).inert(), field)
        var wheel = 0
        val root = node(LayoutType.Column, Modifier.size(100.dp, 100.dp).onPointerEvent { e -> (e.type == PointerEventType.Scroll).also { if (it) wheel++ } }, cover)
        PolyLayoutEngine.layout(root, 100f, 100f)
        val input = PolyInput(root)
        assertFalse(input.key(KeyEvent.KEY_TAB, 0))
        assertNull(input.focused)
        input.key(2, 0)
        assertEquals(0, keys)
        input.move(5f, 5f)
        input.press(0)
        input.release(0)
        assertEquals(0, clicks)
        assertTrue(input.scroll(1f))
        assertEquals(1, wheel)

        input.focus(field)
        assertSame(field, input.focused)
        input.key(2, 0)
        assertNull(input.focused)
        assertEquals(0, keys)
    }

    @Test
    fun onFocusChangedChainsWithFocusable() {
        val seen = ArrayList<String>()
        val field = box(
            Modifier.onFocusChanged { seen += "a${it.isFocused}" }.size(10.dp, 10.dp)
                .focusable { seen += "f$it" }.onFocusChanged { seen += "b${it.isFocused}" },
        )
        val root = node(LayoutType.Column, Modifier.size(100.dp, 100.dp), field)
        PolyLayoutEngine.layout(root, 100f, 100f)
        val input = PolyInput(root)
        input.focus(field)
        input.focus(null)
        assertEquals(listOf("ftrue", "atrue", "btrue", "ffalse", "afalse", "bfalse"), seen)
    }

    @Test
    fun severalPointerHandlersOnOneNode() {
        var hovered = false
        var clicks = 0
        var wheel = 0f
        val n = box(Modifier.size(50.dp, 50.dp).hoverable { hovered = it }.clickable { clicks++ }.onPointerEvent {
            it.type == PointerEventType.Scroll && run { wheel += it.scroll; true }
        })
        val root = node(LayoutType.Column, Modifier.size(100.dp, 100.dp), n)
        PolyLayoutEngine.layout(root, 100f, 100f)
        val input = PolyInput(root)
        input.move(10f, 10f)
        assertTrue(hovered)
        input.press(0)
        input.release(0)
        assertEquals(1, clicks)
        assertTrue(input.scroll(2f))
        assertEquals(2f, wheel)
        input.move(80f, 80f)
        assertFalse(hovered)
        assertNull(input.focused)

        var under = 0
        val target = box(Modifier.size(50.dp, 50.dp).clickable { under++ })
        val cover = box(Modifier.size(100.dp, 100.dp))
        val stack = box(Modifier.size(100.dp, 100.dp), target, cover)
        PolyLayoutEngine.layout(stack, 100f, 100f)
        val input2 = PolyInput(stack)
        input2.move(10f, 10f)
        input2.press(0)
        input2.release(0)
        assertEquals(1, under)
    }

    @Test
    fun springSettles() {
        var x = 1f
        var v = 0f
        repeat(60) { val s = springStep(x, v, 38.7f, 1f / 60f); x = s[0]; v = s[1] }
        assertTrue(abs(x) < 0.001f && x >= 0f)
    }

    @Test
    fun layoutIfChangedSkipsOnlyUnchangedTrees() {
        val clock = PolyComposeClock()
        val runtime = PolyComposeRuntime(clock)
        val w = androidx.compose.runtime.mutableStateOf(10f)
        runtime.setContent { Box(Modifier.size(w.value.dp, 10.dp)) }
        assertTrue(runtime.layoutIfChanged(100f, 100f))
        assertFalse(runtime.layoutIfChanged(100f, 100f))
        assertTrue(runtime.layoutIfChanged(120f, 100f))
        w.value = 30f
        clock.frame(1L)
        assertTrue(runtime.layoutIfChanged(120f, 100f))
        assertEquals(30f, runtime.root.children.single().width)
        assertFalse(runtime.layoutIfChanged(120f, 100f))
    }

    @Test
    fun focusTraversalAndKeys() {
        val keys = ArrayList<String>()
        var focusA = false
        val a = box(Modifier.size(10.dp, 10.dp).focusable { focusA = it }.onKeyDown { k, _ -> keys += "a$k"; k == 1 })
        val b = box(Modifier.size(10.dp, 10.dp).focusable().onKeyDown { k, _ -> keys += "b$k"; k == 3 })
        var rootKeys = 0
        val root = node(LayoutType.Column, Modifier.size(100.dp, 100.dp).onKeyDown { k, _ -> k == 2 && run { rootKeys++; true } }, a, b)
        PolyLayoutEngine.layout(root, 100f, 100f)
        val input = PolyInput(root)

        assertTrue(input.key(KeyEvent.KEY_TAB, 0))
        assertSame(a, input.focused)
        assertTrue(focusA)
        keys.clear()
        input.key(2, 0)
        assertEquals(listOf("a2"), keys)
        assertEquals(1, rootKeys)
        input.key(KeyEvent.KEY_TAB, 0)
        assertSame(b, input.focused)
        assertFalse(focusA)
        input.key(KeyEvent.KEY_TAB, KeyEvent.SHIFT)
        assertSame(a, input.focused)

        input.move(5f, 15f)
        input.press(0)
        input.release(0)
        assertSame(b, input.focused)
    }

    @Test
    fun focusRequesterMovesFocus() {
        val requester = FocusRequester()
        var fieldFocused = false
        val field = box(Modifier.size(10.dp, 10.dp).focusable { fieldFocused = it })
        val editor = box(Modifier.size(50.dp, 50.dp).focusRequester(requester).focusable())
        val root = node(LayoutType.Column, Modifier.size(100.dp, 100.dp), field, editor)
        PolyLayoutEngine.layout(root, 100f, 100f)
        val input = PolyInput(root)
        input.focus(field)
        input.refresh()
        assertSame(field, input.focused)
        requester.requestFocus()
        Modifier.size(50.dp, 50.dp).focusRequester(requester).focusable().applyTo(editor.style)
        input.refresh()
        assertSame(editor, input.focused)
        assertFalse(fieldFocused)
        Modifier.size(50.dp, 50.dp).focusRequester(requester).focusable().applyTo(editor.style)
        input.focus(field)
        input.refresh()
        assertSame(field, input.focused)
    }
}
