package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.containVerticalScroll
import org.polyfrost.oneconfig.internal.ui.components.fadingEdges
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric
import kotlin.math.roundToInt

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

/** Per-row state handed to the row content so it can match the row's hover and focus styling */
internal class ListRowContext(
    val contentColor: Color,
    val fieldInteraction: MutableInteractionSource,
    val focused: Boolean,
)

@Composable
internal fun <T> rememberListEntries(key: Any?, initial: List<T>, onCommit: (List<T>) -> Unit): ListEntriesState<T> =
    remember(key) { ListEntriesState(initial, onCommit) }

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
            val containmentModifier = if (containScroll) {
                Modifier.containVerticalScroll(scrollState)
            } else {
                Modifier
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .fadingEdges(scrollState, theme.componentBackground)
                    .then(containmentModifier)
                    .verticalScroll(scrollState),
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
                            val animatedOffset by animateFloatAsState(targetOffset)
                            val visualOffset = if (isDragging) targetOffset else animatedOffset

                            EntryRow(
                                valid = !invalid(entry.value),
                                dragging = isDragging,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(entryHeight)
                                    .offset { IntOffset(0, visualOffset.roundToInt()) }
                                    .zIndex(if (isDragging) 1f else 0f),
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
    val fieldInteraction = rememberInteractionSource()
    val isFocused by fieldInteraction.collectIsFocusedAsState()

    val contentColor by animateColorAsState(
        if (dragging || isHovered || isFocused) theme.textColor else theme.textColorSecondary
    )
    val borderColor by animateColorAsState(if (valid) theme.borderColor else ListInvalidColor)

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

        content(ListRowContext(contentColor, fieldInteraction, isFocused))

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
    val contentColor by animateColorAsState(if (isHovered) theme.textColor else theme.textColorSecondary)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(entryHeight)
            .clip(EntryShape)
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
            modifier = Modifier.weight(1f, fill = false),
            color = contentColor,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
