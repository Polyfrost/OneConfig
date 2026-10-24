package org.polyfrost.compose.foundation.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.focusable
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.input.key.KeyEvent
import org.polyfrost.compose.ui.input.key.KeyEventType
import org.polyfrost.compose.ui.input.key.onKeyEvent
import org.polyfrost.compose.ui.input.pointer.PointerEvent
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.text.style.TextAlign
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.render.PolyFont
import kotlin.math.max
import kotlin.math.min

class TextFieldState(initial: String = "", val multiline: Boolean = false) {
    var text by mutableStateOf(initial)
        private set
    var caret by mutableIntStateOf(initial.length)
        private set
    var anchor by mutableIntStateOf(initial.length)
        private set
    private var _focused by mutableStateOf(false)
    var focused: Boolean
        get() = _focused
        internal set(value) {
            if (value == _focused) return
            _focused = value
            if (!value) setPreedit("", 0)
            focusListener(this, value)
        }

    var preedit by mutableStateOf("")
        private set

    var preeditCaret by mutableIntStateOf(0)
        private set

    internal var focusRequested by mutableStateOf(false)

    fun requestFocus(selectAll: Boolean = false) {
        if (selectAll) selectAll()
        focusRequested = true
    }

    var edits by mutableIntStateOf(0)
        private set

    val selectionStart get() = min(caret, anchor)
    val selectionEnd get() = max(caret, anchor)
    val hasSelection get() = caret != anchor
    val selectedText get() = text.substring(selectionStart, selectionEnd)

    var scrollX = 0f
        internal set
    var scrollY = 0f
        internal set

    internal var scrolledFor = -1

    private class Snapshot(val text: String, val caret: Int, val anchor: Int)

    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()

    private var typing = false

    fun replaceText(value: String) {
        text = value
        caret = caret.coerceIn(0, value.length)
        anchor = anchor.coerceIn(0, value.length)
        undoStack.clear()
        redoStack.clear()
        typing = false
        edits++
    }

    fun moveCaret(index: Int, extend: Boolean = false) {
        caret = index.coerceIn(0, text.length)
        if (!extend) anchor = caret
        typing = false
        edits++
    }

    fun select(start: Int, end: Int) {
        anchor = start.coerceIn(0, text.length)
        caret = end.coerceIn(0, text.length)
        typing = false
        edits++
    }

    fun selectAll() = select(0, text.length)

    fun insert(s: String) = edit(s, typed = false)

    private fun edit(s: String, typed: Boolean) {
        val clean = if (multiline) s.replace("\r\n", "\n").replace('\r', '\n').filter { it == '\n' || it == ' ' || !it.isISOControl() }
        else s.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ').filter { it == ' ' || !it.isISOControl() }
        if (clean.isEmpty() && !hasSelection) return
        if (!typed || !typing) push(undoStack, Snapshot(text, caret, anchor))
        redoStack.clear()
        typing = typed
        val start = selectionStart
        text = text.substring(0, start) + clean + text.substring(selectionEnd)
        caret = start + clean.length
        anchor = caret
        edits++
    }

    private fun push(stack: ArrayDeque<Snapshot>, s: Snapshot) {
        stack.addLast(s)
        if (stack.size > 100) stack.removeFirst()
    }

    private fun restore(from: ArrayDeque<Snapshot>, to: ArrayDeque<Snapshot>): Boolean {
        val s = from.removeLastOrNull() ?: return false
        push(to, Snapshot(text, caret, anchor))
        text = s.text
        caret = s.caret
        anchor = s.anchor
        typing = false
        edits++
        return true
    }

    fun undo() = restore(undoStack, redoStack)

    fun redo() = restore(redoStack, undoStack)

    fun setPreedit(s: String, caretInIt: Int) {
        if (s == preedit && caretInIt == preeditCaret) return
        preedit = s
        preeditCaret = caretInIt.coerceIn(0, s.length)
        edits++
    }

    fun backspace(word: Boolean = false) {
        if (!hasSelection) anchor = if (word) wordLeft(caret) else prevChar(caret)
        insert("")
    }

    fun delete(word: Boolean = false) {
        if (!hasSelection) anchor = if (word) wordRight(caret) else nextChar(caret)
        insert("")
    }

