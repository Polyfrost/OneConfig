package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import org.polyfrost.compose.foundation.layout.Spacer
import org.polyfrost.compose.foundation.layout.absoluteOffset
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.offset
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.text.BasicTextField
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.layout.onPlaced
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.api.settings.NumberChainOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.screens.LocalUiClipboard
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val ChainShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape

private val CaretWidth = 2.dp

/** One slot in the chain where [id] is stable across reorders so equal values stay distinct rows */
private data class ChainEntry(val id: Int, val value: Float)

private class ChainDrag {
    var pressedIndex = -1
    var pressY = 0f
    var lastY = 0f
    var lastTickIndex = -1
}

/**
 * An ordered chain of numbers where the handle reorders a row clicking edits its value in place and the
 * trailing button appends the next entry
 *
 * Leading entries the property locks are shown but cannot be touched
 */
@Composable
fun NumberChainOption(data: NumberChainOptionData) {
    val theme = LocalTheme.current

    val idGen = remember(data.prop) { intArrayOf(0) }
    var entries by remember(data.prop) {
        mutableStateOf(data.read().map { ChainEntry(idGen[0]++, it) })
    }
    var editingId by remember(data.prop) { mutableIntStateOf(-1) }

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableFloatStateOf(36f) }
    val drag = remember { ChainDrag() }
    val spacing = 8f
    val stride = rowHeight + spacing
    val locked = data.lockedLeading
    val firstMovable = locked.coerceAtMost(entries.size)
    val dragTargetIndex = if (draggingIndex == -1) {
        -1
    } else {
        (draggingIndex + (dragAccum / stride).roundToInt()).coerceIn(firstMovable, entries.lastIndex)
    }
    val contentHeight = (rowHeight * entries.size + spacing * (entries.size - 1).coerceAtLeast(0)).dp

    // the property's setter is free to normalize so re-read rather than trusting what was written
    fun commit(next: List<ChainEntry>) {
        data.write(next.map { it.value })
        entries = data.read().map { ChainEntry(idGen[0]++, it) }
    }

    Column(
        modifier = Modifier
            .width(LocalOptionWidth.current)
            .clip(ChainShape)
            .background(theme.componentBackground, ChainShape)
            .border(1.dp, theme.borderColor, ChainShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(contentHeight)) {
            for (index in entries.indices.sortedBy { it == draggingIndex }) {
                val entry = entries[index]
                key(entry.id) {
                    val movable = index >= locked
                    val isDragging = draggingIndex == index
                    val rowInteraction = rememberInteractionSource()
                    val isHovered by rowInteraction.collectIsHoveredAsState()
                    val baseOffset = index * stride
                    val targetOffset = when {
                        isDragging -> baseOffset + dragAccum
                        draggingIndex == -1 -> baseOffset
                        dragTargetIndex > draggingIndex && index in (draggingIndex + 1)..dragTargetIndex -> baseOffset - stride
                        dragTargetIndex < draggingIndex && index in dragTargetIndex until draggingIndex -> baseOffset + stride
                        else -> baseOffset
                    }
                    val animatedOffset by animateFloatAsState(targetOffset, initialValue = targetOffset)
                    val visualOffset = if (isDragging) targetOffset else animatedOffset
                    val bgColor = animateColorAsState(if (isDragging) Accent else theme.chipBackground)
                    val contentColor = animateColorAsState(
                        when {
                            !movable -> theme.textColorSecondary
                            isDragging || isHovered -> theme.textColor
                            else -> theme.textColorSecondary
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .absoluteOffset(y = visualOffset.dp)
                            .onPlaced { if (index == 0) rowHeight = it.height }
                            .clip(ChainShape)
                            .background(bgColor, ChainShape)
                            .border(1.dp, theme.borderColor, ChainShape)
                            .hoverable(rowInteraction)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            "dots-grid",
                            modifier = Modifier
                                .size(14.dp)
                                .then(
                                    if (!movable) Modifier else Modifier
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .onPointerEvent { e ->
                                            when (e.type) {
                                                PointerEventType.Press -> e.button == 0 && run {
                                                    drag.pressedIndex = index
                                                    drag.pressY = e.y
                                                    drag.lastY = e.y
                                                    drag.lastTickIndex = index
                                                    true
                                                }
                                                PointerEventType.Move -> {
                                                    if (draggingIndex == -1 && drag.pressedIndex != -1 && abs(e.y - drag.pressY) >= 4f) {
                                                        draggingIndex = drag.pressedIndex
                                                        dragAccum = 0f
                                                    }
                                                    if (draggingIndex != -1) {
                                                        dragAccum = (dragAccum + e.y - drag.lastY).coerceIn(
                                                            -(draggingIndex - firstMovable) * stride,
                                                            (entries.lastIndex - draggingIndex) * stride,
                                                        )
                                                        val target = (draggingIndex + (dragAccum / stride).roundToInt()).coerceIn(firstMovable, entries.lastIndex)
                                                        if (target != drag.lastTickIndex) {
                                                            drag.lastTickIndex = target
                                                            UiSounds.play(UiSoundEvent.SLIDER_TICK)
                                                        }
                                                    }
                                                    drag.lastY = e.y
                                                    true
                                                }
                                                PointerEventType.Release -> {
                                                    drag.pressedIndex = -1
                                                    if (dragTargetIndex != draggingIndex && draggingIndex >= 0) {
                                                        val list = entries.toMutableList()
                                                        list.add(dragTargetIndex, list.removeAt(draggingIndex))
                                                        commit(list)
                                                    }
                                                    draggingIndex = -1
                                                    dragAccum = 0f
                                                    true
                                                }
                                                else -> false
                                            }
                                        }
                                ),
                            color = contentColor,
                        )

                        if (editingId == entry.id) {
                            ChainValueEditor(
                                initial = entry.value,
                                min = data.min,
                                max = data.max,
                                unit = data.unit,
                                modifier = Modifier.weight(1f),
                                onCommit = { value ->
                                    // losing focus commits and can arrive after another row claimed the editor
                                    // so only the row still being edited may write
                                    if (editingId == entry.id) {
                                        editingId = -1
                                        commit(entries.map { if (it.id == entry.id) it.copy(value = value) else it })
                                    }
                                },
                            )
                        } else {
                            Text(
                                data.label(entry.value),
                                color = contentColor,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .then(
                                        if (!movable) Modifier else Modifier
                                            .onClick(rememberInteractionSource()) { editingId = entry.id }
                                            .pointerHoverIcon(PointerIcon.Hand)
                                    ),
                            )
                        }

                        if (movable) {
                            val deleteInteraction = rememberInteractionSource()
                            val deleteHovered by deleteInteraction.collectIsHoveredAsState()
                            Icon(
                                "close",
                                modifier = Modifier
                                    .size(14.dp)
                                    .onClick(deleteInteraction) { commit(entries.filterNot { it.id == entry.id }) }
                                    .pointerHoverIcon(PointerIcon.Hand),
                                color = if (deleteHovered) theme.textColor else theme.textColorSecondary,
                            )
                        } else {
                            Spacer(Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        if (entries.size < data.maxEntries) {
            val addInteraction = rememberInteractionSource()
            val addHovered by addInteraction.collectIsHoveredAsState()
            val addColor = animateColorAsState(if (addHovered) theme.textColor else theme.textColorSecondary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ChainShape)
                    .border(1.dp, theme.borderColor, ChainShape)
                    .onClick(addInteraction) {
                        val values = entries.map { it.value }
                        commit(entries + ChainEntry(idGen[0]++, data.nextValue(values)))
                    }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon("plus", modifier = Modifier.size(14.dp), color = addColor)
                Text("Add", color = addColor, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ChainValueEditor(
    initial: Float,
    min: Float,
    max: Float,
    unit: String?,
    modifier: Modifier,
    onCommit: (Float) -> Unit,
) {
    val theme = LocalTheme.current
    val clipboard = LocalUiClipboard.current
    val state = remember { TextFieldState(formatSpinnerValue(initial)).also { it.requestFocus() } }
    val focused = remember { booleanArrayOf(false) }
    val font = PolyFont(theme.typography.family.resolve(FontWeight.Normal), 13f)

    // a text field fills whatever width it is given so the unit sits against a field measured to its text
    // the field keeps room for a caret past the last digit which the unit is shifted back across
    val fieldWidth = font.measureTextWidth(state.text).dp + CaretWidth

    fun commit() = onCommit(state.text.toFloatOrNull()?.coerceIn(min, max) ?: initial)

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            state, font,
            color = theme.textColor,
            caretColor = theme.textColor,
            selectionColor = Accent.copy(alpha = 0.4f),
            readClipboard = clipboard.read,
            writeClipboard = clipboard.write,
            modifier = Modifier.width(fieldWidth),
            onKey = editKeys(clipboard, state, onEnter = ::commit) {
                val filtered = filterNumberInput(state.text)
                if (filtered != state.text) state.replaceText(filtered)
            },
        )
        if (unit != null) {
            Text(
                unit,
                color = theme.textColor,
                fontSize = 13.sp,
                modifier = Modifier.offset(x = -CaretWidth),
            )
        }
    }

    LaunchedEffect(state.focused) {
        if (state.focused) focused[0] = true else if (focused[0]) commit()
    }
}
