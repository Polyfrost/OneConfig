package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text as CmText
import org.polyfrost.oneconfig.api.ui.v1.ChangelogSources
import org.polyfrost.oneconfig.internal.ui.api.ChangeKind
import org.polyfrost.oneconfig.internal.ui.api.ChangelogData
import org.polyfrost.oneconfig.internal.ui.api.ChangelogEntry
import org.polyfrost.oneconfig.internal.ui.api.ChangelogGroup
import org.polyfrost.oneconfig.internal.ui.api.ChangelogRelease
import org.polyfrost.oneconfig.internal.ui.components.ScrollbarBox
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.animatedFrame
import org.polyfrost.oneconfig.internal.ui.components.glassHover
import org.polyfrost.oneconfig.internal.ui.components.liquidGlass
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.components.wheelScroll
import org.polyfrost.oneconfig.internal.ui.search.searchMatches
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.shell.pageSearchQuery
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val LocalInstalledVersion = compositionLocalOf<String?> { null }

private val installedVersion: String?
    @Composable get() = LocalInstalledVersion.current

private var selectedVersion by mutableStateOf<String?>(null)

private val VersionListWidth = 200.dp

private fun ChangeKind.label(): String = translated(key, heading)

private fun formatDate(date: String?, style: FormatStyle): String? = date?.let {
    runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofLocalizedDate(style)) }.getOrDefault(it)
}

private fun seriesOf(version: String): String = version.split('.', '-').take(2).joinToString(".")

@Composable
fun Changelog() {
    val theme = LocalTheme.current
    val query = pageSearchQuery().trim()
    val source = ChangelogSources.active()
    val view by produceState(ChangelogData.cached(source), source) {
        value = ChangelogData.cached(source) ?: source?.let { ChangelogData.load(it) }
    }
    val current = view
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(translated("oneconfig.changelog.loading", "Loading the changelog"), color = theme.textColorSecondary, fontSize = 12.sp)
        }
        return
    }
    val releases = current.releases
    val visible = remember(query, releases) {
        if (query.isBlank()) releases else releases.filter { searchMatches(it.searchText, query.lowercase()) }
    }

    if (visible.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val message = if (releases.isEmpty()) translated("oneconfig.changelog.empty", "No changelog was bundled with this build")
            else translated("oneconfig.changelog.nomatch", "No releases match \"%s\"", query)
            Text(message, color = theme.textColorSecondary, fontSize = 12.sp)
        }
        return
    }

    val selected = visible.firstOrNull { it.version == selectedVersion }
        ?: visible.firstOrNull { it.version == current.installedVersion }
        ?: visible.first()

    CompositionLocalProvider(LocalInstalledVersion provides current.installedVersion) {
        Row(Modifier.fillMaxSize().padding(start = PageGutter)) {
            VersionList(visible, selected) { selectedVersion = it.version }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                ReleaseDetail(selected)
            }
        }
    }
}

