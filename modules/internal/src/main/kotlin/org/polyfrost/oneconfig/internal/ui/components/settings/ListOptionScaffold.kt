package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.abs
import kotlin.math.roundToInt
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.RowScope
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.heightIn
import org.polyfrost.compose.foundation.layout.offset
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.rememberScrollState
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.foundation.verticalScroll
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.input.key.KeyEvent
import org.polyfrost.compose.ui.input.pointer.PointerEvent
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.text.style.TextOverflow
import org.polyfrost.compose.ui.unit.Dp
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.poly.FadingEdges
import org.polyfrost.oneconfig.internal.ui.screens.UiClipboard
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
internal val ListInvalidColor = Color(0xFFFF4444)

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

    fun add(value: T) = apply(entries + ListRowEntry(nextId++, value))

    fun update(id: Int, value: T) = apply(entries.map { if (it.id == id) it.copy(value = value) else it })

    fun remove(id: Int) = apply(entries.filterNot { it.id == id })

    fun move(from: Int, to: Int) {
        val list = entries.toMutableList()
        list.add(to, list.removeAt(from))
        apply(list)
    }

    fun replace(values: List<T>) {
        val existing = HashMap<T, java.util.ArrayDeque<ListRowEntry<T>>>()
        entries.forEach { entry ->
            existing.getOrPut(entry.value) { java.util.ArrayDeque() }.addLast(entry)
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

internal class ListRowContext(
    val contentColor: Color,
    val onFieldFocus: (Boolean) -> Unit,
    val focused: Boolean,
)

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
    invalid: (T) -> Boolean = { false },
    row: @Composable RowScope.(entry: ListRowEntry<T>, ctx: ListRowContext) -> Unit,
) {
    val theme = LocalTheme.current
    val entries = state.entries

    var pressedIndex by remember { mutableStateOf(-1) }
    var pressY by remember { mutableStateOf(0f) }
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragAccum by remember { mutableStateOf(0f) }
    var lastTickIndex by remember { mutableStateOf(-1) }
    val stride = (entryHeight + ListEntrySpacing).value
    fun targetIndex(accum: Float) = (draggingIndex + (accum / stride).roundToInt()).coerceIn(0, entries.lastIndex)
    val dragTargetIndex = if (draggingIndex == -1) {
        -1
    } else {
        targetIndex(dragAccum)
    }
    val contentHeight = entryHeight * entries.size + ListEntrySpacing * (entries.size - 1).coerceAtLeast(0)

    fun onHandlePointer(index: Int, e: PointerEvent): Boolean = when (e.type) {
        PointerEventType.Press -> e.button == 0 && run { pressedIndex = index; pressY = e.y; true }
        PointerEventType.Move -> {
            if (pressedIndex == -1) return false
            if (draggingIndex == -1) {
                if (abs(e.y - pressY) < 4f) return true
                draggingIndex = pressedIndex
                lastTickIndex = pressedIndex
            }
            dragAccum = (e.y - pressY).coerceIn(
                -draggingIndex * stride,
                (entries.lastIndex - draggingIndex) * stride,
            )
            val target = targetIndex(dragAccum)
            if (target != lastTickIndex) {
                lastTickIndex = target
                UiSounds.play(UiSoundEvent.SLIDER_TICK)
            }
            true
        }
        PointerEventType.Release -> e.button == 0 && run {
            pressedIndex = -1
            if (draggingIndex != -1) {
                if (dragTargetIndex != draggingIndex) state.move(draggingIndex, dragTargetIndex)
                draggingIndex = -1
                dragAccum = 0f
            }
            true
        }
        else -> false
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
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxHeight)
                        .verticalScroll(scrollState),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(contentHeight),
                    ) {
                        val order = if (draggingIndex == -1) entries.indices.toList() else entries.indices.filter { it != draggingIndex } + draggingIndex
                        for (index in order) {
                            val entry = entries[index]
                            key(entry.id) {
                                val isDragging = draggingIndex == index
                                val baseOffset = index * stride
                                val targetOffset = when {
                                    isDragging -> baseOffset + dragAccum
                                    draggingIndex == -1 -> baseOffset
                                    dragTargetIndex > draggingIndex && index in (draggingIndex + 1)..dragTargetIndex -> baseOffset - stride
                                    dragTargetIndex < draggingIndex && index in dragTargetIndex until draggingIndex -> baseOffset + stride
                                    else -> baseOffset
                                }
                                val animatedOffset by animateFloatAsState(targetOffset)
                                val visualOffset = if (isDragging) targetOffset else animatedOffset

                                EntryRow(
                                    valid = !invalid(entry.value),
                                    dragging = isDragging,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(entryHeight)
                                        .offset(y = visualOffset.dp),
                                    dragHandle = if (!reorderable) null else { handleModifier ->
                                        handleModifier.onPointerEvent { onHandlePointer(index, it) }
                                    },
                                    onRemove = { state.remove(entry.id) },
                                    content = { ctx -> row(entry, ctx) },
                                )
                            }
                        }
                    }
                }
                FadingEdges(
                    scrollState,
                    theme.componentBackground,
                    Modifier
                        .fillMaxWidth()
                        .height(minOf(contentHeight, maxHeight)),
                )
            }
        }

        if (showAddWhenFull || maxEntries <= 0 || entries.size < maxEntries) {
            AddEntryButton(addText, entryHeight, onAdd)
        }
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
    var isFocused by remember { mutableStateOf(false) }

    val contentColor by animateColorAsState(
        if (dragging || isHovered || isFocused) theme.textColor else theme.textColorSecondary
    )
    val borderColor = animateColorAsState(if (valid) theme.borderColor else ListInvalidColor)

    Row(
        modifier = modifier
            .clip(EntryShape)
            .background(theme.modCardBackground, EntryShape)
            .border(1.dp, borderColor, EntryShape)
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
                color = contentColor,
            )
        }

        content(ListRowContext(contentColor, { isFocused = it }, isFocused))

        Icon(
            "trash",
            modifier = Modifier
                .size(16.dp)
                .pointerHoverIcon(PointerIcon.Hand)
                .onClick(rememberInteractionSource(), onRemove),
            color = contentColor,
        )
    }
}

@Composable
private fun AddEntryButton(label: Any, entryHeight: Dp, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val contentColor = animateColorAsState(if (isHovered) theme.textColor else theme.textColorSecondary)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(entryHeight)
            .border(1.dp, theme.borderColor, EntryShape)
            .pointerHoverIcon(PointerIcon.Hand)
            .hoverable(interactionSource)
            .onClick(interactionSource, onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon("plus", modifier = Modifier.size(16.dp), color = contentColor)
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = contentColor,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun textKeys(clipboard: UiClipboard, state: TextFieldState, onEdit: (String) -> Unit): (KeyEvent) -> Boolean = { ev ->
    val before = state.text
    val used = state.onKey(ev, clipboard.read, clipboard.write)
    if (used && state.text != before) onEdit(state.text)
    used
}

