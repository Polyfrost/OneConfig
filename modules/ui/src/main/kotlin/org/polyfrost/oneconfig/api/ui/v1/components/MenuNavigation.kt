@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import kotlinx.coroutines.delay
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import kotlin.math.abs
import kotlin.math.roundToInt

internal val LocalMenuLevel = staticCompositionLocalOf<MenuNavLevel?> { null }

private const val UNPLACED = Int.MIN_VALUE

internal class MenuNavItem {
    var label: String = ""
    var enabled: Boolean = true
    var hasSubmenu: Boolean = false
    var activate: () -> Unit = {}
    var top: Int = UNPLACED
    var left: Int = 0
    var seq: Int = 0
}

internal class MenuNavLevel(val controller: MenuNavController, val parent: MenuNavLevel?) {
    private val registered = ArrayList<MenuNavItem>()
    private var nextSeq = 0

    var highlighted by mutableStateOf<MenuNavItem?>(null)
    var openChild by mutableStateOf<MenuNavItem?>(null)
    var hoverIntent by mutableStateOf<MenuNavItem?>(null)
    var child: MenuNavLevel? = null

    fun register(item: MenuNavItem) {
        item.seq = nextSeq++
        registered += item
    }

    fun unregister(item: MenuNavItem) {
        registered -= item
        if (highlighted === item) highlighted = null
        if (openChild === item) openChild = null
        if (hoverIntent === item) hoverIntent = null
    }

    fun ordered(): List<MenuNavItem> =
        if (registered.all { it.top != UNPLACED }) registered.sortedWith(compareBy<MenuNavItem>({ it.top }, { it.left }, { it.seq }))
        else registered.sortedBy { it.seq }

    fun contains(level: MenuNavLevel): Boolean = generateSequence(level) { it.parent }.any { it === this }

    fun hover(item: MenuNavItem) {
        controller.active = this
        highlighted = item
        hoverIntent = item
        parent?.holdOpen()
    }

    fun unhover(item: MenuNavItem) {
        if (highlighted === item && openChild !== item) highlighted = null
    }

    private fun holdOpen() {
        val open = openChild ?: return
        highlighted = open
        hoverIntent = open
        parent?.holdOpen()
    }

    fun open(item: MenuNavItem, focusFirst: Boolean) {
        val shown = child
        if (openChild === item && shown != null) {
            if (focusFirst) shown.highlightFirst()
            return
        }
        controller.focusFirstOf = if (focusFirst) item else null
        highlighted = item
        hoverIntent = item
        openChild = item
        controller.active = this
    }

    fun toggle(item: MenuNavItem, focusFirst: Boolean) {
        if (openChild === item) closeChild() else open(item, focusFirst)
    }

    fun closeChild() {
        val open = openChild ?: return
        openChild = null
        highlighted = open
        hoverIntent = open
        controller.active = this
    }

    fun deepestOpen(): MenuNavLevel {
        val next = child
        return if (openChild != null && next != null) next.deepestOpen() else this
    }

    fun highlightFirst() {
        highlighted = ordered().firstOrNull()
        controller.active = this
    }

    fun select(item: MenuNavItem?) {
        controller.active = this
        if (item == null) return
        highlighted = item
        hoverIntent = null
        if (openChild != null && openChild !== item) openChild = null
    }
}

internal class MenuNavController {
    val root = MenuNavLevel(this, null)
    var active: MenuNavLevel = root
    var focusFirstOf: MenuNavItem? = null
    private val typed = StringBuilder()
    private var typedAt = 0L

    fun onKey(event: KeyEvent, rtl: Boolean): Boolean {
        typedChar(event)?.let { return typeAhead(it) }
        if (event.type != KeyEventType.KeyDown) return false
        if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return false
        val level = active
        val forward = if (rtl) Key.DirectionLeft else Key.DirectionRight
        val back = if (rtl) Key.DirectionRight else Key.DirectionLeft
        return when (event.key) {
            Key.DirectionDown -> stepRow(level, 1)
            Key.DirectionUp -> stepRow(level, -1)
            Key.MoveHome, Key.PageUp -> {
                level.select(level.ordered().firstOrNull())
                true
            }
            Key.MoveEnd, Key.PageDown -> {
                level.select(level.ordered().lastOrNull())
                true
            }
            Key.Enter, Key.NumPadEnter, Key.Spacebar -> activate(level)
            forward -> forward(level)
            back -> back(level)
            Key.Escape -> {
                val deepest = root.deepestOpen()
                val parent = deepest.parent ?: return false
                parent.closeChild()
                true
            }
            else -> false
        }
    }

