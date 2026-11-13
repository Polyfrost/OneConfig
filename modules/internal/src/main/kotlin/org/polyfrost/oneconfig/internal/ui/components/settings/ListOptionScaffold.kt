package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import java.util.ArrayDeque
import kotlin.math.roundToInt
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.IconButton
import org.polyfrost.oneconfig.api.ui.v1.components.MenuAction
import org.polyfrost.oneconfig.api.ui.v1.components.MenuEntry
import org.polyfrost.oneconfig.api.ui.v1.components.MenuSeparator
import org.polyfrost.oneconfig.api.ui.v1.components.ScrollContainer
import org.polyfrost.oneconfig.api.ui.v1.components.TextButton
import org.polyfrost.oneconfig.api.ui.v1.components.contextMenu
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.animatedFrame
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.containVerticalScroll
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.components.wheelScroll
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric

/**
 * Shared machinery for the user-editable drag-reorderable list options
 *
 * Each option supplies the contents of a single row while adding removing reordering scrolling and the
 * surrounding chrome live here
 */

internal val ListEntryHeight = 34.dp
internal val ListEntrySpacing = 8.dp

private val ContainerPadding = 8.dp
private val ContainerShape @Composable get() = LocalTheme.current.modCardShape
private val EntryShape @Composable get() = ContainerShape.concentric(ContainerPadding)

internal data class ListRowEntry<T>(val id: Int, val value: T)

/**
 * Holds the working copy of the list
 *
 * Every mutation rewrites the state and immediately reports the new values through [onCommit] where the
 * option writes back to its property
 */
internal class ListEntriesState<T>(initial: List<T>, private val onCommit: (List<T>) -> Unit) {
    private var nextId = 0

    var entries by mutableStateOf(initial.map { ListRowEntry(nextId++, it) })
        private set

    val values: List<T> get() = entries.map { it.value }

    val size: Int get() = entries.size

    private var insertAt = -1

    fun add(value: T) {
        val at = insertAt
        if (at in 0..entries.size) {
            insertAt = at + 1
            val list = entries.toMutableList()
            list.add(at, ListRowEntry(nextId++, value))
            apply(list)
        } else {
            apply(entries + ListRowEntry(nextId++, value))
        }
    }

    fun update(id: Int, value: T) = apply(entries.map { if (it.id == id) it.copy(value = value) else it })

    fun remove(id: Int) {
        insertAt = -1
        apply(entries.filterNot { it.id == id })
    }

    fun move(from: Int, to: Int) {
        insertAt = -1
        val list = entries.toMutableList()
        list.add(to, list.removeAt(from))
        apply(list)
    }

    fun indexOf(id: Int): Int = entries.indexOfFirst { it.id == id }

    fun moveBy(id: Int, delta: Int): Boolean {
        val from = indexOf(id)
        val to = from + delta
        if (from < 0 || to !in entries.indices) return false
        move(from, to)
        return true
    }

    fun duplicate(id: Int) {
        val index = indexOf(id)
        if (index < 0) return
        val list = entries.toMutableList()
        list.add(index + 1, ListRowEntry(nextId++, entries[index].value))
        apply(list)
    }

    fun addAtEnd(onAdd: () -> Unit) {
        insertAt = -1
        onAdd()
    }

    fun insertAbove(id: Int, onAdd: () -> Unit) {
        val index = indexOf(id)
        if (index < 0) return
        val before = entries.size
        insertAt = index
        onAdd()
        if (entries.size != before) insertAt = -1
    }

    fun replace(values: List<T>) {
        insertAt = -1
        val existing = HashMap<T, ArrayDeque<ListRowEntry<T>>>()
        entries.forEach { entry ->
            existing.getOrPut(entry.value) { ArrayDeque() }.addLast(entry)
        }
        apply(values.map { value ->
            existing[value]?.pollFirst() ?: ListRowEntry(nextId++, value)
        })
    }

    private fun apply(list: List<ListRowEntry<T>>) {
        entries = list
        onCommit(list.map { it.value })
    }
}

/** Per-row state handed to the row content so it can match the row's hover and focus styling */
internal class ListRowContext(
    private val color: State<Color>,
    val fieldInteraction: MutableInteractionSource,
    val focused: Boolean,
) {
    val contentColor: Color get() = color.value
}

