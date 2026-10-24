package org.polyfrost.compose.mc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.polyfrost.compose.node.McTextNode
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.runtime.PolyApplier
import org.polyfrost.compose.ui.Modifier

@Composable
fun McText(
    text: String,
    modifier: Modifier = Modifier,
    color: PolyColor = PolyColor.WHITE,
    shadow: Boolean = true,
    scale: Float = 1f,
) {
    ComposeNode<McTextNode, PolyApplier>(
        factory = { McTextNode() },
        update = {
            set(text) { this.text = it; remeasure() }
            set(scale) { this.scale = it; remeasure() }
            set(color) { this.color = it }
            set(shadow) { this.shadow = it }
            set(modifier) { it.applyTo(style); remeasure() }
        },
    )
}
