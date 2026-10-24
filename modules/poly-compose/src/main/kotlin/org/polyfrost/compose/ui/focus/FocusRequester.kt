package org.polyfrost.compose.ui.focus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.layoutElement

class FocusRequester {
    internal var requested by mutableStateOf(false)

    fun requestFocus() {
        requested = true
    }
}

fun Modifier.focusRequester(focusRequester: FocusRequester): Modifier =
    if (!focusRequester.requested) this
    else layoutElement { focusRequest = { focusRequester.requested = false; true } }
