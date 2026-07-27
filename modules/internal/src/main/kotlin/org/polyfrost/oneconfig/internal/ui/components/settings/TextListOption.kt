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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.api.settings.TextListOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.fadingEdges
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import kotlin.math.roundToInt

private val ContainerShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape
private val EntryShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape

private val EntryHeight = 34.dp
private val EntrySpacing = 8.dp
private val InvalidColor = Color(0xFFFF4444)

@Suppress("UNCHECKED_CAST")
@Composable
fun TextListOption(data: TextListOptionData) {
    val theme = LocalTheme.current
    val density = LocalDensity.current

    val idGen = remember(data.prop) { intArrayOf(0) }
    var entries by remember(data.prop) {
        mutableStateOf(stringListOf(data.prop).map { TextEntry(idGen[0]++, it) })
    }

    val regex = remember(data.regex) { data.regex?.let { runCatching { Regex(it) }.getOrNull() } }
    fun isValid(value: String) = value.isEmpty() || regex == null || regex.matches(value)

    fun save(list: List<TextEntry>) {
        val values = list.map { it.text }.filter { it.isNotBlank() && isValid(it) }
        (data.prop as Property<Any>).set(if (data.isList) ArrayList(values) else values.toTypedArray())
    }

    var draggingIndex by remember { mutableStateOf(-1) }
    var dragAccum by remember { mutableStateOf(0f) }
    var lastTickIndex by remember { mutableStateOf(-1) }
    val stridePx = with(density) { (EntryHeight + EntrySpacing).toPx() }
    val dragTargetIndex = if (draggingIndex == -1) {
        -1
    } else {
        (draggingIndex + (dragAccum / stridePx).roundToInt()).coerceIn(0, entries.lastIndex)
    }
    val contentHeight = EntryHeight * entries.size + EntrySpacing * (entries.size - 1).coerceAtLeast(0)

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
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(EntrySpacing),
    ) {
        if (entries.isNotEmpty()) {
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .fadingEdges(scrollState, theme.componentBackground)
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
                                data = data,
                                entry = entry,
                                valid = isValid(entry.text),
                                dragging = isDragging,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(EntryHeight)
                                    .offset { IntOffset(0, visualOffset.roundToInt()) }
                                    .zIndex(if (isDragging) 1f else 0f),
                                dragHandle = if (!data.reorderable) null else { handleModifier ->
                                    handleModifier.draggable(
                                        orientation = Orientation.Vertical,
                                        state = draggableState,
                                        onDragStarted = { draggingIndex = index; dragAccum = 0f; lastTickIndex = index },
                                        onDragStopped = {
                                            if (dragTargetIndex != draggingIndex) {
                                                val list = entries.toMutableList()
                                                list.add(dragTargetIndex, list.removeAt(draggingIndex))
                                                entries = list
                                                save(list)
                                            }
                                            draggingIndex = -1
                                            dragAccum = 0f
                                        },
                                    )
                                },
                                onTextChange = { value ->
                                    val list = entries.map { if (it.id == entry.id) it.copy(text = value) else it }
                                    entries = list
                                    save(list)
                                },
                                onRemove = {
                                    val list = entries.filterNot { it.id == entry.id }
                                    entries = list
                                    save(list)
                                },
                            )
                        }
                    }
                }
            }
        }

        if (data.maxEntries <= 0 || entries.size < data.maxEntries) {
            AddEntryButton(data.addText) {
                entries = entries + TextEntry(idGen[0]++, "")
            }
        }
    }
}

@Composable
private fun EntryRow(
    data: TextListOptionData,
    entry: TextEntry,
    valid: Boolean,
    dragging: Boolean,
    modifier: Modifier,
    dragHandle: ((Modifier) -> Modifier)?,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val theme = LocalTheme.current
    val rowInteraction = rememberInteractionSource()
    val isHovered by rowInteraction.collectIsHoveredAsState()
    val fieldInteraction = rememberInteractionSource()
    val isFocused by fieldInteraction.collectIsFocusedAsState()

    val contentColor by animateColorAsState(
        if (dragging || isHovered || isFocused) theme.textColor else theme.textColorSecondary
    )
    val borderColor by animateColorAsState(if (valid) theme.borderColor else InvalidColor)

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

        EntryTextField(
            data = data,
            entry = entry,
            focused = isFocused,
            interactionSource = fieldInteraction,
            onTextChange = onTextChange,
        )

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
private fun RowScope.EntryTextField(
    data: TextListOptionData,
    entry: TextEntry,
    focused: Boolean,
    interactionSource: MutableInteractionSource,
    onTextChange: (String) -> Unit,
) {
    val theme = LocalTheme.current
    BasicTextField(
        value = entry.text,
        onValueChange = onTextChange,
        singleLine = true,
        textStyle = TextStyle(
            color = theme.textColor,
            fontSize = 13.sp,
            fontFamily = theme.typography.family,
        ),
        interactionSource = interactionSource,
        cursorBrush = SolidColor(theme.textColor),
        modifier = Modifier.weight(1f),
        decorationBox = { innerTextField ->
            Box {
                if (entry.text.isEmpty() && data.placeholder != null && !focused) {
                    Text(data.placeholder!!, color = theme.textColorSecondary, fontSize = 13.sp)
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun AddEntryButton(label: Any, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val contentColor by animateColorAsState(if (isHovered) theme.textColor else theme.textColorSecondary)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(EntryHeight)
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

private data class TextEntry(val id: Int, val text: String)

private fun stringListOf(prop: Property<*>): List<String> = when (val v = prop.get()) {
    is Array<*> -> v.filterIsInstance<String>()
    is List<*> -> v.filterIsInstance<String>()
    else -> emptyList()
}
