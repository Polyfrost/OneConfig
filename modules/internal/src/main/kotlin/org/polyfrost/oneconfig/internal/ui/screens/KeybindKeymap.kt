@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.api.ui.v1.components.Chip
import org.polyfrost.oneconfig.api.ui.v1.components.EscapeCloses
import org.polyfrost.oneconfig.api.ui.v1.components.IconButton
import org.polyfrost.oneconfig.api.ui.v1.components.Placement
import org.polyfrost.oneconfig.api.ui.v1.components.ScrollContainer
import org.polyfrost.oneconfig.api.ui.v1.components.SearchField
import org.polyfrost.oneconfig.api.ui.v1.components.rememberAnchoredPosition
import org.polyfrost.oneconfig.internal.ui.components.ShellPopup
import org.polyfrost.oneconfig.internal.ui.components.shellTransition
import java.awt.im.InputContext
import java.util.Locale
import java.util.ServiceLoader
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.components.ArrowDirection
import org.polyfrost.oneconfig.api.ui.v1.components.ArrowKey
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.Icon
import org.polyfrost.oneconfig.api.ui.v1.components.SectionHeader
import org.polyfrost.oneconfig.api.ui.v1.components.SectionHeaderDefaults
import org.polyfrost.oneconfig.api.ui.v1.components.KeyCap
import org.polyfrost.oneconfig.api.ui.v1.components.Select
import org.polyfrost.oneconfig.api.ui.v1.components.SurfaceKind
import org.polyfrost.oneconfig.api.ui.v1.components.Text
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipBox
import org.polyfrost.oneconfig.api.ui.v1.components.ControlDefaults
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.api.ui.v1.components.themedSurface
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.KeybindCodec
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeStyle
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.settings.KeybindConflicts
import org.polyfrost.oneconfig.internal.ui.components.settings.writeKeybind
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindEntry
import org.polyfrost.oneconfig.internal.ui.keybind.KeybindGroup

private val MAX_UNIT = 42.dp
private val MIN_UNIT = 26.dp
private val KEY_GAP = 4.dp
private val CASE_PADDING = 14.dp
private val LEGEND_PAD = 4.dp
private const val MAX_WORD_SP = 12f
private const val MIN_WORD_SP = 7f
private const val BLOCK_GAP = 0.6f
private const val KEYBOARD_HEIGHT = 6.25f
private const val MOUSE_WIDTH = 3.4f
private const val TOOLTIP_ROWS = 5

enum class KeyboardLayout(
    val titleKey: String,
    val fallback: String,
    val iso: Boolean,
    val numberRow: String,
    val topRow: String,
    val homeRow: String,
    val bottomRow: String,
) {
    UsQwerty("oneconfig.keymap.layout.us", "US QWERTY", false, "`1234567890-=", "QWERTYUIOP[]\\", "ASDFGHJKL;'", "ZXCVBNM,./"),
    UkQwerty("oneconfig.keymap.layout.uk", "UK QWERTY", true, "`1234567890-=", "QWERTYUIOP[]", "ASDFGHJKL;'#", "\\ZXCVBNM,./"),
    Azerty("oneconfig.keymap.layout.azerty", "French AZERTY", true, "²&é\"'(-è_çà)=", "AZERTYUIOP^$", "QSDFGHJKLMù*", "<WXCVBN,;:!"),
    Qwertz("oneconfig.keymap.layout.qwertz", "German QWERTZ", true, "^1234567890ß´", "QWERTZUIOPÜ+", "ASDFGHJKLÖÄ#", "<YXCVBNM,.-"),
    Dvorak("oneconfig.keymap.layout.dvorak", "Dvorak", false, "`1234567890[]", "',.PYFGCRL/=\\", "AOEUIDHTNS-", ";QJKXBMWVZ"),
    Colemak("oneconfig.keymap.layout.colemak", "Colemak", false, "`1234567890-=", "QWFPGJLUY;[]\\", "ARSTDHNEIO'", "ZXCVBKM,./");

    val title: String get() = translated(titleKey, fallback)
}

private object KeymapStore {
    var layout by mutableStateOf<KeyboardLayout?>(null)
    var selected by mutableStateOf<String?>(null)
    val detected: KeyboardLayout by lazy { detectLayout() }
}

private class KeySpec(
    val name: String,
    val label: String,
    val x: Float,
    val y: Float,
    val w: Float = 1f,
    val h: Float = 1f,
    val arrow: ArrowDirection? = null,
) {
    val id: String get() = "key:$name"
}

