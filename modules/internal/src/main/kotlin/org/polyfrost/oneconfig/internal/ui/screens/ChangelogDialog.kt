package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.ThematicBreak
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.DialogTitle
import org.polyfrost.oneconfig.api.ui.v1.components.Divider
import org.polyfrost.oneconfig.api.ui.v1.components.EmptyState
import org.polyfrost.oneconfig.api.ui.v1.components.IconButton
import org.polyfrost.oneconfig.api.ui.v1.components.LoadState
import org.polyfrost.oneconfig.api.ui.v1.components.ModalDialog
import org.polyfrost.oneconfig.api.ui.v1.components.ScrollContainer
import org.polyfrost.oneconfig.api.ui.v1.components.TextButton
import org.polyfrost.oneconfig.internal.ui.api.ModrinthChangelog
import org.polyfrost.oneconfig.internal.ui.api.ModrinthChangelogs
import org.polyfrost.oneconfig.internal.ui.api.ModrinthVersionLink
import org.polyfrost.oneconfig.internal.ui.components.shellTransition
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.utils.v1.NetworkUtils

private val DialogMaxWidth = 680.dp
private const val DialogWidthFraction = 0.9f
private const val DialogHeightFraction = 0.85f

private sealed interface DialogState {
    data object Loading : DialogState
    class Loaded(val changelog: ModrinthChangelog) : DialogState
    data object Failed : DialogState
}

@Composable
internal fun ModrinthChangelogDialog(link: ModrinthVersionLink, onDismiss: () -> Unit) {
    var attempt by remember(link) { mutableIntStateOf(0) }
    val state by produceState<DialogState>(ModrinthChangelogs.cached(link)?.let(DialogState::Loaded) ?: DialogState.Loading, link, attempt) {
        val cached = ModrinthChangelogs.cached(link)
        if (cached != null) {
            value = DialogState.Loaded(cached)
            return@produceState
        }
        value = DialogState.Loading
        value = ModrinthChangelogs.load(link)?.let(DialogState::Loaded) ?: DialogState.Failed
    }
    val current = state
    val title = (current as? DialogState.Loaded)?.changelog?.title ?: link.version
    val window = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val width = with(density) { (window.width.toDp() * DialogWidthFraction).coerceAtMost(DialogMaxWidth) }
    val maxHeight = with(density) { window.height.toDp() * DialogHeightFraction }

    ModalDialog(
        onDismiss,
        Modifier.shellTransition().heightIn(max = maxHeight),
        width = width,
        title = {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DialogTitle(title, Modifier.weight(1f))
                IconButton("close", translated("oneconfig.changelog.modrinth.close", "Close"), onDismiss, size = ControlSize.Compact)
            }
        },
        buttons = {
            TextButton(
                translated("oneconfig.changelog.modrinth.open", "Open on Modrinth"),
                "external-link",
                { runCatching { NetworkUtils.browseLink(link.pageUrl) } },
                size = ControlSize.Compact,
            )
        },
    ) {
        when (current) {
            DialogState.Loading -> EmptyState(
                translated("oneconfig.changelog.modrinth.loading", "Loading the changelog"),
                Modifier.fillMaxWidth(),
                state = LoadState.Loading,
            )
            DialogState.Failed -> EmptyState(
                translated("oneconfig.changelog.modrinth.failed", "Couldn't load this changelog"),
                Modifier.fillMaxWidth(),
                state = LoadState.Error(""),
                onRetry = { attempt++ },
            )
            is DialogState.Loaded -> {
                val document = current.changelog.document
                if (document.firstChild == null) {
                    EmptyState(translated("oneconfig.changelog.modrinth.empty", "This version has no changelog"), Modifier.fillMaxWidth())
                } else {
                    val scroll = remember(link) { ScrollState(0) }
                    ScrollContainer(scroll, Modifier.weight(1f, fill = false)) {
                        Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            MarkdownBlocks(document, lists = 0)
                        }
                    }
                }
            }
        }
    }
}

