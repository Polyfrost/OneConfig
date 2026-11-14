@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import org.polyfrost.oneconfig.api.notifications.v1.NotificationType
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Announce
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

private const val STATUS_FADE_MILLIS = 150

@Stable
@ExperimentalOneConfigUiApi
public class StatusMessage internal constructor() {
    public var text: String? by mutableStateOf(null)
        private set

    public var type: NotificationType by mutableStateOf(NotificationType.INFO)
        private set

    internal var serial: Int by mutableIntStateOf(0)
        private set

    public fun show(text: String, type: NotificationType = NotificationType.INFO) {
        this.text = text
        this.type = type
        serial++
    }

    public fun clear() {
        text = null
        serial++
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun rememberStatusMessage(durationMillis: Long = 2000L): StatusMessage {
    val state = remember { StatusMessage() }
    val serial = state.serial
    LaunchedEffect(state, serial, durationMillis) {
        if (state.text != null && durationMillis > 0L) {
            delay(durationMillis)
            state.clear()
        }
    }
    return state
}

@ExperimentalOneConfigUiApi
@Composable
public fun StatusText(state: StatusMessage, modifier: Modifier = Modifier) {
    val theme = OneConfigTheme.current
    val current = state.text
    val last = remember { arrayOf("" to NotificationType.INFO) }
    if (current != null) last[0] = current to state.type
    val (text, type) = last[0]
    val alpha by animateFloatAsState(if (current != null) 1f else 0f, Motion.spec(tween(STATUS_FADE_MILLIS)), label = "statusText")
    Announce(current)
    val color = if (type == NotificationType.INFO) theme.textColorSecondary else toastColor(type)
    Text(
        text,
        modifier.graphicsLayer { this.alpha = alpha }.semantics { liveRegion = LiveRegionMode.Polite },
        color,
        theme.typography.small,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
