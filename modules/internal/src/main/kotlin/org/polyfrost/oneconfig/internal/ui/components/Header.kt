package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.IconButton
import org.polyfrost.oneconfig.api.ui.v1.components.IconButtonStyle
import org.polyfrost.oneconfig.api.ui.v1.components.SearchField
import org.polyfrost.oneconfig.api.ui.v1.components.TextField
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipBox
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipPlacement
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.api.ui.v1.keybind.trackTextInputFocus
import org.polyfrost.oneconfig.internal.ui.LocalCloseRequest
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.navigation.currentPageTitle
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsRoute
import org.polyfrost.oneconfig.internal.ui.navigation.searchPlaceholder
import org.polyfrost.oneconfig.internal.ui.screens.ModSortControl
import org.polyfrost.oneconfig.internal.ui.search.submenuTrail
import org.polyfrost.oneconfig.internal.ui.shell.LocalShell
import org.polyfrost.oneconfig.internal.ui.shell.LocalShellNavController
import org.polyfrost.oneconfig.internal.ui.shell.SearchFocus
import org.polyfrost.oneconfig.internal.ui.shell.ShellNavigation
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.api.ui.v1.components.Text as KitText

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
                IconButton(
                    "left-arrow",
                    iconButtonLabel("left-arrow"),
                    { controller.back() },
                    Modifier.size(32.dp),
                    enabled = canGoBack,
                    style = IconButtonStyle.Ghost,
                    tint = LocalTheme.current.textColor,
                )
                IconButton(
                    "right-arrow",
                    iconButtonLabel("right-arrow"),
                    { controller.forward() },
                    Modifier.size(32.dp),
                    enabled = canGoForward,
                    style = IconButtonStyle.Ghost,
                    tint = LocalTheme.current.textColor,
                )
            }
            Spacer(Modifier.size(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(
                    title,
                    transitionSpec = {
                        if (Motion.reduced) fadeIn() togetherWith fadeOut() using null
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
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PageFilterBar()
            if (backStackEntry?.destination?.hasRoute(ModsRoute::class) == true) ModSortControl()
            val entryId = backStackEntry?.id
            val pageMenu = HeaderMenus.entriesFor(entryId)
            val modMenu = title?.configId?.takeIf { hasModHeaderMenu(it, openOriginalScreen) }
                ?.let { id -> { modHeaderMenu(id, openOriginalScreen) } }
            (pageMenu ?: modMenu)?.let { HeaderMoreButton(it) }
            IconButton("close", iconButtonLabel("close"), { closeRequest() }, Modifier.padding(start = 8.dp).size(32.dp))
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
    val configId: String? = null,
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
            configId = route.id,
        )
    }

    return entry.currentPageTitle()?.let { HeaderTitle(listOf(TitleSegment(it))) }
}

@Composable
private fun TitleBreadcrumb(segments: List<TitleSegment>, controller: ShellNavigation) {
    val theme = LocalTheme.current
    val style = theme.typography.display.copy(fontWeight = FontWeight.Medium)
    Row(verticalAlignment = Alignment.CenterVertically) {
        segments.forEachIndexed { index, segment ->
            val route = segment.route.takeIf { index < segments.lastIndex }
            if (index > 0) {
                KitText("/", Modifier.padding(horizontal = 6.dp), theme.textColorSecondary, style)
            }
            if (route == null) {
                KitText(segment.text.asRenderText(), color = theme.textColor, style = style, legacyFormatting = true)
            } else {
                val interactionSource = rememberInteractionSource()
                val isHovered by interactionSource.collectIsHoveredAsState()
                val color by Motion.animateColorAsState(if (isHovered) theme.textColor else theme.textColorSecondary)
                KitText(
                    segment.text.asRenderText(),
                    Modifier.oneConfigClickable(
                        segment.text.asRenderText(),
                        { controller.navigate(route) },
                        role = A11yRole.Link,
                        indication = false,
                        interactionSource = interactionSource,
                    ),
                    color = color,
                    style = style,
                    legacyFormatting = true,
                )
            }
        }
    }
}

@Composable
private fun TitleAuthorByline(authors: String?) {
    val byline = authors?.authorByline() ?: return
    val theme = LocalTheme.current
    KitText(byline, color = theme.textColorSecondary, style = theme.typography.bodyMedium)
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
                    KitText(translated("oneconfig.header.author", "Author: %s", authors), color = theme.textColor, style = theme.typography.small)
                }
                if (credits != null) {
                    KitText(translated("oneconfig.header.credits", "Credits: %s", credits), color = theme.textColor, style = theme.typography.small)
                }
                if (version != null) {
                    KitText(translated("oneconfig.header.version", "Version: %s", version), color = theme.textColor, style = theme.typography.small)
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
fun GlobalSearchBar(
    /** Replaces the page's own placeholder and stays shown while the empty field is focused */
    placeholderText: String? = null,
    /** Replaces the magnifying glass, and with it the bar fills its [modifier] at [fontSize] */
    leading: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    size: ControlSize = ControlSize.ExtraLarge,
) {
    val navController = LocalShellNavController.current
    val search = LocalShell.current.search
    val backStackEntry by navController.currentBackStackEntryAsState()
    val placeholder by remember(backStackEntry) { derivedStateOf { searchPlaceholder(backStackEntry) } }

    var searchText by remember { mutableStateOf(TextFieldValue(search.query, TextRange(search.query.length))) }
    val interactionSource = rememberInteractionSource()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val focusRequester = remember { FocusRequester() }
    val searchFocused = LocalSearchFieldFocused.current

    LaunchedEffect(searchText.text) { search.update(searchText.text) }
    LaunchedEffect(search.query) {
        if (search.query != searchText.text) searchText = TextFieldValue(search.query, TextRange(search.query.length))
    }
    LaunchedEffect(isFocused) { search.updateFieldFocused(isFocused) }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(search.focusRequest) {
        val request = search.focusRequest ?: return@LaunchedEffect
        withFrameNanos { }
        if (request == SearchFocus.Unfocus) {
            if (isFocused) focusManager.clearFocus()
        } else {
            if (request == SearchFocus.SelectAll) {
                searchText = searchText.copy(selection = TextRange(0, searchText.text.length))
            }
            if (!isFocused) {
                try {
                    focusRequester.requestFocus()
                } catch (_: IllegalStateException) {
                }
            }
        }
        search.consumeFocusRequest()
    }

    val fieldModifier = Modifier.trackTextInputFocus().onFocusChanged { searchFocused?.value = it.hasFocus }
    if (leading != null) {
        // the kit's search field has no slot for its icon, so the minimal menu's bar is a text field with its own clear button
        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = modifier.then(fieldModifier),
            placeholder = placeholderText ?: placeholder,
            size = size,
            leading = leading,
            trailing = if (searchText.text.isEmpty()) null else ({
                IconButton(
                    "close",
                    translated("oneconfig.ui.search.clear", "Clear search"),
                    { searchText = TextFieldValue() },
                    Modifier.size(22.dp),
                    size = ControlSize.Compact,
                    tooltip = null,
                )
            }),
            fontSize = fontSize,
            focusRequester = focusRequester,
            interactionSource = interactionSource,
        )
        return
    }
    SearchField(
        value = searchText,
        onValueChange = { searchText = it },
        modifier = Modifier.width(256.dp).then(fieldModifier),
        placeholder = placeholderText ?: placeholder,
        size = ControlSize.Regular,
        focusRequester = focusRequester,
        interactionSource = interactionSource,
        hidePlaceholderWhenFocused = placeholderText == null,
    )
}
