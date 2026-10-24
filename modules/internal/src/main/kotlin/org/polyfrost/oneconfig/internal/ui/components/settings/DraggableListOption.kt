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
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.heightIn
import org.polyfrost.compose.foundation.layout.offset
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.rememberScrollState
import org.polyfrost.compose.foundation.verticalScroll
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.input.pointer.PointerEvent
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.text.style.TextOverflow
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.api.settings.DraggableListOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.poly.FadingEdges
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val ListContainerShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape
private val ListItemShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape
private val PlainListItemHeight = 32.dp
private val CheckableListItemHeight = 40.dp
private val ListItemSpacing = 14.dp

@Suppress("UNCHECKED_CAST")
@Composable
fun DraggableListOption(data: DraggableListOptionData) {
    val theme = LocalTheme.current

    val allOptions: List<String> = remember(data.prop) {
        data.options?.toList() ?: propValueList(data.prop)
    }
    val optionLabels = data.optionLabels

    // stable per-slot id so duplicate values render and reorder independently
    val idGen = remember(data.prop) { intArrayOf(0) }

    // when checkable this includes disabled items appended after the enabled ones
    var items by remember(data.prop) {
        val ordered = propValueList(data.prop)
        val result = if (data.checkable) {
            val enabledSet = ordered.toSet()
            ordered + allOptions.filter { it !in enabledSet }
        } else {
            ordered.ifEmpty { allOptions }
        }
        mutableStateOf(result.map { ListEntry(idGen[0]++, it) })
    }

    var enabledIds by remember(data.prop) {
        if (!data.checkable) return@remember mutableStateOf(emptySet<Int>())
        val enabled = propValueList(data.prop).toSet()
        mutableStateOf(items.filter { it.value in enabled }.map { it.id }.toSet())
    }

    var pressedIndex by remember { mutableStateOf(-1) }
    var pressY by remember { mutableStateOf(0f) }
    var draggingIndex by remember { mutableStateOf(-1) }
    var dragAccum by remember { mutableStateOf(0f) }
    val itemHeight = if (data.checkable) CheckableListItemHeight else PlainListItemHeight
    val stride = (itemHeight + ListItemSpacing).value
    fun targetIndex(accum: Float) = (draggingIndex + (accum / stride).roundToInt()).coerceIn(0, items.lastIndex)
    val dragTargetIndex = if (draggingIndex == -1) {
        -1
    } else {
        targetIndex(dragAccum)
    }
    val contentHeight = itemHeight * items.size + ListItemSpacing * (items.size - 1).coerceAtLeast(0)

    fun save(list: List<ListEntry> = items) {
        val toSave = if (data.checkable) list.filter { it.id in enabledIds } else list
        (data.prop as Property<Any>).set(toSave.map { it.value }.toTypedArray())
    }

    var lastTickIndex by remember { mutableStateOf(-1) }

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
                (items.lastIndex - draggingIndex) * stride,
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
                if (dragTargetIndex != draggingIndex) {
                    val list = items.toMutableList()
                    val moved = list.removeAt(draggingIndex)
                    list.add(dragTargetIndex, moved)
                    items = list
                    save(list)
                } else {
                    save()
                }
                draggingIndex = -1
                dragAccum = 0f
            }
            true
        }
        else -> false
    }

    Box(
        modifier = Modifier
            .width(LocalOptionWidth.current)
            .clip(ListContainerShape)
            .background(theme.componentBackground, ListContainerShape)
            .border(1.dp, theme.borderColor, ListContainerShape)
            .padding(15.dp),
    ) {
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .verticalScroll(scrollState),
        ) {
          Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight),
          ) {
            val order = if (draggingIndex == -1) items.indices.toList() else items.indices.filter { it != draggingIndex } + draggingIndex
            for (index in order) {
                val item = items[index]
                key(item.id) {
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
                    val animatedOffset by animateFloatAsState(targetOffset)
                    val visualOffset = if (isDragging) targetOffset else animatedOffset

                    val bgColor = animateColorAsState(
                        if (isDragging) Accent else theme.componentBackground
                    )
                    val contentColor = animateColorAsState(
                        if (isDragging || isHovered) theme.textColor else theme.textColorSecondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = visualOffset.dp)
                            .height(itemHeight)
                            .clip(ListItemShape)
                            .background(bgColor, ListItemShape)
                            .border(1.dp, theme.borderColor, ListItemShape)
                            .hoverable(rowInteraction)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                    ) {
                        Icon(
                            "dots-grid",
                            modifier = Modifier
                                .size(16.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .onPointerEvent { onHandlePointer(index, it) },
                            color = contentColor,
                        )

                        if (data.checkable) {
                            val checked = item.id in enabledIds
                            CheckboxIndicator(
                                checked = checked,
                                onToggle = {
                                    enabledIds = if (checked) enabledIds - item.id else enabledIds + item.id
                                    save()
                                },
                            )
                        }

                        data.icon?.takeIf { it.isNotEmpty() }?.let { Icon(it, modifier = Modifier.size(16.dp), color = contentColor) }

                        Text(
                            optionLabels[item.value] ?: item.value,
                            modifier = Modifier.weight(1f),
                            color = contentColor,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
          }
        }
        FadingEdges(
            scrollState,
            theme.componentBackground,
            Modifier
                .fillMaxWidth()
                .height(minOf(contentHeight, 280.dp)),
        )
    }
}

/** A single list slot where [id] is stable across reorders so duplicate [value]s stay distinct */
private data class ListEntry(val id: Int, val value: String)

private fun propValueList(prop: Property<*>): List<String> = when (val v = prop.shown()) {
    is Array<*> -> v.filterIsInstance<String>()
    is List<*> -> v.filterIsInstance<String>()
    else -> emptyList()
}
