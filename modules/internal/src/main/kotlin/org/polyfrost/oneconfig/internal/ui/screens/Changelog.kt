package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.HtmlInline
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.polyfrost.oneconfig.api.ui.v1.ChangelogSource
import org.polyfrost.oneconfig.api.ui.v1.ChangelogSources
import org.polyfrost.oneconfig.api.ui.v1.components.Badge
import org.polyfrost.oneconfig.api.ui.v1.components.BadgeTone
import org.polyfrost.oneconfig.api.ui.v1.components.EmptyState
import org.polyfrost.oneconfig.api.ui.v1.components.LoadState
import org.polyfrost.oneconfig.api.ui.v1.components.MasterDetailLayout
import org.polyfrost.oneconfig.api.ui.v1.components.NavItem
import org.polyfrost.oneconfig.api.ui.v1.components.ScrollContainer
import org.polyfrost.oneconfig.api.ui.v1.components.SectionHeader
import org.polyfrost.oneconfig.api.ui.v1.components.SectionHeaderDefaults
import org.polyfrost.oneconfig.api.ui.v1.components.StatusDot
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.internal.ui.api.ChangeKind
import org.polyfrost.oneconfig.internal.ui.api.ChangelogData
import org.polyfrost.oneconfig.internal.ui.api.ChangelogEntry
import org.polyfrost.oneconfig.internal.ui.api.ChangelogGroup
import org.polyfrost.oneconfig.internal.ui.api.ChangelogRelease
import org.polyfrost.oneconfig.internal.ui.api.ModrinthChangelogs
import org.polyfrost.oneconfig.internal.ui.api.ModrinthVersionLink
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.search.searchMatches
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.shell.pageSearchQuery
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.utils.v1.NetworkUtils
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.commonmark.node.Text as CmText

private val LocalInstalledVersion = compositionLocalOf<String?> { null }

private val installedVersion: String?
    @Composable get() = LocalInstalledVersion.current

private var selection by mutableStateOf<Pair<ChangelogSource?, String>?>(null)

private var openedVersion by mutableStateOf<ModrinthVersionLink?>(null)

private val bareUrl = Regex("""https?://[^\s<>()]+""", RegexOption.IGNORE_CASE)

internal fun openChangelogLink(url: String) {
    val target = url.trim()
    val version = ModrinthChangelogs.parseLink(target)
    if (version != null) {
        openedVersion = version
        return
    }
    val scheme = runCatching { URI(target).scheme?.lowercase() }.getOrNull()
    if (scheme == "http" || scheme == "https") runCatching { NetworkUtils.browseLink(target) }
}

private val ChangelogLinkListener = LinkInteractionListener { link ->
    (link as? LinkAnnotation.Clickable)?.tag?.let(::openChangelogLink)
}

private val VersionListWidth = 200.dp

private fun ChangeKind.label(): String = translated(key, heading)

private fun formatDate(date: String?, style: FormatStyle): String? = date?.let {
    runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofLocalizedDate(style)) }.getOrDefault(it)
}

private fun seriesOf(version: String): String = version.split('.', '-').take(2).joinToString(".")

@Composable
fun Changelog() {
    val query = pageSearchQuery().trim()
    val source = ChangelogSources.active()
    DisposableEffect(Unit) { onDispose { openedVersion = null } }
    openedVersion?.let { ModrinthChangelogDialog(it) { openedVersion = null } }
    val view by produceState(ChangelogData.cached(source), source) {
        value = ChangelogData.cached(source)
        if (value == null && source != null) value = ChangelogData.load(source)
    }
    val current = view
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(translated("oneconfig.changelog.loading", "Loading the changelog"), state = LoadState.Loading)
        }
        return
    }
    val releases = current.releases
    val visible = remember(query, releases) {
        if (query.isBlank()) releases else releases.filter { searchMatches(it.searchText, query.lowercase()) }
    }

    if (visible.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (releases.isEmpty()) {
                EmptyState(translated("oneconfig.changelog.empty", "No changelog was bundled with this build"))
            } else {
                EmptyState(translated("oneconfig.changelog.nomatch", "No releases match \"%s\"", query), icon = "search")
            }
        }
        return
    }

    val selectedVersion = selection?.takeIf { it.first === source }?.second
    val selected = visible.firstOrNull { it.version == selectedVersion }
        ?: visible.firstOrNull { it.version == current.installedVersion }
        ?: visible.first()

    CompositionLocalProvider(LocalInstalledVersion provides current.installedVersion) {
        MasterDetailLayout(
            list = { VersionList(visible, selected) { selection = source to it.version } },
            detail = { ReleaseDetail(selected) },
            modifier = Modifier.padding(start = PageGutter),
            listWidth = VersionListWidth,
            gap = 0.dp,
        )
    }
}

