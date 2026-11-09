package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.api.ui.v1.page.PageGroup
import org.polyfrost.oneconfig.internal.ui.api.Tooltip
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

    fun isCollapsed(title: String): Boolean = collapsed[title] == true

    fun toggle(title: String) {
        collapsed[title] = !isCollapsed(title)
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
            Box(
                Modifier.fillMaxWidth().weight(1f, fill = false)
                    .onGloballyPositioned { overflow.viewportBottom = it.positionInRoot().y + it.size.height }
                    .verticalScroll(scroll),
                contentAlignment = Alignment.TopCenter,
            ) {
                CompositionLocalProvider(LocalNavOverflow provides overflow) {
                    Box(Modifier.padding(vertical = 4.dp)) { NavigationEntries() }
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
                            .onClick(source) { scope.launch { scroll.animateScrollTo(scroll.maxValue) } }
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

private class NavOverflow {
    val centers = mutableStateMapOf<Any, Float>()
    var viewportBottom by mutableFloatStateOf(0f)

    fun hidden(): Int = centers.values.count { it > viewportBottom }
}

private val LocalNavOverflow = staticCompositionLocalOf<NavOverflow?> { null }

@Composable
private fun TrackOverflow(key: Any, content: @Composable () -> Unit) {
    val overflow = LocalNavOverflow.current
    if (overflow == null) {
        content()
        return
    }
    DisposableEffect(key) { onDispose { overflow.centers.remove(key) } }
    Box(Modifier.onGloballyPositioned { overflow.centers[key] = it.positionInRoot().y + it.size.height / 2f }) { content() }
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
            val title = it.title

            NavigationSection(title, collapsed = SidebarCollapse.isCollapsed(title), onToggle = { SidebarCollapse.toggle(title) }) {
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
private fun NavigationSection(title: String? = null, collapsed: Boolean? = null, onToggle: () -> Unit = {}, content: @Composable () -> Unit) {
    if (collapsed != null && title != null) {
        CollapsibleSection(title, collapsed, onToggle, content)
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
private fun CollapsibleSection(title: String, collapsed: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    val chevronRotation by animateFloatAsState(if (collapsed) 90f else 180f)
    val theme = LocalTheme.current
    Column(horizontalAlignment = Alignment.Start) {
        Row(
            Modifier.width(225.dp).padding(start = 6.dp, end = 6.dp, bottom = 2.dp)
                .onClick(source) { onToggle() }
                .pointerHoverIcon(PointerIcon.Hand),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title.uppercase(),
                color = if (hovered) theme.textColor else theme.textColor.copy(0.8f),
                fontWeight = FontWeight.Medium, fontSize = 11.sp
            )
            Icon("up", color = theme.textColorSecondary, modifier = Modifier.size(12.dp).rotate(chevronRotation))
        }
        AnimatedVisibility(
            visible = !collapsed,
            enter = ReducedMotion.enter(expandVertically()),
            exit = ReducedMotion.exit(shrinkVertically()),
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
    val selectedAlpha = animateFloatAsState(if (isSelected) 1f else 0f)
    val textAndIconColor = animateColorAsState(
        if (isSelected && translucent) theme.textColor
        else if (isSelected) theme.accentTextColor
        else if (isHovered) theme.textColor
        else theme.textColor.copy(0.8f)
    )
    val borderColor = animateColorAsState(
        if (isSelected && translucent) theme.borderColor.copy(0f)
        else if (isHovered || isSelected) theme.borderColor
        else theme.borderColor.copy(0f)
    )

    Row(
        modifier = Modifier.size(225.dp, 33.dp)
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
    var bellHeightPx by remember { mutableStateOf(0) }
    val interactionSource = rememberInteractionSource()

    Box(
        modifier = Modifier
            .onSizeChanged { bellHeightPx = it.height }
            .onClick(interactionSource) {
                expanded = !expanded
                if (!expanded) NotificationsManager.markAllRead()
            }
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        IconWithIndicator(
            iconName = "bell",
            color = LocalTheme.current.textColor,
            modifier = Modifier.size(18.dp),
            showIndicator = NotificationsManager.unreadCount > 0,
        )

        if (expanded) {
            ShellPopup(
                // open up and to the right of the bell
                alignment = Alignment.BottomStart,
                offset = IntOffset(0, -(bellHeightPx + 12)),
                onDismissRequest = {
                    expanded = false
                    NotificationsManager.markAllRead()
                },
                properties = PopupProperties(focusable = true),
            ) {
                Box(Modifier.shellTransition()) { NotificationsCenter() }
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerHead(Modifier.size(32.dp).offset(x = 2.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val session = LocalShell.current.session
                    Text(session.playerName, color = LocalTheme.current.textColor)
                    Text(
                        session.versionLabel.ifBlank { "OneConfig" },
                        color = LocalTheme.current.textColorSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                AboutButton()
                NotificationBell()
            }
        }
    }
}

@Composable
private fun AboutButton() {
    var expanded by remember { mutableStateOf(false) }
    var heightPx by remember { mutableStateOf(0) }
    val interactionSource = rememberInteractionSource()
    val hovered by interactionSource.collectIsHoveredAsState()
    val theme = LocalTheme.current
    val controller = LocalShellNavController.current
    val shell = LocalShell.current
    val entry by controller.currentBackStackEntryAsState()

    Tooltip(
        text = { Text(translated("oneconfig.sidebar.about", "Changelog and credits"), color = theme.textColor, fontSize = 12.sp) },
        modifier = Modifier,
        anchor = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .onSizeChanged { heightPx = it.height }
                .onClick(interactionSource) { expanded = !expanded }
                .pointerHoverIcon(PointerIcon.Hand)
        ) {
            Icon("info", modifier = Modifier.size(18.dp), color = if (hovered || expanded) theme.textColor else theme.textColor.copy(0.8f))

            if (expanded) {
                ShellPopup(
                    alignment = Alignment.BottomStart,
                    offset = IntOffset(0, -(heightPx + 12)),
                    onDismissRequest = { expanded = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    SidebarMenu {
                        registeredPages().filter { it.group == PageGroup.ONECONFIG && it.showInSidebar }.forEach { page ->
                            val route = pageRoute(page)
                            NavigationEntry(page.icon, page.title.resolve(), route, entry.isOn(route)) {
                                expanded = false
                                shell.navigation.navigate(route)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarMenu(content: @Composable () -> Unit) {
    val theme = LocalTheme.current
    Column(
        Modifier.shellTransition()
            .liquidGlass(theme.popupShape, frosted = true)
            .background(theme.popupBackground, theme.popupShape)
            .border(1.dp, theme.borderColor, theme.popupShape)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}