private class KeyBlock(val width: Float, val keys: List<KeySpec>)

private class KeyBinding(val group: KeybindGroup, val entry: KeybindEntry, val bind: OneConfigKeybind) {
    val title: String get() = KeybindConflicts.displayName(entry.prop)
    val modTitle: String get() = group.modTitle.asRenderText()
    val singleKey: Boolean get() = entry.prop.getMetadata<Boolean>("singleKey") == true
}

private object KeymapCodec {
    private val codec: KeybindCodec? by lazy {
        runCatching {
            ServiceLoader.load(KeybindCodec::class.java, KeybindCodec::class.java.classLoader).firstOrNull()
        }.getOrNull()
    }

    fun key(name: String): Int? = runCatching { codec?.keyCode("key.keyboard.$name") }.getOrNull()

    fun mouse(name: String): Int? = runCatching { codec?.mouseButton("key.mouse.$name") }.getOrNull()
}

private fun detectLayout(): KeyboardLayout {
    fun printed(position: String): String? = KeymapCodec.key(position)
        ?.let { runCatching { OneConfigKeybind.keyName(it) }.getOrNull() }
        ?.trim()?.uppercase()
    when {
        printed("q") == "A" -> return KeyboardLayout.Azerty
        printed("y") == "Z" -> return KeyboardLayout.Qwertz
        printed("e") == "." -> return KeyboardLayout.Dvorak
        printed("s") == "R" -> return KeyboardLayout.Colemak
    }
    val locale = inputLocale()
    return when {
        locale.language == "fr" && locale.country != "CA" -> KeyboardLayout.Azerty
        locale.language == "de" || locale.country in setOf("DE", "AT", "CH") -> KeyboardLayout.Qwertz
        locale.country in setOf("GB", "IE") -> KeyboardLayout.UkQwerty
        else -> KeyboardLayout.UsQwerty
    }
}

private fun inputLocale(): Locale {
    val mac = System.getProperty("os.name").orEmpty().lowercase().contains("mac")
    if (mac) return Locale.getDefault()
    return runCatching { InputContext.getInstance()?.locale }.getOrNull() ?: Locale.getDefault()
}

private class RowBuilder(private val y: Float, private val keys: MutableList<KeySpec>) {
    private var x = 0f

    fun key(name: String, label: String, w: Float = 1f, h: Float = 1f) {
        keys += KeySpec(name, label, x, y, w, h)
        x += w
    }

    fun printed(names: List<String>, labels: String) {
        names.forEachIndexed { i, name -> key(name, labels.getOrNull(i)?.toString() ?: name) }
    }

    fun gap(w: Float) {
        x += w
    }
}

private val NUMBER_ROW = listOf("grave.accent", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "minus", "equal")
private val TOP_ROW = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "left.bracket", "right.bracket")
private val HOME_ROW = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "semicolon", "apostrophe")
private val BOTTOM_ROW = listOf("z", "x", "c", "v", "b", "n", "m", "comma", "period", "slash")

private fun block(width: Float, build: (MutableList<KeySpec>) -> Unit): KeyBlock {
    val keys = ArrayList<KeySpec>()
    build(keys)
    return KeyBlock(width, keys)
}

