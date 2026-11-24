@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Announce
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
public class ConfirmRequest internal constructor(
    public val title: String,
    public val text: String,
    public val confirmLabel: String,
    public val style: ControlStyle,
    public val icon: String?,
    internal val owner: Any?,
    private val onConfirm: () -> Unit,
    private val onCancel: () -> Unit,
) {
    public val isPending: Boolean
        get() = this in Confirm.queue

    public fun confirm(): Boolean = Confirm.finish(this, true)

    public fun cancel(): Boolean = Confirm.finish(this, false)

    internal fun complete(confirmed: Boolean) {
        if (confirmed) onConfirm() else onCancel()
    }
}

@ExperimentalOneConfigUiApi
public object Confirm {
    private val LOGGER = LogManager.getLogger("OneConfig/UI")

    internal val queue = mutableStateListOf<ConfirmRequest>()

    internal val hosts = HostStack(::release)

    @JvmStatic
    public val hasPending: Boolean
        get() = queue.isNotEmpty()

    @JvmStatic
    public fun ask(
        title: String,
        text: String,
        confirmLabel: String,
        onConfirm: () -> Unit,
        style: ControlStyle = ControlStyle.Danger,
        icon: String? = null,
        onCancel: () -> Unit = {},
    ): ConfirmRequest {
        val owner = currentScreen()
        val request = ConfirmRequest(title, text, confirmLabel, style, icon, owner, onConfirm, onCancel)
        if (owner == null) {
            LOGGER.warn("Confirm.ask was called with no screen open, so nothing can show it: {}", title)
            return request
        }
        val stale = queue.filter { it.owner !== owner && !hosts.hosts(it.owner) }
        mutateFeedbackState {
            queue.removeAll(stale)
            queue += request
        }
        stale.forEach { it.complete(false) }
        return request
    }

    @JvmStatic
    public fun cancel(request: ConfirmRequest): Boolean = finish(request, false)

    @JvmStatic
    public fun cancelAll() {
        queue.toList().forEach { finish(it, false) }
    }

    internal fun finish(request: ConfirmRequest, confirmed: Boolean): Boolean {
        var removed = false
        mutateFeedbackState { removed = queue.remove(request) }
        if (removed) request.complete(confirmed)
        return removed
    }

    internal fun release(owner: Any) {
        queue.filter { it.owner === owner }.forEach { finish(it, false) }
    }
}

@ExperimentalOneConfigUiApi
public object ConfirmDefaults {
    public const val timeoutMillis: Long = 3000L

    public const val armDelayMillis: Long = 400L

    @JvmStatic
    public fun armedText(text: String): String {
        val action = text.lowercase()
        return kitString("oneconfig.ui.confirm.armed", "Click again to $action", action)
    }
}

@Composable
internal fun ConfirmHost(slot: HostSlot) {
    val request = Confirm.queue.firstOrNull { it.owner === slot.owner } ?: return
    if (!Confirm.hosts.isActive(slot)) return
    key(request) {
        ConfirmDialog(
            request.title,
            request.text,
            request.confirmLabel,
            onConfirm = { request.confirm() },
            onDismiss = { request.cancel() },
            style = request.style,
            icon = request.icon,
        )
    }
}

@ExperimentalOneConfigUiApi
public object ConfirmPopoverDefaults {
    public val width: Dp = 280.dp

    public val padding: PaddingValues = PaddingValues(12.dp)

    public val spacing: Dp = 10.dp

    public val gap: Dp = 6.dp
}

