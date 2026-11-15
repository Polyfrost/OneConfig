package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.AnchoredPopupButton
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.DropdownMenu
import org.polyfrost.oneconfig.api.ui.v1.components.IconButton
import org.polyfrost.oneconfig.api.ui.v1.components.MenuAction
import org.polyfrost.oneconfig.api.ui.v1.components.NavItem
import org.polyfrost.oneconfig.api.ui.v1.components.NotificationsCenterPanel
import org.polyfrost.oneconfig.api.ui.v1.components.Placement
import org.polyfrost.oneconfig.api.ui.v1.components.PlayerHead
import org.polyfrost.oneconfig.api.ui.v1.components.ScrollContainer
import org.polyfrost.oneconfig.api.ui.v1.components.SectionHeader
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.components.SectionHeaderDefaults
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipPlacement
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.internal.ui.navigation.inAboutMenu
import org.polyfrost.oneconfig.internal.ui.navigation.isOn
import org.polyfrost.oneconfig.internal.ui.navigation.pageRoute
import org.polyfrost.oneconfig.internal.ui.navigation.registeredPages
import org.polyfrost.oneconfig.internal.ui.navigation.sidebarSections
import org.polyfrost.oneconfig.internal.ui.shell.LocalShell
import org.polyfrost.oneconfig.internal.ui.shell.LocalShellNavController
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.DEFAULT_SIDEBAR_OPACITY
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.surfaceOpacity

internal val SidebarWidth = 264.dp

private object SidebarCollapse {
    private val collapsed = mutableStateMapOf<String, Boolean>()

    fun isCollapsed(id: String): Boolean = collapsed[id] == true

    fun toggle(id: String) {
        collapsed[id] = !isCollapsed(id)
    }
}

@Composable
fun Sidebar(background: Boolean = true) {
    val appearance = LocalShell.current.appearance
    Column(
        modifier = Modifier.fillMaxHeight()
            .width(SidebarWidth)
            .then(if (background) Modifier.background(LocalTheme.current.surfaceOpacity(LocalTheme.current.sidebarBackground, appearance.sidebarOpacity, DEFAULT_SIDEBAR_OPACITY)).rightBorder(LocalTheme.current.borderColor, 1f) else Modifier),
    ) {
        Navigation(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TopOptionsSection()
            }
            Account()
        }
    }
}

@Composable
private fun Navigation(modifier: Modifier) {
    val scroll = rememberScrollState()
    val overflow = remember { NavOverflow() }
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(top = 28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Branding()
        Column(Modifier.fillMaxWidth().weight(1f, fill = false)) {
            ScrollContainer(
                scroll,
                Modifier.fillMaxWidth().weight(1f, fill = false)
                    .onGloballyPositioned { overflow.viewportBottom = it.positionInRoot().y + it.size.height },
            ) {
                CompositionLocalProvider(LocalNavOverflow provides overflow) {
                    Box(Modifier.padding(start = (SidebarWidth - SidebarEntryWidth) / 2, top = 4.dp, bottom = 4.dp)) { NavigationEntries() }
                }
            }
            Box(Modifier.fillMaxWidth().height(EditHudGap)) {
                val hidden = if (scroll.canScrollForward) overflow.hidden() else 0
                if (hidden > 0) {
                    val source = rememberInteractionSource()
                    val hovered by source.collectIsHoveredAsState()
                    val label = translated("oneconfig.sidebar.more", "+%s more", hidden)
                    Text(
                        label,
                        modifier = Modifier.padding(start = 26.dp, top = 6.dp)
                            .oneConfigClickable(
                                label,
                                {
                                    scope.launch {
                                        if (Motion.reduced) scroll.scrollTo(scroll.maxValue) else scroll.animateScrollTo(scroll.maxValue)
                                    }
                                },
                                indication = false,
                                interactionSource = source,
                            )
                            .pointerHoverIcon(PointerIcon.Hand),
                        color = LocalTheme.current.textColorSecondary.copy(alpha = if (hovered) 1f else 0.7f),
                        fontSize = LocalTheme.current.typography.caption.fontSize,
                    )
                }
            }
        }
    }
}

private val EditHudGap = 74.dp

private val SidebarEntryWidth = 225.dp

private class NavOverflow {
    val centers = mutableStateMapOf<Any, Float>()
    var viewportBottom by mutableFloatStateOf(0f)

    fun hidden(): Int = centers.values.count { it > viewportBottom }
}

private val LocalNavOverflow = staticCompositionLocalOf<NavOverflow?> { null }

@Composable
private fun TrackOverflow(key: Any, enabled: Boolean = true, content: @Composable () -> Unit) {
    val overflow = LocalNavOverflow.current
    if (overflow == null) {
        content()
        return
    }
    DisposableEffect(key, enabled) { onDispose { overflow.centers.remove(key) } }
    val track = if (enabled) Modifier.onGloballyPositioned { overflow.centers[key] = it.positionInRoot().y + it.size.height / 2f } else Modifier
    Box(track) { content() }
}

@Composable
private fun Branding() {
    val logoPath = LocalTheme.current.logoPath
    val logo = rememberSvgResourcePainter(logoPath) ?: return
    Image(
        painter = logo,
        contentDescription = null,
        colorFilter = rememberAccentBrandTint(logoPath),
        modifier = Modifier.size(167.dp, 19.dp).accentGradientFill()
    )
}

@Composable
private fun TopOptionsSection() {
    NavigationSection {
        NavigationEntry(
            "hud",
            translated("oneconfig.sidebar.edit_hud", "Edit HUD"),
        ) {
            HudManager.openEditor()
        }
    }
}