private fun mainBlock(layout: KeyboardLayout): KeyBlock = block(15f) { keys ->
    RowBuilder(0f, keys).apply {
        key("escape", "Esc"); gap(1f)
        for (i in 1..4) key("f$i", "F$i"); gap(0.5f)
        for (i in 5..8) key("f$i", "F$i"); gap(0.5f)
        for (i in 9..12) key("f$i", "F$i")
    }
    RowBuilder(1.25f, keys).apply {
        printed(NUMBER_ROW, layout.numberRow)
        key("backspace", "Backspace", 2f)
    }
    if (layout.iso) {
        RowBuilder(2.25f, keys).apply {
            key("tab", "Tab", 1.5f)
            printed(TOP_ROW, layout.topRow)
            gap(0.25f)
            key("enter", "Enter", 1.25f, 2f)
        }
        RowBuilder(3.25f, keys).apply {
            key("caps.lock", "Caps Lock", 1.75f)
            printed(HOME_ROW + "backslash", layout.homeRow)
        }
        RowBuilder(4.25f, keys).apply {
            key("left.shift", "Shift", 1.25f)
            printed(listOf("world.2") + BOTTOM_ROW, layout.bottomRow)
            key("right.shift", "Shift", 2.75f)
        }
    } else {
        RowBuilder(2.25f, keys).apply {
            key("tab", "Tab", 1.5f)
            printed(TOP_ROW, layout.topRow)
            key("backslash", layout.topRow.getOrNull(12)?.toString() ?: "\\", 1.5f)
        }
        RowBuilder(3.25f, keys).apply {
            key("caps.lock", "Caps Lock", 1.75f)
            printed(HOME_ROW, layout.homeRow)
            key("enter", "Enter", 2.25f)
        }
        RowBuilder(4.25f, keys).apply {
            key("left.shift", "Shift", 2.25f)
            printed(BOTTOM_ROW, layout.bottomRow)
            key("right.shift", "Shift", 2.75f)
        }
    }
    RowBuilder(5.25f, keys).apply {
        key("left.control", "Ctrl", 1.25f); key("left.win", "Win", 1.25f); key("left.alt", "Alt", 1.25f)
        key("space", "", 6.25f)
        key("right.alt", "Alt", 1.25f); key("right.win", "Win", 1.25f); key("menu", "Menu", 1.25f); key("right.control", "Ctrl", 1.25f)
    }
}

private val NAV_BLOCK = block(3f) { keys ->
    RowBuilder(0f, keys).apply { key("print.screen", "PrtSc"); key("scroll.lock", "ScrLk"); key("pause", "Pause") }
    RowBuilder(1.25f, keys).apply { key("insert", "Ins"); key("home", "Home"); key("page.up", "PgUp") }
    RowBuilder(2.25f, keys).apply { key("delete", "Del"); key("end", "End"); key("page.down", "PgDn") }
    keys += KeySpec("up", "Up", 1f, 4.25f, arrow = ArrowDirection.Up)
    keys += KeySpec("left", "Left", 0f, 5.25f, arrow = ArrowDirection.Left)
    keys += KeySpec("down", "Down", 1f, 5.25f, arrow = ArrowDirection.Down)
    keys += KeySpec("right", "Right", 2f, 5.25f, arrow = ArrowDirection.Right)
}

private val NUMPAD_BLOCK = block(4f) { keys ->
    RowBuilder(1.25f, keys).apply {
        key("num.lock", "Num"); key("keypad.divide", "/"); key("keypad.multiply", "*"); key("keypad.subtract", "-")
    }
    RowBuilder(2.25f, keys).apply { key("keypad.7", "7"); key("keypad.8", "8"); key("keypad.9", "9") }
    RowBuilder(3.25f, keys).apply { key("keypad.4", "4"); key("keypad.5", "5"); key("keypad.6", "6") }
    RowBuilder(4.25f, keys).apply { key("keypad.1", "1"); key("keypad.2", "2"); key("keypad.3", "3") }
    keys += KeySpec("keypad.add", "+", 3f, 2.25f, h = 2f)
    keys += KeySpec("keypad.enter", "Enter", 3f, 4.25f, h = 2f)
    keys += KeySpec("keypad.0", "0", 0f, 5.25f, w = 2f)
    keys += KeySpec("keypad.decimal", ".", 2f, 5.25f)
}

private fun blockSets(layout: KeyboardLayout): List<List<KeyBlock>> {
    val main = mainBlock(layout)
    return listOf(listOf(main, NAV_BLOCK, NUMPAD_BLOCK), listOf(main, NAV_BLOCK), listOf(main))
}

private fun allKeys(layout: KeyboardLayout): List<KeySpec> = mainBlock(layout).keys + NAV_BLOCK.keys + NUMPAD_BLOCK.keys

private fun List<KeyBlock>.totalWidth(): Float = sumOf { it.width.toDouble() }.toFloat() + BLOCK_GAP * (size - 1)

private class KeymapIndex(val keys: Map<Int, List<KeyBinding>>, val mouse: Map<Int, List<KeyBinding>>)

private fun buildIndex(groups: List<KeybindGroup>): KeymapIndex {
    val keys = HashMap<Int, MutableList<KeyBinding>>()
    val mouse = HashMap<Int, MutableList<KeyBinding>>()
    for (group in groups) {
        for (entry in group.entries) {
            val bind = entry.prop.get() as? OneConfigKeybind ?: continue
            val binding = KeyBinding(group, entry, bind)
            bind.keyCodes?.forEach { if (it > 0) keys.getOrPut(it) { ArrayList() } += binding }
            bind.mouseBtns?.forEach { if (it >= 0) mouse.getOrPut(it) { ArrayList() } += binding }
        }
    }
    return KeymapIndex(keys, mouse)
}