    fun wordLeft(i: Int): Int {
        var j = i
        while (j > 0 && !text[j - 1].isLetterOrDigit()) j--
        while (j > 0 && text[j - 1].isLetterOrDigit()) j--
        return j
    }

    fun wordRight(i: Int): Int {
        var j = i
        while (j < text.length && !text[j].isLetterOrDigit()) j++
        while (j < text.length && text[j].isLetterOrDigit()) j++
        return j
    }

    private fun prevChar(i: Int) = if (i > 0 && text[i - 1].isLowSurrogate() && i > 1) i - 2 else max(0, i - 1)
    private fun nextChar(i: Int) = if (i < text.length && text[i].isHighSurrogate() && i + 1 < text.length) i + 2 else min(text.length, i + 1)

    fun lineStart(i: Int) = text.lastIndexOf('\n', i - 1) + 1

    fun lineEnd(i: Int) = text.indexOf('\n', i).let { if (it < 0) text.length else it }

    fun wordAt(i: Int): IntRange {
        if (text.isEmpty()) return 0..0
        val at = i.coerceIn(0, text.length - 1)
        val inWord = text[at].isLetterOrDigit()
        var s = at
        var e = at
        while (s > 0 && text[s - 1].isLetterOrDigit() == inWord) s--
        while (e < text.length && text[e].isLetterOrDigit() == inWord) e++
        return s..e
    }

    internal var wrapWidth = 0f

    class Line(val start: Int, val end: Int, val hard: Boolean)

    fun lines(font: PolyFont?, width: Float = wrapWidth, s: String = text): List<Line> {
        val out = ArrayList<Line>()
        var a = 0
        while (true) {
            val e = if (multiline) s.indexOf('\n', a).let { if (it < 0) s.length else it } else s.length
            var from = a
            while (font != null && width > 0f && e - from > 1 && font.measureTextWidth(s.substring(from, e)) > width) {
                var lo = from + 1
                var hi = e - 1
                while (lo < hi) {
                    val mid = (lo + hi + 1) / 2
                    if (font.measureTextWidth(s.substring(from, mid)) <= width) lo = mid else hi = mid - 1
                }
                val space = s.lastIndexOf(' ', lo - 1)
                val to = if (space >= from && space + 1 < e) space + 1 else lo
                out += Line(from, to, false)
                from = to
            }
            out += Line(from, e, true)
            if (e >= s.length) return out
            a = e + 1
        }
    }

    fun lineOf(lines: List<Line>, i: Int): Int {
        for ((n, l) in lines.withIndex()) if (i < l.end || i == l.end && l.hard) return n
        return lines.lastIndex
    }

    private fun lineMove(down: Boolean, font: PolyFont?): Int {
        val ls = lines(font)
        val at = lineOf(ls, caret)
        val target = ls.getOrNull(if (down) at + 1 else at - 1) ?: return if (down) text.length else 0
        val cur = ls[at]
        if (font == null) return min(target.start + caret - cur.start, target.end)
        return indexInRange(target.start, target.end, font.measureTextWidth(text.substring(cur.start, caret)), font)
    }