@Composable
private fun NavigationEntries() {
    val controller = LocalShellNavController.current
    val shell = LocalShell.current

    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        sidebarSections().forEach {
            val entry by controller.currentBackStackEntryAsState()
            val id = it.id

            NavigationSection(it.title, collapsed = SidebarCollapse.isCollapsed(id), onToggle = { SidebarCollapse.toggle(id) }, trackKey = "section:$id") {
                it.entries.forEach { def ->
                    val selected = entry.isOn(def.route)

                    TrackOverflow(def.route) {
                        NavigationEntry(
                            def.icon,
                            def.label,
                            selected
                        ) {
                            val nav = shell.navigation
                            if (nav.currentRoute == def.route) shell.search.clear() else nav.navigate(def.route)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationSection(title: String? = null, collapsed: Boolean? = null, onToggle: () -> Unit = {}, trackKey: Any? = null, content: @Composable () -> Unit) {
    if (collapsed != null && title != null) {
        CollapsibleSection(title, collapsed, onToggle, trackKey ?: title, content)
        return
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        title?.let {
            SectionHeader(
                it,
                Modifier.width(SidebarEntryWidth),
                contentPadding = SectionHeaderPadding,
                textStyle = SectionHeaderDefaults.labelStyle,
                color = OneConfigTheme.current.textColor,
            )
        }
        content()
    }
}

private val SectionHeaderPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = 2.dp)

@Composable
private fun CollapsibleSection(title: String, collapsed: Boolean, onToggle: () -> Unit, trackKey: Any, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.Start) {
        TrackOverflow(trackKey, enabled = collapsed) {
            SectionHeader(
                title,
                Modifier.width(SidebarEntryWidth),
                expanded = !collapsed,
                onToggle = onToggle,
                contentPadding = SectionHeaderPadding,
                textStyle = SectionHeaderDefaults.labelStyle,
                color = OneConfigTheme.current.textColor.copy(0.8f),
            )
        }
        AnimatedVisibility(
            visible = !collapsed,
            enter = Motion.enter(expandVertically()),
            exit = Motion.exit(shrinkVertically()),
        ) {
            Column(
                Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                content()
            }
        }
    }
}

@Composable
private fun NavigationEntry(icon: String, title: String, isSelected: Boolean = false, onSelect: () -> Unit) {
    NavItem(isSelected, title, onSelect, Modifier.width(SidebarEntryWidth), icon = icon)
}

@Composable
private fun NotificationBell() {
    Box {
        AnchoredPopupButton(
            "bell",
            translated("oneconfig.sidebar.notifications", "Notifications"),
            placement = Placement.AboveStart,
            gap = PopupGap - BellOutset,
            size = ControlSize.Large,
        ) {
            DisposableEffect(Unit) { onDispose { NotificationsManager.markAllRead() } }
            NotificationsCenterPanel(Modifier.shellTransition())
        }
        if (NotificationsManager.unreadCount > 0) {
            Box(Modifier.align(Alignment.Center).size(BellGlyph)) {
                UnreadDot(
                    Modifier.align(Alignment.TopEnd).offset(2.dp, (-2).dp),
                    translated("oneconfig.sidebar.notifications.unread", "Unread notifications"),
                )
            }
        }
    }
}

@Composable
private fun UnreadDot(modifier: Modifier, description: String) {
    val circle = LocalTheme.current.shapes.circle
    Box(modifier.semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(8.dp).background(Accent, circle).blur(7.dp))
        Box(Modifier.size(6.dp).background(Accent, circle))
    }
}

@Composable
private fun Account() {
    Box(
        modifier = Modifier.fillMaxWidth().height(73.dp)
            .topBorder(LocalTheme.current.borderColor, 1f),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerHead(32.dp, Modifier.offset(x = 2.dp), shape = LocalTheme.current.sideBarNavigationEntryShape)
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val session = LocalShell.current.session
                    Text(session.playerName, color = LocalTheme.current.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        session.versionLabel.ifBlank { "OneConfig" },
                        color = LocalTheme.current.textColorSecondary,
                        fontSize = LocalTheme.current.typography.caption.fontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(start = 12.dp).offset(x = BellOutset),
                horizontalArrangement = Arrangement.spacedBy(FooterButtonGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AboutButton()
                NotificationBell()
            }
        }
    }
}

@Composable
private fun AboutButton() {
    var expanded by remember { mutableStateOf(false) }
    val controller = LocalShellNavController.current
    val shell = LocalShell.current
    val entry by controller.currentBackStackEntryAsState()
    val pages = registeredPages().filter { it.inAboutMenu && it.showInSidebar }
    val label = translated("oneconfig.sidebar.about", "Changelog and credits")

    Box {
        IconButton(
            "info",
            label,
            { expanded = !expanded },
            size = ControlSize.Large,
            selected = expanded,
            tooltip = if (expanded) null else label,
            tooltipPlacement = TooltipPlacement.AboveCenter,
        )
        DropdownMenu(
            expanded = expanded,
            onDismiss = { expanded = false },
            entries = pages.map { page ->
                val route = pageRoute(page)
                MenuAction(
                    page.title.resolve(),
                    {
                        expanded = false
                        val nav = shell.navigation
                        if (nav.currentRoute == route) shell.search.clear() else nav.navigate(route)
                    },
                    icon = page.icon,
                    checked = entry.isOn(route),
                )
            },
            placement = Placement.AboveStart,
        )
    }
}

private val PopupGap = 12.dp

private val FooterButtonGap = 4.dp

private val BellGlyph = 18.dp

private val BellOutset = (ControlSize.Large.height - BellGlyph) / 2
