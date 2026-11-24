@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.isSpecified
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
@Composable
public fun CodeBlock(
    text: String,
    modifier: Modifier = Modifier,
    height: Dp = 240.dp,
    maxHeight: Dp = Dp.Unspecified,
    actions: (@Composable RowScope.() -> Unit)? = null,
    header: (@Composable RowScope.() -> Unit)? = null,
    wrap: Boolean = false,
) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    val shape = theme.shapes.field
    val fitContent = !height.isSpecified
    val bounded = !fitContent || maxHeight.isSpecified
    Column(
        modifier
            .fillMaxWidth()
            .then(if (fitContent) Modifier.heightIn(max = maxHeight) else Modifier.height(height))
            .clip(shape)
            .background(theme.componentBackground, shape)
            .border(1.dp, theme.borderColor, shape),
    ) {
        if (header != null) {
            Row(
                Modifier.fillMaxWidth().padding(start = spacing.l, top = spacing.m, end = spacing.l),
                horizontalArrangement = Arrangement.spacedBy(spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, content = header)
                if (actions != null) CodeBlockActions(actions, Modifier)
            }
        }
        CodeBlockBody(
            text,
            wrap,
            fitContent,
            bounded,
            Modifier.then(if (bounded) Modifier.weight(1f, fill = !fitContent) else Modifier).fillMaxWidth(),
            if (header == null) actions else null,
        )
    }
}

@Composable
private fun CodeBlockActions(actions: @Composable RowScope.() -> Unit, modifier: Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(OneConfigTheme.current.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        content = actions,
    )
}

@Composable
private fun CodeBlockBody(
    text: String,
    wrap: Boolean,
    fitContent: Boolean,
    scrolls: Boolean,
    modifier: Modifier,
    actions: (@Composable RowScope.() -> Unit)?,
) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    val shown = remember(text) { text.replace("\t", "    ") }
    Box(if (scrolls) modifier.smoothWheelScroll(vertical) else modifier) {
        val fill = if (fitContent) Modifier.fillMaxWidth() else Modifier.fillMaxSize()
        SelectionContainer(fill) {
            LayoutText(
                shown,
                theme.typography.mono,
                theme.textColor,
                fill
                    .then(if (scrolls) Modifier.verticalScroll(vertical) else Modifier)
                    .then(if (wrap) Modifier else Modifier.horizontalScroll(horizontal))
                    .padding(spacing.l),
                softWrap = wrap,
            )
        }
        Box(Modifier.matchParentSize()) {
            if (scrolls) {
                VerticalScrollbar(
                    rememberScrollbarAdapter(vertical),
                    Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = spacing.xs, horizontal = spacing.xxs),
                )
            }
            if (!wrap) {
                HorizontalScrollbar(
                    rememberScrollbarAdapter(horizontal),
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(vertical = spacing.xxs, horizontal = spacing.xs),
                )
            }
        }
        if (actions != null) CodeBlockActions(actions, Modifier.align(Alignment.TopEnd).padding(spacing.s))
    }
}
