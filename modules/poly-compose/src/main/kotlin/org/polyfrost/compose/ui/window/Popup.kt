package org.polyfrost.compose.ui.window

import androidx.compose.runtime.Composable
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.BoxScope
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.input.key.KeyEvent
import org.polyfrost.compose.ui.input.key.onKeyDown
import org.polyfrost.compose.ui.input.pointer.PointerEventType
import org.polyfrost.compose.ui.input.pointer.onPointerEvent
import org.polyfrost.compose.ui.layoutElement
import org.polyfrost.compose.ui.unit.DpOffset

@Composable
fun Popup(
    alignment: Alignment = Alignment.TopStart,
    offset: DpOffset = DpOffset.Zero,
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    anchor: Alignment = alignment,
    roomier: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var m = modifier.layoutElement {
        overlay = true
        overlayAnchor = anchor
        overlayRoomier = roomier
        align = alignment
        x = offset.x.value
        y = offset.y.value
    }
    if (onDismissRequest != null) {
        if (properties.dismissOnClickOutside) m = m.onPointerEvent { it.type == PointerEventType.Outside && run { onDismissRequest(); true } }
        if (properties.dismissOnBackPress) m = m.onKeyDown { key, _ -> key == KeyEvent.KEY_ESCAPE && run { onDismissRequest(); true } }
    }
    Box(m, content = content)
}

data class PopupProperties(
    val focusable: Boolean = false,
    val dismissOnBackPress: Boolean = true,
    val dismissOnClickOutside: Boolean = true,
)
