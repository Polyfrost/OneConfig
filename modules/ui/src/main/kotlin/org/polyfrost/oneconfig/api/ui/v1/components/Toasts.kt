@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.isSpecified
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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.delay
import org.polyfrost.oneconfig.api.notifications.v1.Notification
import org.polyfrost.oneconfig.api.notifications.v1.NotificationType
import org.polyfrost.oneconfig.api.notifications.v1.Notifications
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Announce
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
public object Toasts {
    internal class Toast(val text: String, val type: NotificationType, durationMillis: Long, val owner: Any?, val style: ToastStyle? = null) {
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

    private val fallbacks = CopyOnWriteArrayList<Notification>()

    internal val hosts = HostStack(::release)

    @JvmStatic
    public fun show(text: String, type: NotificationType = NotificationType.INFO, durationMillis: Long = 4000L): ToastHandle =
        show(text, type, durationMillis, null)

    @JvmStatic
    public fun show(text: String, type: NotificationType = NotificationType.INFO, durationMillis: Long = 4000L, style: ToastStyle?): ToastHandle {
        val owner = currentScreen()
        if (owner == null || !hosts.hosts(owner)) {
            val notification = Notifications.send(
                text,
                "",
                type,
                durationMillis = if (durationMillis > 0L) durationMillis else Notification.DEFAULT_DURATION_MILLIS,
                persistent = durationMillis <= 0L,
            )
            fallbacks.removeIf { shown -> NotificationsManager.active.none { it === shown } }
            fallbacks += notification
            return ToastHandle(null, notification)
        }
        val toast = Toast(text, type, durationMillis, owner, style)
        mutateFeedbackState { queue += toast }
        return ToastHandle(toast, null)
    }

    @JvmStatic
    public fun dismissAll() {
        mutateFeedbackState { queue.forEach { it.close() } }
        for (notification in fallbacks) {
            fallbacks.remove(notification)
            Notifications.dismiss(notification)
        }
    }

    internal fun remove(toast: Toast) {
        mutateFeedbackState { queue -= toast }
    }

    internal fun release(owner: Any) {
        mutateFeedbackState { queue.removeAll { it.owner === owner } }
    }
}

@ExperimentalOneConfigUiApi
public class ToastHandle internal constructor(private val toast: Toasts.Toast?, private val notification: Notification?) {
    public val isShowing: Boolean
        get() = when {
            toast != null -> toast in Toasts.queue && toast.visibility.targetState
            notification != null -> notification in NotificationsManager.active
            else -> false
        }

    public fun dismiss() {
        toast?.let { mutateFeedbackState { it.close() } }
        notification?.let(Notifications::dismiss)
    }
}

@ExperimentalOneConfigUiApi
public class ToastStyle(
    public val icon: String? = null,
    public val iconColor: Color = Color.Unspecified,
    public val borderColor: Color = Color.Unspecified,
) {
    override fun equals(other: Any?): Boolean =
        other is ToastStyle && other.icon == icon && other.iconColor == iconColor && other.borderColor == borderColor

    override fun hashCode(): Int = ((icon?.hashCode() ?: 0) * 31 + iconColor.hashCode()) * 31 + borderColor.hashCode()
}

@ExperimentalOneConfigUiApi
public object ToastDefaults {
    public const val maxVisible: Int = 3

    public val maxWidth: Dp = 360.dp

    public val margin: Dp = 16.dp

    public val alignment: Alignment = Alignment.TopCenter

    public val borderWidth: Dp = 1.dp
}

@ExperimentalOneConfigUiApi
@Composable
public fun ToastHost(
    modifier: Modifier = Modifier,
    alignment: Alignment = ToastDefaults.alignment,
    inPlace: Boolean = false,
    margin: Dp = ToastDefaults.margin,
) {
    if (feedbackSuppressed()) return
    val slot = Toasts.hosts.rememberSlot(fallback = false, depth = feedbackDepth(), owner = rememberFeedbackOwner())
    ToastHostLayer(slot, modifier, alignment, inPlace, margin)
}

@Composable
internal fun ToastHostLayer(
    slot: HostSlot,
    modifier: Modifier,
    alignment: Alignment,
    inPlace: Boolean = false,
    margin: Dp = ToastDefaults.margin,
) {
    val shown = Toasts.queue.filter { it.owner === slot.owner }.take(ToastDefaults.maxVisible)
    if (shown.isEmpty()) return
    if (!Toasts.hosts.isActive(slot)) return
    val marginPx = with(LocalDensity.current) { margin.roundToPx() }
    val provider = remember(alignment, marginPx, inPlace) {
        if (inPlace) RegionAlignedPosition(alignment, marginPx) else WindowAlignedPosition(alignment, marginPx)
    }
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
    val style = toast.style
    val border = style?.borderColor ?: Color.Unspecified
    val iconColor = style?.iconColor ?: Color.Unspecified
    Row(
        modifier = componentsBridge().popupSurface(Modifier, theme.popupShape)
            .then(if (border.isSpecified) Modifier.border(ToastDefaults.borderWidth, border, theme.popupShape) else Modifier)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(style?.icon ?: toast.type.iconName, null, Modifier.size(16.dp), if (iconColor.isSpecified) iconColor else toastColor(toast.type))
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

private class RegionAlignedPosition(private val alignment: Alignment, private val margin: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val space = IntSize((anchorBounds.width - margin * 2).coerceAtLeast(0), (anchorBounds.height - margin * 2).coerceAtLeast(0))
        val placed = alignment.align(popupContentSize, space, layoutDirection)
        val x = anchorBounds.left + margin + placed.x
        val y = anchorBounds.top + margin + placed.y
        return IntOffset(
            x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
        )
    }
}

private class WindowAlignedPosition(private val alignment: Alignment, private val margin: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val space = IntSize((windowSize.width - margin * 2).coerceAtLeast(0), (windowSize.height - margin * 2).coerceAtLeast(0))
        val placed = alignment.align(popupContentSize, space, layoutDirection)
        return IntOffset(placed.x + margin, placed.y + margin)
    }
}
