@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.delay
import org.polyfrost.oneconfig.api.notifications.v1.NotificationType
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Announce
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
public object Toasts {
    internal class Toast(val text: String, val type: NotificationType, durationMillis: Long) {
        val id: Long = nextId.incrementAndGet()
        val persistent: Boolean = durationMillis <= 0L
        var remainingMillis: Long = durationMillis
        val visibility: MutableTransitionState<Boolean> = MutableTransitionState(false).apply { targetState = true }

        fun close() {
            visibility.targetState = false
        }
    }

    private val nextId = AtomicLong()

    internal val queue = mutableStateListOf<Toast>()

    internal val hosts = HostStack()

    @JvmStatic
    public fun show(text: String, type: NotificationType = NotificationType.INFO, durationMillis: Long = 4000L) {
        val toast = Toast(text, type, durationMillis)
        mutateFeedbackState { queue += toast }
    }

    internal fun remove(toast: Toast) {
        mutateFeedbackState { queue -= toast }
    }
}

@ExperimentalOneConfigUiApi
public object ToastDefaults {
    public const val maxVisible: Int = 3

    public val maxWidth: Dp = 360.dp

    public val margin: Dp = 16.dp

    public val alignment: Alignment = Alignment.TopCenter
}

@ExperimentalOneConfigUiApi
@Composable
public fun ToastHost(modifier: Modifier = Modifier, alignment: Alignment = ToastDefaults.alignment) {
    val slot = Toasts.hosts.rememberSlot(fallback = false, depth = feedbackDepth())
    ToastHostLayer(slot, modifier, alignment)
}

@Composable
internal fun ToastHostLayer(slot: Any, modifier: Modifier, alignment: Alignment) {
    val shown = Toasts.queue.take(ToastDefaults.maxVisible)
    if (shown.isEmpty()) return
    if (!Toasts.hosts.isActive(slot)) return
    val marginPx = with(LocalDensity.current) { ToastDefaults.margin.roundToPx() }
    val provider = remember(alignment, marginPx) { WindowAlignedPosition(alignment, marginPx) }
    val fromBottom = alignment == Alignment.BottomStart || alignment == Alignment.BottomCenter || alignment == Alignment.BottomEnd
    val horizontal = when (alignment) {
        Alignment.TopStart, Alignment.CenterStart, Alignment.BottomStart -> Alignment.Start
        Alignment.TopEnd, Alignment.CenterEnd, Alignment.BottomEnd -> Alignment.End
        else -> Alignment.CenterHorizontally
    }
    componentsBridge().Popup(provider, null, false) {
        val hover = rememberHoverState()
        Column(
            modifier = modifier.widthIn(max = ToastDefaults.maxWidth).hoverable(hover.interactionSource),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = horizontal,
        ) {
            for (toast in if (fromBottom) shown.asReversed() else shown) {
                key(toast.id) { ToastItem(toast, hover.hovered) }
            }
        }
    }
}

@Composable
private fun ToastItem(toast: Toasts.Toast, paused: Boolean) {
    val state = toast.visibility
    if (state.isIdle && !state.targetState) {
        LaunchedEffect(toast) { Toasts.remove(toast) }
    }
    LaunchedEffect(toast, paused) {
        if (paused || toast.persistent || !toast.visibility.targetState) return@LaunchedEffect
        val start = System.currentTimeMillis()
        try {
            delay(toast.remainingMillis.coerceAtLeast(0L))
        } finally {
            toast.remainingMillis -= System.currentTimeMillis() - start
        }
        toast.close()
    }
    Announce(toast.text)
    AnimatedVisibility(
        visibleState = state,
        enter = Motion.enter(fadeIn() + expandVertically(expandFrom = Alignment.Top)),
        exit = Motion.exit(fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)),
    ) {
        ToastCard(toast)
    }
}

@Composable
private fun ToastCard(toast: Toasts.Toast) {
    val theme = OneConfigTheme.current
    Row(
        modifier = componentsBridge().popupSurface(Modifier, theme.popupShape)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(toast.type.iconName, null, Modifier.size(16.dp), toastColor(toast.type))
        Text(toast.text, Modifier.weight(1f, fill = false).padding(vertical = 6.dp), theme.textColor, theme.typography.label)
        IconButton("close", kitString("oneconfig.ui.toast.dismiss", "Dismiss"), toast::close, size = ControlSize.Compact, tooltip = null)
    }
}

@Composable
internal fun toastColor(type: NotificationType): Color {
    val theme = OneConfigTheme.current
    return when {
        type == NotificationType.SUCCESS -> theme.success
        type == NotificationType.ERROR -> theme.error
        type.name == "WARNING" -> theme.warning
        type == NotificationType.PROGRESS -> OneConfigTheme.accent
        else -> theme.info
    }
}

private class WindowAlignedPosition(private val alignment: Alignment, private val margin: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val space = IntSize((windowSize.width - margin * 2).coerceAtLeast(0), (windowSize.height - margin * 2).coerceAtLeast(0))
        val placed = alignment.align(popupContentSize, space, layoutDirection)
        return IntOffset(placed.x + margin, placed.y + margin)
    }
}
