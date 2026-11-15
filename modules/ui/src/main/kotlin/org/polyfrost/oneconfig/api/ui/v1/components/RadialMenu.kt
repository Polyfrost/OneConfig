@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Announce
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.a11y.a11y
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigUITheme
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeStyle

@ExperimentalOneConfigUiApi
@Immutable
public class RadialMenuItem(
    public val label: String,
    public val icon: String? = null,
    public val detail: String? = null,
    public val enabled: Boolean = true,
    public val dot: Color? = null,
    public val drawIcon: (DrawScope.(size: Size, tint: Color) -> Unit)? = null,
)

@ExperimentalOneConfigUiApi
public enum class RadialMenuInput {
    Pointer,

    Steered,
}

@ExperimentalOneConfigUiApi
@Immutable
public class RadialMenuSounds(
    public val open: UiSound? = UiSound.Open,
    public val highlight: UiSound? = UiSound.SliderTick,
    public val select: UiSound? = UiSound.Click,
    public val dismiss: UiSound? = UiSound.Close,
)

@ExperimentalOneConfigUiApi
public object RadialMenuDefaults {
    public const val fadeMillis: Int = 120

    public val minDiameter: Dp = 240.dp

    public val maxDiameter: Dp = 340.dp

    public const val windowShare: Float = 0.34f

    public val sounds: RadialMenuSounds = RadialMenuSounds()

    public val silent: RadialMenuSounds = RadialMenuSounds(null, null, null, null)

    public val pixelRatio: Float
        get() = runCatching { Platform.screen().pixelRatio() }.getOrDefault(1f).takeIf { it > 0f && it.isFinite() } ?: 1f

    public val glass: Boolean
        @Composable get() = OneConfigTheme.current.style == ThemeStyle.Glass &&
            runCatching { Platform.screen().supportsBackdropGlass() }.getOrDefault(false)

    public fun hint(input: RadialMenuInput): String = when (input) {
        RadialMenuInput.Pointer -> layoutString("oneconfig.ui.radial.hint.pointer", "Esc to cancel")
        RadialMenuInput.Steered -> layoutString("oneconfig.ui.radial.hint.steered", "Right click to cancel")
    }

    public fun numberHint(count: Int): String {
        val keys = when {
            count >= MAX_NUMBER_KEYS -> "1-9, 0"
            count <= 1 -> "1"
            else -> "1-$count"
        }
        return layoutString("oneconfig.ui.radial.hint.numbers", "Keys %s pick, Esc cancels", keys)
    }
}

@ExperimentalOneConfigUiApi
@Stable
public class RadialMenuState {
    public var pointer: Offset by mutableStateOf(Offset.Zero)
        private set

    public var highlighted: Int by mutableIntStateOf(-1)
        private set

    public var closing: Boolean by mutableStateOf(false)
        private set

    public var picked: Int by mutableIntStateOf(-1)
        private set

    public var instantClose: Boolean = false
        private set

    @Volatile
    private var radius = 0f

    @Volatile
    private var square = false

    @Volatile
    private var deadZone = 0f

    @Volatile
    private var slotRadius = 0f

    @Volatile
    private var enabled: List<Boolean> = emptyList()

    private var raw = Offset.Zero
    private var aimed = -1
    private var configured = false

    public fun steer(dx: Float, dy: Float) {
        if (closing) return
        move(raw.x + dx, raw.y + dy)
    }

    public fun aimAt(x: Float, y: Float) {
        if (closing) return
        move(x, y)
    }

    public fun highlight(index: Int): Boolean {
        if (closing || index !in enabled.indices || !enabled[index]) return false
        aimed = index
        val angle = midAngle(index, 360f / enabled.size)
        val at = Offset((cos(angle) * slotRadius).toFloat(), (sin(angle) * slotRadius).toFloat())
        raw = at
        pointer = at
        highlighted = index
        return true
    }

    public fun step(delta: Int): Boolean {
        val count = enabled.size
        if (closing || count == 0 || delta == 0) return false
        val direction = sign(delta.toFloat()).toInt()
        var at = if (highlighted >= 0) highlighted else if (direction > 0) -1 else 0
        repeat(count) {
            at = Math.floorMod(at + direction, count)
            if (enabled[at]) return highlight(at)
        }
        return false
    }

    public fun select(instant: Boolean = false): Int {
        if (closing) return picked
        picked = highlighted
        instantClose = instant
        closing = true
        return picked
    }

    public fun dismiss(instant: Boolean = false) {
        if (closing) return
        picked = -1
        instantClose = instant
        closing = true
    }

    public fun reset() {
        raw = Offset.Zero
        aimed = -1
        pointer = Offset.Zero
        highlighted = -1
        picked = -1
        instantClose = false
        closing = false
    }

    internal fun configure(radius: Float, square: Boolean, deadZone: Float, slotRadius: Float, enabled: List<Boolean>) {
        if (configured && this.radius == radius && this.square == square && this.deadZone == deadZone &&
            this.slotRadius == slotRadius && this.enabled == enabled
        ) return
        configured = true
        this.radius = radius
        this.square = square
        this.deadZone = deadZone
        this.slotRadius = slotRadius
        this.enabled = enabled
        if (!closing) move(raw.x, raw.y)
    }