@Composable
private fun VersionList(releases: List<ChangelogRelease>, selected: ChangelogRelease, onSelect: (ChangelogRelease) -> Unit) {
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

    ScrollContainer(state, Modifier.width(VersionListWidth).fillMaxHeight(), scrollbar = false, fadingEdges = true) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = PageGutter),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            series.forEachIndexed { i, (name, list) ->
                item(key = "series:$name") {
                    SectionHeader(
                        name,
                        Modifier.width(VersionListWidth - 16.dp),
                        contentPadding = PaddingValues(start = 6.dp, top = if (i == 0) 0.dp else 14.dp, bottom = 2.dp),
                        textStyle = SectionHeaderDefaults.labelStyle,
                        color = OneConfigTheme.current.textColor.copy(0.8f),
                        uppercase = false,
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
    val caption = LocalTheme.current.typography.caption.fontSize
    val installed = release.version == installedVersion
    val date = formatDate(release.date, FormatStyle.SHORT)
    NavItem(
        isSelected,
        release.version,
        onClick,
        Modifier.width(VersionListWidth - 16.dp),
        trailing = { state ->
            if (installed) StatusDot(BadgeTone.Accent, onAccent = state.onAccent)
            Spacer(Modifier.weight(1f))
            date?.let {
                Text(it, color = Color.Unspecified, colorProducer = { state.secondary() }, fontSize = caption, maxLines = 1)
            }
        },
    )
}

@Composable
private fun ReleaseDetail(release: ChangelogRelease) {
    val theme = LocalTheme.current
    val scroll = remember(release.version) { ScrollState(0) }
    val forPlayers = release.groups.mapNotNull { group -> group.entries.filter { !it.forDevelopers }.takeIf { it.isNotEmpty() }?.let { ChangelogGroup(group.kind, it) } }
    val forDevelopers = release.groups.mapNotNull { group -> group.entries.filter { it.forDevelopers }.takeIf { it.isNotEmpty() }?.let { ChangelogGroup(group.kind, it) } }

    ScrollContainer(scroll, Modifier.fillMaxSize(), gutter = PageGutter) {
        Column(
            Modifier.fillMaxWidth().padding(start = 18.dp, bottom = PageGutter),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(release.version, color = theme.textColor, fontSize = theme.typography.display.fontSize, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        formatDate(release.date, FormatStyle.LONG)?.let { Text(it, color = theme.textColorSecondary, fontSize = theme.typography.small.fontSize) }
                        if (release.version == installedVersion) {
                            Badge(translated("oneconfig.changelog.installed", "Installed"))
                        }
                    }
                }
            }
            forPlayers.forEach { KindSection(it, developers = false) }
            if (forDevelopers.isNotEmpty()) {
                SectionHeader(
                    translated("oneconfig.changelog.developers", "For mod developers"),
                    eyebrow = false,
                    contentPadding = PaddingValues(top = 32.dp),
                    textStyle = OneConfigTheme.current.typography.titleSmall,
                )
                forDevelopers.forEach { KindSection(it, developers = true) }
            }
        }
    }
}

@Composable
private fun KindSection(group: ChangelogGroup, developers: Boolean) {
    SectionHeader(
        group.kind.label(),
        contentPadding = PaddingValues(top = if (developers) 16.dp else 24.dp, bottom = 8.dp, start = 2.dp),
        textStyle = SectionHeaderDefaults.labelStyle,
        color = OneConfigTheme.current.textColor.copy(0.8f),
    )
    EntryList(group.entries, depth = 0)
}

@Composable
private fun EntryList(entries: List<ChangelogEntry>, depth: Int) {
    val theme = LocalTheme.current
    val body = changelogBodyStyle(depth)
    val styles = changelogInlineStyles()
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
                            .background(theme.textColorSecondary.copy(if (depth == 0) 0.7f else 0.5f), theme.shapes.circle)
                    )
                    BasicText(entry.paragraph?.let { inline(it, styles) } ?: AnnotatedString(entry.text), style = body)
                }
                if (entry.children.isNotEmpty()) EntryList(entry.children, depth + 1)
            }
        }
    }
}

@Composable
internal fun changelogBodyStyle(depth: Int): TextStyle {
    val theme = LocalTheme.current
    return TextStyle(
        fontFamily = theme.typography.family,
        fontSize = if (depth == 0) theme.typography.body.fontSize else theme.typography.small.fontSize,
        lineHeight = if (depth == 0) 20.sp else 18.sp,
        color = if (depth == 0) theme.textColor else theme.textColorSecondary,
    )
}

@Composable
internal fun changelogInlineStyles(): InlineStyles {
    val theme = LocalTheme.current
    val accent = Accent
    return InlineStyles(
        bold = SpanStyle(fontWeight = FontWeight.SemiBold),
        italic = SpanStyle(fontStyle = FontStyle.Italic),
        code = SpanStyle(background = theme.chipBackground, fontSize = theme.typography.small.fontSize),
        link = TextLinkStyles(
            style = SpanStyle(color = accent, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline),
            hoveredStyle = SpanStyle(color = accent.copy(alpha = 0.75f)),
        ),
    )
}

internal data class InlineStyles(val bold: SpanStyle, val italic: SpanStyle, val code: SpanStyle, val link: TextLinkStyles)

internal fun inline(node: Node, styles: InlineStyles): AnnotatedString = buildAnnotatedString {
    fun text(literal: String, linked: Boolean) {
        if (linked) return append(literal)
        var last = 0
        bareUrl.findAll(literal).forEach { match ->
            val url = match.value.trimEnd('.', ',', ';', ':', '!', '?', '\'', '"')
            append(literal.substring(last, match.range.first))
            withLink(LinkAnnotation.Clickable(url, styles.link, ChangelogLinkListener)) { append(url) }
            last = match.range.first + url.length
        }
        append(literal.substring(last))
    }

    fun walk(n: Node, linked: Boolean) {
        var c = n.firstChild
        while (c != null) {
            when (c) {
                is CmText -> text(c.literal, linked)
                is StrongEmphasis -> { pushStyle(styles.bold); walk(c, linked); pop() }
                is Emphasis -> { pushStyle(styles.italic); walk(c, linked); pop() }
                is Code -> { pushStyle(styles.code); append(" ${c.literal} "); pop() }
                is Link -> withLink(LinkAnnotation.Clickable(c.destination, styles.link, ChangelogLinkListener)) { walk(c, true) }
                is HtmlInline -> Unit
                is SoftLineBreak -> append(" ")
                is HardLineBreak -> append("\n")
                else -> walk(c, linked)
            }
            c = c.next
        }
    }
    walk(node, false)
}
