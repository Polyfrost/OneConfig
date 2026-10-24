package org.polyfrost.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import org.polyfrost.compose.layout.LayoutType
import org.polyfrost.compose.node.PolyNode
import org.polyfrost.compose.node.SpacerNode
import org.polyfrost.compose.runtime.PolyApplier
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier

interface BoxScope
interface RowScope
interface ColumnScope

private object BoxScopeInstance : BoxScope
private object RowScopeInstance : RowScope
private object ColumnScopeInstance : ColumnScope

@Composable
fun Box(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit = {},
) {
    ComposeNode<PolyNode, PolyApplier>(
        factory = { PolyNode().also { it.style.layoutType = LayoutType.Box } },
        update = {
            set(contentAlignment) { style.contentAlignment = it }
            set(modifier) { it.applyTo(style) }
        },
        content = { BoxScopeInstance.content() },
    )
}

@Composable
fun Row(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit = {},
) {
    ComposeNode<PolyNode, PolyApplier>(
        factory = { PolyNode().also { it.style.layoutType = LayoutType.Row } },
        update = {
            set(horizontalArrangement) { style.arrangement = it.mode; style.gap = it.spacing.value }
            set(verticalAlignment) { style.crossAlignment = it.bias }
            set(modifier) { it.applyTo(style) }
        },
        content = { RowScopeInstance.content() },
    )
}

@Composable
fun Column(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    ComposeNode<PolyNode, PolyApplier>(
        factory = { PolyNode().also { it.style.layoutType = LayoutType.Column } },
        update = {
            set(verticalArrangement) { style.arrangement = it.mode; style.gap = it.spacing.value }
            set(horizontalAlignment) { style.crossAlignment = it.bias }
            set(modifier) { it.applyTo(style) }
        },
        content = { ColumnScopeInstance.content() },
    )
}

@Composable
fun Spacer(modifier: Modifier) {
    ComposeNode<SpacerNode, PolyApplier>(
        factory = { SpacerNode() },
        update = { set(modifier) { it.applyTo(style) } },
    )
}
