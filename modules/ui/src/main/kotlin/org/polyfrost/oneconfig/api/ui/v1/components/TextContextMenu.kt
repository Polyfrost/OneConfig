@file:OptIn(ExperimentalOneConfigUiApi::class, ExperimentalFoundationApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.foundation.text.TextContextMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import java.util.Locale
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import androidx.compose.foundation.ContextMenuState as FoundationContextMenuState

@ExperimentalOneConfigUiApi
@Composable
public fun ProvideOneConfigTextContextMenu(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextContextMenu provides OneConfigTextContextMenu, content = content)
}

private object OneConfigTextContextMenu : TextContextMenu {
    @Composable
    override fun Area(textManager: TextContextMenu.TextManager, state: FoundationContextMenuState, content: @Composable () -> Unit) {
        val latest by rememberUpdatedState(textManager)
        val entries = remember { { textMenuEntries(latest) } }
        val outer = LocalContextMenuHost.current
        val local = remember { ContextMenuHostState() }
        Box(Modifier.contextMenuTrigger(outer ?: local, null, entries), propagateMinConstraints = true) {
            content()
            if (outer == null) ContextMenuLayer(local)
        }
    }
}

private fun textMenuEntries(textManager: TextContextMenu.TextManager): List<MenuEntry> = buildList {
    textManager.cut?.let { add(textAction("oneconfig.menu.text.cut", "Cut", "scissors", "X", it)) }
    textManager.copy?.let { add(textAction("oneconfig.menu.text.copy", "Copy", "copy", "C", it)) }
    textManager.paste?.let { add(textAction("oneconfig.menu.text.paste", "Paste", "clipboard", "V", it)) }
    textManager.selectAll?.let { add(textAction("oneconfig.menu.text.select_all", "Select all", "select-all", "A", it)) }
}

private fun textAction(key: String, fallback: String, icon: String, letter: String, action: TextContextMenu.Action): MenuAction =
    MenuAction(kitString(key, fallback), action.execute, icon = icon, enabled = action.enabled, shortcutHint = "$SHORTCUT_PREFIX$letter")

private val SHORTCUT_PREFIX: String =
    if (System.getProperty("os.name", "").lowercase(Locale.ROOT).contains("mac")) "Cmd+" else "Ctrl+"