@Composable
internal fun <T> rememberListEntries(key: Any?, initial: () -> List<T>, onCommit: (List<T>) -> Unit): ListEntriesState<T> =
    remember(key) { ListEntriesState(initial(), onCommit) }

@Composable
internal fun <T> ListOptionContainer(
    state: ListEntriesState<T>,
    reorderable: Boolean,
    maxEntries: Int,
    addText: Any,
    onAdd: () -> Unit,
    showAddWhenFull: Boolean = false,
    entryHeight: Dp = ListEntryHeight,
    maxHeight: Dp = 280.dp,
    containScroll: Boolean = false,
    invalid: (T) -> Boolean = { false },
    row: @Composable RowScope.(entry: ListRowEntry<T>, ctx: ListRowContext) -> Unit,
) {
    val theme = LocalTheme.current
    val density = LocalDensity.current
    val entries = state.entries

    var draggingIndex by remember { mutableStateOf(-1) }
    var dragAccum by remember { mutableStateOf(0f) }
    var lastTickIndex by remember { mutableStateOf(-1) }
    val stridePx = with(density) { (entryHeight + ListEntrySpacing).toPx() }
    val dragTargetIndex = if (draggingIndex == -1) {
        -1
    } else {
        (draggingIndex + (dragAccum / stridePx).roundToInt()).coerceIn(0, entries.lastIndex)
    }
    val contentHeight = entryHeight * entries.size + ListEntrySpacing * (entries.size - 1).coerceAtLeast(0)

    val draggableState = rememberDraggableState { delta ->
        if (draggingIndex == -1) return@rememberDraggableState
        dragAccum = (dragAccum + delta).coerceIn(
            -draggingIndex * stridePx,
            (entries.lastIndex - draggingIndex) * stridePx,
        )
        val target = (draggingIndex + (dragAccum / stridePx).roundToInt()).coerceIn(0, entries.lastIndex)
        if (target != lastTickIndex) {
            lastTickIndex = target
            UiSounds.play(UiSoundEvent.SLIDER_TICK)
        }
    }

    val full = maxEntries > 0 && entries.size >= maxEntries

    fun rowMenu(entry: ListRowEntry<T>): List<MenuEntry> {
        val index = state.indexOf(entry.id)
        if (index < 0) return emptyList()
        val canAdd = maxEntries <= 0 || state.size < maxEntries
        return buildList<MenuEntry> {
            if (reorderable) {
                add(
                    MenuAction(
                        translated("oneconfig.list.move_up", "Move up"),
                        { state.moveBy(entry.id, -1) },
                        icon = "up",
                        enabled = index > 0,
                        shortcutHint = translated("oneconfig.list.move_up.shortcut", "Alt+Up"),
                    ),
                )
                add(
                    MenuAction(
                        translated("oneconfig.list.move_down", "Move down"),
                        { state.moveBy(entry.id, 1) },
                        icon = "down",
                        enabled = index < state.size - 1,
                        shortcutHint = translated("oneconfig.list.move_down.shortcut", "Alt+Down"),
                    ),
                )
                add(MenuSeparator)
            }
            add(MenuAction(translated("oneconfig.list.duplicate", "Duplicate"), { state.duplicate(entry.id) }, icon = "copy-plus", enabled = canAdd))
            add(MenuAction(translated("oneconfig.list.insert_above", "Insert above"), { state.insertAbove(entry.id, onAdd) }, icon = "plus", enabled = canAdd))
            add(MenuSeparator)
            add(MenuAction(translated("oneconfig.list.remove", "Remove"), { state.remove(entry.id) }, icon = "trash", danger = true))
        }
    }

    Column(
        modifier = Modifier
            .width(LocalOptionWidth.current)
            .clip(ContainerShape)
            .background(theme.componentBackground, ContainerShape)
            .border(1.dp, theme.borderColor, ContainerShape)
            .padding(ContainerPadding),
        verticalArrangement = Arrangement.spacedBy(ListEntrySpacing),
    ) {
        if (entries.isNotEmpty()) {
            val scrollState = rememberScrollState()
            val containment = if (containScroll) {
                Modifier
                    .containVerticalScroll(scrollState)
                    .wheelScroll(scrollState, contain = { scrollState.maxValue > 0 })
            } else {
                Modifier
            }
            ScrollContainer(
                state = scrollState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .then(containment),
                scrollbar = scrollState.maxValue > 0,
                fadingEdges = true,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(contentHeight),
                ) {
                    entries.forEachIndexed { index, entry ->
                        key(entry.id) {
                            val isDragging = draggingIndex == index
                            val baseOffset = index * stridePx
                            val targetOffset = when {
                                isDragging -> baseOffset + dragAccum
                                draggingIndex == -1 -> baseOffset
                                dragTargetIndex > draggingIndex && index in (draggingIndex + 1)..dragTargetIndex -> baseOffset - stridePx
                                dragTargetIndex < draggingIndex && index in dragTargetIndex until draggingIndex -> baseOffset + stridePx
                                else -> baseOffset
                            }
                            val animatedOffset = Motion.animateFloatAsState(targetOffset, spring(), label = "listEntryOffset")

                            EntryRow(
                                valid = !invalid(entry.value),
                                dragging = isDragging,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(entryHeight)
                                    .offset { IntOffset(0, (if (isDragging) targetOffset else animatedOffset.value).roundToInt()) }
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .contextMenu { rowMenu(entry) }
                                    .onPreviewKeyEvent { event ->
                                        val delta = event.reorderDelta()
                                        reorderable && delta != 0 && state.moveBy(entry.id, delta).also { moved ->
                                            if (moved) UiSounds.play(UiSoundEvent.SLIDER_TICK)
                                        }
                                    },
                                dragHandle = if (!reorderable) null else { handleModifier ->
                                    handleModifier.draggable(
                                        orientation = Orientation.Vertical,
                                        state = draggableState,
                                        onDragStarted = { draggingIndex = index; dragAccum = 0f; lastTickIndex = index },
                                        onDragStopped = {
                                            if (dragTargetIndex != draggingIndex) state.move(draggingIndex, dragTargetIndex)
                                            draggingIndex = -1
                                            dragAccum = 0f
                                        },
                                    )
                                },
                                onRemove = { state.remove(entry.id) },
                                content = { ctx -> row(entry, ctx) },
                            )
                        }
                    }
                }
            }
        }

        if (showAddWhenFull || !full) {
            AddEntryButton(addText, entryHeight) { state.addAtEnd(onAdd) }
        }
    }
}

