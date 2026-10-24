package org.polyfrost.compose.ui.input.key

import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.layoutElement

fun Modifier.onKeyEvent(onKeyEvent: (KeyEvent) -> Boolean) = layoutElement { onKey += onKeyEvent }

fun Modifier.onKeyDown(handler: (key: Int, modifiers: Int) -> Boolean) =
    onKeyEvent { it.type == KeyEventType.KeyDown && handler(it.key, it.modifiers) }