private class KeyTarget(val id: String, val label: String, val code: Int?, val mouse: Boolean, val bindings: List<KeyBinding>)

private enum class KeyState { Unbound, Bound, Conflict, Unavailable }

private fun KeyTarget.state(conflicting: Set<Any>): KeyState = when {
    code == null -> KeyState.Unavailable
    bindings.any { it.entry.prop in conflicting } -> KeyState.Conflict
    bindings.isNotEmpty() -> KeyState.Bound
    else -> KeyState.Unbound
}

@Composable
private fun stateColor(state: KeyState): Color? = when (state) {
    KeyState.Bound -> OneConfigTheme.accent
    KeyState.Conflict -> OneConfigTheme.current.error
    else -> null
}

@Composable
private fun ledShape(): Shape = OneConfigTheme.current.shapes.circle

@Composable
fun KeybindKeymap(groups: List<KeybindGroup>, allGroups: List<KeybindGroup>, modifier: Modifier = Modifier) {
    val conflictRevision = KeybindConflicts.revision.intValue
    val index = remember(groups, conflictRevision) { buildIndex(groups) }
    val conflicting: Set<Any> = KeybindConflicts.conflictingProps()
    val layout = KeymapStore.layout ?: KeymapStore.detected

    fun target(spec: KeySpec): KeyTarget {
        val code = KeymapCodec.key(spec.name)
        return KeyTarget(spec.id, spec.label, code, false, code?.let { index.keys[it] }.orEmpty())
    }

    fun mouseTarget(name: String, label: String): KeyTarget {
        val code = KeymapCodec.mouse(name)
        return KeyTarget("mouse:$name", label, code, true, code?.let { index.mouse[it] }.orEmpty())
    }

    val mouseTargets = listOf(
        mouseTarget("left", translated("oneconfig.keymap.mouse.left", "Left click")),
        mouseTarget("right", translated("oneconfig.keymap.mouse.right", "Right click")),
        mouseTarget("middle", translated("oneconfig.keymap.mouse.middle", "Middle click")),
        mouseTarget("4", translated("oneconfig.keymap.mouse.back", "Back button")),
        mouseTarget("5", translated("oneconfig.keymap.mouse.forward", "Forward button")),
    )

    val wordFit = remember { WordFit(MAX_WORD_SP) }
    CompositionLocalProvider(LocalKeymapGroups provides allGroups, LocalWordFit provides wordFit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KeymapHeader()
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val available = maxWidth - CASE_PADDING * 4
            val sets = blockSets(layout)
            val unitFor = { blocks: List<KeyBlock> -> available / (blocks.totalWidth() + MOUSE_WIDTH + BLOCK_GAP * 2) }
            val chosen = sets.firstOrNull { unitFor(it) >= MIN_UNIT } ?: sets.last()
            val unit = min(MAX_UNIT, unitFor(chosen))
            LaunchedEffect(unit) { wordFit.sp = MAX_WORD_SP }
            Row(horizontalArrangement = Arrangement.spacedBy(unit * BLOCK_GAP)) {
                Box(
                    Modifier
                        .themedSurface(SurfaceKind.Card)
                        .padding(CASE_PADDING)
                        .size(unit * chosen.totalWidth(), unit * KEYBOARD_HEIGHT - KEY_GAP),
                ) {
                    var left = 0f
                    for (block in chosen) {
                        for (spec in block.keys) {
                            val target = target(spec)
                            PlacedKey(spec, target, target.state(conflicting), unit, left)
                        }
                        left += block.width + BLOCK_GAP
                    }
                }
                Box(
                    Modifier
                        .themedSurface(SurfaceKind.Card)
                        .padding(CASE_PADDING)
                        .size(unit * MOUSE_WIDTH, unit * KEYBOARD_HEIGHT - KEY_GAP),
                    contentAlignment = Alignment.Center,
                ) {
                    MouseBody(mouseTargets, conflicting, unit)
                }
            }
        }
        KeymapFooter()
    }
    }
}

