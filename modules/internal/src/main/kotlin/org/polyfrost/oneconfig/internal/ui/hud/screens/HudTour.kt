package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.animation.core.tween
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.Spacer
import org.polyfrost.compose.foundation.layout.absoluteOffset
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.shape.CircleShape
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.geometry.Offset
import org.polyfrost.compose.ui.geometry.Rect
import org.polyfrost.compose.ui.geometry.Size
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.compositeOver
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.layout.onPlaced
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

internal enum class HudTourFocus {
    None,
    Library,
    Hud,
    AnchorMenu,
    Panel;
}

internal enum class HudTourStep(
    val id: String,
    val focus: HudTourFocus,
    val interactive: Boolean = false,
    val category: StudioCategory = StudioCategory.Settings,
) {
    Welcome("welcome", HudTourFocus.None),
    Adding("adding", HudTourFocus.Library),
    Dragging("dragging", HudTourFocus.Hud),
    Merging("merging", HudTourFocus.None, interactive = true),
    Anchoring("anchoring", HudTourFocus.AnchorMenu),
    Settings("settings", HudTourFocus.Panel),
    Designer("designer", HudTourFocus.Panel, category = StudioCategory.Designer),
    Fonts("fonts", HudTourFocus.Panel, category = StudioCategory.Designer),
    Done("done", HudTourFocus.None);

    val title: String get() = tourString("$id.title")
    val body: String get() = tourString("$id.body")
    val isFirst: Boolean get() = ordinal == 0
    val isLast: Boolean get() = ordinal == entries.lastIndex

    fun offset(delta: Int): HudTourStep? = entries.getOrNull(ordinal + delta)
}

internal fun tourString(key: String): String = Platform.i18n().translateString("oneconfig.hud.tour.$key")

private val tourScrimColor = Color.Black.copy(0.6f)
private const val TOUR_CARD_WIDTH = 280f
private const val TOUR_HOLE_RADIUS = 8f

private fun Rect.inflate(delta: Float) = Rect(left - delta, top - delta, right + delta, bottom + delta)

private fun Rect.overlaps(other: Rect) =
    left < other.right && other.left < right && top < other.bottom && other.top < bottom

private fun placeTourCard(
    root: Size,
    card: Size,
    focus: Rect?,
    gap: Float,
    margin: Float,
    bottom: Boolean,
    avoid: Rect?,
): Offset {
    val centered = Offset((root.width - card.width) / 2f, (root.height - card.height) / 2f)
    val maxX = (root.width - card.width - margin).coerceAtLeast(margin)
    val maxY = (root.height - card.height - margin).coerceAtLeast(margin)
    if (bottom) {
        val slot = Offset(centered.x.coerceAtLeast(margin), maxY)
        return if (avoid != null && avoid.overlaps(Rect(slot, card))) {
            Offset((avoid.left - gap - card.width).coerceAtLeast(margin), maxY)
        } else {
            slot
        }
    }
    if (focus == null) return centered
    val besideY = (focus.center.y - card.height / 2f).coerceIn(margin, maxY)
    val aroundX = (focus.center.x - card.width / 2f).coerceIn(margin, maxX)
    val candidates = listOf(
        Offset(focus.right + gap, besideY),
        Offset(focus.left - gap - card.width, besideY),
        Offset(aroundX, focus.bottom + gap),
        Offset(aroundX, focus.top - gap - card.height),
    )
    return candidates.firstOrNull { it.x >= margin && it.y >= margin && it.x <= maxX && it.y <= maxY }
        ?: Offset(centered.x, maxY)
}

