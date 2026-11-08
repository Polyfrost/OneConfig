@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
object EscapeStack {
    private class Closer(val owner: Any?, val close: () -> Unit)

    private val closers = ArrayList<Closer>()

    @JvmStatic
    val hasOpen: Boolean
        get() = top() != null

    @JvmStatic
    fun closeTop(): Boolean {
        val close = top() ?: return false
        close()
        return true
    }

    @JvmStatic
    fun push(close: () -> Unit) {
        val owner = currentScreen()
        synchronized(closers) { closers += Closer(owner, close) }
    }

    @JvmStatic
    fun remove(close: () -> Unit) {
        synchronized(closers) { closers.removeAll { it.close === close } }
    }

    private fun top(): (() -> Unit)? {
        val screen = currentScreen()
        return synchronized(closers) { closers.lastOrNull { it.owner === screen }?.close }
    }

    private fun currentScreen(): Any? = runCatching { Platform.screen().current<Any?>() }.getOrNull()
}

@ExperimentalOneConfigUiApi
@Composable
fun EscapeCloses(onClose: () -> Unit) {
    val latest by rememberUpdatedState(onClose)
    DisposableEffect(Unit) {
        val close = { latest() }
        EscapeStack.push(close)
        onDispose { EscapeStack.remove(close) }
    }
}