@Composable
private fun KeymapHeader() {
    val auto = translated("oneconfig.keymap.layout.auto", "Auto-detect (%s)", KeymapStore.detected.title)
    SectionHeader(
        translated("oneconfig.keymap.title", "Keyboard and mouse"),
        icon = "keyboard",
        contentPadding = PaddingValues(),
        textStyle = SectionHeaderDefaults.labelStyle,
        color = OneConfigTheme.current.textColorSecondary,
        uppercase = false,
        iconSize = 18.dp,
    ) {
        Select(
            selected = KeymapStore.layout,
            options = listOf<KeyboardLayout?>(null) + KeyboardLayout.entries,
            onSelect = { KeymapStore.layout = it },
            modifier = Modifier.width(220.dp),
            label = { it?.title ?: auto },
            size = ControlSize.Compact,
        )
    }
}

@Composable
private fun KeymapFooter() {
    val theme = OneConfigTheme.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(OneConfigTheme.accent, translated("oneconfig.keymap.legend.bound", "In use"))
        LegendItem(theme.error, translated("oneconfig.keymap.legend.conflict", "Clashing"))
        LegendItem(lerp(theme.componentBackground, theme.textColor, 0.2f), translated("oneconfig.keymap.legend.free", "Free"))
        Spacer(Modifier.weight(1f))
        Text(
            translated("oneconfig.keymap.hint", "Hover a key to see what it does. Click it to change it."),
            color = theme.textColorSecondary,
            style = theme.typography.caption,
            maxLines = 1,
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    val theme = OneConfigTheme.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(7.dp).background(color, ledShape()))
        Text(label, color = theme.textColorSecondary, style = theme.typography.caption, maxLines = 1)
    }
}

@Composable
private fun hoverTint(base: Color?, hovered: Boolean): Color? {
    if (!hovered) return base
    val text = OneConfigTheme.current.textColor
    return if (base == null) text else lerp(base, text, 0.3f)
}

private class WordFit(start: Float) {
    var sp by mutableFloatStateOf(start)
}

private val LocalWordFit = staticCompositionLocalOf { WordFit(11f) }

private val LocalKeymapGroups = staticCompositionLocalOf<List<KeybindGroup>> { emptyList() }

@Composable
private fun PlacedKey(spec: KeySpec, target: KeyTarget, state: KeyState, unit: Dp, originX: Float) {
    val selected = KeymapStore.selected == target.id
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val tint = hoverTint(stateColor(state), (hovered || selected) && state != KeyState.Unavailable)
    val width = unit * spec.w - KEY_GAP
    val height = unit * spec.h - KEY_GAP
    Box(Modifier.offset(unit * (originX + spec.x), unit * spec.y)) {
        if (selected) SelectionRing(width, height)
        KeyTooltip(target, state, enabled = !selected) {
            val click = if (state == KeyState.Unavailable) Modifier.alpha(0.35f)
            else Modifier.oneConfigClickable(
                target.label,
                { KeymapStore.selected = if (selected) null else target.id },
                indication = false,
                interactionSource = source,
            )
            if (spec.arrow != null) {
                ArrowKey(spec.arrow, click, size = width, tint = tint)
            } else {
                KeyCap(click, width = width, height = height, tint = tint) {
                    KeyLegend(spec, target.label, state)
                }
            }
        }
        if (selected) BindPopover(target, state)
    }
}

@Composable
private fun SelectionRing(width: Dp, height: Dp) {
    val theme = OneConfigTheme.current
    val shape = theme.shapes.medium
    Box(
        Modifier
            .offset((-3).dp, (-3).dp)
            .size(width + 6.dp, height + 6.dp)
            .border(1.5.dp, OneConfigTheme.accent, shape),
    )
}

@Composable
private fun KeyLegend(spec: KeySpec, label: String, state: KeyState) {
    val theme = OneConfigTheme.current
    val word = label.length > 1
    val base = theme.typography.captionMedium
    val fit = LocalWordFit.current
    Box(
        Modifier.fillMaxSize().padding(horizontal = LEGEND_PAD),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (state == KeyState.Unbound) theme.textColor.copy(alpha = 0.7f) else theme.textColor,
            style = if (word) base.copy(fontSize = fit.sp.sp) else base,
            maxLines = 1,
            softWrap = false,
            onTextLayout = if (word) {
                { result -> if (result.didOverflowWidth && fit.sp > MIN_WORD_SP) fit.sp -= 0.5f }
            } else null,
        )
    }
}

@Composable
private fun KeyTooltip(target: KeyTarget, state: KeyState, enabled: Boolean = true, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    TooltipBox(tooltip = { KeyTooltipCard(target, state) }, maxWidth = 360.dp, content = content)
}