    private fun move(x0: Float, y0: Float) {
        var x = x0
        var y = y0
        val limit = radius
        if (limit > 0f) {
            val reach = reach(x, y)
            val cap = limit * RAW_CAP
            if (reach > cap) {
                x *= cap / reach
                y *= cap / reach
            }
        }
        raw = Offset(x, y)
        val next = if (limit > 0f) rubberBand(x, y, limit) else raw
        pointer = next
        highlighted = aim(next)
    }

    private fun reach(x: Float, y: Float): Float = if (square) max(abs(x), abs(y)) else hypot(x, y)

    private fun rubberBand(x: Float, y: Float, limit: Float): Offset {
        val reach = reach(x, y)
        val knee = limit * KNEE_SHARE
        if (reach <= knee) return Offset(x, y)
        val band = limit - knee
        val soft = knee + band * (1f - exp(-(reach - knee) / band))
        return Offset(x * (soft / reach), y * (soft / reach))
    }

    private fun aim(p: Offset): Int {
        val flags = enabled
        val count = flags.size
        if (count == 0 || reach(p.x, p.y) < deadZone) {
            aimed = -1
            return -1
        }
        val sector = 360f / count
        val angle = (Math.toDegrees(atan2(p.y.toDouble(), p.x.toDouble())).toFloat() + 90f + 720f) % 360f
        val previous = aimed
        val keep = previous in 0 until count && run {
            var diff = abs(angle - previous * sector) % 360f
            if (diff > 180f) diff = 360f - diff
            diff <= sector / 2f + minOf(HYSTERESIS_DEG, sector * HYSTERESIS_SHARE)
        }
        if (!keep) aimed = ((angle + sector / 2f) / sector).toInt() % count
        return if (flags[aimed]) aimed else -1
    }

