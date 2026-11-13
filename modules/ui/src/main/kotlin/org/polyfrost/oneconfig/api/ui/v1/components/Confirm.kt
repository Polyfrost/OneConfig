@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Announce

@ExperimentalOneConfigUiApi
public object Confirm {
    internal class Request(
        val title: String,
        val text: String,
        val confirmLabel: String,
        val onConfirm: () -> Unit,
        val style: ControlStyle,
        val icon: String?,
    )

    internal val queue = mutableStateListOf<Request>()

    internal val hosts = HostStack()

    @JvmStatic
    public fun ask(
        title: String,
        text: String,
        confirmLabel: String,
        onConfirm: () -> Unit,
        style: ControlStyle = ControlStyle.Danger,
        icon: String? = null,
    ) {
        val request = Request(title, text, confirmLabel, onConfirm, style, icon)
        mutateFeedbackState { queue += request }
    }

    internal fun finish(request: Request) {
        mutateFeedbackState { queue -= request }
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
internal fun ConfirmHost() {
    val slot = Confirm.hosts.rememberSlot(fallback = false)
    if (!Confirm.hosts.isActive(slot)) return
    val request = Confirm.queue.firstOrNull() ?: return
    if (runCatching { Platform.screen().current<Any?>() }.getOrNull() == null) return
    key(request) {
        ConfirmDialog(
            request.title,
            request.text,
            request.confirmLabel,
            onConfirm = {
                Confirm.finish(request)
                request.onConfirm()
            },
            onDismiss = { Confirm.finish(request) },
            style = request.style,
            icon = request.icon,
        )
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
    if (armed) Announce(armedText)
    TextButton(
        if (armed) armedText else text,
        onClick = {
            val at = armedAt
            when {
                at == null -> armedAt = System.currentTimeMillis()
                System.currentTimeMillis() - at >= ConfirmDefaults.armDelayMillis -> {
                    armedAt = null
                    onConfirm()
                }
            }
        },
        modifier = modifier
            .onFocusChanged { if (!it.hasFocus) armedAt = null }
            .onSizeChanged { if (!armed) restWidth = it.width }
            .then(if (armed && restWidth > 0) Modifier.widthIn(min = with(density) { restWidth.toDp() }) else Modifier),
        style = if (armed) armedStyle(style) else style,
        size = size,
        enabled = enabled,
        interactionSource = interactionSource,
    )
}

private fun armedStyle(style: ControlStyle): ControlStyle = when (style) {
    ControlStyle.Danger, ControlStyle.DangerFilled -> ControlStyle.DangerFilled
    ControlStyle.Primary, ControlStyle.Secondary, ControlStyle.Ghost -> ControlStyle.Primary
}
