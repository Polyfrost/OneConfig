@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigUITheme
import org.polyfrost.oneconfig.api.ui.v1.theme.legibleOn

@ExperimentalOneConfigUiApi
public enum class BadgeTone {
    Neutral,

    Accent,

    Success,

    Warning,

    Danger,
}

private const val BADGE_FILL_ALPHA = 0.16f
private const val NEUTRAL_FILL_ALPHA = 0.08f
private val BADGE_DOT = 6.dp

@ExperimentalOneConfigUiApi
@Composable
public fun Badge(text: String, modifier: Modifier = Modifier, tone: BadgeTone = BadgeTone.Neutral, overImage: Boolean = false) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    val shape = theme.shapes.pill
    val toneColor = badgeToneColor(theme, tone, OneConfigTheme.accent)
    val fill = when {
        overImage -> theme.onMediaShadow
        tone == BadgeTone.Neutral -> theme.textColor.copy(alpha = NEUTRAL_FILL_ALPHA)
        else -> toneColor.copy(alpha = BADGE_FILL_ALPHA)
    }
    val content = if (overImage) theme.onMedia else toneColor
    Row(
        modifier
            .clip(shape)
            .background(fill, shape)
            .padding(horizontal = spacing.s, vertical = spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (overImage && tone != BadgeTone.Neutral) {
            Box(Modifier.size(BADGE_DOT).background(toneColor, theme.shapes.circle))
        }
        LayoutText(text, theme.typography.caption.merge(fontWeight = FontWeight.Medium), content, maxLines = 1)
    }
}

private fun badgeToneColor(theme: OneConfigUITheme, tone: BadgeTone, accent: Color): Color = when (tone) {
    BadgeTone.Neutral -> theme.mutedText
    BadgeTone.Accent -> accent.legibleOn(theme.pageSurface)
    BadgeTone.Success -> theme.success
    BadgeTone.Warning -> theme.warning
    BadgeTone.Danger -> theme.error
}

@ExperimentalOneConfigUiApi
@Composable
public fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    eyebrow: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val theme = OneConfigTheme.current
    Row(
        modifier.fillMaxWidth().padding(vertical = theme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val heading = Modifier.weight(1f).semantics { heading() }
        if (eyebrow) {
            LayoutText(text.uppercase(), theme.typography.eyebrow, theme.mutedText, heading, maxLines = 1)
        } else {
            LayoutText(text, theme.typography.heading, theme.textColor, heading, maxLines = 1)
        }
        trailing?.invoke()
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun Divider(modifier: Modifier = Modifier, vertical: Boolean = false) {
    val color = OneConfigTheme.current.borderColor
    Box(
        if (vertical) modifier.fillMaxHeight().width(1.dp).background(color)
        else modifier.fillMaxWidth().height(1.dp).background(color),
    )
}

@ExperimentalOneConfigUiApi
@Composable
public fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier) {
    val theme = OneConfigTheme.current
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(theme.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LayoutText(label, theme.typography.small, theme.mutedText, maxLines = 1)
        LayoutText(value, theme.typography.small, theme.textColor, Modifier.weight(1f), maxLines = 1, align = TextAlign.End)
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun PageScaffold(
    modifier: Modifier = Modifier,
    toolbar: (@Composable RowScope.() -> Unit)? = null,
    sidePanel: (@Composable () -> Unit)? = null,
    sidePanelWidth: Dp = 320.dp,
    content: @Composable () -> Unit,
) {
    val spacing = OneConfigTheme.current.spacing
    Column(modifier.fillMaxSize()) {
        if (toolbar != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = ControlSize.Large.height)
                    .padding(horizontal = spacing.pageGutter, vertical = spacing.m),
                horizontalArrangement = Arrangement.spacedBy(spacing.m),
                verticalAlignment = Alignment.CenterVertically,
                content = toolbar,
            )
        }
        Row(Modifier.fillMaxWidth().weight(1f)) {
            Box(Modifier.weight(1f).fillMaxHeight()) { content() }
            if (sidePanel != null) {
                Divider(vertical = true)
                Box(Modifier.width(sidePanelWidth).fillMaxHeight()) { sidePanel() }
            }
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun CenteredPanel(
    modifier: Modifier = Modifier,
    width: Dp = 650.dp,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val theme = OneConfigTheme.current
    val spacing = theme.spacing
    val shape = theme.shapes.large
    Box(modifier.fillMaxSize().padding(spacing.xl), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .widthIn(max = width)
                .fillMaxWidth()
                .clip(shape)
                .background(theme.popupBackground, shape)
                .border(1.dp, theme.borderColor, shape),
        ) {
            val scroll = rememberScrollState()
            Column(
                Modifier
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.verticalScroll(scroll) else Modifier)
                    .padding(spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(spacing.l),
                content = content,
            )
            if (scrollable) {
                Box(Modifier.matchParentSize()) {
                    VerticalScrollbar(
                        rememberScrollbarAdapter(scroll),
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = spacing.m, horizontal = spacing.xxs),
                    )
                }
            }
        }
    }
}