    private companion object {
        const val RAW_CAP = 1.25f
        const val KNEE_SHARE = 0.85f
        const val HYSTERESIS_DEG = 5f
        const val HYSTERESIS_SHARE = 0.2f
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun rememberRadialMenuState(): RadialMenuState = remember { RadialMenuState() }

private const val MAX_NUMBER_KEYS = 10
private const val HUB_SHARE = 0.44f
private const val HIDDEN_SCALE = 0.9f
private const val HOVER_SCALE = 0.06f
private const val SURFACE_ALPHA = 0.78f
private const val HUB_ALPHA = 0.88f
private const val EMPTY_SURFACE_ALPHA = 0.75f
private const val SHADOW_ALPHA = 0.22f
private const val HALO_ALPHA = 0.05f
private const val HALO_RINGS = 6
private const val ARC_STEP_DEG = 3.0
private const val HOVER_MILLIS = 110
private const val MAX_GLASS_RADIUS_PX = 255f
private const val HINT_ALPHA = 0.85f
private const val HINT_SCALE = 0.85f
private const val NUMBER_ALPHA = 0.7f
private const val DISABLED_LIFT = 0.15f
private const val GLYPH_EM = 10
private const val PIXEL_ICON_CELLS = 12
private const val PIXEL_ICON_ROUND_UP = 0.6f
private const val CURSOR_SHADOW_ALPHA = 0.35f
private const val CURSOR_OUTLINE_ALPHA = 0.85f
private const val CURSOR_ARM_UNITS = 3
private const val QUARTER = PI / 2
private const val EIGHTH = PI / 4
private const val DEAD_ZONE_SHARE = 0.45f
private const val KNOB_OMEGA = 95f
private const val MAX_SPRING_STEP = 0.1f
private const val SETTLE_EPSILON = 1e-4f
private const val BAND_ALPHA = 0.55f
private const val BAND_THIN = 0.35f
private const val BAND_PIXEL_ROOT = 4f
private const val BAND_PIXEL_TIP = 2f
private const val SCROLL_NOTCH = 1f

private val WEDGE_GAP = 3.dp
private val HUB_GAP = 8.dp
private val HOVER_POP = 3.dp
private val ICON_SIZE = 22.dp
private val SLOT_MIN_WIDTH = 40.dp
private val SLOT_INSET = 10.dp
private val HALO_STEP = 1.5.dp
private val SHADOW_DROP = 2.dp
private val CURSOR_DOT = 4.dp
private val CURSOR_RING = 1.5.dp
private val CURSOR_SHADOW = 7.dp
private val BAND_ROOT = 7.dp
private val BAND_TIP = 2.5.dp

private val PopEasing = CubicBezierEasing(0.0f, 0.55f, 0.45f, 1.0f)

private class WheelColors(
    val surface: Color,
    val hub: Color,
    val empty: Color,
)

private class WheelText(
    val label: TextStyle,
    val number: TextStyle,
    val eyebrow: TextStyle,
    val headline: TextStyle,
    val detail: TextStyle,
    val hint: TextStyle,
)

@ExperimentalOneConfigUiApi
@Composable
public fun RadialMenu(
    items: List<RadialMenuItem>,
    state: RadialMenuState,
    modifier: Modifier = Modifier,
    onSelect: ((Int) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    title: String? = null,
    subtitle: String? = null,
    eyebrow: String? = null,
    input: RadialMenuInput = RadialMenuInput.Pointer,
    hint: String? = if (onDismiss != null || input != RadialMenuInput.Pointer) RadialMenuDefaults.hint(input) else null,
    showNumbers: Boolean = false,
    showCursor: Boolean = input == RadialMenuInput.Steered,
    glass: Boolean = RadialMenuDefaults.glass,
    surfaceAlpha: Float? = null,
    hubAlpha: Float? = null,
    sounds: RadialMenuSounds = RadialMenuDefaults.sounds,
    pixelRatio: Float = RadialMenuDefaults.pixelRatio,
    minDiameter: Dp = RadialMenuDefaults.minDiameter,
    maxDiameter: Dp = RadialMenuDefaults.maxDiameter,
    onScroll: ((Int) -> Unit)? = null,
    center: (@Composable (highlighted: RadialMenuItem?) -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
    slot: (@Composable (item: RadialMenuItem, index: Int, highlighted: Boolean, tint: Color) -> Unit)? = null,
) {
    val theme = OneConfigTheme.current
    val square = theme.style == ThemeStyle.Pixel
    val reduced = Motion.reduced
    val visibility = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(state.closing) {
        val target = if (state.closing) 0f else 1f
        if (Motion.reduced || state.instantClose) visibility.snapTo(target)
        else visibility.animateTo(target, tween(RadialMenuDefaults.fadeMillis, easing = PopEasing))
    }
    RadialMenuSoundEffects(state, sounds)
    val active = state.highlighted
    val activeItem = items.getOrNull(active)
    Announce(activeItem?.let { listOfNotNull(it.label, it.detail).joinToString(", ") })

    val colors = wheelColors(theme, glass, surfaceAlpha, hubAlpha)
    val text = wheelText(theme, square, pixelRatio)
    val shape: Shape = theme.shapes.circle
    val latestSelect by rememberUpdatedState(onSelect)
    val latestDismiss by rememberUpdatedState(onDismiss)
    val latestScroll by rememberUpdatedState(onScroll)
    val pointerMode = input == RadialMenuInput.Pointer
    if (pointerMode && onDismiss != null && !state.closing) EscapeCloses { if (!state.closing) { state.dismiss(); latestDismiss?.invoke() } }

    fun pick() {
        val chosen = state.select()
        if (chosen >= 0) latestSelect?.invoke(chosen) else latestDismiss?.invoke()
    }

    val focus = remember { FocusRequester() }
    if (pointerMode) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .then(
                if (!pointerMode) Modifier else Modifier
                    .a11y(title ?: eyebrow, A11yRole.Dialog, null, null, null, false)
                    .pointerInput(state) {
                        var armed = false
                        var scrollCarry = 0f
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue
                                val middle = Offset(size.width / 2f, size.height / 2f)
                                val at = (change.position - middle) / density
                                when (event.type) {
                                    PointerEventType.Move, PointerEventType.Enter -> state.aimAt(at.x, at.y)
                                    PointerEventType.Press -> {
                                        state.aimAt(at.x, at.y)
                                        if (event.buttons.isSecondaryPressed) {
                                            armed = false
                                            if (!state.closing) {
                                                state.dismiss()
                                                latestDismiss?.invoke()
                                            }
                                        } else if (event.buttons.isPrimaryPressed) {
                                            armed = true
                                        }
                                        change.consume()
                                    }
                                    PointerEventType.Release -> {
                                        if (armed && !event.buttons.isPrimaryPressed) {
                                            armed = false
                                            if (!state.closing) pick()
                                        }
                                        change.consume()
                                    }
                                    PointerEventType.Scroll -> {
                                        val scroll = latestScroll ?: continue
                                        scrollCarry += change.scrollDelta.y
                                        while (abs(scrollCarry) >= SCROLL_NOTCH) {
                                            val down = scrollCarry > 0f
                                            scroll(if (down) 1 else -1)
                                            scrollCarry -= if (down) SCROLL_NOTCH else -SCROLL_NOTCH
                                        }
                                        change.consume()
                                    }
                                }
                            }
                        }
                    }
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown || state.closing) return@onKeyEvent false
                        val number = numberKey(event.key)
                        when {
                            number != null -> {
                                if (number < items.size && state.highlight(number)) pick()
                                true
                            }
                            event.key == Key.Tab -> state.step(if (event.isShiftPressed) -1 else 1)
                            event.key == Key.DirectionRight || event.key == Key.DirectionDown -> state.step(1)
                            event.key == Key.DirectionLeft || event.key == Key.DirectionUp -> state.step(-1)
                            event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.Spacebar -> {
                                pick()
                                true
                            }
                            else -> false
                        }
                    }
                    .focusRequester(focus)
                    .focusable(),
            ),
        contentAlignment = Alignment.Center,
    ) {
        val count = items.size
        val sector = if (count == 0) 0f else 360f / count
        val diameter = (min(maxWidth, maxHeight) * RadialMenuDefaults.windowShare).coerceIn(minDiameter, maxDiameter)
        val hub = diameter * HUB_SHARE
        val outer = diameter / 2
        val inner = hub / 2 + HUB_GAP
        val hubHalf = hub.value / 2f
        val flags = items.map { it.enabled }
        SideEffect { state.configure(outer.value, square, hubHalf * DEAD_ZONE_SHARE, (outer.value + inner.value) / 2f, flags) }
        Box(
            Modifier.size(diameter).graphicsLayer {
                val progress = visibility.value
                val scale = HIDDEN_SCALE + (1f - HIDDEN_SCALE) * progress
                scaleX = scale
                scaleY = scale
                alpha = progress
            },
            contentAlignment = Alignment.Center,
        ) {
            val pops = hoverPops(count, active)
            val glassRing = glass && fitsGlass(outer, pixelRatio)
            if (glassRing) Box(Modifier.size(diameter).liquidGlass(shape, frosted = true))
            WheelRing(items, pops, sector, outer, inner, colors, glass, square)
            val iconSize = wheelIconSize(square, pixelRatio)
            items.forEachIndexed { i, item ->
                WheelSlot(item, i, i == active, sector, pops[i], outer, inner, square, iconSize, showNumbers, text, slot)
            }
            WheelHub(activeItem, title, subtitle, eyebrow, hint, hub, shape, colors.hub, glass && fitsGlass(hub / 2, pixelRatio), text, center)
            if (showCursor) WheelCursor(state, square, hub / 2, outer, Modifier.matchParentSize())
        }
        if (below != null) {
            Box(Modifier.offset(y = outer + theme.spacing.l + theme.spacing.xxs).graphicsLayer { alpha = visibility.value }) { below() }
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun PageDots(
    count: Int,
    index: Int,
    modifier: Modifier = Modifier,
    framed: Boolean = false,
    glass: Boolean = true,
) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    val label = layoutString("oneconfig.ui.radial.page", "Page %s of %s", index + 1, count)
    Row(
        modifier
            .then(if (framed) componentsBridge().popupSurface(Modifier, theme.popupShape, glass, glass).padding(horizontal = spacing.m, vertical = spacing.s) else Modifier)
            .a11y(label, A11yRole.ProgressBar, null, null, null, false),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { page -> PageDot(page == index) }
    }
}

private val DOT_SIZE = 6.dp
private val ACTIVE_DOT_WIDTH = 18.dp
private const val DOT_MILLIS = 140

@Composable
private fun PageDot(active: Boolean) {
    val theme = OneConfigTheme.current
    val width by Motion.animateDpAsState(if (active) ACTIVE_DOT_WIDTH else DOT_SIZE, tween(DOT_MILLIS), "pageDot")
    val rest = theme.textColorSecondary
    Box(
        Modifier
            .width(width)
            .height(DOT_SIZE)
            .clip(theme.shapes.pill)
            .background(if (active) OneConfigTheme.accent else rest.copy(alpha = rest.alpha * theme.disabledAlpha)),
    )
}

@Composable
private fun RadialMenuSoundEffects(state: RadialMenuState, sounds: RadialMenuSounds) {
    LaunchedEffect(Unit) { sounds.open?.let(::playUiSound) }
    val seen = remember { intArrayOf(-1) }
    LaunchedEffect(state.highlighted) {
        val now = state.highlighted
        if (now >= 0 && now != seen[0] && !state.closing) sounds.highlight?.let(::playUiSound)
        seen[0] = now
    }
    LaunchedEffect(state.closing) {
        if (!state.closing) return@LaunchedEffect
        (if (state.picked >= 0) sounds.select else sounds.dismiss)?.let(::playUiSound)
    }
}

private fun numberKey(key: Key): Int? = when (key) {
    Key.One, Key.NumPad1 -> 0
    Key.Two, Key.NumPad2 -> 1
    Key.Three, Key.NumPad3 -> 2
    Key.Four, Key.NumPad4 -> 3
    Key.Five, Key.NumPad5 -> 4
    Key.Six, Key.NumPad6 -> 5
    Key.Seven, Key.NumPad7 -> 6
    Key.Eight, Key.NumPad8 -> 7
    Key.Nine, Key.NumPad9 -> 8
    Key.Zero, Key.NumPad0 -> 9
    else -> null
}

private fun wheelColors(theme: OneConfigUITheme, glass: Boolean, forced: Float?, forcedHub: Float?): WheelColors {
    val base = theme.popupBackground
    val surface = when {
        forced != null -> base.copy(alpha = forced)
        glass || theme.style == ThemeStyle.Pixel || base.alpha >= SURFACE_ALPHA -> base
        else -> base.copy(alpha = SURFACE_ALPHA)
    }
    val hub = when {
        forcedHub != null -> base.copy(alpha = forcedHub)
        forced == null && !glass && surface.alpha < HUB_ALPHA -> surface.copy(alpha = HUB_ALPHA)
        else -> surface
    }
    return WheelColors(surface, hub, surface.copy(alpha = surface.alpha * EMPTY_SURFACE_ALPHA))
}

@Composable
private fun wheelText(theme: OneConfigUITheme, square: Boolean, ratio: Float): WheelText {
    val type = theme.typography
    val density = LocalDensity.current
    fun TextStyle.snapped(): TextStyle {
        if (!square || !fontSize.isSp) return this
        val pxPerSp = density.density * density.fontScale * ratio
        val scale = max(1, (fontSize.value * pxPerSp / GLYPH_EM).roundToInt())
        return copy(fontSize = (scale * GLYPH_EM / pxPerSp).sp)
    }
    val caption = type.caption
    return WheelText(
        label = type.label.snapped(),
        number = caption.snapped(),
        eyebrow = type.eyebrow.snapped(),
        headline = type.body.copy(fontWeight = FontWeight.SemiBold).snapped(),
        detail = caption.snapped(),
        hint = caption.copy(fontSize = caption.fontSize * HINT_SCALE).snapped(),
    )
}

@Composable
private fun wheelIconSize(square: Boolean, ratio: Float): Dp {
    if (!square) return ICON_SIZE
    val pxPerDp = LocalDensity.current.density * ratio
    val scale = max(1, (ICON_SIZE.value * pxPerDp / PIXEL_ICON_CELLS + PIXEL_ICON_ROUND_UP).toInt())
    return (scale * PIXEL_ICON_CELLS / pxPerDp).dp
}

@Composable
private fun hoverPops(count: Int, active: Int): List<State<Float>> = (0 until count).map { i ->
    Motion.animateFloatAsState(if (i == active) 1f else 0f, tween(HOVER_MILLIS), "radialPop")
}

@Composable
private fun fitsGlass(radius: Dp, ratio: Float): Boolean = with(LocalDensity.current) { radius.toPx() * ratio <= MAX_GLASS_RADIUS_PX }

private fun midAngle(index: Int, sector: Float): Double = Math.toRadians(index * sector - 90.0)

private fun squareReach(angle: Double): Float = max(abs(cos(angle)), abs(sin(angle))).toFloat()

@Composable
private fun WheelRing(
    items: List<RadialMenuItem>,
    pops: List<State<Float>>,
    sector: Float,
    outer: Dp,
    inner: Dp,
    colors: WheelColors,
    glass: Boolean,
    square: Boolean,
) {
    val theme = OneConfigTheme.current
    val accent = OneConfigTheme.accent
    val border = theme.borderColor
    val shadow = theme.shadowColor
    val halo = theme.shadowEnabled || square
    val cornerShape = theme.buttonShape as? CornerBasedShape
    val enabled = items.map { it.enabled }
    Spacer(
        Modifier.size(outer * 2).drawWithCache {
            val corner = cornerShape?.topStart?.toPx(size, this) ?: 0f
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerPx = if (square) outer.toPx().roundToInt().toFloat() else outer.toPx()
            val innerPx = if (square) inner.toPx().roundToInt().toFloat() else inner.toPx()
            val drop = Offset(0f, SHADOW_DROP.toPx())
            val haloStep = HALO_STEP.toPx()
            val haloStroke = Stroke(haloStep)
            val line = if (square) max(1f, 1.dp.toPx().roundToInt().toFloat()) else 1.dp.toPx()
            val stroke = Stroke(line)
            val gap = if (enabled.size > 1) WEDGE_GAP.toPx() else 0f
            val pop = HOVER_POP.toPx()
            val shadowFill = shadow.copy(alpha = SHADOW_ALPHA)
            val haloColor = shadow.copy(alpha = HALO_ALPHA)
            val paths = List(enabled.size) { i ->
                val mid = midAngle(i, sector)
                val start = mid - Math.toRadians(sector / 2.0)
                val end = mid + Math.toRadians(sector / 2.0)
                if (square) squareWedgePath(center, innerPx, outerPx, start, end, gap / 2f)
                else wedgePath(center, innerPx, outerPx, start, end, gap / 2f, corner)
            }
            val shifts = List(enabled.size) { i ->
                val mid = midAngle(i, sector)
                Offset((cos(mid) * pop).toFloat(), (sin(mid) * pop).toFloat())
            }
            onDrawBehind {
                if (halo) {
                    if (square) {
                        if (!glass) drawRect(shadowFill, center + drop - Offset(outerPx, outerPx), Size(outerPx * 2, outerPx * 2))
                        for (ring in 1..HALO_RINGS) {
                            val r = outerPx + ring * haloStep
                            drawRect(haloColor, center + drop - Offset(r, r), Size(r * 2, r * 2), style = haloStroke)
                        }
                    } else {
                        if (!glass) drawCircle(shadowFill, outerPx, center + drop)
                        for (ring in 1..HALO_RINGS) drawCircle(haloColor, outerPx + ring * haloStep, center + drop, style = haloStroke)
                    }
                }
                for (i in paths.indices) {
                    val p = pops[i].value
                    val fill = if (enabled[i]) lerp(colors.surface, accent, p) else colors.empty
                    val shift = shifts[i]
                    translate(shift.x * p, shift.y * p) {
                        drawPath(paths[i], fill)
                        drawPath(paths[i], if (p > 0.5f) accent else border, style = stroke)
                    }
                }
            }
        },
    )
}

private fun wedgePath(center: Offset, inner: Float, outer: Float, start: Double, end: Double, halfGap: Float, corner: Float): Path {
    fun polar(r: Float, a: Double) = Offset(center.x + (cos(a) * r).toFloat(), center.y + (sin(a) * r).toFloat())
    fun startEdge(r: Float) = polar(r, start + asin((halfGap / r).coerceAtMost(1f).toDouble()))
    fun endEdge(r: Float) = polar(r, end - asin((halfGap / r).coerceAtMost(1f).toDouble()))

    val outerStart = start + asin((halfGap / outer).toDouble())
    val outerEnd = end - asin((halfGap / outer).toDouble())
    val innerStart = start + asin((halfGap / inner).coerceAtMost(1f).toDouble())
    val innerEnd = end - asin((halfGap / inner).coerceAtMost(1f).toDouble())
    val innerArc = (innerEnd - innerStart).coerceAtLeast(0.0) * inner
    val c = corner.coerceAtMost((outer - inner) / 3f).coerceAtMost((innerArc / 3.0).toFloat()).coerceAtLeast(0f)

    val path = Path()
    fun arc(r: Float, from: Double, to: Double) {
        val steps = max(1, ceil(Math.toDegrees(abs(to - from)) / ARC_STEP_DEG).toInt())
        for (s in 1..steps) {
            val point = polar(r, from + (to - from) * s / steps)
            path.lineTo(point.x, point.y)
        }
    }

    val begin = startEdge(inner + c)
    path.moveTo(begin.x, begin.y)
    startEdge(outer - c).let { path.lineTo(it.x, it.y) }
    val outerA = outerStart + c / outer
    val outerB = outerEnd - c / outer
    startEdge(outer).let { control -> polar(outer, outerA).let { path.quadraticTo(control.x, control.y, it.x, it.y) } }
    arc(outer, outerA, outerB)
    endEdge(outer).let { control -> endEdge(outer - c).let { path.quadraticTo(control.x, control.y, it.x, it.y) } }
    endEdge(inner + c).let { path.lineTo(it.x, it.y) }
    val innerB = innerEnd - c / inner
    val innerA = innerStart + c / inner
    endEdge(inner).let { control -> polar(inner, innerB).let { path.quadraticTo(control.x, control.y, it.x, it.y) } }
    arc(inner, innerB, innerA)
    startEdge(inner).let { control -> path.quadraticTo(control.x, control.y, begin.x, begin.y) }
    path.close()
    return path
}

private fun squareWedgePath(center: Offset, inner: Float, outer: Float, start: Double, end: Double, halfGap: Float): Path {
    fun cut(half: Float, angle: Double, shift: Float): Offset {
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        val nx = -dy * shift
        val ny = dx * shift
        var t = Float.MAX_VALUE
        if (abs(dx) > 1e-6f) t = minOf(t, ((if (dx > 0f) half else -half) - nx) / dx)
        if (abs(dy) > 1e-6f) t = minOf(t, ((if (dy > 0f) half else -half) - ny) / dy)
        return Offset(center.x + nx + dx * t, center.y + ny + dy * t)
    }
    fun corner(half: Float, angle: Double) =
        Offset(center.x + sign(cos(angle)).toFloat() * half, center.y + sign(sin(angle)).toFloat() * half)

    val first = floor((start - EIGHTH) / QUARTER).toInt() + 1
    val corners = generateSequence(first) { it + 1 }.map { EIGHTH + it * QUARTER }.takeWhile { it < end }.toList()

    val path = Path()
    cut(outer, start, halfGap).let { path.moveTo(it.x, it.y) }
    for (a in corners) corner(outer, a).let { path.lineTo(it.x, it.y) }
    cut(outer, end, -halfGap).let { path.lineTo(it.x, it.y) }
    cut(inner, end, -halfGap).let { path.lineTo(it.x, it.y) }
    for (a in corners.asReversed()) corner(inner, a).let { path.lineTo(it.x, it.y) }
    cut(inner, start, halfGap).let { path.lineTo(it.x, it.y) }
    path.close()
    return path
}

@Composable
private fun WheelSlot(
    item: RadialMenuItem,
    index: Int,
    hovered: Boolean,
    sector: Float,
    pop: State<Float>,
    outer: Dp,
    inner: Dp,
    square: Boolean,
    iconSize: Dp,
    showNumber: Boolean,
    text: WheelText,
    slot: (@Composable (item: RadialMenuItem, index: Int, highlighted: Boolean, tint: Color) -> Unit)?,
) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    val ink = when {
        hovered -> theme.accentTextColor
        item.enabled -> theme.textColor
        else -> theme.textColorSecondary
    }
    val mid = midAngle(index, sector)
    val dirX = cos(mid).toFloat()
    val dirY = sin(mid).toFloat()
    val base = (outer + inner) / 2
    val radius = if (square) base / squareReach(mid) else base
    val chord = base * (2.0 * sin(PI / max(2.0, 360.0 / sector))).toFloat()
    val width = min(chord - WEDGE_GAP * 2 - SLOT_INSET, (outer - inner) * 1.4f).coerceAtLeast(SLOT_MIN_WIDTH)
    val disabledAlpha = theme.disabledAlpha
    Column(
        modifier = Modifier
            .offset {
                val r = radius.toPx()
                IntOffset((dirX * r).roundToInt(), (dirY * r).roundToInt())
            }
            .width(width)
            .graphicsLayer {
                val p = pop.value
                val shift = HOVER_POP.toPx() * p
                translationX = dirX * shift
                translationY = dirY * shift
                val scale = 1f + HOVER_SCALE * p
                scaleX = scale
                scaleY = scale
                alpha = if (item.enabled) 1f else (disabledAlpha + DISABLED_LIFT).coerceAtMost(1f)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        if (slot != null) {
            slot(item, index, hovered, ink)
            return@Column
        }
        val draw = item.drawIcon
        if (draw != null) {
            WheelIcon(draw, ink, square, iconSize)
        } else if (item.icon != null) {
            Icon(item.icon, null, Modifier.size(iconSize), ink)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
            item.dot?.let { color -> Box(Modifier.size(StatusDotDefaults.size).clip(theme.shapes.circle).background(color)) }
            LayoutText(item.label, text.label, ink, Modifier.widthIn(max = width), maxLines = 2, align = TextAlign.Center)
        }
        if (showNumber && index < MAX_NUMBER_KEYS) {
            LayoutText("${(index + 1) % MAX_NUMBER_KEYS}", text.number, ink.copy(alpha = ink.alpha * NUMBER_ALPHA), maxLines = 1, align = TextAlign.Center)
        }
    }
}

@Composable
private fun WheelIcon(draw: DrawScope.(Size, Color) -> Unit, ink: Color, square: Boolean, iconSize: Dp) {
    val origin = remember { FloatArray(2) }
    Spacer(
        Modifier
            .size(iconSize)
            .then(if (square) Modifier.onPlaced { val at = it.positionInRoot(); origin[0] = at.x; origin[1] = at.y } else Modifier)
            .drawBehind {
                if (square) {
                    translate(origin[0].roundToInt() - origin[0], origin[1].roundToInt() - origin[1]) { draw(size, ink) }
                } else {
                    draw(size, ink)
                }
            },
    )
}

@Composable
private fun WheelHub(
    hovered: RadialMenuItem?,
    title: String?,
    subtitle: String?,
    eyebrow: String?,
    hint: String?,
    hub: Dp,
    shape: Shape,
    surface: Color,
    glass: Boolean,
    text: WheelText,
    center: (@Composable (RadialMenuItem?) -> Unit)?,
) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    Box(
        modifier = Modifier
            .size(hub)
            .then(if (glass) Modifier.liquidGlass(shape, frosted = true) else Modifier)
            .clip(shape)
            .background(surface)
            .border(1.dp, theme.borderColor, shape)
            .padding(horizontal = spacing.l + spacing.xxs, vertical = spacing.l),
        contentAlignment = Alignment.Center,
    ) {
        if (center != null) {
            center(hovered)
            return@Box
        }
        val headline = hovered?.label ?: title
        val detail = if (hovered != null) hovered.detail else subtitle
        val secondary = theme.textColorSecondary
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            if (eyebrow != null) LayoutText(eyebrow.uppercase(), text.eyebrow, secondary, maxLines = 1, align = TextAlign.Center)
            if (headline != null) LayoutText(headline, text.headline, theme.textColor, maxLines = 2, align = TextAlign.Center)
            if (detail != null) LayoutText(detail, text.detail, secondary, maxLines = 2, align = TextAlign.Center)
            if (hint != null) LayoutText(hint, text.hint, secondary.copy(alpha = secondary.alpha * HINT_ALPHA), maxLines = 2, align = TextAlign.Center)
        }
    }
}

