package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import org.polyfrost.oneconfig.internal.ui.navigation.NavigationGroups
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.withOpacityPercent
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import java.util.Locale
import java.util.Locale.getDefault

@Composable
fun Sidebar() {
    val flipTopOrder = ShellState.flipTopOptionOrder
    Column(
        modifier = Modifier.fillMaxHeight()
            .width(264.dp)
            .background(LocalTheme.current.sidebarBackground.withOpacityPercent(ShellState.sidebarOpacity))
            .rightBorder(LocalTheme.current.borderColor, 1f),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Navigation(showTopOptions = !flipTopOrder)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (flipTopOrder) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TopOptionsSection()
                }
            }
            Account()
        }
    }
}

@Composable
private fun Navigation(showTopOptions: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(top = 28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Branding()
        NavigationEntries(showTopOptions)
    }
}

@Composable
private fun Branding() {
    val logoPath = LocalTheme.current.branding.logoPath
    val logo = rememberSvgResourcePainter(logoPath) ?: return
    Image(
        painter = logo,
        contentDescription = null,
        colorFilter = rememberBrandTint(logoPath, Accent),
        modifier = Modifier.size(167.dp, 19.dp)
    )
}

@Composable
private fun TopOptionsSection() {
    NavigationSection {
        NavigationEntry(
            "hud",
            "Edit HUD",
        ) {
            HudManager.openEditor()
        }
    }
}

@Composable
private fun NavigationEntries(showTopOptions: Boolean) {
    val controller = LocalNavController.current

    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showTopOptions) {
            TopOptionsSection()
        }

        NavigationGroups.forEach {
            val entry by controller.currentBackStackEntryAsState()
            val currentDestination = entry?.destination

            NavigationSection(it.id) {
                it.routes.forEach { def ->
                    val selected = currentDestination?.hierarchy?.any { arche ->
                        arche.hasRoute(def.route::class)
                    } ?: false

                    if (selected) {
                        ShellState.title = def.id.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(getDefault()) else char.toString() }
                    }

                    // todo: i18n
                    NavigationEntry(
                        def.icon,
                        def.id.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(getDefault()) else char.toString() },
                        def.route,
                        selected
                    ) {
                        LocalNavController.wrapper.navigate(def.route)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationSection(title: String? = null, content: @Composable () -> Unit) {
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
private fun NavigationEntry(icon: String, title: String, route: Any? = null, isSelected: Boolean = false, onSelect: (Any?) -> Unit) {
    val interactionSource = rememberInteractionSource()

    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor by animateColorAsState(
        if (isSelected) Accent
        else Accent.copy(0f)
    )
    val textAndIconColor by animateColorAsState(
        if (isSelected) LocalTheme.current.accentTextColor
        else if (isHovered) LocalTheme.current.textColor
        else LocalTheme.current.textColor.copy(0.8f)
    )
    val borderColor by animateColorAsState(
        if (isHovered || isSelected) LocalTheme.current.borderColor
        else LocalTheme.current.borderColor.copy(0f)
    )

    Row(
        modifier = Modifier.size(225.dp, 33.dp)
            .background(backgroundColor, LocalTheme.current.sideBarNavigationEntryShape)
            .border(1.dp, borderColor, LocalTheme.current.sideBarNavigationEntryShape)
            .onClick(interactionSource) { onSelect(route) }
            .pointerHoverIcon(PointerIcon.Hand),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, modifier = Modifier.padding(start = 12.dp), color = textAndIconColor, fitAspectRatio = true)
        Text(title, color = textAndIconColor)
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
            Popup(
                // open up and to the right of the bell
                alignment = Alignment.BottomStart,
                offset = IntOffset(0, -(bellHeightPx + 12)),
                onDismissRequest = {
                    expanded = false
                    NotificationsManager.markAllRead()
                },
                properties = PopupProperties(focusable = true),
            ) {
                NotificationsCenter()
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
                PlayerHead(Modifier.size(32.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(ShellState.playerName, color = LocalTheme.current.textColor)
                    Text(
                        ShellState.versionLabel.ifBlank { "OneConfig" },
                        color = LocalTheme.current.textColorSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            NotificationBell()
        }
    }
}
