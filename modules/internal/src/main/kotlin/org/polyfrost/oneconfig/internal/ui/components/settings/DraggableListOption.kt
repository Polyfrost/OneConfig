package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.a11y.a11y
import org.polyfrost.oneconfig.api.ui.v1.components.MenuAction
import org.polyfrost.oneconfig.api.ui.v1.components.MenuEntry
import org.polyfrost.oneconfig.api.ui.v1.components.ScrollContainer
import org.polyfrost.oneconfig.api.ui.v1.components.contextMenu
import org.polyfrost.oneconfig.api.ui.v1.components.rememberReorderState
import org.polyfrost.oneconfig.api.ui.v1.components.reorderHandle
import org.polyfrost.oneconfig.api.ui.v1.components.reorderItem
import org.polyfrost.oneconfig.internal.ui.api.settings.DraggableListOptionData
import org.polyfrost.oneconfig.internal.ui.api.shown
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.animatedFrame
import org.polyfrost.oneconfig.internal.ui.components.asRenderText
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.animateAccentColorAsState
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

    val itemHeight = if (data.checkable) CheckableListItemHeight else PlainListItemHeight
    val stridePx = with(density) { (itemHeight + ListItemSpacing).toPx() }
    val contentHeight = itemHeight * items.size + ListItemSpacing * (items.size - 1).coerceAtLeast(0)

    fun save(list: List<ListEntry> = items) {
        val toSave = if (data.checkable) list.filter { it.id in enabledIds } else list
        (data.prop as Property<Any>).set(toSave.map { it.value }.toTypedArray())
    }

    fun moveBy(id: Int, delta: Int): Boolean {
        val from = items.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in items.indices) return false
        val list = items.toMutableList()
        list.add(to, list.removeAt(from))
        items = list
        save(list)
        return true
    }

    fun rowMenu(item: ListEntry): List<MenuEntry> {
        val index = items.indexOfFirst { it.id == item.id }
        if (index < 0) return emptyList()
        return listOf(
            MenuAction(
                translated("oneconfig.list.move_up", "Move up"),
                { moveBy(item.id, -1) },
                icon = "up",
                enabled = index > 0,
                shortcutHint = translated("oneconfig.list.move_up.shortcut", "Alt+Up"),
            ),
            MenuAction(
                translated("oneconfig.list.move_down", "Move down"),
                { moveBy(item.id, 1) },
                icon = "down",
                enabled = index < items.lastIndex,
                shortcutHint = translated("oneconfig.list.move_down.shortcut", "Alt+Down"),
            ),
        )
    }

    val reorder = rememberReorderState(
        Orientation.Vertical,
        { from, to ->
            if (from in items.indices && to in items.indices) {
                items = items.toMutableList().also { it.add(to, it.removeAt(from)) }
                UiSounds.play(UiSoundEvent.SLIDER_TICK)
            }
        },
        { save() },
    )

    Box(
        modifier = Modifier
            .width(LocalOptionWidth.current)
            .clip(ListContainerShape)
            .background(theme.componentBackground, ListContainerShape)
            .border(1.dp, theme.borderColor, ListContainerShape)
            .padding(15.dp),
    ) {
        val scrollState = rememberScrollState()
        ScrollContainer(
            state = scrollState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp),
            scrollbar = scrollState.maxValue > 0,
            fadingEdges = true,
        ) {
          Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight),
          ) {
            items.forEachIndexed { index, item ->
                key(item.id) {
                    val isDragging = reorder.draggingKey == item.id
                    val rowInteraction = rememberInteractionSource()
                    val isHovered by rowInteraction.collectIsHoveredAsState()
                    val baseOffset = index * stridePx
                    val animatedOffset = Motion.animateFloatAsState(baseOffset, if (isDragging) snap() else spring(), label = "listEntryOffset")

                    val bgColor = animateAccentColorAsState(
                        isDragging,
                        if (isDragging) Accent else theme.componentBackground
                    )
                    val contentColor = Motion.animateColorAsState(
                        if (isDragging || isHovered) theme.textColor else theme.textColorSecondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, baseOffset.roundToInt()) }
                            .height(itemHeight)
                            .reorderItem(reorder, item.id, index, false)
                            .graphicsLayer { translationY = animatedOffset.value - baseOffset }
                            .clip(ListItemShape)
                            .graphicsLayer {}
                            .animatedFrame(ListItemShape, fill = { bgColor.value }, border = { theme.borderColor })
                            .hoverable(rowInteraction)
                            .contextMenu { rowMenu(item) }
                            .onPreviewKeyEvent { event ->
                                val delta = event.reorderDelta()
                                delta != 0 && moveBy(item.id, delta).also { moved ->
                                    if (moved) UiSounds.play(UiSoundEvent.SLIDER_TICK)
                                }
                            }
                            .a11y((optionLabels[item.value] ?: item.value).asRenderText(), A11yRole.Option, null, null, ListItemShape, true)
                            .focusable(interactionSource = rowInteraction)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                    ) {
                        Icon(
                            "dots-grid",
                            modifier = Modifier
                                .size(16.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                                .reorderHandle(reorder, item.id),
                            tint = { contentColor.value },
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

                        data.icon?.takeIf { it.isNotEmpty() }?.let { Icon(it, modifier = Modifier.size(16.dp), tint = { contentColor.value }) }

                        Text(
                            optionLabels[item.value] ?: item.value,
                            modifier = Modifier.weight(1f, fill = false),
                            color = Color.Unspecified,
                            colorProducer = { contentColor.value },
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

private fun propValueList(prop: Property<*>): List<String> = when (val v = prop.shown()) {
    is Array<*> -> v.filterIsInstance<String>()
    is List<*> -> v.filterIsInstance<String>()
    else -> emptyList()
}