private class KnobSpring {
    var position by mutableStateOf(Offset.Zero)
    private var velocity = Offset.Zero

    fun step(target: Offset, dt: Float) {
        val y = position - target
        if (y.getDistanceSquared() < SETTLE_EPSILON && velocity.getDistanceSquared() < SETTLE_EPSILON) {
            if (position != target) position = target
            velocity = Offset.Zero
            return
        }
        val decay = exp(-KNOB_OMEGA * dt)
        val drive = velocity + y * KNOB_OMEGA
        position = target + (y + drive * dt) * decay
        velocity = (velocity - drive * (KNOB_OMEGA * dt)) * decay
    }
}

@Composable
private fun WheelCursor(state: RadialMenuState, square: Boolean, hubRadius: Dp, outer: Dp, modifier: Modifier) {
    val accent = OneConfigTheme.accent
    val theme = OneConfigTheme.current
    val shadow = theme.shadowColor.takeIf { theme.shadowEnabled || square }
    val ink = theme.onMedia
    val outline = ink.copy(alpha = CURSOR_OUTLINE_ALPHA)
    val inkEdge = theme.onMediaShadow.copy(alpha = CURSOR_OUTLINE_ALPHA)
    val band = accent.copy(alpha = accent.alpha * BAND_ALPHA)
    val reduced = Motion.reduced
    val knob = remember(state) { KnobSpring() }
    val path = remember { Path() }
    LaunchedEffect(state, reduced) {
        if (reduced) return@LaunchedEffect
        var last = System.nanoTime()
        while (true) {
            withFrameNanos {
                val now = System.nanoTime()
                val dt = ((now - last) / 1_000_000_000f).coerceIn(0f, MAX_SPRING_STEP)
                last = now
                knob.step(if (state.closing) Offset.Zero else state.pointer, dt)
            }
        }
    }
    Spacer(
        modifier.drawBehind {
            val at = if (reduced) state.pointer else knob.position
            val center = Offset(size.width / 2f, size.height / 2f)
            val tip = Offset(at.x.dp.toPx(), at.y.dp.toPx())
            val hubPx = hubRadius.toPx()
            val stretch = if (reduced) 0f else ((tip.getDistance() - hubPx) / (outer.toPx() - hubPx).coerceAtLeast(1f)).coerceIn(0f, 1f)
            if (square) drawPixelBand(path, center, tip, hubPx, stretch, band) else drawRoundBand(path, center, tip, hubPx, stretch, band)
            val x = center.x + tip.x
            val y = center.y + tip.y
            if (square) drawPixelCursor(x, y, accent, shadow, ink, inkEdge) else drawRoundCursor(x, y, accent, outline, shadow)
        },
    )
}