@Composable
internal fun HudTourOverlay(
    step: HudTourStep,
    focus: Rect?,
    task: String?,
    taskDone: Boolean,
    dragHint: Pair<Rect, Rect>?,
    avoid: Rect?,
    blocksEditor: Boolean,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val theme = LocalTheme.current
    var rootSize by remember { mutableStateOf(Size.Zero) }
    var cardSize by remember { mutableStateOf(Size.Zero) }

    val hole = focus?.inflate(6f)
    val target = hole ?: Rect(Offset(rootSize.width / 2f, rootSize.height / 2f), Size.Zero)
    val holeSpec = tween(220)
    val holeLeft by animateFloatAsState(target.left, holeSpec)
    val holeTop by animateFloatAsState(target.top, holeSpec)
    val holeRight by animateFloatAsState(target.right, holeSpec)
    val holeBottom by animateFloatAsState(target.bottom, holeSpec)
    val ringAlpha by animateFloatAsState(if (hole != null) 1f else 0f, holeSpec)
    val scrimAlpha by animateFloatAsState(if (step.interactive) 0f else 1f, holeSpec)

    val measured = rootSize.width > 0f && cardSize.width > 0f
    var cardPlaced by remember { mutableStateOf(false) }
    val cardSlot = if (measured) {
        placeTourCard(rootSize, cardSize, hole, 12f, 16f, bottom = step.interactive, avoid = avoid)
    } else {
        Offset.Zero
    }
    val cardSpec = tween(if (cardPlaced) 220 else 1)
    val cardX by animateFloatAsState(cardSlot.x, cardSpec, cardSlot.x)
    val cardY by animateFloatAsState(cardSlot.y, cardSpec, cardSlot.y)
    LaunchedEffect(measured) { if (measured) cardPlaced = true }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onPlaced { rootSize = Size(it.width, it.height) }
            .then(if (blocksEditor) Modifier.onPointerEvent { true } else Modifier)
    ) {
        Canvas(Modifier.fillMaxSize()) { x, y, w, h ->
            val scrim = tourScrimColor.copy(alpha = tourScrimColor.alphaF * scrimAlpha)
            rectWithHole(x, y, w, h, x + holeLeft, y + holeTop, holeRight - holeLeft, holeBottom - holeTop, TOUR_HOLE_RADIUS, scrim)
            if (ringAlpha > 0f) {
                rectStroke(
                    x + holeLeft, y + holeTop, holeRight - holeLeft, holeBottom - holeTop,
                    Accent.copy(alpha = Accent.alphaF * ringAlpha), 1.5f, TOUR_HOLE_RADIUS,
                )
            }
        }

        if (dragHint != null) TourDragHint(dragHint.first, dragHint.second)

        Column(
            modifier = Modifier
                .absoluteOffset((if (cardPlaced) cardX else cardSlot.x).dp, (if (cardPlaced) cardY else cardSlot.y).dp)
                .alpha(if (measured) 1f else 0f)
                .then(cardModifier)
                .onPlaced { cardSize = Size(it.width, it.height) }
                .width(TOUR_CARD_WIDTH.dp)
                .background(theme.popupBackground, theme.popupShape)
                .border(1.dp, theme.borderColor, theme.popupShape)
                .onPointerEvent { true }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (entry in HudTourStep.entries) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(if (entry.ordinal <= step.ordinal) Accent else theme.borderColor, CircleShape)
                    )
                }
            }
            Text(step.title, color = theme.textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                step.body,
                modifier = Modifier.fillMaxWidth(),
                color = theme.textColorSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                softWrap = true,
            )
            if (task != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Accent.copy(alpha = 0.12f), theme.buttonShape)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(if (taskDone) "check-circle" else "move", color = Accent, modifier = Modifier.size(16.dp))
                    Text(
                        task,
                        modifier = Modifier.weight(1f),
                        color = theme.textColor,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        softWrap = true,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (!step.isLast) TourButton(tourString("skip"), primary = false, onClick = onSkip)
                Spacer(Modifier.weight(1f))
                if (!step.isFirst) TourButton(tourString("back"), primary = false, onClick = onBack)
                TourButton(
                    tourString(if (step.isLast) "finish" else "next"),
                    primary = true,
                    enabled = !step.interactive || taskDone,
                    onClick = onNext,
                )
            }
        }
    }
}

private fun dragHintProgress(nanos: Long): Float {
    val ms = (nanos / 1_000_000L % 2200L).toFloat()
    val t = ((ms - 400f) / 1200f).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

@Composable
private fun TourDragHint(from: Rect, to: Rect) {
    Canvas(Modifier.fillMaxSize(), live = true) { x, y, _, _ ->
        val progress = dragHintProgress(System.nanoTime())
        val radius = 4f
        val stroke = 1.5f
        dashedRect(x + to.left, y + to.top, to.width, to.height, Accent.copy(alpha = 0.8f), floatArrayOf(6f, 4f))
        dashedLine(
            x + from.center.x, y + from.center.y, x + to.center.x, y + to.center.y,
            Accent.copy(alpha = 0.5f), stroke, floatArrayOf(6f, 4f),
        )
        val gx = x + from.left + (to.left - from.left) * progress
        val gy = y + from.top + (to.top - from.top) * progress
        rect(gx, gy, from.width, from.height, Accent.copy(alpha = 0.2f), radius)
        rectStroke(gx, gy, from.width, from.height, Accent, stroke, radius)
        val cx = gx + from.width / 2f
        val cy = gy + from.height / 2f
        rect(cx - 7f, cy - 7f, 14f, 14f, Color.White, 7f)
        rect(cx - 5f, cy - 5f, 10f, 10f, Accent, 5f)
    }
}

@Composable
private fun TourButton(text: String, primary: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val isHovered by source.collectIsHoveredAsState()
    val base = if (primary) Accent else Color.Transparent
    val background = animateColorAsState(
        if (isHovered && enabled) Color.White.copy(0.12f).compositeOver(base) else base
    )
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f)
    Box(
        modifier = Modifier
            .alpha(alpha)
            .background(background, theme.buttonShape)
            .onClick(source, enabled, onClick)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            color = if (primary) Color.White.copy(0.9f) else theme.textColorSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
