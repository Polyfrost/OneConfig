package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus
import org.polyfrost.oneconfig.internal.ui.LocalCloseRequest
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.Tooltip
import org.polyfrost.oneconfig.internal.ui.components.dropdown.DropdownPositionProvider
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ChangeLogEntryRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ChangeLogRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.CreditsRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.KeybindsRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.PreferencesRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ProfilesRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ThemesRoute
import org.polyfrost.oneconfig.internal.ui.navigation.searchPlaceholder
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.shell.SearchFocus
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Header(modifier: Modifier = Modifier) {
    val controller = LocalNavController.wrapper
    val closeRequest = LocalCloseRequest.current
    val backStackEntry by LocalNavController.current.currentBackStackEntryAsState()
    val title = remember(backStackEntry, ConfigRegistry.revision) { backStackEntry.headerTitle() }
    val openOriginalScreen = title?.openOriginalScreen
    // historyRevision reads snapshot state so this recomposes and re-queries the stacks whenever the
    // (plain ArrayDeque) history changes; the stacks stay the single source of truth.
    controller.historyRevision
    val canGoBack = controller.canGoBack()
    val canGoForward = controller.canGoForward()

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
                IconButton("left-arrow", Modifier.size(32.dp), enabled = canGoBack, hoverBorder = true) { controller.back() }
                IconButton("right-arrow", Modifier.size(32.dp), enabled = canGoForward, hoverBorder = true) { controller.forward() }
            }
            Spacer(Modifier.size(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(
                    title,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    }
                ) { headerTitle ->
                    headerTitle?.let { info ->
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
                }
                // Separate animation keeps the button composed exactly once per frame while still giving
                // it the same fade as the title. Sharing the title's AnimatedContent duplicated the
                // button per transition target, and the outgoing copy had no byline or tooltip (both
                // gated on the current title) so it briefly showed against the bare title.
                //
                // AnimatedVisibility rather than AnimatedContent: the header Row vertically centers its
                // children, so animating AnimatedContent's measured size re-centers it and drags the
                // button around. AnimatedVisibility only fades and keeps the content's natural size for
                // the whole transition, so the Row never has to re-center it.
                var lastAction by remember { mutableStateOf<Runnable?>(null) }
                LaunchedEffect(openOriginalScreen) {
                    if (openOriginalScreen != null) lastAction = openOriginalScreen
                }
                AnimatedVisibility(
                    visible = openOriginalScreen != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    // one Row so the spacer and the button share a single measured node and keep their
                    // horizontal order
                    // prefer the live action so switching between two mods never shows the previous
                    // mod's runnable for the frame before the effect updates lastAction
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        (openOriginalScreen ?: lastAction)?.let {
                            Spacer(Modifier.size(8.dp))
                            OpenOriginalScreenButton(it)
                        }
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            GlobalSearchBar()
            IconButton("close", Modifier.size(32.dp), hoverBorder = true) { closeRequest() }
        }
    }
}

/**
 * The header title and its extra info, resolved purely from the entry currently on top of the back
 * stack.
 *
 * Deliberately derived from the live back-stack entry instead of screen-owned `ShellState` fields: a
 * screen-scoped `DisposableEffect` only clears on dispose, which NavHost delays until the outgoing
 * screen's exit transition finishes, so the outgoing screen's `onDispose` wiped the incoming screen's
 * title info mid-transition (the byline vanished and the "Open original screen" button outlived its
 * page). Deriving it here makes the title a pure function of the current route, so each transition
 * target renders its own content.
 */
private data class HeaderTitle(
    val text: String,
    val authors: String? = null,
    val credits: String? = null,
    val version: String? = null,
    val openOriginalScreen: Runnable? = null,
)

private fun NavBackStackEntry?.headerTitle(): HeaderTitle? {
    val entry = this ?: return null
    val destination = entry.destination

    if (destination.hasRoute(ModConfigRoute::class)) {
        val id = runCatching { entry.toRoute<ModConfigRoute>().id }.getOrNull() ?: return null
        val config = ConfigRegistry.findById(id) ?: return null
        val tree = ConfigRegistry.findTree(id)
        return HeaderTitle(
            text = config.title.asRenderText(),
            authors = config.authors,
            credits = config.credits,
            version = config.version,
            openOriginalScreen = tree?.getMetadata<Runnable>("open_original_screen"),
        )
    }

    return when {
        destination.hierarchy.any { it.hasRoute(ModsRoute::class) } -> HeaderTitle("Mods")
        destination.hierarchy.any { it.hasRoute(ProfilesRoute::class) } -> HeaderTitle("Profiles")
        destination.hierarchy.any { it.hasRoute(KeybindsRoute::class) } -> HeaderTitle("Keybinds")
        destination.hierarchy.any { it.hasRoute(ThemesRoute::class) } -> HeaderTitle("Themes")
        destination.hierarchy.any { it.hasRoute(PreferencesRoute::class) } -> HeaderTitle("Preferences")
        destination.hierarchy.any { it.hasRoute(ChangeLogRoute::class) || it.hasRoute(ChangeLogEntryRoute::class) } ->
            HeaderTitle("Changelog")
        destination.hierarchy.any { it.hasRoute(CreditsRoute::class) } -> HeaderTitle("Credits")
        else -> null
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
                .border(1.dp, LocalTheme.current.borderColor, LocalTheme.current.buttonShape)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon("external-link", color = theme.accentTextColor, modifier = Modifier.size(14.dp))
                Text("Original menu", color = theme.accentTextColor, fontSize = 13.sp)
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
        "by $first and $remaining more..."
    } else {
        "by $first"
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

private fun searchFieldValue(text: String) = TextFieldValue(text, TextRange(text.length))

@Composable
fun GlobalSearchBar() {
    val navController = LocalNavController.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val placeholder = searchPlaceholder(backStackEntry?.destination)

    var searchText by remember { mutableStateOf(searchFieldValue(ShellState.searchQuery)) }
    val interactionSource = rememberInteractionSource()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(searchText.text) { ShellState.searchQuery = searchText.text }
    LaunchedEffect(ShellState.searchQuery) {
        if (ShellState.searchQuery != searchText.text) searchText = searchFieldValue(ShellState.searchQuery)
    }
    LaunchedEffect(isFocused) { ShellState.searchFieldFocused = isFocused }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(ShellState.searchFocus) {
        val request = ShellState.searchFocus ?: return@LaunchedEffect
        withFrameNanos { }
        if (request == SearchFocus.Unfocus) {
            if (isFocused) focusManager.clearFocus()
        } else {
            if (request == SearchFocus.SelectAll) {
                // a kept query is selected so typing starts a new search while leaving it alone keeps it
                searchText = searchText.copy(selection = TextRange(0, searchText.text.length))
            }
            if (!isFocused) {
                try {
                    focusRequester.requestFocus()
                } catch (_: IllegalStateException) {
                }
            }
        }
        ShellState.searchFocus = null
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
                    if (searchText.text.isEmpty() && !isFocused) {
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
                if (searchText.text.isNotEmpty()) {
                    IconButton("close", modifier = Modifier.size(16.dp)) {
                        searchText = TextFieldValue()
                    }
                }
            }
        }
    }
}