private fun bandStart(tip: Offset, hubPx: Float, square: Boolean): Offset? {
    val length = tip.getDistance()
    if (length < 1e-3f) return null
    val dir = tip / length
    val edge = if (square) hubPx / max(abs(dir.x), abs(dir.y)) else hubPx
    return if (length > edge) dir * edge else null
}

private fun DrawScope.drawRoundBand(path: Path, center: Offset, tip: Offset, hubPx: Float, stretch: Float, color: Color) {
    val start = bandStart(tip, hubPx, false) ?: return
    val dir = tip / tip.getDistance()
    val normal = Offset(-dir.y, dir.x)
    val root = BAND_ROOT.toPx() * (1f - BAND_THIN * stretch) / 2f
    val end = BAND_TIP.toPx() / 2f
    val a = center + start
    val b = center + tip
    path.reset()
    path.moveTo(a.x + normal.x * root, a.y + normal.y * root)
    path.lineTo(b.x + normal.x * end, b.y + normal.y * end)
    path.lineTo(b.x - normal.x * end, b.y - normal.y * end)
    path.lineTo(a.x - normal.x * root, a.y - normal.y * root)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawPixelBand(path: Path, center: Offset, tip: Offset, hubPx: Float, stretch: Float, color: Color) {
    val start = bandStart(tip, hubPx, true) ?: return
    val u = max(1f, 1.dp.toPx().roundToInt().toFloat())
    val a = center + start
    val b = center + tip
    val length = (b - a).getDistance()
    val steps = max(1, (length / u).toInt())
    val root = BAND_PIXEL_ROOT * (1f - BAND_THIN * stretch)
    path.reset()
    for (i in 0..steps) {
        val t = i.toFloat() / steps
        val units = max(1, (root + (BAND_PIXEL_TIP - root) * t).roundToInt())
        val side = units * u
        val p = a + (b - a) * t
        val x = ((p.x - side / 2f) / u).roundToInt() * u
        val y = ((p.y - side / 2f) / u).roundToInt() * u
        path.addRect(Rect(x, y, x + side, y + side))
    }
    drawPath(path, color)
}

private fun DrawScope.drawRoundCursor(x: Float, y: Float, fill: Color, outline: Color, shadow: Color?) {
    val center = Offset(x, y)
    val dot = CURSOR_DOT.toPx()
    val ring = CURSOR_RING.toPx()
    if (shadow != null) {
        drawCircle(shadow.copy(alpha = CURSOR_SHADOW_ALPHA * 0.5f), CURSOR_SHADOW.toPx(), center + Offset(0f, ring))
        drawCircle(shadow.copy(alpha = CURSOR_SHADOW_ALPHA), dot + ring * 2f, center + Offset(0f, ring * 0.5f))
    }
    drawCircle(outline, dot + ring, center)
    drawCircle(fill, dot, center)
}

private fun DrawScope.drawPixelCursor(x: Float, y: Float, fill: Color, shadow: Color?, ink: Color, inkEdge: Color) {
    val u = max(1f, 1.dp.toPx().roundToInt().toFloat())
    val cx = (x / u).roundToInt() * u
    val cy = (y / u).roundToInt() * u
    val arm = CURSOR_ARM_UNITS * u
    fun cross(ox: Float, oy: Float, thick: Float, reach: Float, color: Color) {
        drawRect(color, Offset(ox - reach, oy - thick / 2f), Size(reach * 2, thick))
        drawRect(color, Offset(ox - thick / 2f, oy - reach), Size(thick, reach * 2))
    }
    if (shadow != null) cross(cx + u, cy + u, u * 4, arm + u * 2, shadow.copy(alpha = CURSOR_SHADOW_ALPHA))
    cross(cx, cy, u * 4, arm + u * 2, inkEdge)
    cross(cx, cy, u * 2, arm + u, ink)
    drawRect(fill, Offset(cx - u, cy - u), Size(u * 2, u * 2))
}