@Composable
private fun VersionList(releases: List<ChangelogRelease>, selected: ChangelogRelease, onSelect: (ChangelogRelease) -> Unit) {
    val theme = LocalTheme.current
    val state = rememberLazyListState()
    val series = remember(releases) { releases.groupBy { seriesOf(it.version) }.entries.toList() }

    LaunchedEffect(releases) {
        var index = 0
        for ((_, list) in series) {
            index++
            val at = list.indexOf(selected)
            if (at >= 0) {
                state.scrollToItem((index + at - 3).coerceAtLeast(0))
                break
            }
            index += list.size
        }
    }

    val topFade by animateFloatAsState(if (state.canScrollBackward) 1f else 0f)
    val bottomFade by animateFloatAsState(if (state.canScrollForward) 1f else 0f)
    Box(Modifier.width(VersionListWidth).fillMaxHeight().wheelScroll(state).scrollable(state, Orientation.Vertical, reverseDirection = true)) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val edge = 32.dp.toPx().coerceAtMost(size.height / 2f)
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(1f - topFade),
                            edge / size.height to Color.Black,
                            1f - edge / size.height to Color.Black,
                            1f to Color.Black.copy(1f - bottomFade),
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
            contentPadding = PaddingValues(bottom = PageGutter),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            series.forEachIndexed { i, (name, list) ->
                item(key = "series:$name") {
                    Text(
                        name,
                        color = theme.textColor.copy(0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 6.dp, top = if (i == 0) 0.dp else 14.dp, bottom = 2.dp),
                    )
                }
                list.forEach { release ->
                    item(key = release.version) {
                        VersionEntry(release, release === selected) { onSelect(release) }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionEntry(release: ChangelogRelease, isSelected: Boolean, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    val translucent = theme.translucentSelection
    val selectedAlpha by animateFloatAsState(if (isSelected) 1f else 0f)
    val foreground by animateColorAsState(
        if (isSelected && !translucent) theme.accentTextColor
        else if (isSelected || hovered) theme.textColor
        else theme.textColor.copy(0.8f)
    )
    val secondary by animateColorAsState(
        if (isSelected && !translucent) theme.accentTextColor.copy(0.75f) else theme.textColorSecondary
    )
    val border by animateColorAsState(
        if (isSelected && translucent) theme.borderColor.copy(0f)
        else if (hovered || isSelected) theme.borderColor
        else theme.borderColor.copy(0f)
    )
    val installed = release.version == installedVersion

    Row(
        Modifier.width(VersionListWidth - 16.dp).height(33.dp)
            .glassHover(theme.sideBarNavigationEntryShape, pull = 0.75.dp)
            .graphicsLayer {}
            .animatedFrame(
                theme.sideBarNavigationEntryShape,
                fill = { if (translucent) theme.textColor.copy(alpha = 0.12f * selectedAlpha) else Color.Transparent },
                accentAlpha = { if (translucent) 0f else selectedAlpha },
                border = { border },
            )
            .then(if (translucent) Modifier.liquidGlass(theme.sideBarNavigationEntryShape) { selectedAlpha } else Modifier)
            .onClick(source, onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(release.version, color = Color.Unspecified, colorProducer = { foreground }, fontSize = 14.sp, maxLines = 1)
        if (installed) {
            Box(Modifier.size(5.dp).background(if (isSelected && !translucent) theme.accentTextColor else Accent, CircleShape))
        }
        Spacer(Modifier.weight(1f))
        formatDate(release.date, FormatStyle.SHORT)?.let {
            Text(it, color = Color.Unspecified, colorProducer = { secondary }, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ReleaseDetail(release: ChangelogRelease) {
    val theme = LocalTheme.current
    val scroll = remember(release.version) { ScrollState(0) }
    val forPlayers = release.groups.mapNotNull { group -> group.entries.filter { !it.forDevelopers }.takeIf { it.isNotEmpty() }?.let { ChangelogGroup(group.kind, it) } }
    val forDevelopers = release.groups.mapNotNull { group -> group.entries.filter { it.forDevelopers }.takeIf { it.isNotEmpty() }?.let { ChangelogGroup(group.kind, it) } }

    ScrollbarBox(scroll, PageGutter, Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll).padding(start = 18.dp, end = PageGutter, bottom = PageGutter),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(release.version, color = theme.textColor, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        formatDate(release.date, FormatStyle.LONG)?.let { Text(it, color = theme.textColorSecondary, fontSize = 12.sp) }
                        if (release.version == installedVersion) {
                            Text(
                                translated("oneconfig.changelog.installed", "Installed"),
                                color = theme.textColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.background(theme.chipBackground, theme.buttonShape).padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
            forPlayers.forEach { KindSection(it, developers = false) }
            if (forDevelopers.isNotEmpty()) {
                Text(
                    translated("oneconfig.changelog.developers", "For mod developers"),
                    color = theme.textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 32.dp),
                )
                forDevelopers.forEach { KindSection(it, developers = true) }
            }
        }
    }
}

@Composable
private fun KindSection(group: ChangelogGroup, developers: Boolean) {
    val theme = LocalTheme.current
    Text(
        group.kind.label().uppercase(),
        color = theme.textColor.copy(0.8f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = if (developers) 16.dp else 24.dp, bottom = 8.dp, start = 2.dp),
    )
    EntryList(group.entries, depth = 0)
}

@Composable
private fun EntryList(entries: List<ChangelogEntry>, depth: Int) {
    val theme = LocalTheme.current
    val body = TextStyle(
        fontFamily = theme.typography.family,
        fontSize = if (depth == 0) 14.sp else 12.sp,
        lineHeight = if (depth == 0) 20.sp else 18.sp,
        color = if (depth == 0) theme.textColor else theme.textColorSecondary,
    )
    val styles = InlineStyles(
        bold = SpanStyle(fontWeight = FontWeight.SemiBold),
        italic = SpanStyle(fontStyle = FontStyle.Italic),
        code = SpanStyle(background = theme.chipBackground, fontSize = 12.sp),
        link = SpanStyle(color = Accent, fontWeight = FontWeight.Medium),
    )
    Column(
        Modifier.padding(start = (depth * 16).dp),
        verticalArrangement = Arrangement.spacedBy(if (depth == 0) 8.dp else 4.dp),
    ) {
        entries.forEach { entry ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.padding(start = 4.dp, top = if (depth == 0) 8.dp else 7.dp)
                            .size(4.dp)
                            .background(theme.textColorSecondary.copy(if (depth == 0) 0.7f else 0.5f), CircleShape)
                    )
                    BasicText(entry.paragraph?.let { inline(it, styles) } ?: AnnotatedString(entry.text), style = body)
                }
                if (entry.children.isNotEmpty()) EntryList(entry.children, depth + 1)
            }
        }
    }
}

private class InlineStyles(val bold: SpanStyle, val italic: SpanStyle, val code: SpanStyle, val link: SpanStyle)

private fun inline(node: Node, styles: InlineStyles): AnnotatedString = buildAnnotatedString {
    fun walk(n: Node) {
        var c = n.firstChild
        while (c != null) {
            when (c) {
                is CmText -> append(c.literal)
                is StrongEmphasis -> { pushStyle(styles.bold); walk(c); pop() }
                is Emphasis -> { pushStyle(styles.italic); walk(c); pop() }
                is Code -> { pushStyle(styles.code); append(" ${c.literal} "); pop() }
                is Link -> { pushStyle(styles.link); walk(c); pop() }
                is SoftLineBreak -> append(" ")
                is HardLineBreak -> append("\n")
                else -> walk(c)
            }
            c = c.next
        }
    }
    walk(node)
}