@ExperimentalOneConfigUiApi
@Composable
public fun ConfirmPopover(
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    dismissLabel: String = DialogDefaults.cancelLabel,
    style: ControlStyle = ControlStyle.Danger,
    icon: String? = null,
    width: Dp = ConfirmPopoverDefaults.width,
    padding: PaddingValues = ConfirmPopoverDefaults.padding,
    spacing: Dp = ConfirmPopoverDefaults.spacing,
    size: ControlSize = ControlSize.Compact,
) {
    val theme = OneConfigTheme.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(focus) { runCatching { focus.requestFocus() } }
    Announce(title ?: text)
    Column(
        modifier = componentsBridge().popupSurface(modifier.width(width), theme.popupShape)
            .swallowClicks()
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        if (title != null) Text(title, Modifier.semantics { heading() }, theme.textColor, theme.typography.label)
        Text(text, Modifier, if (title == null) theme.textColor else theme.textColorSecondary, theme.typography.body)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DialogDefaults.buttonSpacing, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(dismissLabel, onDismiss, Modifier.focusRequester(focus), size = size)
            if (icon != null) TextButton(confirmLabel, icon, onConfirm, style = style, size = size)
            else TextButton(confirmLabel, onConfirm, style = style, size = size)
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun AnchoredConfirm(
    expanded: Boolean,
    onDismiss: () -> Unit,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    dismissLabel: String = DialogDefaults.cancelLabel,
    style: ControlStyle = ControlStyle.Danger,
    icon: String? = null,
    placement: Placement = Placement.BelowEnd,
    gap: Dp = ConfirmPopoverDefaults.gap,
    width: Dp = ConfirmPopoverDefaults.width,
    popupSounds: Boolean = LocalPopupSounds.current,
) {
    val latestConfirm by rememberUpdatedState(onConfirm)
    val latestDismiss by rememberUpdatedState(onDismiss)
    CompositionLocalProvider(LocalPopupSounds provides popupSounds) {
        AnchoredPopup(expanded, onDismiss, placement, gap) {
            ConfirmPopover(
                text,
                confirmLabel,
                onConfirm = {
                    latestDismiss()
                    latestConfirm()
                },
                onDismiss = { latestDismiss() },
                modifier = modifier,
                title = title,
                dismissLabel = dismissLabel,
                style = style,
                icon = icon,
                width = width,
            )
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun ArmedConfirmButton(
    text: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    armedText: String = ConfirmDefaults.armedText(text),
    timeoutMillis: Long = ConfirmDefaults.timeoutMillis,
    style: ControlStyle = ControlStyle.Danger,
    size: ControlSize = ControlSize.Regular,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    ArmedConfirmButton(
        AnnotatedString(text), onConfirm, modifier, AnnotatedString(armedText),
        timeoutMillis, style, size, enabled, interactionSource,
    )
}

@ExperimentalOneConfigUiApi
@Composable
public fun ArmedConfirmButton(
    text: AnnotatedString,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    armedText: AnnotatedString = AnnotatedString(ConfirmDefaults.armedText(text.text)),
    timeoutMillis: Long = ConfirmDefaults.timeoutMillis,
    style: ControlStyle = ControlStyle.Danger,
    size: ControlSize = ControlSize.Regular,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    ArmedConfirmContent(text, null, onConfirm, modifier, armedText, timeoutMillis, style, size, enabled, interactionSource)
}

@ExperimentalOneConfigUiApi
@Composable
public fun ArmedConfirmButton(
    text: String,
    icon: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    armedText: String = ConfirmDefaults.armedText(text),
    timeoutMillis: Long = ConfirmDefaults.timeoutMillis,
    style: ControlStyle = ControlStyle.Danger,
    size: ControlSize = ControlSize.Regular,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    ArmedConfirmContent(
        AnnotatedString(text), icon, onConfirm, modifier, AnnotatedString(armedText),
        timeoutMillis, style, size, enabled, interactionSource,
    )
}

@ExperimentalOneConfigUiApi
@Composable
public fun ArmedConfirmButton(
    text: AnnotatedString,
    icon: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    armedText: AnnotatedString = AnnotatedString(ConfirmDefaults.armedText(text.text)),
    timeoutMillis: Long = ConfirmDefaults.timeoutMillis,
    style: ControlStyle = ControlStyle.Danger,
    size: ControlSize = ControlSize.Regular,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    ArmedConfirmContent(text, icon, onConfirm, modifier, armedText, timeoutMillis, style, size, enabled, interactionSource)
}

@Composable
private fun ArmedConfirmContent(
    text: AnnotatedString,
    icon: String?,
    onConfirm: () -> Unit,
    modifier: Modifier,
    armedText: AnnotatedString,
    timeoutMillis: Long,
    style: ControlStyle,
    size: ControlSize,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
) {
    var armedAt by remember { mutableStateOf<Long?>(null) }
    val armed = armedAt != null
    var restWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    LaunchedEffect(armedAt) {
        if (armedAt != null) {
            delay(timeoutMillis)
            armedAt = null
        }
    }
    LaunchedEffect(enabled) { if (!enabled) armedAt = null }
    if (armed) Announce(armedText.text)
    val label = if (armed) armedText else text
    val onClick: () -> Unit = {
        val at = armedAt
        when {
            at == null -> armedAt = System.currentTimeMillis()
            System.currentTimeMillis() - at >= ConfirmDefaults.armDelayMillis -> {
                armedAt = null
                onConfirm()
            }
        }
    }
    val buttonModifier = modifier
        .onFocusChanged { if (!it.hasFocus) armedAt = null }
        .onSizeChanged { if (!armed) restWidth = it.width }
        .then(if (armed && restWidth > 0) Modifier.widthIn(min = with(density) { restWidth.toDp() }) else Modifier)
    val buttonStyle = if (armed) armedStyle(style) else style
    if (icon != null) {
        TextButton(label, icon, onClick, buttonModifier, buttonStyle, size, enabled, interactionSource = interactionSource)
    } else {
        TextButton(label, onClick, buttonModifier, buttonStyle, size, enabled, interactionSource = interactionSource)
    }
}

private fun armedStyle(style: ControlStyle): ControlStyle = when (style) {
    ControlStyle.Danger, ControlStyle.DangerFilled -> ControlStyle.DangerFilled
    else -> ControlStyle.Primary
}
