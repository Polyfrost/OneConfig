package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus
import org.polyfrost.oneconfig.internal.ui.components.dropdown.DropdownPositionProvider
import androidx.navigation.compose.currentBackStackEntryAsState
import org.polyfrost.oneconfig.internal.ui.LocalCloseRequest
import org.polyfrost.oneconfig.internal.ui.navigation.searchPlaceholder
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import org.polyfrost.oneconfig.internal.ui.api.Tooltip
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Header() {
    val controller = LocalNavController.wrapper
    val closeRequest = LocalCloseRequest.current

    Row(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton("left-arrow") { controller.back() }
                IconButton("right-arrow") { controller.forward() }
            }
            AnimatedContent(
                ShellState.title,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }
            ) { text ->
                text?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(it, color = LocalTheme.current.textColor, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                        TitleAuthorByline(it)
                        TitleInfoTooltip(it)
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            ShellState.openOriginalScreen?.let { OpenOriginalScreenButton(it) }
            GlobalSearchBar()
            IconButton("close") { closeRequest() }
        }
    }
}

@Composable
private fun OpenOriginalScreenButton(runnable: Runnable) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bgColor by animateColorAsState(if (isHovered) Accent.copy(alpha = 0.75f) else Accent)

    Tooltip(
        text = {
            Text(
                "Opening the original screen may show options that OneConfig doesn't.",
                color = theme.textColor,
                fontSize = 13.sp,
            )
        },
        modifier = Modifier.widthIn(max = 260.dp),
        anchor = Alignment.BottomCenter,
    ) {
        Box(
            modifier = Modifier
                .pointerHoverIcon(PointerIcon.Hand)
                .background(bgColor, theme.buttonShape)
                .onClick(interactionSource) { runnable.run() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Open original screen", color = theme.accentTextColor, fontSize = 13.sp)
        }
    }
}

@Composable
private fun TitleAuthorByline(title: String) {
    if (ShellState.titleInfoForTitle != title) return
    val byline = ShellState.titleAuthors?.authorByline() ?: return
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
        "by $first and $remaining more..."
    } else {
        "by $first"
    }
}

@Composable
private fun TitleInfoTooltip(title: String) {
    if (ShellState.titleInfoForTitle != title) return
    val version = ShellState.titleVersion
    val authors = ShellState.titleAuthors
    val credits = ShellState.titleCredits
    if (version == null && authors == null && credits == null) return
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val density = LocalDensity.current

    Box(
        modifier = Modifier.size(22.dp).hoverable(interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon("info", color = LocalTheme.current.textColorSecondary, modifier = Modifier.size(14.dp))

        if (isHovered) {
            // popup so the tooltip takes no layout space in the header Row and is not clipped by its
            // fixed height
            Popup(
                popupPositionProvider = DropdownPositionProvider(DpOffset(0.dp, 6.dp), density),
                properties = PopupProperties(focusable = false)
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
                        Text("Author: $authors", color = LocalTheme.current.textColor, fontSize = 12.sp)
                    }
                    if (credits != null) {
                        Text("Credits: $credits", color = LocalTheme.current.textColor, fontSize = 12.sp)
                    }
                    if (version != null) {
                        Text("Version: $version", color = LocalTheme.current.textColor, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalSearchBar() {
    val navController = LocalNavController.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val placeholder = searchPlaceholder(backStackEntry?.destination)

    var searchText by remember { mutableStateOf(ShellState.searchQuery) }
    val interactionSource = rememberInteractionSource()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(searchText) { ShellState.searchQuery = searchText }
    LaunchedEffect(ShellState.searchQuery) { if (ShellState.searchQuery != searchText) searchText = ShellState.searchQuery }
    LaunchedEffect(isFocused) { ShellState.searchFieldFocused = isFocused }

    LaunchedEffect(ShellState.focusSearchField) {
        if (ShellState.focusSearchField) {
            withFrameNanos { }
            try {
                focusRequester.requestFocus()
            } catch (_: IllegalStateException) {
            }
            ShellState.focusSearchField = false
        }
    }

    val searchTextStyle = TextStyle(
        color = LocalTheme.current.textColorSecondary,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        fontFamily = LocalTheme.current.typography.family,
        lineHeight = 12.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both,
        ),
    )

    BasicTextField(
        value = searchText,
        onValueChange = { searchText = it },
        singleLine = true,
        cursorBrush = SolidColor(LocalTheme.current.textColorSecondary),
        textStyle = searchTextStyle,
        interactionSource = interactionSource,
        modifier = Modifier.trackTextInputFocus().focusRequester(focusRequester)
    ) { innerTextField ->
        Box(
            modifier = Modifier
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
                    if (searchText.isEmpty() && !isFocused) {
                        Text(
                            placeholder,
                            color = LocalTheme.current.textColorSecondary,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 12.sp,
                        )
                    }
                    innerTextField()
                }
                if (searchText.isNotEmpty()) {
                    IconButton("close", modifier = Modifier.size(16.dp)) {
                        searchText = ""
                        ShellState.globalSearchActive = false
                    }
                }
            }
        }
    }
}
