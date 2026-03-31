package org.polyfrost.compose.composables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.jetbrains.skia.Image
import org.polyfrost.compose.layout.LayoutType
import org.polyfrost.compose.layout.PolySize
import org.polyfrost.compose.node.*
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.runtime.PolyApplier

@Composable
fun PolyBox(
    modifier: PolyModifier = PolyModifier,
    content: @Composable () -> Unit = {},
) {
    ComposeNode<PolyNode, PolyApplier>(
        factory = { PolyNode().also { it.style.layoutType = LayoutType.Box } },
        update = { set(modifier) { modifier.applyTo(style) } },
        content = content,
    )
}

@Composable
fun PolyImage(
    modifier: PolyModifier = PolyModifier,
    image: Image,
) {
    ComposeNode<ImageNode, PolyApplier>(
        factory = { ImageNode(image).also { it.style.layoutType = LayoutType.Box; it.remeasure() } },
        update = { set(modifier) { modifier.applyTo(style); remeasure() } },
    )
}

@Composable
fun PolyRow(
    gap: Float = 0f,
    modifier: PolyModifier = PolyModifier,
    content: @Composable () -> Unit = {},
) {
    ComposeNode<PolyNode, PolyApplier>(
        factory = { PolyNode().also { it.style.layoutType = LayoutType.Row } },
        update = {
            set(gap) { style.gap = it }
            set(modifier) { modifier.applyTo(style) }
        },
        content = content,
    )
}

@Composable
fun PolyColumn(
    gap: Float = 0f,
    modifier: PolyModifier = PolyModifier,
    content: @Composable () -> Unit = {},
) {
    ComposeNode<PolyNode, PolyApplier>(
        factory = { PolyNode().also { it.style.layoutType = LayoutType.Column } },
        update = {
            set(gap) { style.gap = it }
            set(modifier) { modifier.applyTo(style) }
        },
        content = content,
    )
}

@Composable
fun PolyText(
    text: String,
    color: PolyColor = PolyColor.WHITE,
    fontSize: Float = 12f,
    shadow: Boolean = false,
    shadowColor: PolyColor = PolyColor(0x80000000.toInt()),
    shadowOffset: Float = 1f,
    modifier: PolyModifier = PolyModifier,
) {
    ComposeNode<TextNode, PolyApplier>(
        factory = { TextNode() },
        update = {
            set(text) { this.text = it; remeasure() }
            set(fontSize) { this.fontSize = it; remeasure() }
            set(color) { this.color = it }
            set(shadow) { this.shadow = it }
            set(shadowColor) { this.shadowColor = it }
            set(shadowOffset) { this.shadowOffset = it }
            set(modifier) { modifier.applyTo(style); remeasure() }
        },
    )
}

@Composable
fun PolyMcText(
    text: String,
    color: Int = 0xFFFFFFFF.toInt(),
    shadow: Boolean = true,
    scale: Float = 1f,
    modifier: PolyModifier = PolyModifier,
) {
    ComposeNode<McTextNode, PolyApplier>(
        factory = { McTextNode() },
        update = {
            set(text) { this.text = it; remeasure() }
            set(scale) { this.scale = it; remeasure() }
            set(color) { this.color = it }
            set(shadow) { this.shadow = it }
            set(modifier) { modifier.applyTo(style); remeasure() }
        },
    )
}

@Composable
fun PolySpacer(width: Float = 0f, height: Float = 0f) {
    ComposeNode<SpacerNode, PolyApplier>(
        factory = { SpacerNode() },
        update = {
            set(width) { style.width = PolySize.Px(it) }
            set(height) { style.height = PolySize.Px(it) }
        },
    )
}

@Composable
fun PolyCanvas(
    modifier: PolyModifier,
    onDraw: RenderContext.(x: Float, y: Float, width: Float, height: Float) -> Unit,
) {
    ComposeNode<CanvasNode, PolyApplier>(
        factory = { CanvasNode() },
        update = {
            set(modifier) { modifier.applyTo(style) }
            set(onDraw) { this.onDraw = it }
        },
    )
}

@Composable
fun PolyRect(
    color: PolyColor,
    modifier: PolyModifier = PolyModifier,
) {
    ComposeNode<PolyNode, PolyApplier>(
        factory = { PolyNode().also { it.style.layoutType = LayoutType.Box } },
        update = {
            set(modifier) { modifier.applyTo(style); style.background = color }
            set(color) { style.background = it }
        },
    )
}