@Composable
private fun stateText(target: KeyTarget, state: KeyState): String = when (state) {
    KeyState.Unavailable -> translated("oneconfig.keymap.unavailable", "Minecraft can't use this key")
    KeyState.Unbound -> translated("oneconfig.keymap.unbound", "Nothing uses this key yet")
    KeyState.Conflict -> translated("oneconfig.keymap.tooltip.conflict", "Some of these clash")
    KeyState.Bound -> if (target.bindings.size == 1) translated("oneconfig.keymap.bound_one", "Used by 1 keybind")
    else translated("oneconfig.keymap.bound_many", "Used by %s keybinds", target.bindings.size.toString())
}

@Composable
private fun KeyTooltipCard(target: KeyTarget, state: KeyState) {
    val theme = OneConfigTheme.current
    val conflicting: Set<Any> = KeybindConflicts.conflictingProps()
    Column(Modifier.widthIn(min = 200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KeyCap(target.label.ifEmpty { translated("oneconfig.keymap.space", "Space") }, tint = stateColor(state))
            Text(
                stateText(target, state),
                color = if (state == KeyState.Conflict) theme.error else theme.textColor,
                style = theme.typography.smallMedium,
            )
        }
        if (target.bindings.isNotEmpty()) {
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(theme.borderColor.copy(alpha = 0.6f)))
            target.bindings.take(TOOLTIP_ROWS).forEach { binding ->
                BindingRow(binding, binding.entry.prop in conflicting, compact = true)
            }
            if (target.bindings.size > TOOLTIP_ROWS) {
                Text(
                    translated("oneconfig.keymap.more", "and %s more", (target.bindings.size - TOOLTIP_ROWS).toString()),
                    color = theme.textColorSecondary,
                    style = theme.typography.caption,
                )
            }
        }
        if (state != KeyState.Unavailable) {
            Text(
                if (target.bindings.isEmpty()) translated("oneconfig.keymap.click_free", "Click to bind something to it")
                else translated("oneconfig.keymap.click", "Click to change it"),
                color = theme.textColorSecondary,
                style = theme.typography.caption,
            )
        }
    }
}

@Composable
private fun ModIcon(binding: KeyBinding) {
    val theme = OneConfigTheme.current
    val icon = binding.group.modIcon
    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
        if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = theme.textColorSecondary)
        else Box(Modifier.size(6.dp).background(theme.textColorSecondary, ledShape()))
    }
}

@Composable
private fun ComboCaps(bind: OneConfigKeybind, height: Dp, dim: Boolean = false) {
    Row(
        Modifier.alpha(if (dim) 0.6f else 1f),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bind.displayName().split(" + ").forEach { KeyCap(it, height = height) }
    }
}

@Composable
private fun BindingRow(binding: KeyBinding, conflict: Boolean, compact: Boolean, trailing: (@Composable () -> Unit)? = null) {
    val theme = OneConfigTheme.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModIcon(binding)
        Column(Modifier.weight(1f)) {
            Text(binding.title, color = theme.textColor, style = theme.typography.small, maxLines = 1)
            Text(
                if (conflict) translated("oneconfig.keymap.clash", "%s, clashes", binding.modTitle) else binding.modTitle,
                color = if (conflict) theme.error else theme.textColorSecondary,
                style = theme.typography.caption,
                maxLines = 1,
            )
        }
        ComboCaps(binding.bind, if (compact) 19.dp else 21.dp)
        trailing?.invoke()
    }
}

