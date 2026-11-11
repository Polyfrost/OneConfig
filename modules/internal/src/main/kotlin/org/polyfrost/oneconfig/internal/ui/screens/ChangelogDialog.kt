package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
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
import org.polyfrost.oneconfig.api.ui.v1.components.EscapeCloses
import org.polyfrost.oneconfig.internal.ui.api.ModrinthChangelog
import org.polyfrost.oneconfig.internal.ui.api.ModrinthChangelogs
import org.polyfrost.oneconfig.internal.ui.api.ModrinthVersionLink
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.components.ScrollbarBox
import org.polyfrost.oneconfig.internal.ui.components.ShellPopup
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.shellTransition
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.utils.v1.NetworkUtils
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import org.polyfrost.oneconfig.internal.ui.util.cachedDropShadow

private val DialogMaxWidth = 680.dp
private val DialogPadding = 20.dp

private sealed interface DialogState {
    data object Loading : DialogState
    class Loaded(val changelog: ModrinthChangelog) : DialogState
    data object Failed : DialogState
}

private object WindowOrigin : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset.Zero
}

@Composable
internal fun ModrinthChangelogDialog(link: ModrinthVersionLink, onDismiss: () -> Unit) {
    val theme = LocalTheme.current
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
    val popupShadow = remember(theme.shadowColor) { Shadow(24.dp, theme.shadowColor, 0.dp, DpOffset(0.dp, 8.dp), .3f, BlendMode.SrcOver) }

    EscapeCloses(onDismiss)
    ShellPopup(
        popupPositionProvider = WindowOrigin,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnClickOutside = false),
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize()
                .background(theme.shadowColor.copy(alpha = 0.7f))
                .onClick(rememberInteractionSource(), onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.widthIn(max = DialogMaxWidth)
                    .fillMaxWidth(0.9f)
                    .heightIn(max = maxHeight * 0.85f)
                    .shellTransition()
                    .then(if (theme.shadowEnabled) Modifier.cachedDropShadow(theme.popupShape, popupShadow) else Modifier)
                    .background(theme.popupBackground.compositeOver(theme.pageBackground.copy(alpha = 1f)), theme.popupShape)
                    .border(1.dp, theme.borderColor, theme.popupShape)
                    .onClick(rememberInteractionSource()) {},
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(start = DialogPadding, end = 14.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(title, color = theme.textColor, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 2, modifier = Modifier.weight(1f))
                    IconButton("close", Modifier.size(16.dp), onClick = onDismiss)
                }
                when (current) {
                    DialogState.Loading -> DialogMessage(translated("oneconfig.changelog.modrinth.loading", "Loading the changelog"))
                    DialogState.Failed -> Column(
                        Modifier.fillMaxWidth().padding(horizontal = DialogPadding, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(translated("oneconfig.changelog.modrinth.failed", "Couldn't load this changelog"), color = theme.textColorSecondary, fontSize = 12.sp)
                        DialogButton(translated("oneconfig.changelog.modrinth.retry", "Try again"), primary = true) { attempt++ }
                    }
                    is DialogState.Loaded -> {
                        val document = current.changelog.document
                        if (document.firstChild == null) {
                            DialogMessage(translated("oneconfig.changelog.modrinth.empty", "This version has no changelog"))
                        } else {
                            val scroll = remember(link) { ScrollState(0) }
                            ScrollbarBox(scroll, 12.dp, Modifier.weight(1f, fill = false)) {
                                Column(Modifier.fillMaxWidth().verticalScroll(scroll).padding(start = DialogPadding, end = DialogPadding, bottom = 8.dp)) {
                                    MarkdownBlocks(document, lists = 0)
                                }
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = DialogPadding, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    DialogButton(translated("oneconfig.changelog.modrinth.open", "Open on Modrinth"), primary = false, icon = "external-link") {
                        runCatching { NetworkUtils.browseLink(link.pageUrl) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogMessage(text: String) {
    Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
        Text(text, color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun DialogButton(text: String, primary: Boolean, icon: String? = null, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    val base = if (primary) Accent else theme.textColor.copy(0.08f)
    val background by animateColorAsState(if (hovered) theme.textColor.copy(0.12f).compositeOver(base) else base)
    val ink = if (primary) theme.accentTextColor else theme.textColor
    Row(
        Modifier.clip(theme.buttonShape)
            .background(background, theme.buttonShape)
            .onClick(source, onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, color = ink, modifier = Modifier.size(12.dp))
        Text(text, color = ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                                    .background(theme.textColorSecondary.copy(if (itemDepth == 0) 0.7f else 0.5f), CircleShape)
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
        is ThematicBreak -> Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(theme.borderColor))
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