internal fun KeyEvent.reorderDelta(): Int {
    if (type != KeyEventType.KeyDown || !isAltPressed) return 0
    return when (key) {
        Key.DirectionUp -> -1
        Key.DirectionDown -> 1
        else -> 0
    }
}

@Composable
private fun EntryRow(
    valid: Boolean,
    dragging: Boolean,
    modifier: Modifier,
    dragHandle: ((Modifier) -> Modifier)?,
    onRemove: () -> Unit,
    content: @Composable RowScope.(ListRowContext) -> Unit,
) {
    val theme = LocalTheme.current
    val rowInteraction = rememberInteractionSource()
    val isHovered by rowInteraction.collectIsHoveredAsState()
    val fieldInteraction = rememberInteractionSource()
    val isFocused by fieldInteraction.collectIsFocusedAsState()

    val contentColor = Motion.animateColorAsState(
        if (dragging || isHovered || isFocused) theme.textColor else theme.textColorSecondary
    )
    val borderColor = Motion.animateColorAsState(if (valid) theme.borderColor else theme.error)

    Row(
        modifier = modifier
            .clip(EntryShape)
            .graphicsLayer {}
            .animatedFrame(EntryShape, fill = { theme.modCardBackground }, border = { borderColor.value })
            .hoverable(rowInteraction)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (dragHandle != null) {
            Icon(
                "dots-grid",
                modifier = dragHandle(
                    Modifier
                        .size(16.dp)
                        .pointerHoverIcon(PointerIcon.Hand)
                ),
                tint = { contentColor.value },
            )
        }

        content(ListRowContext(contentColor, fieldInteraction, isFocused))

        IconButton(
            "trash",
            translated("oneconfig.list.remove", "Remove"),
            onRemove,
            size = ControlSize.Compact,
            danger = true,
        )
    }
}

@Composable
private fun AddEntryButton(label: Any, entryHeight: Dp, onClick: () -> Unit) {
    TextButton(
        label.asRenderText(),
        "plus",
        onClick,
        Modifier.height(entryHeight),
        size = ControlSize.Compact,
        fillWidth = true,
    )
}