@Composable
private fun MouseBody(targets: List<KeyTarget>, conflicting: Set<Any>, unit: Dp) {
    val theme = OneConfigTheme.current
    val pixel = theme.style == ThemeStyle.Pixel
    val bodyW = unit * 2.4f
    val bodyH = unit * 4.2f
    val face = lerp(theme.componentBackground, theme.textColor, 0.06f).copy(alpha = 1f)
    val rim = lerp(theme.componentBackground, theme.textColor, 0.2f).copy(alpha = 1f)
    val (left, right, middle, back, forward) = targets
    val split = bodyH * 0.42f
    val corner = if (pixel) 0.dp else bodyW / 2
    val groove = 1.5.dp
    val sideW = unit * 0.26f
    val sideH = unit * 0.62f
    val leftShape = remember(corner) { RoundedCornerShape(topStart = corner) }
    val rightShape = remember(corner) { RoundedCornerShape(topEnd = corner) }
    val wheelShape = theme.shapes.pill
    val sideShape = theme.shapes.pill
    Box(Modifier.size(bodyW + sideW, bodyH)) {
        Box(Modifier.offset(sideW / 2, 0.dp).size(bodyW, bodyH)) {
            Canvas(Modifier.size(bodyW, bodyH)) {
                val r = if (pixel) 0f else size.width / 2f
                drawRoundRect(rim, Offset.Zero, size, CornerRadius(r))
                val inset = 1.dp.toPx()
                drawRoundRect(
                    face,
                    Offset(inset, inset),
                    Size(size.width - inset * 2, size.height - inset * 3),
                    CornerRadius((r - inset).coerceAtLeast(0f)),
                )
            }
            MouseButton(left, conflicting, Offset.Zero, bodyW / 2 - groove / 2, split, leftShape)
            MouseButton(right, conflicting, Offset((bodyW / 2 + groove / 2).value, 0f), bodyW / 2 - groove / 2, split, rightShape)
            MouseButton(
                middle,
                conflicting,
                Offset((bodyW / 2 - unit * 0.15f).value, (split * 0.2f).value),
                unit * 0.3f,
                split * 0.42f,
                wheelShape,
            )
        }
        MouseButton(forward, conflicting, Offset(0f, (bodyH * 0.5f).value), sideW, sideH, sideShape)
        MouseButton(back, conflicting, Offset(0f, (bodyH * 0.5f + sideH + 3.dp).value), sideW, sideH, sideShape)
    }
}

@Composable
private fun MouseButton(target: KeyTarget, conflicting: Set<Any>, at: Offset, width: Dp, height: Dp, shape: Shape) {
    val theme = OneConfigTheme.current
    val state = target.state(conflicting)
    val selected = KeymapStore.selected == target.id
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val tint = hoverTint(stateColor(state), (hovered || selected) && state != KeyState.Unavailable)
    val baseFace = lerp(theme.componentBackground, theme.textColor, 0.06f).copy(alpha = 1f)
    val baseRim = lerp(theme.componentBackground, theme.textColor, 0.2f).copy(alpha = 1f)
    val face = tint?.let { lerp(baseFace, it, 0.16f) } ?: baseFace
    val rim = tint?.let { lerp(baseRim, it, 0.55f) } ?: baseRim
    Box(Modifier.offset(at.x.dp, at.y.dp)) {
        KeyTooltip(target, state, enabled = !selected) {
            Box(
                Modifier
                    .size(width, height)
                    .background(face, shape)
                    .border(if (selected) 1.5.dp else 1.dp, if (selected) OneConfigTheme.accent else rim, shape)
                    .then(
                        if (state == KeyState.Unavailable) Modifier.alpha(0.35f)
                        else Modifier.oneConfigClickable(
                            target.label,
                            { KeymapStore.selected = if (selected) null else target.id },
                            shape = shape,
                            indication = false,
                            interactionSource = source,
                        )
                    ),
            )
        }
        if (selected) BindPopover(target, state)
    }
}

private fun modsOf(ctrl: Boolean, shift: Boolean, alt: Boolean): Byte {
    var mods = 0
    if (ctrl) mods = mods or KeyModifiers.CTRL.toInt()
    if (shift) mods = mods or KeyModifiers.SHIFT.toInt()
    if (alt) mods = mods or KeyModifiers.ALT.toInt()
    return mods.toByte()
}

