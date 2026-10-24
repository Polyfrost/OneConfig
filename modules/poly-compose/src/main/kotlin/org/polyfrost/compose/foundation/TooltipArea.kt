package org.polyfrost.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.window.Popup

@Composable
fun TooltipArea(
    tooltip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    delayMillis: Long = 200,
    below: Boolean = false,
    content: @Composable () -> Unit,
) {
    var hovered by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(hovered) {
        if (!hovered) { shown = false; return@LaunchedEffect }
        delay(delayMillis)
        shown = true
    }
    Box(modifier.hoverable { hovered = it }) {
        content()
        if (shown) Popup(
            alignment = if (below) Alignment.TopCenter else Alignment.BottomCenter,
            anchor = if (below) Alignment.BottomCenter else Alignment.TopCenter,
        ) { tooltip() }
    }
}
