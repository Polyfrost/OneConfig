package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
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
private val TOUR_CARD_WIDTH = 280.dp

private fun placeTourCard(
    root: IntSize,
    card: IntSize,
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
        val cardRect = Rect(slot, Size(card.width.toFloat(), card.height.toFloat()))
        return if (avoid != null && avoid.overlaps(cardRect)) {
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
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val theme = LocalTheme.current
    val density = LocalDensity.current
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }

    val hole = focus?.inflate(with(density) { 6.dp.toPx() })
    val target = hole ?: Rect(Offset(rootSize.width / 2f, rootSize.height / 2f), Size.Zero)
    val holeSpec = tween<Float>(220)
    val holeLeft by animateFloatAsState(target.left, holeSpec, label = "tourHoleLeft")
    val holeTop by animateFloatAsState(target.top, holeSpec, label = "tourHoleTop")
    val holeRight by animateFloatAsState(target.right, holeSpec, label = "tourHoleRight")
    val holeBottom by animateFloatAsState(target.bottom, holeSpec, label = "tourHoleBottom")
    val ringAlpha by animateFloatAsState(if (hole != null) 1f else 0f, holeSpec, label = "tourRingAlpha")
    val scrimAlpha by animateFloatAsState(if (step.interactive) 0f else 1f, holeSpec, label = "tourScrimAlpha")

    val measured = rootSize.width > 0 && cardSize.width > 0
    var cardPlaced by remember { mutableStateOf(false) }
    val cardSlot = if (measured) {
        placeTourCard(
            rootSize,
            cardSize,
            hole,
            with(density) { 12.dp.toPx() },
            with(density) { 16.dp.toPx() },
            bottom = step.interactive,
            avoid = avoid,
        )
    } else {
        Offset.Zero
    }
    val cardSpec = tween<Float>(durationMillis = if (cardPlaced) 220 else 0)
    val cardX by animateFloatAsState(cardSlot.x, cardSpec, label = "tourCardX")
    val cardY by animateFloatAsState(cardSlot.y, cardSpec, label = "tourCardY")
    LaunchedEffect(measured) { if (measured) cardPlaced = true }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { rootSize = it }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = CornerRadius(8.dp.toPx())
            val cutout = Path().apply {
                fillType = PathFillType.EvenOdd
                val margin = 2.dp.toPx()
                addRect(
                    Rect(
                        minOf(0f, holeLeft - margin),
                        minOf(0f, holeTop - margin),
                        maxOf(size.width, holeRight + margin),
                        maxOf(size.height, holeBottom + margin),
                    )
                )
                addRoundRect(RoundRect(holeLeft, holeTop, holeRight, holeBottom, radius))
            }
            drawPath(cutout, tourScrimColor.copy(alpha = tourScrimColor.alpha * scrimAlpha))
            if (ringAlpha > 0f) {
                drawRoundRect(
                    color = Accent.copy(alpha = ringAlpha),
                    topLeft = Offset(holeLeft, holeTop),
                    size = Size(holeRight - holeLeft, holeBottom - holeTop),
                    cornerRadius = radius,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
        }

        if (dragHint != null) TourDragHint(dragHint.first, dragHint.second)

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    val x = if (cardPlaced) cardX else cardSlot.x
                    val y = if (cardPlaced) cardY else cardSlot.y
                    IntOffset(x.roundToInt(), y.roundToInt())
                }
                .graphicsLayer { alpha = if (measured) 1f else 0f }
                .then(cardModifier)
                .onSizeChanged { cardSize = it }
                .width(TOUR_CARD_WIDTH)
                .background(theme.popupBackground, theme.popupShape)
                .border(1.dp, theme.borderColor, theme.popupShape)
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
            Text(step.body, color = theme.textColorSecondary, fontSize = 12.sp, lineHeight = 17.sp)
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
                    Text(task, color = theme.textColor, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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

@Composable
private fun TourDragHint(from: Rect, to: Rect) {
    val transition = rememberInfiniteTransition(label = "tourDragHint")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 2200
                0f at 0
                0f at 400 using FastOutSlowInEasing
                1f at 1600
                1f at 2200
            }
        ),
        label = "tourDragProgress",
    )
    Canvas(Modifier.fillMaxSize()) {
        val radius = CornerRadius(4.dp.toPx())
        val stroke = 1.5.dp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
        drawRoundRect(
            color = Accent.copy(alpha = 0.8f),
            topLeft = to.topLeft,
            size = to.size,
            cornerRadius = radius,
            style = Stroke(width = stroke, pathEffect = dash),
        )
        drawLine(Accent.copy(alpha = 0.5f), from.center, to.center, strokeWidth = stroke, pathEffect = dash)
        val ghost = from.translate((to.topLeft - from.topLeft) * progress)
        drawRoundRect(Accent.copy(alpha = 0.2f), ghost.topLeft, ghost.size, radius)
        drawRoundRect(Accent, ghost.topLeft, ghost.size, radius, style = Stroke(width = stroke))
        drawCircle(Color.White, radius = 7.dp.toPx(), center = ghost.center)
        drawCircle(Accent, radius = 5.dp.toPx(), center = ghost.center)
    }
}

@Composable
private fun TourButton(text: String, primary: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val isHovered by source.collectIsHoveredAsState()
    val base = if (primary) Accent else Color.Transparent
    val background by animateColorAsState(
        if (isHovered && enabled) Color.White.copy(0.12f).compositeOver(base) else base
    )
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f, label = "tourButtonAlpha")
    Box(
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(theme.buttonShape)
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