@Composable
private fun BindPopover(target: KeyTarget, state: KeyState) {
    val theme = OneConfigTheme.current
    val conflicting: Set<Any> = KeybindConflicts.conflictingProps()
    val groups = LocalKeymapGroups.current
    val code = target.code ?: return
    val label = target.label.ifEmpty { translated("oneconfig.keymap.space", "Space") }
    val close = { KeymapStore.selected = null }
    var query by remember { mutableStateOf("") }
    var ctrl by remember { mutableStateOf(false) }
    var shift by remember { mutableStateOf(false) }
    var alt by remember { mutableStateOf(false) }
    val revision = KeybindConflicts.revision.intValue
    val candidates = remember(groups, revision) {
        groups.flatMap { group ->
            group.entries.mapNotNull { entry ->
                (entry.prop.get() as? OneConfigKeybind)?.let { KeyBinding(group, entry, it) }
            }
        }
    }
    val boundHere = target.bindings.map { it.entry.prop }.toSet()
    val results = remember(candidates, query, boundHere) {
        val q = query.trim().lowercase()
        candidates
            .filter { it.entry.prop !in boundHere }
            .filter { q.isEmpty() || it.title.lowercase().contains(q) || it.modTitle.lowercase().contains(q) }
            .sortedWith(compareBy({ it.bind.isBound }, { it.modTitle }, { it.title }))
    }
    EscapeCloses(close)
    val position = rememberAnchoredPosition(Placement.BelowCenter, 10.dp)
    ShellPopup(position, close, PopupProperties(focusable = true)) {
        Column(
            Modifier
                .width(380.dp)
                .shellTransition()
                .themedSurface(SurfaceKind.Popup)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KeyCap(label, tint = stateColor(state))
                Text(
                    stateText(target, state),
                    Modifier.weight(1f),
                    color = if (state == KeyState.Conflict) theme.error else theme.textColor,
                    style = theme.typography.smallMedium,
                    maxLines = 1,
                )
                IconButton("close", translated("oneconfig.keymap.close", "Close"), close)
            }
            target.bindings.forEach { binding ->
                BindingRow(binding, binding.entry.prop in conflicting, compact = true) {
                    IconButton(
                        "trash",
                        translated("oneconfig.keymap.unbind", "Unbind"),
                        { writeKeybind(binding.entry.prop, null, null, KeyModifiers.NONE) },
                    )
                }
            }
            if (target.bindings.isNotEmpty()) {
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(theme.borderColor.copy(alpha = 0.6f)))
            }
            Text(
                translated("oneconfig.keymap.add", "Bind something to %s", label),
                color = theme.textColorSecondary,
                style = theme.typography.captionMedium,
            )
            SearchField(
                query,
                { query = it },
                Modifier.fillMaxWidth(),
                placeholder = translated("oneconfig.keymap.search", "Search keybinds"),
            )
            if (!target.mouse) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        translated("oneconfig.keymap.hold", "Hold"),
                        color = theme.textColorSecondary,
                        style = theme.typography.caption,
                    )
                    Chip("Ctrl", ctrl, { ctrl = !ctrl })
                    Chip("Shift", shift, { shift = !shift })
                    Chip("Alt", alt, { alt = !alt })
                }
            }
            val listState = rememberLazyListState()
            ScrollContainer(listState, Modifier.fillMaxWidth().heightIn(max = 240.dp), scrollbar = false, smoothWheel = false) {
                LazyColumn(Modifier.fillMaxWidth(), state = listState) {
                    if (results.isEmpty()) {
                        item {
                            Text(
                                translated("oneconfig.keymap.no_results", "No keybinds match"),
                                Modifier.padding(vertical = 8.dp),
                                color = theme.textColorSecondary,
                                style = theme.typography.caption,
                            )
                        }
                    }
                    items(results, key = { System.identityHashCode(it.entry.prop) }) { candidate ->
                        val blocked = (ctrl || shift || alt) && !target.mouse && candidate.singleKey
                        BindResultRow(candidate, blocked) {
                            val mods = if (target.mouse) KeyModifiers.NONE else modsOf(ctrl, shift, alt)
                            if (target.mouse) writeKeybind(candidate.entry.prop, null, intArrayOf(code), mods)
                            else writeKeybind(candidate.entry.prop, intArrayOf(code), null, mods)
                            query = ""
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BindResultRow(candidate: KeyBinding, blocked: Boolean, onPick: () -> Unit) {
    val theme = OneConfigTheme.current
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (blocked) 0.45f else 1f)
            .oneConfigClickable(candidate.title, onPick, enabled = !blocked, recipe = ControlDefaults.Media)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ModIcon(candidate)
        Column(Modifier.weight(1f)) {
            Text(candidate.title, color = theme.textColor, style = theme.typography.small, maxLines = 1)
            Text(
                if (blocked) translated("oneconfig.keymap.single_key", "%s, single keys only", candidate.modTitle) else candidate.modTitle,
                color = theme.textColorSecondary,
                style = theme.typography.caption,
                maxLines = 1,
            )
        }
        if (candidate.bind.isBound) {
            ComboCaps(candidate.bind, 19.dp, dim = true)
        } else {
            Text(
                translated("oneconfig.keymap.not_bound", "Not bound"),
                color = theme.textColorSecondary,
                style = theme.typography.caption,
            )
        }
    }
}
