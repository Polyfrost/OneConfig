package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
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
import org.polyfrost.compose.foundation.Canvas
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.clickable
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.lazy.LazyColumn
import org.polyfrost.compose.foundation.lazy.LazyListState
import org.polyfrost.compose.foundation.rememberScrollState
import org.polyfrost.compose.foundation.verticalScroll
import org.polyfrost.compose.node.PolyTextSpan
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.compose.render.PolyFade
import org.polyfrost.compose.render.RenderContext
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.draw.fadeMask
import org.polyfrost.compose.ui.graphics.Brush
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.ImageBitmap
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.text.font.FontFamily
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.unit.TextUnit
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.api.ChangelogData
import org.polyfrost.oneconfig.internal.ui.api.ChangelogSection
import org.polyfrost.oneconfig.internal.ui.components.ScrollbarBox
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.poly.GUTTER
import org.polyfrost.oneconfig.internal.ui.poly.UiText
import org.polyfrost.oneconfig.internal.ui.search.searchMatches
import org.polyfrost.oneconfig.internal.ui.shell.PolyPage
import org.polyfrost.oneconfig.internal.ui.shell.ShellHost
import org.polyfrost.oneconfig.internal.ui.shell.ShellNav
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

private data class MdTextStyle(
    val fontFamily: FontFamily,
    val fontSize: TextUnit,
    val color: Color,
    val fontWeight: FontWeight = FontWeight.Normal,
) {
    val font: String get() = fontFamily.resolve(fontWeight)
}

private data class MdSpanStyle(
    val color: Color? = null,
    val font: String? = null,
    val background: Color? = null,
    val fontSize: TextUnit? = null,
) {
    operator fun plus(other: MdSpanStyle) = MdSpanStyle(
        other.color ?: color,
        other.font ?: font,
        other.background ?: background,
        other.fontSize ?: fontSize,
    )
}

private data class MdStyles(
    val body: MdTextStyle,
    val headingColor: Color,
    val bold: MdSpanStyle,
    val italic: MdSpanStyle,
    val code: MdSpanStyle,
    val link: MdSpanStyle,
) {
    fun heading(level: Int): MdTextStyle = when (level) {
        1 -> body.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = headingColor)
        2 -> body.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = headingColor)
        else -> body.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = headingColor)
    }
}

private fun buildInlineString(node: Node, styles: MdStyles): Pair<String, List<PolyTextSpan>> {
    val text = StringBuilder()
    val spans = ArrayList<PolyTextSpan>()
    fun append(s: String, style: MdSpanStyle) {
        if (s.isEmpty()) return
        spans += PolyTextSpan(s, style.color, style.font, background = style.background, size = style.fontSize?.value)
        text.append(s)
    }
    fun appendNode(n: Node, style: MdSpanStyle) {
        var c = n.firstChild
        while (c != null) {
            when (c) {
                is CmText -> append(c.literal, style)
                is StrongEmphasis -> appendNode(c, style + styles.bold)
                is Emphasis -> appendNode(c, style + styles.italic)
                is Code -> append(c.literal, style + styles.code)
                is Link -> appendNode(c, style + styles.link)
                is SoftLineBreak -> append(" ", style)
                is HardLineBreak -> append("\n", style)
                else -> appendNode(c, style)
            }
            c = c.next
        }
    }
    appendNode(node, MdSpanStyle())
    return text.toString() to spans
}

@Composable
private fun BasicText(text: Pair<String, List<PolyTextSpan>>, style: MdTextStyle, modifier: Modifier = Modifier) =
    UiText(text.first, style.color, style.fontSize.value, style.font, modifier, wrap = true, spans = text.second)

@Composable
private fun BasicText(text: String, style: MdTextStyle) =
    UiText(text, style.color, style.fontSize.value, style.font)

