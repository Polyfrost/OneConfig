package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.animation.core.spring
import org.polyfrost.compose.foundation.TooltipArea
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.hoverable
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.Spacer
import org.polyfrost.compose.foundation.layout.fillMaxHeight
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.layout.widthIn
import org.polyfrost.compose.foundation.shape.RoundedCornerShape
import org.polyfrost.compose.foundation.text.BasicTextField
import org.polyfrost.compose.foundation.text.TextFieldState
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.compose.ui.window.Popup
import org.polyfrost.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.navigation.currentPageTitle
import org.polyfrost.oneconfig.internal.ui.navigation.searchPlaceholder
import org.polyfrost.oneconfig.internal.ui.screens.LocalUiClipboard
import org.polyfrost.oneconfig.internal.ui.shell.ModConfigTarget
import org.polyfrost.oneconfig.internal.ui.shell.ShellNav
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.AccentBrush
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Header(nav: ShellNav, search: TextFieldState, closeRequest: () -> Unit, modifier: Modifier = Modifier) {
    val title = remember(nav.current, nav.arg, ConfigRegistry.revision) { nav.headerTitle() }
    val openOriginalScreen = (nav.arg as? ModConfigTarget)?.let { ConfigRegistry.findTree(it.mod.id)?.getMetadata<Runnable>("open_original_screen") }
    val canGoBack = nav.canGoBack()
    val canGoForward = nav.canGoForward()

    Row(
        modifier = modifier.fillMaxWidth().height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton("left-arrow", Modifier.size(32.dp), enabled = canGoBack, hoverBorder = true) { nav.back() }
                IconButton("right-arrow", Modifier.size(32.dp), enabled = canGoForward, hoverBorder = true) { nav.forward() }
            }
            Spacer(Modifier.size(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FadingTitle(title) { info ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            info.text,
                            color = LocalTheme.current.textColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium
                        )
                        TitleAuthorByline(info.authors)
                        TitleInfoTooltip(info)
                    }
                }
                var lastAction by remember { mutableStateOf<Runnable?>(null) }
                if (openOriginalScreen != null && openOriginalScreen !== lastAction) SideEffect { lastAction = openOriginalScreen }
                val target = if (openOriginalScreen != null) 1f else 0f
                val alpha = animateFloatAsState(target, spring(stiffness = 400f), initialValue = target)
                val gone by remember { derivedStateOf { alpha.value == 0f } }
                if (!gone && (openOriginalScreen ?: lastAction) != null) {
                    // one Row so the spacer and the button share a single measured node and keep their
                    // horizontal order
                    Row(Modifier.alpha(alpha, target), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.size(8.dp))
                        OpenOriginalScreenButton(openOriginalScreen)
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            GlobalSearchBar(search, searchPlaceholder(nav.current, nav.arg))
            IconButton("close", Modifier.size(32.dp), hoverBorder = true) { closeRequest() }
        }
    }
}

private data class HeaderTitle(
    val text: String,
    val authors: String? = null,
    val credits: String? = null,
    val version: String? = null,
)

private fun ShellNav.headerTitle(): HeaderTitle {
    val config = (arg as? ModConfigTarget)?.mod ?: return HeaderTitle(currentPageTitle() ?: current.title)
    return HeaderTitle(
        text = config.title.asRenderText(),
        authors = config.authors,
        credits = config.credits,
        version = config.version,
    )
}

@Composable
private fun FadingTitle(title: HeaderTitle, content: @Composable (HeaderTitle) -> Unit) {
    val family = LocalTheme.current.typography.family
    val titles = remember { mutableStateListOf(title) }
    if (title !in titles) titles.add(title)
    val width = remember(title, family) {
        val byline = title.authors?.authorByline()?.let { 8f + PolyFont(family.resolve(FontWeight.Medium), 14f).measureTextWidth(it) } ?: 0f
        val info = if (title.version == null && title.authors == null && title.credits == null) 0f else 8f + 22f
        PolyFont(family.resolve(FontWeight.Medium), 24f).measureTextWidth(title.text) + byline + info + 1f
    }
    val w by animateFloatAsState(width, spring(stiffness = 400f, visibilityThreshold = 0.5f))
    Box(Modifier.width(w.dp).clip()) {
        for (t in titles.toList()) key(t) {
            val alpha by animateFloatAsState(if (t == title) 1f else 0f, spring(stiffness = 400f), initialValue = if (titles.size == 1) 1f else 0f)
            if (alpha == 0f && t != title) SideEffect { titles.remove(t) }
            Box(Modifier.alpha(alpha)) { content(t) }
        }
    }
}

