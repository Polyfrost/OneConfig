package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
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
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.DropdownMenu
import org.polyfrost.oneconfig.api.ui.v1.components.EscapeCloses
import org.polyfrost.oneconfig.api.ui.v1.components.MenuAction
import org.polyfrost.oneconfig.api.ui.v1.components.NotificationsCenterPanel
import org.polyfrost.oneconfig.api.ui.v1.components.Placement
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipBox
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipPlacement
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.api.ui.v1.components.rememberAnchoredPosition
import org.polyfrost.oneconfig.api.ui.v1.components.rememberHoverState
import org.polyfrost.oneconfig.api.ui.v1.components.smoothWheelScroll
import org.polyfrost.oneconfig.internal.ui.navigation.inAboutMenu
import org.polyfrost.oneconfig.internal.ui.navigation.isOn
import org.polyfrost.oneconfig.internal.ui.navigation.pageRoute
import org.polyfrost.oneconfig.internal.ui.navigation.registeredPages
import org.polyfrost.oneconfig.internal.ui.navigation.sidebarSections
import org.polyfrost.oneconfig.internal.ui.shell.LocalShell
import org.polyfrost.oneconfig.internal.ui.shell.LocalShellNavController
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
            Box(Modifier.fillMaxWidth().weight(1f, fill = false)) {
                Box(
                    Modifier.fillMaxWidth()
                        .onGloballyPositioned { overflow.viewportBottom = it.positionInRoot().y + it.size.height }
                        .smoothWheelScroll(scroll)
                        .verticalScroll(scroll),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    CompositionLocalProvider(LocalNavOverflow provides overflow) {
                        Box(Modifier.padding(vertical = 4.dp)) { NavigationEntries() }
                    }
                }
                val thickness = LocalScrollbarStyle.current.thickness
                Box(Modifier.matchParentSize()) {
                    VerticalScrollbar(
                        rememberScrollbarAdapter(scroll),
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                            .padding(end = (((SidebarWidth - SidebarEntryWidth) / 2 - thickness) / 2).coerceAtLeast(0.dp)),
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(EditHudGap)) {
                val hidden = if (scroll.canScrollForward) overflow.hidden() else 0
                if (hidden > 0) {
                    val source = rememberInteractionSource()
                    val hovered by source.collectIsHoveredAsState()
                    Text(
                        translated("oneconfig.sidebar.more", "+%s more", hidden),
                        modifier = Modifier.padding(start = 26.dp, top = 6.dp)
                            .onClick(source) {
                                scope.launch {
                                    if (Motion.reduced) scroll.scrollTo(scroll.maxValue) else scroll.animateScrollTo(scroll.maxValue)
                                }
                            }
                            .pointerHoverIcon(PointerIcon.Hand),
                        color = LocalTheme.current.textColorSecondary.copy(alpha = if (hovered) 1f else 0.7f),
                        fontSize = 11.sp,
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
                            def.route,
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
            Text(
                title.uppercase(),
                modifier = Modifier.padding(start = 6.dp, bottom = 2.dp),
                color = LocalTheme.current.textColor,
                fontWeight = FontWeight.Medium, fontSize = 11.sp
            )
        }
        content()
    }
}

@Composable
private fun CollapsibleSection(title: String, collapsed: Boolean, onToggle: () -> Unit, trackKey: Any, content: @Composable () -> Unit) {
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    val theme = LocalTheme.current
    Column(horizontalAlignment = Alignment.Start) {
        TrackOverflow(trackKey, enabled = collapsed) {
            CollapsibleHeader(
                label = title,
                expanded = !collapsed,
                onToggle = onToggle,
                modifier = Modifier.width(SidebarEntryWidth),
                contentPadding = PaddingValues(start = 6.dp, end = 6.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                interactionSource = source,
            ) {
                Text(
                    title.uppercase(),
                    modifier = Modifier.weight(1f),
                    color = if (hovered) theme.textColor else theme.textColor.copy(0.8f),
                    fontWeight = FontWeight.Medium, fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
private fun NavigationEntry(icon: String, title: String, route: Any? = null, isSelected: Boolean = false, onSelect: (Any?) -> Unit) {
    val interactionSource = rememberInteractionSource()

    val isHovered by interactionSource.collectIsHoveredAsState()

    val theme = LocalTheme.current
    val translucent = theme.translucentSelection
    val selectedAlpha = Motion.animateFloatAsState(if (isSelected) 1f else 0f)
    val textAndIconColor = Motion.animateColorAsState(
        if (isSelected && translucent) theme.textColor
        else if (isSelected) theme.accentTextColor
        else if (isHovered) theme.textColor
        else theme.textColor.copy(0.8f)
    )
    val borderColor = Motion.animateColorAsState(
        if (isSelected && translucent) theme.borderColor.copy(0f)
        else if (isHovered || isSelected) theme.borderColor
        else theme.borderColor.copy(0f)
    )

    Row(
        modifier = Modifier.size(SidebarEntryWidth, 33.dp)
            .glassHover(theme.sideBarNavigationEntryShape, pull = 0.75.dp)
            .graphicsLayer {}
            .animatedFrame(
                theme.sideBarNavigationEntryShape,
                fill = { if (translucent) theme.textColor.copy(alpha = 0.12f * selectedAlpha.value) else Color.Transparent },
                accentAlpha = { if (translucent) 0f else selectedAlpha.value },
                border = { borderColor.value },
            )
            .then(if (translucent) Modifier.liquidGlass(theme.sideBarNavigationEntryShape) { selectedAlpha.value } else Modifier)
            .onClick(interactionSource) { onSelect(route) }
            .pointerHoverIcon(PointerIcon.Hand),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, modifier = Modifier.padding(start = 12.dp), fitAspectRatio = true, tint = { textAndIconColor.value })
        Text(title, color = Color.Unspecified, colorProducer = { textAndIconColor.value })
    }
}

@Composable
private fun NotificationBell() {
    var expanded by remember { mutableStateOf(false) }
    val theme = LocalTheme.current
    val close: () -> Unit = {
        expanded = false
        NotificationsManager.markAllRead()
    }

    Box(
        modifier = Modifier.oneConfigClickable(
            translated("oneconfig.sidebar.notifications", "Notifications"),
            {
                expanded = !expanded
                if (!expanded) NotificationsManager.markAllRead()
            },
            indication = false,
        ),
    ) {
        IconWithIndicator(
            iconName = "bell",
            color = theme.textColor,
            modifier = Modifier.size(18.dp),
            showIndicator = NotificationsManager.unreadCount > 0,
        )

        if (expanded) {
            EscapeCloses(close)
            ShellPopup(
                popupPositionProvider = rememberAnchoredPosition(Placement.AboveStart, gap = PopupGap),
                onDismissRequest = close,
                properties = PopupProperties(focusable = true),
            ) {
                NotificationsCenterPanel(Modifier.shellTransition())
            }
        }
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
                PlayerHead(Modifier.size(32.dp).offset(x = 2.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val session = LocalShell.current.session
                    Text(session.playerName, color = LocalTheme.current.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        session.versionLabel.ifBlank { "OneConfig" },
                        color = LocalTheme.current.textColorSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(start = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
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
    val hover = rememberHoverState()
    val theme = LocalTheme.current
    val controller = LocalShellNavController.current
    val shell = LocalShell.current
    val entry by controller.currentBackStackEntryAsState()
    val pages = registeredPages().filter { it.inAboutMenu && it.showInSidebar }
    val active = pages.any { entry.isOn(pageRoute(it)) }
    val label = translated("oneconfig.sidebar.about", "Changelog and credits")

    TooltipBox(if (expanded) null else label, placement = TooltipPlacement.AboveCenter) {
        Box(Modifier.oneConfigClickable(label, { expanded = !expanded }, indication = false, interactionSource = hover.interactionSource)) {
            Icon("info", modifier = Modifier.size(18.dp), color = if (hover.hovered || expanded || active) theme.textColor else theme.textColor.copy(0.8f))

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
                    )
                },
                placement = Placement.AboveStart,
            )
        }
    }
}

private val PopupGap = 12.dp