    fun onKey(ev: KeyEvent, readClipboard: () -> String?, writeClipboard: (String) -> Unit, font: PolyFont? = null): Boolean {
        if (ev.type == KeyEventType.Preedit) {
            setPreedit(ev.text, ev.codepoint)
            return true
        }
        if (ev.type == KeyEventType.Char) {
            if (ev.codepoint < 0x20 || ev.codepoint == 0x7F || ev.shortcut && !ev.alt) return false
            setPreedit("", 0)
            edit(String(Character.toChars(ev.codepoint)), typed = true)
            return true
        }
        if (ev.type != KeyEventType.KeyDown) return false
        if (preedit.isNotEmpty()) return ev.key != KeyEvent.KEY_ESCAPE && ev.key != KeyEvent.KEY_TAB
        val shift = ev.shift
        val mac = ev.shortcut && KeyEvent.MAC
        when (ev.key) {
            KeyEvent.KEY_LEFT -> moveCaret(when {
                mac -> lineStart(caret)
                ev.word -> wordLeft(caret)
                hasSelection && !shift -> selectionStart
                else -> prevChar(caret)
            }, shift)
            KeyEvent.KEY_RIGHT -> moveCaret(when {
                mac -> lineEnd(caret)
                ev.word -> wordRight(caret)
                hasSelection && !shift -> selectionEnd
                else -> nextChar(caret)
            }, shift)
            KeyEvent.KEY_UP -> moveCaret(if (multiline && !mac) lineMove(false, font) else 0, shift)
            KeyEvent.KEY_DOWN -> moveCaret(if (multiline && !mac) lineMove(true, font) else text.length, shift)
            KeyEvent.KEY_HOME -> moveCaret(if (multiline && !ev.ctrl) lines(font).let { it[lineOf(it, caret)].start } else 0, shift)
            KeyEvent.KEY_END -> moveCaret(if (multiline && !ev.ctrl) lines(font).let { ls -> ls[lineOf(ls, caret)].let { if (it.hard) it.end else it.end - 1 } } else text.length, shift)
            KeyEvent.KEY_BACKSPACE -> backspace(ev.word)
            KeyEvent.KEY_DELETE -> delete(ev.word)
            KeyEvent.KEY_ENTER -> if (multiline) insert("\n") else return false
            else -> {
                if (!ev.shortcut) return false
                when (ev.key) {
                    KeyEvent.KEY_A -> selectAll()
                    KeyEvent.KEY_C -> if (hasSelection) writeClipboard(selectedText)
                    KeyEvent.KEY_X -> if (hasSelection) { writeClipboard(selectedText); insert("") }
                    KeyEvent.KEY_V -> readClipboard()?.let(::insert)
                    KeyEvent.KEY_Z -> if (shift) redo() else undo()
                    KeyEvent.KEY_Y -> redo()
                    else -> return false
                }
            }
        }
        return true
    }

    fun indexAt(localX: Float, font: PolyFont): Int = indexInLine(0, localX, font)

    fun indexInLine(start: Int, localX: Float, font: PolyFont): Int = indexInRange(start, lineEnd(start), localX, font)

    fun indexInRange(start: Int, end: Int, localX: Float, font: PolyFont): Int {
        if (localX <= 0f) return start
        var prev = 0f
        var i = start
        while (i < end) {
            val next = nextChar(i)
            val w = font.measureTextWidth(text.substring(start, next))
            if (localX < (prev + w) / 2f) return i
            prev = w
            i = next
        }
        return end
    }

    fun lineStartOf(line: Int): Int {
        var s = 0
        repeat(line) { s = text.indexOf('\n', s).takeIf { it >= 0 }?.plus(1) ?: return s }
        return s
    }

    companion object {
        @JvmStatic
        var focusListener: (TextFieldState, Boolean) -> Unit = { _, _ -> }
    }
}