@Composable
private fun OpenOriginalScreenButton(runnable: Runnable?) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val accentAlpha by animateFloatAsState(if (isHovered) 0.75f else 1f)

    TooltipArea(
        tooltip = {
            val tipShape = RoundedCornerShape(8.dp)
            Box(Modifier.widthIn(max = 260.dp).background(theme.popupBackground, tipShape).border(1.dp, theme.borderColor, tipShape).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    translated("oneconfig.header.original_menu.tooltip", "Opening the original screen may show options that OneConfig doesn't."),
                    color = theme.textColor,
                    fontSize = 13.sp,
                    softWrap = true,
                )
            }
        },
        below = true,
    ) {
        Box(
            modifier = Modifier
                .pointerHoverIcon(PointerIcon.Hand)
                .background(AccentBrush, theme.buttonShape, accentAlpha)
                .onClick(interactionSource, runnable != null) { runnable?.run() }
                .border(1.dp, LocalTheme.current.borderColor, LocalTheme.current.buttonShape)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon("external-link", color = theme.accentTextColor, modifier = Modifier.size(14.dp))
                Text(translated("oneconfig.header.original_menu", "Original menu"), color = theme.accentTextColor, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TitleAuthorByline(authors: String?) {
    val byline = authors?.authorByline() ?: return
    Text(
        byline,
        color = LocalTheme.current.textColorSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
    )
}

private fun String.authorByline(): String? {
    val authors = split(',')
        .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
    val first = authors.firstOrNull() ?: return null
    val remaining = authors.size - 1
    return if (remaining > 0) {
        translated("oneconfig.header.byline.more", "by %s and %s more...", first, remaining)
    } else {
        translated("oneconfig.header.byline", "by %s", first)
    }
}

@Composable
private fun TitleInfoTooltip(title: HeaderTitle) {
    val version = title.version
    val authors = title.authors
    val credits = title.credits
    if (version == null && authors == null && credits == null) return
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier.size(22.dp).hoverable(interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon("info", color = LocalTheme.current.textColorSecondary, modifier = Modifier.size(14.dp))

        if (isHovered) {
            // popup so the tooltip takes no layout space in the header Row and is not clipped by its
            // fixed height
            Popup(
                alignment = Alignment.TopStart,
                offset = DpOffset(0.dp, 6.dp),
                properties = PopupProperties(focusable = false),
                anchor = Alignment.BottomStart,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .background(LocalTheme.current.popupBackground, shape = LocalTheme.current.popupShape)
                        .border(1.dp, LocalTheme.current.borderColor, shape = LocalTheme.current.popupShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (authors != null) {
                        Text(translated("oneconfig.header.author", "Author: %s", authors), color = LocalTheme.current.textColor, fontSize = 12.sp, softWrap = true)
                    }
                    if (credits != null) {
                        Text(translated("oneconfig.header.credits", "Credits: %s", credits), color = LocalTheme.current.textColor, fontSize = 12.sp, softWrap = true)
                    }
                    if (version != null) {
                        Text(translated("oneconfig.header.version", "Version: %s", version), color = LocalTheme.current.textColor, fontSize = 12.sp, softWrap = true)
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalSearchBar(searchText: TextFieldState, placeholder: String, modifier: Modifier = Modifier) {
    val clipboard = LocalUiClipboard.current
    val family = LocalTheme.current.typography.family

    Box(
        modifier = modifier
            .width(256.dp).height(32.dp)
            .background(
                LocalTheme.current.chipBackground,
                LocalTheme.current.sideBarNavigationEntryShape
            )
            .border(
                1.dp,
                LocalTheme.current.borderColor,
                LocalTheme.current.sideBarNavigationEntryShape
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    "search",
                    color = LocalTheme.current.textColorSecondary,
                    modifier = Modifier.size(14.dp),
                )
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    searchText,
                    PolyFont(family.resolve(FontWeight.Medium), 12f),
                    color = LocalTheme.current.textColorSecondary,
                    caretColor = LocalTheme.current.textColorSecondary,
                    selectionColor = Accent.copy(alpha = 0.4f),
                    readClipboard = clipboard.read,
                    writeClipboard = clipboard.write,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = placeholder,
                    placeholderColor = LocalTheme.current.textColorSecondary,
                    placeholderFont = PolyFont(family.resolve(FontWeight.Normal), 12f),
                )
            }
            if (searchText.text.isNotEmpty()) {
                IconButton("close", modifier = Modifier.size(16.dp)) {
                    searchText.replaceText("")
                    searchText.moveCaret(0)
                }
            }
        }
    }
}