    private fun rows(level: MenuNavLevel): List<List<MenuNavItem>> {
        val rows = ArrayList<MutableList<MenuNavItem>>()
        for (item in level.ordered()) {
            val last = rows.lastOrNull()
            if (last != null && item.top != UNPLACED && last.first().top == item.top) last += item else rows += mutableListOf(item)
        }
        return rows
    }

    private fun stepRow(level: MenuNavLevel, direction: Int): Boolean {
        val rows = rows(level)
        if (rows.isEmpty()) return true
        val current = level.highlighted
        val at = rows.indexOfFirst { row -> row.any { it === current } }
        if (at < 0) {
            level.select(if (direction > 0) rows.first().first() else rows.last().first())
            return true
        }
        val target = rows[(at + direction).mod(rows.size)]
        level.select(target.minByOrNull { abs(it.left - (current?.left ?: 0)) })
        return true
    }

    private fun stepInRow(level: MenuNavLevel, direction: Int): Boolean {
        val current = level.highlighted ?: return false
        val row = rows(level).firstOrNull { row -> row.any { it === current } } ?: return false
        val target = row.getOrNull(row.indexOf(current) + direction) ?: return false
        level.select(target)
        return true
    }

    private fun forward(level: MenuNavLevel): Boolean {
        if (stepInRow(level, 1)) return true
        val current = level.highlighted ?: return true
        if (current.hasSubmenu && current.enabled) level.open(current, true)
        return true
    }

    private fun back(level: MenuNavLevel): Boolean {
        if (stepInRow(level, -1)) return true
        level.parent?.closeChild()
        return true
    }

    private fun activate(level: MenuNavLevel): Boolean {
        val current = level.highlighted ?: return true
        if (!current.enabled) return true
        if (current.hasSubmenu) level.open(current, true) else current.activate()
        return true
    }

    private fun typedChar(event: KeyEvent): Char? {
        val native = event.awtEventOrNull ?: return null
        if (native.id != java.awt.event.KeyEvent.KEY_TYPED) return null
        val char = native.keyChar
        if (char == java.awt.event.KeyEvent.CHAR_UNDEFINED || char.isISOControl()) return null
        if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return null
        return char
    }

    private fun typeAhead(char: Char): Boolean {
        val now = System.currentTimeMillis()
        if (now - typedAt > MenuDefaults.TYPE_AHEAD_RESET_MILLIS) typed.setLength(0)
        if (typed.isEmpty() && char.isWhitespace()) return false
        typedAt = now
        typed.append(char)
        val level = active
        val items = level.ordered()
        if (items.isEmpty()) return true
        val repeated = typed.all { it.equals(typed[0], ignoreCase = true) }
        val prefix = if (repeated) typed[0].toString() else typed.toString()
        val at = items.indexOfFirst { it === level.highlighted }
        val start = if (repeated || at < 0) at + 1 else at
        val match = items.indices
            .map { items[(start + it).mod(items.size)] }
            .firstOrNull { it.label.trimStart().startsWith(prefix, ignoreCase = true) }
        if (match != null) level.select(match)
        return true
    }
}

@Composable
internal fun rememberMenuNavItem(
    level: MenuNavLevel?,
    label: String,
    enabled: Boolean,
    submenu: Boolean,
    activate: () -> Unit,
): MenuNavItem? {
    if (level == null) return null
    val item = remember(level) { MenuNavItem() }
    val latest by rememberUpdatedState(activate)
    SideEffect {
        item.label = label
        item.enabled = enabled
        item.hasSubmenu = submenu
        item.activate = { latest() }
    }
    DisposableEffect(level, item) {
        level.register(item)
        onDispose { level.unregister(item) }
    }
    return item
}

@Composable
internal fun MenuNavHover(level: MenuNavLevel?, item: MenuNavItem?, hovered: Boolean) {
    if (level == null || item == null) return
    val seen = remember(item) { booleanArrayOf(false) }
    LaunchedEffect(item, hovered) {
        if (hovered) level.hover(item) else if (seen[0]) level.unhover(item)
        seen[0] = hovered
    }
}

@Composable
internal fun MenuHoverIntentEffect(level: MenuNavLevel) {
    val intent = level.hoverIntent
    LaunchedEffect(level, intent) {
        if (intent == null || intent === level.openChild) return@LaunchedEffect
        delay(MenuDefaults.SUBMENU_DELAY_MILLIS)
        level.openChild = intent.takeIf { it.hasSubmenu && it.enabled }
    }
}

internal fun Modifier.menuNavPosition(item: MenuNavItem?): Modifier =
    if (item == null) this
    else onGloballyPositioned {
        val at = it.positionInWindow()
        item.top = at.y.roundToInt()
        item.left = at.x.roundToInt()
    }
