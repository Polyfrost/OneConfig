package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text as CmText
import org.commonmark.parser.Parser
import org.polyfrost.oneconfig.internal.ui.api.ChangelogData
import org.polyfrost.oneconfig.internal.ui.api.ChangelogSection
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.searchMatches
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ChangeLogEntryRoute
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private data class ChangelogImpl(override val sections: List<ChangelogSection>) : ChangelogData
private data class ChangelogSectionImpl(
    override val headerImageUrl: String?,
    override val title: String,
    override val content: String,
    override val author: String,
    override val date: String
) : ChangelogSection

private val changelogs = ChangelogImpl(listOf(
    ChangelogSectionImpl("header", "ONECONFIG UPDATE 0.2 - \"UI Tweaks\"", """
        Hey everyone, it's been a bit since the last OneConfig update, which is in open beta if you haven't seen all the memes. Nevertheless, here we are announcing the very first update to OneConfig. It's a smaller one, but contains a lot of the features you all have been asking for. If you haven't already, @shoddyy made a great video explaining how to download OneConfig, find it here https://youtu.be/dfv_1s70wGI

        **What's new?**
        - OneConfig now supports your Hypixel API Keys! You can set and sync your key across all supported mods.
        - Sliders now have interaction states, and stepped sliders now only show their steps when hovered or pressed.
        - All searches now utilize fuzzy searching to combat spelling mistakes and misclicks
        - Options now display what's disabling them when hovered over
        - Many UI/UX features and behaviors can now be customized to your hearts content

        **Full changelog:**

        Sliders
        - Sliders now have different animated states for when you're hovered or pressing them
        - Stepped sliders now only show their steps when hovered/pressed.
    """.trimIndent(),
        "refactoring",
        "Mar 10th, 2026"
    ),
    ChangelogSectionImpl(null, "Hotfix v0.2.3", """
        ### Sliders
        - Sliders now have different animated states when hovered or pressed.
        - Stepped sliders now only display their step markers when hovered or pressed.

        ### New Animations
        - Added a new opening / closing animation with both **Subtle** and **Full** modes.
        - Many animations can now be configured in the **OneConfig settings**.
    """.trimIndent(),
        "refactoring",
        "Mar 10th, 2026"
    ),
    ChangelogSectionImpl("header", "ONECONFIG UPDATE 0.2 - \"UI Tweaks\"", """
        Hey everyone, it's been a bit since the last OneConfig update, which is in open beta if you haven't seen all the memes. Nevertheless, here we are announcing the very first update to OneConfig. It's a smaller one, but contains a lot of the features you all have been asking for. If you haven't already, @shoddyy made a great video explaining how to download OneConfig, find it here https://youtu.be/dfv_1s70wGI

        **What's new?**
        - OneConfig now supports your Hypixel API Keys! You can set and sync your key across all supported mods.
        - Sliders now have interaction states, and stepped sliders now only show their steps when hovered or pressed.
        - All searches now utilize fuzzy searching to combat spelling mistakes and misclicks
        - Options now display what's disabling them when hovered over
        - Many UI/UX features and behaviors can now be customized to your hearts content

        **Full changelog:**

        Sliders
        - Sliders now have different animated states for when you're hovered or pressing them
        - Stepped sliders now only show their steps when hovered/pressed.
    """.trimIndent(),
        "refactoring",
        "Mar 10th, 2026"
    ),
    ChangelogSectionImpl(null, "ONECONFIG UPDATE 0.2 - \"UI Tweaks\"", """
        Hey everyone, it's been a bit since the last OneConfig update, which is in open beta if you haven't seen all the memes. Nevertheless, here we are announcing the very first update to OneConfig. It's a smaller one, but contains a lot of the features you all have been asking for. If you haven't already, @shoddyy made a great video explaining how to download OneConfig, find it here https://youtu.be/dfv_1s70wGI

        **What's new?**
        - OneConfig now supports your Hypixel API Keys! You can set and sync your key across all supported mods.
        - Sliders now have interaction states, and stepped sliders now only show their steps when hovered or pressed.
        - All searches now utilize fuzzy searching to combat spelling mistakes and misclicks
        - Options now display what's disabling them when hovered over
        - Many UI/UX features and behaviors can now be customized to your hearts content

        **Full changelog:**

        Sliders
        - Sliders now have different animated states for when you're hovered or pressing them
        - Stepped sliders now only show their steps when hovered/pressed.
    """.trimIndent(),
        "refactoring",
        "Mar 10th, 2026"
    )
))

private val mdParser = Parser.builder().build()

private fun Node.children(): List<Node> = buildList {
    var c = firstChild
    while (c != null) { add(c); c = c.next }
}

private data class MdStyles(
    val body: TextStyle,
    val headingColor: Color,
    val bold: SpanStyle,
    val italic: SpanStyle,
    val code: SpanStyle,
    val link: SpanStyle,
) {
    fun heading(level: Int): TextStyle = when (level) {
        1 -> body.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = headingColor)
        2 -> body.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = headingColor)
        else -> body.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = headingColor)
    }
}

private fun buildInlineString(node: Node, styles: MdStyles): AnnotatedString = buildAnnotatedString {
    fun appendNode(n: Node) {
        var c = n.firstChild
        while (c != null) {
            when (c) {
                is CmText -> append(c.literal)
                is StrongEmphasis -> { pushStyle(styles.bold); appendNode(c); pop() }
                is Emphasis -> { pushStyle(styles.italic); appendNode(c); pop() }
                is Code -> { pushStyle(styles.code); append(c.literal); pop() }
                is Link -> { pushStyle(styles.link); appendNode(c); pop() }
                is SoftLineBreak -> append(" ")
                is HardLineBreak -> append("\n")
                else -> appendNode(c)
            }
            c = c.next
        }
    }
    appendNode(node)
}

