package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.polyfrost.oneconfig.internal.ui.api.settings.NumberChainOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import kotlin.math.roundToInt

private val ChainShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape

/** Compose's default text cursor thickness */
private val CaretWidth = 2.dp

/** One slot in the chain where [id] is stable across reorders so equal values stay distinct rows */
private data class ChainEntry(val id: Int, val value: Float)

/**
 * An ordered chain of numbers where the handle reorders a row clicking edits its value in place and the
 * trailing button appends the next entry
 *
 * Leading entries the property locks are shown but cannot be touched
 */
@Composable
fun NumberChainOption(data: NumberChainOptionData) {
    val theme = LocalTheme.current
    val density = LocalDensity.current

    val idGen = remember(data.prop) { intArrayOf(0) }
    var entries by remember(data.prop) {
        mutableStateOf(data.read().map { ChainEntry(idGen[0]++, it) })
    }
    var editingId by remember(data.prop) { mutableStateOf(-1) }

    var draggingIndex by remember { mutableStateOf(-1) }
    var dragAccum by remember { mutableStateOf(0f) }
    var rowHeightPx by remember { mutableStateOf(36) }
    val spacingPx = with(density) { 8.dp.roundToPx() }
    val stridePx = (rowHeightPx + spacingPx).toFloat()
    val locked = data.lockedLeading
    val firstMovable = locked.coerceAtMost(entries.size)
    val dragTargetIndex = if (draggingIndex == -1) {
        -1
    } else {
        (draggingIndex + (dragAccum / stridePx).roundToInt()).coerceIn(firstMovable, entries.lastIndex)
    }
    val contentHeight = with(density) {
        (rowHeightPx * entries.size + spacingPx * (entries.size - 1).coerceAtLeast(0)).toDp()
    }

    // the property's setter is free to normalize so re-read rather than trusting what was written
    fun commit(next: List<ChainEntry>) {
        data.write(next.map { it.value })
        entries = data.read().map { ChainEntry(idGen[0]++, it) }
    }

    var lastTickIndex by remember { mutableStateOf(-1) }
    val draggableState = rememberDraggableState { delta ->
        if (draggingIndex == -1) return@rememberDraggableState
        dragAccum = (dragAccum + delta).coerceIn(
            -(draggingIndex - firstMovable) * stridePx,
            (entries.lastIndex - draggingIndex) * stridePx,
        )
        val target = (draggingIndex + (dragAccum / stridePx).roundToInt()).coerceIn(firstMovable, entries.lastIndex)
        if (target != lastTickIndex) {
            lastTickIndex = target
            UiSounds.play(UiSoundEvent.SLIDER_TICK)
        }
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
            entries.forEachIndexed { index, entry ->
                key(entry.id) {
                    val movable = index >= locked
                    val isDragging = draggingIndex == index
                    val rowInteraction = rememberInteractionSource()
                    val isHovered by rowInteraction.collectIsHoveredAsState()
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
                    val bgColor by animateColorAsState(if (isDragging) Accent else theme.chipBackground)
                    val contentColor by animateColorAsState(
                        when {
                            !movable -> theme.textColorSecondary
                            isDragging || isHovered -> theme.textColor
                            else -> theme.textColorSecondary
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, visualOffset.roundToInt()) }
                            .onSizeChanged { if (index == 0) rowHeightPx = it.height }
                            .zIndex(if (isDragging) 1f else 0f)
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
                                        .draggable(
                                            orientation = Orientation.Vertical,
                                            state = draggableState,
                                            onDragStarted = {
                                                draggingIndex = index; dragAccum = 0f; lastTickIndex = index
                                            },
                                            onDragStopped = {
                                                if (dragTargetIndex != draggingIndex && draggingIndex >= 0) {
                                                    val list = entries.toMutableList()
                                                    list.add(dragTargetIndex, list.removeAt(draggingIndex))
                                                    commit(list)
                                                }
                                                draggingIndex = -1
                                                dragAccum = 0f
                                            },
                                        )
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
            val addColor by animateColorAsState(if (addHovered) theme.textColor else theme.textColorSecondary)
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
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val focusRequester = remember { FocusRequester() }
    var text by remember { mutableStateOf(formatSpinnerValue(initial)) }
    var focused by remember { mutableStateOf(false) }

    val style = remember(theme) {
        TextStyle(color = theme.textColor, fontSize = 13.sp, fontFamily = theme.typography.family)
    }
    // a text field fills whatever width it is given so the unit sits against a field measured to its text
    // the field keeps room for a caret past the last digit which the unit is shifted back across
    val fieldWidth = with(density) { measurer.measure(text, style).size.width.toDp() } + CaretWidth

    fun commit() = onCommit(text.toFloatOrNull()?.coerceIn(min, max) ?: initial)

    BasicTextField(
        value = text,
        onValueChange = { text = filterNumberInput(it) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        textStyle = style,
        cursorBrush = SolidColor(theme.textColor),
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(fieldWidth)) { innerTextField() }
                if (unit != null) {
                    Text(
                        unit,
                        color = theme.textColor,
                        fontSize = 13.sp,
                        modifier = Modifier.offset(x = -CaretWidth),
                    )
                }
            }
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                if (state.isFocused) focused = true else if (focused) commit()
            },
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