@Composable
fun BasicTextField(
    state: TextFieldState,
    font: PolyFont,
    color: PolyColor,
    caretColor: PolyColor,
    selectionColor: PolyColor,
    readClipboard: () -> String?,
    writeClipboard: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    placeholderColor: PolyColor = color,
    placeholderFont: PolyFont = font,
    align: TextAlign = TextAlign.Start,
    softWrap: Boolean = false,
    onKey: ((KeyEvent) -> Boolean)? = null,
) {
    var caretOn by remember { mutableStateOf(true) }
    val focused = state.focused
    LaunchedEffect(focused, state.edits) {
        caretOn = true
        if (!focused) return@LaunchedEffect
        while (true) {
            delay(500)
            caretOn = !caretOn
        }
    }
    val m = font.metrics
    val lineHeight = m.descent - m.ascent
    val multi = state.multiline
    fun alignOffset(line: String, w: Float) = if (align == TextAlign.Start) 0f else max(0f, w - font.measureTextWidth(line)) * align.factor
    fun indexAt(e: PointerEvent): Int {
        val n = e.node
        val ls = state.lines(font)
        val l = ls[if (multi) ((e.y - n.y + state.scrollY) / lineHeight).toInt().coerceIn(0, ls.lastIndex) else 0]
        val x = e.x - n.x + state.scrollX - alignOffset(state.text.substring(l.start, l.end), n.width)
        return state.indexInRange(l.start, l.end, x, font)
    }
    Canvas(
        (if (multi) modifier else modifier.height(lineHeight.dp)).pointerHoverIcon(PointerIcon.Text)
            .focusable(request = if (state.focusRequested) ({ state.focusRequested = false; true }) else null) { state.focused = it }
            .onKeyEvent { ev -> onKey?.invoke(ev) == true || state.onKey(ev, readClipboard, writeClipboard, font) }
            .onPointerEvent { e ->
                when (e.type) {
                    PointerEventType.Press -> {
                        if (e.button != 0) return@onPointerEvent false
                        val i = indexAt(e)
                        when {
                            e.clicks == 2 -> state.wordAt(i).let { state.select(it.first, it.last) }
                            e.clicks >= 3 -> state.selectAll()
                            else -> state.moveCaret(i, extend = e.modifiers and KeyEvent.SHIFT != 0)
                        }
                        true
                    }
                    PointerEventType.Move -> {
                        state.moveCaret(indexAt(e), extend = true)
                        true
                    }
                    PointerEventType.Release -> true
                    PointerEventType.Scroll -> {
                        if (!multi) return@onPointerEvent false
                        val maxY = max(0f, state.lines(font).size * lineHeight - e.node.height)
                        val to = (state.scrollY - e.scroll * lineHeight * 3f).coerceIn(0f, maxY)
                        if (to == state.scrollY) return@onPointerEvent false
                        state.scrollY = to
                        true
                    }
                    else -> false
                }
            },
    ) { x, y, w, h ->
        state.wrapWidth = if (multi && softWrap) w else 0f
        val text = state.text
        val pre = state.preedit
        if (text.isEmpty() && pre.isEmpty() && !focused) {
            val pm = placeholderFont.metrics
            val py = if (multi) y else y + (h - (pm.descent - pm.ascent)) / 2f
            text(placeholder, x + alignOffset(placeholder, w), py - pm.ascent, placeholderColor, placeholderFont)
            return@Canvas
        }
        val shown = if (pre.isEmpty()) text else text.substring(0, state.caret) + pre + text.substring(state.caret)
        val caret = state.caret + state.preeditCaret
        val lines = state.lines(font, s = shown)
        val caretLine = state.lineOf(lines, caret)
        val caretStart = lines[caretLine].start
        fun str(l: TextFieldState.Line) = shown.substring(l.start, l.end)
        val caretX = font.measureTextWidth(shown.substring(caretStart, caret)) + alignOffset(str(lines[caretLine]), w)
        val widest = lines.maxOf { font.measureTextWidth(str(it)) }
        var sx = state.scrollX.coerceIn(0f, max(0f, widest - w + 1f))
        var sy = state.scrollY.coerceIn(0f, max(0f, lines.size * lineHeight - h))
        if (state.scrolledFor != state.edits) {
            state.scrolledFor = state.edits
            if (caretX - sx > w - 1f) sx = caretX - w + 1f
            if (caretX - sx < 0f) sx = caretX
            val top = caretLine * lineHeight
            if (top + lineHeight - sy > h) sy = top + lineHeight - h
            if (top - sy < 0f) sy = top
        }
        state.scrollX = sx
        state.scrollY = sy
        val top = if (multi) y else y + (h - lineHeight) / 2f
        val selStart = if (pre.isEmpty()) state.selectionStart else 0
        val selEnd = if (pre.isEmpty()) state.selectionEnd else 0
        val preEnd = state.caret + pre.length
        save()
        clipRect(x, y, w, h)
        for ((i, line) in lines.withIndex()) {
            val lineTop = top + i * lineHeight - sy
            val s = line.start
            val e = line.end
            val l = str(line)
            if (lineTop + lineHeight >= y && lineTop <= y + h) {
                val lx = x - sx + alignOffset(l, w)
                fun xAt(at: Int) = font.measureTextWidth(shown.substring(s, at.coerceIn(s, e)))
                if (selEnd > s && selStart <= e && selStart != selEnd) {
                    val a = xAt(selStart)
                    val b = xAt(selEnd) + if (selEnd > e && line.hard) lineHeight / 4f else 0f
                    rect(lx + a, lineTop, b - a, lineHeight, selectionColor)
                }
                text(l, lx, lineTop - m.ascent, color, font)
                if (pre.isNotEmpty() && preEnd > s && state.caret <= e) {
                    val a = xAt(state.caret)
                    rect(lx + a, lineTop - m.ascent + m.underlinePosition, xAt(preEnd) - a, max(1f, m.underlineThickness), color)
                }
            }
        }
        if (focused && caretOn) rect(x + caretX - sx, top + caretLine * lineHeight - sy, 1f, lineHeight, caretColor)
        restore()
    }
}