@Composable
private fun MdBlocks(node: Node, styles: MdStyles, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        node.children().forEach { block ->
            when (block) {
                is Heading -> BasicText(
                    text = buildInlineString(block, styles),
                    style = styles.heading(block.level)
                )
                is Paragraph -> BasicText(
                    text = buildInlineString(block, styles),
                    style = styles.body
                )
                is BulletList -> Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    block.children().filterIsInstance<ListItem>().forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BasicText("•", style = styles.body)
                            val firstChild = item.firstChild
                            if (firstChild is Paragraph) {
                                BasicText(buildInlineString(firstChild, styles), style = styles.body)
                            } else {
                                MdBlocks(item, styles)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Changelog() {
    val lazyListState = rememberLazyListState()
    val localSearchQuery = if (ShellState.globalSearchActive) "" else ShellState.searchQuery.trim()
    val visibleSections = remember(localSearchQuery) {
        changelogs.sections.mapIndexed { index, section -> index to section }
            .filter { (_, section) ->
                localSearchQuery.isBlank() || section.matchesSearch(localSearchQuery)
            }
    }

    if (visibleSections.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No changelog entries match \"$localSearchQuery\"", color = LocalTheme.current.textColorSecondary)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            visibleSections.forEach { (index, section) ->
                item { ChangelogEntry(section, index) }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(lazyListState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}

private fun ChangelogSection.matchesSearch(query: String): Boolean {
    val q = query.lowercase()
    return listOf(title, content, author, date).any { searchMatches(it, q) }
}

@Composable
fun Changelog(index: Int) {
    val data = changelogs.sections.getOrNull(index) ?: return
    val theme = LocalTheme.current
    val styles = MdStyles(
        body = TextStyle(fontFamily = theme.typography.family, fontSize = 13.sp, color = theme.textColorSecondary),
        headingColor = theme.textColor,
        bold = SpanStyle(fontWeight = FontWeight.SemiBold, color = theme.textColor),
        italic = SpanStyle(fontStyle = FontStyle.Italic),
        code = SpanStyle(background = theme.chipBackground, color = theme.textColor, fontSize = 12.sp),
        link = SpanStyle(color = Accent, fontWeight = FontWeight.Medium),
    )
    val doc = remember(data.content) { mdParser.parse(data.content) }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(end = 16.dp)) {
            data.headerImageUrl?.let { url ->
                Box(
                    modifier = Modifier.fillMaxWidth().height(221.dp)
                        .clip(theme.modCardShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource("/assets/oneconfig/images/$url.png"),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(30.dp),
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.tint(Color.Black.copy(0.4f).compositeOver(Accent)
                            .copy(0.4f), BlendMode.Multiply)
                    )
                    Image(
                        painter = painterResource("/assets/oneconfig/images/$url.png"),
                        contentDescription = null,
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Column(
                modifier = Modifier.padding(vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(data.title, color = theme.textColor, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Written by", color = theme.textColorSecondary, fontSize = 12.sp)
                            Text(data.author, color = theme.textColor, fontSize = 12.sp)
                        }
                        Text(data.date, color = theme.textColorSecondary, fontSize = 12.sp)
                    }
                }
                MdBlocks(node = doc, styles = styles, modifier = Modifier.fillMaxWidth())
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}

@Composable
fun ChangelogEntry(data: ChangelogSection, index: Int) {
    val theme = LocalTheme.current
    val styles = MdStyles(
        body = TextStyle(fontFamily = theme.typography.family, fontSize = 13.sp, color = theme.textColorSecondary),
        headingColor = theme.textColor,
        bold = SpanStyle(fontWeight = FontWeight.SemiBold, color = theme.textColor),
        italic = SpanStyle(fontStyle = FontStyle.Italic),
        code = SpanStyle(background = theme.chipBackground, color = theme.textColor, fontSize = 12.sp),
        link = SpanStyle(color = Accent, fontWeight = FontWeight.Medium),
    )
    val doc = remember(data.content) { mdParser.parse(data.content) }

    Box(
        modifier = Modifier.fillMaxWidth()
            .background(theme.modCardBackground, theme.modCardShape)
            .border(
                1.dp, Brush.verticalGradient(
                    listOf(theme.borderColor, theme.borderColor.copy(0f))
                ), theme.modCardShape
            )
            .clip(theme.modCardShape)
    ) {
        val color = theme.textColor
        Box(
            Modifier.fillMaxSize().drawWithCache {
                val gradient = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0f),
                        color.copy(alpha = 0.02f),
                        color.copy(alpha = 0.05f)
                    ),
                    center = size.center,
                    radius = size.minDimension * 0.9f
                )
                onDrawBehind { drawRect(gradient) }
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            data.headerImageUrl?.let { url ->
                Box(
                    modifier = Modifier.width(376.dp).height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource("/assets/oneconfig/images/$url.png"),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(30.dp),
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.tint(Accent.copy(0.4f), BlendMode.Multiply)
                    )
                    Image(
                        painter = painterResource("/assets/oneconfig/images/$url.png"),
                        contentDescription = null,
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Column(
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 30.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(data.title, color = theme.textColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .clipToBounds()
                        .drawWithCache {
                            val fade = Brush.verticalGradient(
                                listOf(Color.Black, Color.Transparent),
                                startY = size.height * 0.75f,
                                endY = size.height
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(brush = fade, blendMode = BlendMode.DstIn)
                            }
                        }
                ) {
                    MdBlocks(node = doc, styles = styles)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(data.date, color = theme.textColorSecondary, fontSize = 12.sp)
                    Box(
                        modifier = Modifier.clickable {
                            LocalNavController.wrapper.navigate(ChangeLogEntryRoute(index))
                        }.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text("Read more", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
