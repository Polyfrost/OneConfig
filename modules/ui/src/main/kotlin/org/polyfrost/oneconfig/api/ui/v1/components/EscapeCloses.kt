@file:OptIn(ExperimentalOneConfigUiApi::class, ExperimentalFoundationApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import org.polyfrost.compose.host.BackStack
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public fun Modifier.handleEscapeStack(): Modifier = composed {
    var swallowRelease by remember { mutableStateOf(false) }
    onKeyEvent { event ->
        if (event.key != Key.Escape) return@onKeyEvent false
        when (event.type) {
            KeyEventType.KeyDown -> BackStack.dispatch().also { swallowRelease = it }
            KeyEventType.KeyUp -> swallowRelease.also { swallowRelease = false }
            else -> false
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun EscapeCloses(onClose: () -> Unit) {
    val latest by rememberUpdatedState(onClose)
    DisposableEffect(Unit) {
        val close = { latest() }
        BackStack.push(close)
        onDispose { BackStack.remove(close) }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun EscapeAwareTextMenus(content: @Composable () -> Unit) {
    val base = LocalTextContextMenu.current
    val wrapped = remember(base) { EscapeAwareTextContextMenu(base) }
    CompositionLocalProvider(LocalTextContextMenu provides wrapped, content = content)
}

private class EscapeAwareTextContextMenu(private val base: TextContextMenu) : TextContextMenu {
    @Composable
    override fun Area(textManager: TextContextMenu.TextManager, state: ContextMenuState, content: @Composable () -> Unit) {
        if (state.status is ContextMenuState.Status.Open) EscapeCloses { state.status = ContextMenuState.Status.Closed }
        base.Area(textManager, state, content)
    }
}
