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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.internal.ui.api.settings.DraggableListOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.fadingEdges
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import kotlin.math.roundToInt

private val ListContainerShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape
private val ListItemShape @Composable get() = LocalTheme.current.sideBarNavigationEntryShape

@Suppress("UNCHECKED_CAST")
@Composable
fun DraggableListOption(data: DraggableListOptionData) {
    val theme = LocalTheme.current
    val density = LocalDensity.current

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

    var draggingIndex by remember { mutableStateOf(-1) }
    var dragAccum by remember { mutableStateOf(0f) }
    var itemHeightPx by remember { mutableStateOf(40) }
    val spacingPx = with(density) { 14.dp.roundToPx() }
    val stridePx = (itemHeightPx + spacingPx).toFloat()
    val dragTargetIndex = if (draggingIndex == -1) {
        -1
    } else {
        (draggingIndex + (dragAccum / stridePx).roundToInt()).coerceIn(0, items.lastIndex)
    }
    val contentHeight = with(density) {
        (itemHeightPx * items.size + spacingPx * (items.size - 1).coerceAtLeast(0)).toDp()
    }

    fun save(list: List<ListEntry> = items) {
        val toSave = if (data.checkable) list.filter { it.id in enabledIds } else list
        (data.prop as Property<Any>).set(toSave.map { it.value }.toTypedArray())
    }

    var lastTickIndex by remember { mutableStateOf(-1) }

    val draggableState = rememberDraggableState { delta ->
        if (draggingIndex == -1) return@rememberDraggableState
        dragAccum = (dragAccum + delta).coerceIn(
            -draggingIndex * stridePx,
            (items.lastIndex - draggingIndex) * stridePx,
        )
        val target = (draggingIndex + (dragAccum / stridePx).roundToInt()).coerceIn(0, items.lastIndex)
        if (target != lastTickIndex) {
            lastTickIndex = target
            UiSounds.play(UiSoundEvent.SLIDER_TICK)
        }
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
                .fadingEdges(scrollState, theme.componentBackground)
                .verticalScroll(scrollState),
        ) {
          Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight),
          ) {
            items.forEachIndexed { index, item ->
                key(item.id) {
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

                    val bgColor by animateColorAsState(
                        if (isDragging) Accent else theme.componentBackground
                    )
                    val contentColor by animateColorAsState(
                        if (isDragging || isHovered) theme.textColor else theme.textColorSecondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, visualOffset.roundToInt()) }
                            .onSizeChanged { if (index == 0) itemHeightPx = it.height }
                            .zIndex(if (isDragging) 1f else 0f)
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
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = draggableState,
                                    onDragStarted = { draggingIndex = index; dragAccum = 0f; lastTickIndex = index },
                                    onDragStopped = {
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
                                    },
                                ),
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
                            modifier = Modifier.weight(1f, fill = false),
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
    }
}

/** A single list slot where [id] is stable across reorders so duplicate [value]s stay distinct */
private data class ListEntry(val id: Int, val value: String)

private fun propValueList(prop: Property<*>): List<String> = when (val v = prop.get()) {
    is Array<*> -> v.filterIsInstance<String>()
    is List<*> -> v.filterIsInstance<String>()
    else -> emptyList()
}
