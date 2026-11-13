@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
@Composable
public fun CodeBlock(
    text: String,
    modifier: Modifier = Modifier,
    height: Dp = 240.dp,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    val shape = theme.shapes.field
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    val shown = remember(text) { text.replace("\t", "    ") }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(theme.componentBackground, shape)
            .border(1.dp, theme.borderColor, shape),
    ) {
        SelectionContainer(Modifier.fillMaxSize()) {
            LayoutText(
                shown,
                theme.typography.mono,
                theme.textColor,
                Modifier
                    .fillMaxSize()
                    .verticalScroll(vertical)
                    .horizontalScroll(horizontal)
                    .padding(spacing.l),
                softWrap = false,
            )
        }
        Box(Modifier.matchParentSize()) {
            VerticalScrollbar(
                rememberScrollbarAdapter(vertical),
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = spacing.xs, horizontal = spacing.xxs),
            )
            HorizontalScrollbar(
                rememberScrollbarAdapter(horizontal),
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(vertical = spacing.xxs, horizontal = spacing.xs),
            )
        }
        if (actions != null) {
            Row(
                Modifier.align(Alignment.TopEnd).padding(spacing.s),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}
