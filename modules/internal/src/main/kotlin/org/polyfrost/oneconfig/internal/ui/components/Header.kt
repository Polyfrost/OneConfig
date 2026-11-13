package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.ControlStyle
import org.polyfrost.oneconfig.api.ui.v1.components.SearchField
import org.polyfrost.oneconfig.api.ui.v1.components.TextButton
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipBox
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipPlacement
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus
import org.polyfrost.oneconfig.internal.ui.LocalCloseRequest
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.navigation.currentPageTitle
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.navigation.searchPlaceholder
import org.polyfrost.oneconfig.internal.ui.search.submenuTrail
import org.polyfrost.oneconfig.internal.ui.shell.LocalShell
import org.polyfrost.oneconfig.internal.ui.shell.LocalShellNavController
import org.polyfrost.oneconfig.internal.ui.shell.SearchFocus
import org.polyfrost.oneconfig.internal.ui.shell.ShellNavigation
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun Header(modifier: Modifier = Modifier) {
    val controller = LocalShell.current.navigation
    val closeRequest = LocalCloseRequest.current
    val backStackEntry by LocalShellNavController.current.currentBackStackEntryAsState()
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
                        if (ReducedMotion.enabled) fadeIn() togetherWith fadeOut() using null
                        else fadeIn() togetherWith fadeOut()
                    }
                ) { headerTitle ->
                    headerTitle?.let { info ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TitleBreadcrumb(info.segments, controller)
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
 */
private data class TitleSegment(val text: Any, val route: ModConfigRoute? = null)

private data class HeaderTitle(
    val segments: List<TitleSegment>,
    val authors: String? = null,
    val credits: String? = null,
    val version: String? = null,
    val openOriginalScreen: Runnable? = null,
)

private fun NavBackStackEntry?.headerTitle(): HeaderTitle? {
    val entry = this ?: return null
    val destination = entry.destination

    if (destination.hasRoute(ModConfigRoute::class)) {
        val route = runCatching { entry.toRoute<ModConfigRoute>() }.getOrNull() ?: return null
        val config = ConfigRegistry.findById(route.id) ?: return null
        val tree = ConfigRegistry.findTree(route.id)
        val ids = route.submenu?.split('/').orEmpty()
        val trail = route.submenu?.let { tree?.submenuTrail(it) }.orEmpty()
        return HeaderTitle(
            segments = listOf(TitleSegment(config.title.asRenderText(), ModConfigRoute(route.id))) +
                trail.mapIndexed { index, tree ->
                    TitleSegment(tree.localizedTitle().asRenderText(), ModConfigRoute(route.id, submenu = ids.take(index + 1).joinToString("/")))
                },
            authors = config.authors,
            credits = config.credits,
            version = config.version,
            openOriginalScreen = tree?.getMetadata<Runnable>("open_original_screen"),
        )
    }

    return entry.currentPageTitle()?.let { HeaderTitle(listOf(TitleSegment(it))) }
}

@Composable
private fun TitleBreadcrumb(segments: List<TitleSegment>, controller: ShellNavigation) {
    val theme = LocalTheme.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        segments.forEachIndexed { index, segment ->
            val route = segment.route.takeIf { index < segments.lastIndex }
            if (index > 0) {
                Text("/", Modifier.padding(horizontal = 6.dp), color = theme.textColorSecondary, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            }
            if (route == null) {
                Text(segment.text, color = theme.textColor, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            } else {
                val interactionSource = rememberInteractionSource()
                val isHovered by interactionSource.collectIsHoveredAsState()
                val color by Motion.animateColorAsState(if (isHovered) theme.textColor else theme.textColorSecondary)
                Text(
                    segment.text,
                    Modifier.oneConfigClickable(
                        segment.text.asRenderText(),
                        { controller.navigate(route) },
                        role = A11yRole.Link,
                        indication = false,
                        interactionSource = interactionSource,
                    ),
                    color = color,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun OpenOriginalScreenButton(runnable: Runnable) {
    TooltipBox(
        translated("oneconfig.header.original_menu.tooltip", "Opening the original screen may show options that OneConfig doesn't."),
        placement = TooltipPlacement.BelowCenter,
    ) {
        TextButton(
            translated("oneconfig.header.original_menu", "Original menu"),
            "external-link",
            { runnable.run() },
            style = ControlStyle.Primary,
            size = ControlSize.Compact,
        )
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
    val theme = LocalTheme.current

    TooltipBox(
        tooltip = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (authors != null) {
                    Text(translated("oneconfig.header.author", "Author: %s", authors), color = theme.textColor, fontSize = 12.sp)
                }
                if (credits != null) {
                    Text(translated("oneconfig.header.credits", "Credits: %s", credits), color = theme.textColor, fontSize = 12.sp)
                }
                if (version != null) {
                    Text(translated("oneconfig.header.version", "Version: %s", version), color = theme.textColor, fontSize = 12.sp)
                }
            }
        },
        placement = TooltipPlacement.BelowStart,
    ) {
        Box(
            modifier = Modifier.size(22.dp).focusable(),
            contentAlignment = Alignment.Center,
        ) {
            Icon("info", color = theme.textColorSecondary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun GlobalSearchBar() {
    val navController = LocalShellNavController.current
    val search = LocalShell.current.search
    val backStackEntry by navController.currentBackStackEntryAsState()
    val placeholder by remember(backStackEntry) { derivedStateOf { searchPlaceholder(backStackEntry) } }

    var searchText by remember { mutableStateOf(search.query) }
    val interactionSource = rememberInteractionSource()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val focusRequester = remember { FocusRequester() }
    val searchFocused = LocalSearchFieldFocused.current

    LaunchedEffect(searchText) { search.update(searchText) }
    LaunchedEffect(search.query) {
        if (search.query != searchText) searchText = search.query
    }
    LaunchedEffect(isFocused) { search.updateFieldFocused(isFocused) }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(search.focusRequest) {
        val request = search.focusRequest ?: return@LaunchedEffect
        withFrameNanos { }
        if (request == SearchFocus.Unfocus) {
            if (isFocused) focusManager.clearFocus()
        } else if (!isFocused) {
            try {
                focusRequester.requestFocus()
            } catch (_: IllegalStateException) {
            }
        }
        search.consumeFocusRequest()
    }

    SearchField(
        query = searchText,
        onQueryChange = { searchText = it },
        modifier = Modifier.width(256.dp)
            .trackTextInputFocus()
            .onFocusChanged { searchFocused?.value = it.hasFocus },
        placeholder = placeholder,
        size = ControlSize.Regular,
        focusRequester = focusRequester,
        interactionSource = interactionSource,
    )
}
