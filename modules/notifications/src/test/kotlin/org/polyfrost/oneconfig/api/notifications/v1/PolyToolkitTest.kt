package org.polyfrost.oneconfig.api.notifications.v1

import androidx.compose.runtime.mutableStateOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.polyfrost.compose.animation.core.springStep
import org.polyfrost.compose.foundation.ScrollState
import org.polyfrost.compose.foundation.clickable
import org.polyfrost.compose.foundation.focusable
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.align
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.widthIn
import org.polyfrost.compose.foundation.lazy.LazyColumn
import org.polyfrost.compose.foundation.lazy.LazyListState
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.foundation.verticalScroll
import org.polyfrost.compose.input.PolyInput
import org.polyfrost.compose.layout.LayoutType
import org.polyfrost.compose.layout.PolyArrangement
import org.polyfrost.compose.layout.PolyLayoutEngine
import org.polyfrost.compose.layout.PolySize
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.node.TextNode
import org.polyfrost.compose.node.requestLayout
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.runtime.PolyComposeClock
import org.polyfrost.compose.runtime.PolyComposeRuntime
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.focus.FocusRequester
import org.polyfrost.compose.ui.focus.onFocusChanged
import org.polyfrost.compose.ui.focus.focusRequester
import org.polyfrost.compose.ui.graphics.lerp
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
        val mid = lerp(PolyColor(0xFF000000.toInt()), PolyColor(-1), 0.5f)
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
    fun textFieldEditing() {
        var clip: String? = null
        val read = { clip }
        val write = { s: String -> clip = s }
        val shortcut = if (KeyEvent.MAC) KeyEvent.SUPER else KeyEvent.CONTROL
        val word = if (KeyEvent.MAC) KeyEvent.ALT else KeyEvent.CONTROL
        val s = TextFieldState()
        for (c in "hello world") s.onKey(KeyEvent(KeyEventType.Char, 0, 0, c.code), read, write)
        assertEquals("hello world", s.text)
        s.onKey(KeyEvent(KeyEventType.Char, 0, 0, 0x1F600), read, write)
        assertEquals("hello world😀", s.text)
        s.onKey(press(KeyEvent.KEY_BACKSPACE), read, write)
        assertEquals("hello world", s.text)

        s.onKey(press(KeyEvent.KEY_LEFT, word or KeyEvent.SHIFT), read, write)
        assertEquals("world", s.selectedText)
        s.onKey(press(67, shortcut), read, write)
        assertEquals("world", clip)
        s.onKey(press(88, shortcut), read, write)
        assertEquals("hello ", s.text)
        s.onKey(press(KeyEvent.KEY_HOME), read, write)
        s.onKey(press(86, shortcut), read, write)
        assertEquals("worldhello ", s.text)
        assertEquals(5, s.caret)

        s.onKey(press(65, shortcut), read, write)
        assertEquals(s.text, s.selectedText)
        s.onKey(press(KeyEvent.KEY_DELETE), read, write)
        assertEquals("", s.text)
        clip = "a\nb"
        s.onKey(press(86, shortcut), read, write)
        assertEquals("a b", s.text)
        assertFalse(s.onKey(press(KeyEvent.KEY_TAB), read, write))

        assertEquals(1, s.indexAt(7f, PolyFont("x", 10f)))
        assertEquals(0..1, s.wordAt(0))
    }

    @Test
    fun textFieldUndoRedo() {
        val read = { null }
        val write = { _: String -> }
        val shortcut = if (KeyEvent.MAC) KeyEvent.SUPER else KeyEvent.CONTROL
        fun type(s: TextFieldState, t: String) = t.forEach { s.onKey(KeyEvent(KeyEventType.Char, 0, 0, it.code), read, write) }
        val s = TextFieldState()
        type(s, "hello")
        s.onKey(press(KeyEvent.KEY_END), read, write)
        type(s, " world")
        s.onKey(press(KeyEvent.KEY_BACKSPACE), read, write)
        assertEquals("hello worl", s.text)

        s.onKey(press(KeyEvent.KEY_Z, shortcut), read, write)
        assertEquals("hello world", s.text)
        s.onKey(press(KeyEvent.KEY_Z, shortcut), read, write)
        assertEquals("hello", s.text)
        assertEquals(5, s.caret)
        s.onKey(press(KeyEvent.KEY_Z, shortcut), read, write)
        assertEquals("", s.text)
        assertFalse(s.undo())

        s.onKey(press(KeyEvent.KEY_Z, shortcut or KeyEvent.SHIFT), read, write)
        assertEquals("hello", s.text)
        s.onKey(press(KeyEvent.KEY_Y, shortcut), read, write)
        assertEquals("hello world", s.text)
        type(s, "!")
        assertFalse(s.redo())
        s.replaceText("x")
        assertFalse(s.undo())
    }

    @Test
    fun textFieldPreeditAndLines() {
        val read = { null }
        val write = { _: String -> }
        val s = TextFieldState("ab")
        s.onKey(KeyEvent(KeyEventType.Preedit, 0, 0, 1, "にほ"), read, write)
        assertEquals("にほ", s.preedit)
        assertEquals("ab", s.text)
        assertTrue(s.onKey(press(KeyEvent.KEY_BACKSPACE), read, write))
        assertEquals("ab", s.text)
        s.onKey(KeyEvent(KeyEventType.Preedit, 0, 0, 0, ""), read, write)
        s.onKey(KeyEvent(KeyEventType.Char, 0, 0, '日'.code), read, write)
        assertEquals("ab日", s.text)
        assertEquals("", s.preedit)

        assertFalse(s.onKey(press(KeyEvent.KEY_ENTER), read, write))
        val m = TextFieldState("one\ntwo", multiline = true)
        m.moveCaret(1)
        m.onKey(press(KeyEvent.KEY_DOWN), read, write)
        assertEquals(5, m.caret)
        m.onKey(press(KeyEvent.KEY_END), read, write)
        m.onKey(press(KeyEvent.KEY_ENTER), read, write)
        m.insert("x\r\ny")
        assertEquals("one\ntwo\nx\ny", m.text)
        m.onKey(press(KeyEvent.KEY_UP), read, write, PolyFont("x", 10f))
        assertEquals(9, m.caret)
        m.onKey(press(KeyEvent.KEY_HOME), read, write)
        assertEquals(8, m.caret)
        assertEquals(8, m.lineStartOf(2))
        assertEquals(10, m.lineStartOf(9))

        val w = TextFieldState("hello world foo\nab\nabcdefghij", multiline = true)
        val ls = w.lines(PolyFont("x", 10f), 30f)
        assertEquals(listOf(0 to 6, 6 to 12, 12 to 15, 16 to 18, 19 to 25, 25 to 29), ls.map { it.start to it.end })
        assertEquals(listOf(false, false, true, true, false, true), ls.map { it.hard })
        assertEquals(1, w.lineOf(ls, 6))
        assertEquals(2, w.lineOf(ls, 15))
        assertEquals(1, w.lines(PolyFont("x", 10f), 0f, "a b").size)
    }

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
    fun focusTraversalKeysAndOverlays() {
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

        var dismissed = 0
        var under = 0
        val popupChild = box(Modifier.size(30.dp, 30.dp).clickable { })
        val popup = box(Modifier.onPointerEvent { it.type == PointerEventType.Outside && run { dismissed++; true } }, popupChild)
        popup.style.overlay = true
        popup.style.x = 50f
        popup.style.y = 50f
        val clipped = box(Modifier.size(20.dp, 20.dp).onPointerEvent { it.type == PointerEventType.Press && run { under++; true } }, popup)
        clipped.style.clipToBounds = true
        val root2 = node(LayoutType.Column, Modifier.size(100.dp, 100.dp), clipped)
        PolyLayoutEngine.layout(root2, 100f, 100f)
        assertEquals(50f, popup.x)
        val input2 = PolyInput(root2)
        input2.move(60f, 60f)
        assertTrue(input2.press(0))
        input2.release(0)
        assertEquals(0, dismissed)
        input2.move(5f, 5f)
        assertTrue(input2.press(0))
        assertEquals(1, dismissed)
        assertEquals(0, under)

        val menu = box(Modifier.size(40.dp, 50.dp))
        menu.style.overlay = true
        menu.style.overlayAnchor = Alignment.BottomStart
        menu.style.y = 10f
        val anchor = box(Modifier.size(40.dp, 20.dp), menu)
        PolyLayoutEngine.layout(node(LayoutType.Column, Modifier.size(100.dp, 100.dp), box(Modifier.size(10.dp, 70.dp)), anchor), 100f, 100f)
        assertEquals(70f, anchor.y)
        assertEquals(10f, menu.y)
    }

    @Test
    fun focusRequesterAndPinnedOverlay() {
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

        val pin = floatArrayOf(Float.NaN, 0f, 0f, 0f)
        val menu = box(Modifier.size(40.dp, 20.dp))
        menu.style.overlay = true
        menu.style.overlayAnchor = Alignment.BottomStart
        menu.style.overlayPin = pin
        val anchor = box(Modifier.size(40.dp, 20.dp), menu)
        val root2 = node(LayoutType.Column, Modifier.size(100.dp, 100.dp), anchor)
        PolyLayoutEngine.layout(root2, 100f, 100f)
        assertEquals(20f, menu.y)
        Modifier.size(40.dp, 50.dp).applyTo(anchor.style)
        PolyLayoutEngine.layout(root2, 100f, 100f)
        assertEquals(50f, anchor.height)
        assertEquals(20f, menu.y)
        menu.style.overlayPin = null
        PolyLayoutEngine.layout(root2, 100f, 100f)
        assertEquals(50f, menu.y)
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
    fun lazyColumnComposesOnlyNearTheViewport() {
        val clock = PolyComposeClock()
        val runtime = PolyComposeRuntime(clock)
        val state = LazyListState()
        val items = (0 until 1000).toList()
        runtime.root.style.width = PolySize.Fill
        runtime.root.style.height = PolySize.Fill
        runtime.setContent {
            LazyColumn(Modifier.fillMaxSize(), state = state, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it }) {
                    Box(Modifier.size(100.dp, 40.dp))
                }
            }
        }
        var t = 1L
        repeat(4) { runtime.update(200f, 200f, t); t += 16_000_000L }
        val list = runtime.root.children.single()
        assertTrue(list.children.size < 30, "composed ${list.children.size}")
        assertEquals(1000 * 50f - 10f, state.scroll.content, 1f)

        state.scroll.scrollTo(25_000f)
        repeat(4) { runtime.update(200f, 200f, t); t += 16_000_000L }
        assertTrue(state.firstIndex in 480..500, "first ${state.firstIndex}")
        val top = list.children.first { it.y + it.height > list.y && it.style.tag != null }
        assertEquals(0f, abs(top.y - list.y) % 50f, 0.5f)
        runtime.dispose()
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
        val w = mutableStateOf(10f)
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
    fun screenUpdateLaysOutOnlyWhenSomethingMoved() {
        val clock = PolyComposeClock()
        val runtime = PolyComposeRuntime(clock)
        val scroll = ScrollState()
        runtime.setContent {
            Column(modifier = Modifier.size(100.dp, 100.dp).verticalScroll(scroll)) { repeat(10) { Box(Modifier.size(100.dp, 50.dp)) } }
        }
        var t = 1L
        fun frame() = runtime.update(200f, 200f, t).also { t += 16_000_000L }
        repeat(3) { frame() }
        val first = runtime.root.children.single().children.first()
        first.y = -1f
        assertFalse(frame())
        assertEquals(-1f, first.y)
        scroll.scrollTo(60f)
        assertTrue(frame())
        assertEquals(-60f, first.y)
        first.y = -1f
        first.requestLayout()
        assertTrue(frame())
        assertEquals(-60f, first.y)
        scroll.wheel(1f)
        while (scroll.animating) assertTrue(frame())
        // the zero delta treats -0f (wheel step scrolled back to the top) as equal to 0f
        assertEquals(-scroll.value, first.y, 0f)
        assertFalse(frame())
        runtime.dispose()
    }
}