private fun childrenOf(node: Node): List<Node> = buildList {
    var c = node.firstChild
    while (c != null) { add(c); c = c.next }
}

@Composable
private fun MarkdownBlocks(parent: Node, lists: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(if (lists <= 1) 8.dp else 4.dp)) {
        childrenOf(parent).forEach { MarkdownBlock(it, lists) }
    }
}

@Composable
private fun MarkdownBlock(node: Node, lists: Int) {
    val theme = LocalTheme.current
    val depth = (lists - 1).coerceAtLeast(0)
    val styles = changelogInlineStyles()
    when (node) {
        is Heading -> {
            val text = inline(node, styles)
            if (text.isNotBlank()) {
                val base = changelogBodyStyle(0)
                val style = when (node.level) {
                    1 -> base.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Medium)
                    2 -> base.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
                    else -> base.copy(fontWeight = FontWeight.Medium)
                }
                BasicText(text, Modifier.padding(top = 8.dp), style = style)
            }
        }
        is Paragraph -> {
            val text = inline(node, styles)
            if (text.isNotBlank()) BasicText(text, style = changelogBodyStyle(depth))
        }
        is BulletList, is OrderedList -> {
            val nested = lists + 1
            val itemDepth = (nested - 1).coerceAtLeast(0)
            val start = (node as? OrderedList)?.markerStartNumber ?: 1
            Column(verticalArrangement = Arrangement.spacedBy(if (itemDepth == 0) 8.dp else 4.dp)) {
                childrenOf(node).filterIsInstance<ListItem>().forEachIndexed { index, item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (node is OrderedList) {
                            BasicText("${start + index}.", style = changelogBodyStyle(itemDepth).copy(color = theme.textColorSecondary))
                        } else {
                            Box(
                                Modifier.padding(start = 4.dp, top = if (itemDepth == 0) 8.dp else 7.dp)
                                    .size(4.dp)
                                    .background(theme.textColorSecondary.copy(if (itemDepth == 0) 0.7f else 0.5f), theme.shapes.circle)
                            )
                        }
                        Column(Modifier.weight(1f)) { MarkdownBlocks(item, nested) }
                    }
                }
            }
        }
        is BlockQuote -> Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(theme.borderColor))
            Column(Modifier.weight(1f).padding(start = 12.dp)) { MarkdownBlocks(node, lists) }
        }
        is FencedCodeBlock -> CodeBlock(node.literal)
        is IndentedCodeBlock -> CodeBlock(node.literal)
        is ThematicBreak -> Divider(Modifier.padding(vertical = 4.dp))
        is HtmlBlock -> {
            val text = htmlText(node.literal.orEmpty())
            if (text.isNotBlank()) BasicText(text, style = changelogBodyStyle(depth))
        }
        else -> Unit
    }
}

private val htmlBreak = Regex("""<br\s*/?>|</(?:p|div|li|h[1-6]|tr|summary|details)>""", RegexOption.IGNORE_CASE)
private val htmlTag = Regex("""<[^>]*>""")
private val htmlEntities = listOf("&nbsp;" to " ", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"", "&#39;" to "'", "&apos;" to "'", "&amp;" to "&")

private fun htmlText(html: String): String {
    var text = htmlTag.replace(htmlBreak.replace(html, "\n"), "")
    htmlEntities.forEach { (entity, value) -> text = text.replace(entity, value, ignoreCase = true) }
    return text.lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
}

@Composable
private fun CodeBlock(literal: String?) {
    val theme = LocalTheme.current
    val text = literal.orEmpty().trimEnd('\n', '\r')
    if (text.isBlank()) return
    Box(Modifier.fillMaxWidth().background(theme.chipBackground, theme.buttonShape).padding(horizontal = 10.dp, vertical = 8.dp)) {
        BasicText(text, style = changelogBodyStyle(0).copy(fontSize = 12.sp, lineHeight = 18.sp))
    }
}