@Composable
private fun MdBlocks(node: Node, styles: MdStyles, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        node.children().forEach { block ->
            when (block) {
                is Heading -> BasicText(
                    text = remember(block, styles) { buildInlineString(block, styles) },
                    style = styles.heading(block.level),
                    modifier = Modifier.fillMaxWidth(),
                )
                is Paragraph -> BasicText(
                    text = remember(block, styles) { buildInlineString(block, styles) },
                    style = styles.body,
                    modifier = Modifier.fillMaxWidth(),
                )
                is BulletList -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    block.children().filterIsInstance<ListItem>().forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BasicText("•", style = styles.body)
                            val firstChild = item.firstChild
                            if (firstChild is Paragraph) {
                                BasicText(
                                    remember(firstChild, styles) { buildInlineString(firstChild, styles) },
                                    style = styles.body,
                                    modifier = Modifier.weight(1f),
                                )
                            } else {
                                MdBlocks(item, styles, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Changelog(searchQuery: String, nav: ShellNav) {
    val localSearchQuery = searchQuery.trim()
    val lazyListState = remember(localSearchQuery) { LazyListState() }
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

    ScrollbarBox(lazyListState, GUTTER.dp, Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().padding(horizontal = GUTTER.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            visibleSections.forEach { (index, section) ->
                item(key = index) { ChangelogEntry(section, index, nav) }
            }
        }
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
        body = MdTextStyle(fontFamily = theme.typography.family, fontSize = 13.sp, color = theme.textColorSecondary),
        headingColor = theme.textColor,
        bold = MdSpanStyle(font = theme.typography.family.resolve(FontWeight.SemiBold), color = theme.textColor),
        italic = MdSpanStyle(font = if (theme.previewImage.startsWith("minecraft")) null else "poppins-italic"),
        code = MdSpanStyle(background = theme.chipBackground, color = theme.textColor, fontSize = 12.sp),
        link = MdSpanStyle(color = Accent, font = theme.typography.family.resolve(FontWeight.Medium)),
    )
    val doc = remember(data.content) { mdParser.parse(data.content) }

    val scrollState = rememberScrollState()

    ScrollbarBox(scrollState, GUTTER.dp, Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = GUTTER.dp)) {
            data.headerImageUrl?.let { url ->
                HeaderImage(
                    url,
                    modifier = Modifier.fillMaxWidth().height(221.dp)
                        .clip(theme.modCardShape),
                    darken = 0.4f,
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 22.dp),
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
    }
}

@Composable
fun ChangelogEntry(data: ChangelogSection, index: Int, nav: ShellNav) {
    val theme = LocalTheme.current
    val styles = MdStyles(
        body = MdTextStyle(fontFamily = theme.typography.family, fontSize = 13.sp, color = theme.textColorSecondary),
        headingColor = theme.textColor,
        bold = MdSpanStyle(font = theme.typography.family.resolve(FontWeight.SemiBold), color = theme.textColor),
        italic = MdSpanStyle(font = if (theme.previewImage.startsWith("minecraft")) null else "poppins-italic"),
        code = MdSpanStyle(background = theme.chipBackground, color = theme.textColor, fontSize = 12.sp),
        link = MdSpanStyle(color = Accent, font = theme.typography.family.resolve(FontWeight.Medium)),
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
        Canvas(Modifier.fillMaxSize()) { x, y, w, h ->
            val gradient = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0f),
                    color.copy(alpha = 0.02f),
                    color.copy(alpha = 0.05f)
                ),
                radius = 0.9f
            )
            rectGradient(x, y, w, h, gradient)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            data.headerImageUrl?.let { url ->
                HeaderImage(url, Modifier.width(376.dp).height(250.dp))
            }
            Column(
                modifier = Modifier.weight(1f).padding(vertical = 16.dp, horizontal = 30.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(data.title, color = theme.textColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip()
                        .fadeMask { PolyFade(true, floatArrayOf(it.height * 0.75f, it.height), floatArrayOf(1f, 0f)) }
                ) {
                    MdBlocks(node = doc, styles = styles, modifier = Modifier.fillMaxWidth())
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(data.date, color = theme.textColorSecondary, fontSize = 12.sp)
                    Box(
                        modifier = Modifier.clickable {
                            nav.navigate(PolyPage.ChangelogEntry, index)
                        }.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Text("Read more", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderImage(name: String, modifier: Modifier, darken: Float = 0f) {
    val image = remember(name) { headerImage(name) } ?: return Box(modifier)
    val blurred = remember(image) { blurred(image) }
    val tint = PolyColor.WHITE.lerp(Accent.withAlpha(255).lerp(PolyColor.BLACK, darken), 0.4f)
    Canvas(modifier) { x, y, w, h ->
        cropped(blurred, x, y, w, h, tint)
        val s = min(w / image.width, h / image.height)
        val iw = image.width * s; val ih = image.height * s
        image(image, x + (w - iw) / 2f, y + (h - ih) / 2f, iw, ih)
    }
}

private fun RenderContext.cropped(bitmap: ImageBitmap, x: Float, y: Float, w: Float, h: Float, tint: PolyColor) {
    val s = max(w / bitmap.width, h / bitmap.height)
    val du = w / (bitmap.width * s) / 2f
    val dv = h / (bitmap.height * s) / 2f
    image(bitmap, x, y, w, h, 0.5f - du, 0.5f - dv, 0.5f + du, 0.5f + dv, tint)
}

private val headerImages = HashMap<String, ImageBitmap?>()

private fun headerImage(name: String) = headerImages.getOrPut(name) {
    ShellHost::class.java.getResourceAsStream("/assets/oneconfig/images/$name.png")?.use { it.readBytes() }?.let { runCatching { ImageBitmap.decode(it) }.getOrNull() }
}

private fun blurred(src: ImageBitmap): ImageBitmap {
    val w = max(1, src.width / 4); val h = max(1, src.height / 4)
    val px = Array(4) { FloatArray(w * h) }
    for (y in 0 until h) for (x in 0 until w) {
        var r = 0f; var g = 0f; var b = 0f; var a = 0f; var n = 0
        for (sy in y * 4 until min(src.height, y * 4 + 4)) for (sx in x * 4 until min(src.width, x * 4 + 4)) {
            val i = (sy * src.width + sx) * 4
            val al = (src.pixels[i + 3].toInt() and 255) / 255f
            r += (src.pixels[i].toInt() and 255) * al; g += (src.pixels[i + 1].toInt() and 255) * al; b += (src.pixels[i + 2].toInt() and 255) * al
            a += al; n++
        }
        val o = y * w + x
        px[0][o] = r / n; px[1][o] = g / n; px[2][o] = b / n; px[3][o] = a / n
    }
    val sigma = 2f
    val e = 6
    val k = FloatArray(e * 2 + 1) { exp(-((it - e) * (it - e)) / (2f * sigma * sigma)) }.let { k -> val s = k.sum(); FloatArray(k.size) { k[it] / s } }
    fun pass(c: FloatArray, horizontal: Boolean) = FloatArray(w * h) { i ->
        val x = i % w; val y = i / w
        var acc = 0f
        for (j in k.indices) {
            val sx = if (horizontal) (x + j - e).coerceIn(0, w - 1) else x
            val sy = if (horizontal) y else (y + j - e).coerceIn(0, h - 1)
            acc += c[sy * w + sx] * k[j]
        }
        acc
    }
    for (c in 0 until 4) px[c] = pass(pass(px[c], true), false)
    val out = ByteArray(w * h * 4)
    for (i in 0 until w * h) {
        val a = px[3][i]
        for (c in 0 until 3) out[i * 4 + c] = (if (a > 0f) px[c][i] / a else 0f).roundToInt().coerceIn(0, 255).toByte()
        out[i * 4 + 3] = (a * 255f).roundToInt().coerceIn(0, 255).toByte()
    }
    return ImageBitmap(w, h, out)
}
