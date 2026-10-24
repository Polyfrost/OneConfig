package org.polyfrost.compose.ui.focus

import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.layoutElement

class FocusState internal constructor(val isFocused: Boolean)

fun Modifier.onFocusChanged(onFocusChanged: (FocusState) -> Unit): Modifier =
    layoutElement { this.onFocusChanged += { onFocusChanged(FocusState(it)) } }
