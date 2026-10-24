package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.animation.core.animateFloatAsState
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.align
import org.polyfrost.compose.foundation.layout.fillMaxHeight
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.offset
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.dropShadow
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.graphics.graphicsLayer
import org.polyfrost.compose.ui.graphics.shadow.Shadow
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.unit.DpOffset
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.compose.ui.window.Popup
import org.polyfrost.compose.ui.window.PopupProperties
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.internal.ui.navigation.isOn
import org.polyfrost.oneconfig.internal.ui.navigation.sidebarSections
import org.polyfrost.oneconfig.internal.ui.poly.OpenTransition
import org.polyfrost.oneconfig.internal.ui.poly.SvgImage
import org.polyfrost.oneconfig.internal.ui.shell.ShellHost
import org.polyfrost.oneconfig.internal.ui.shell.ShellNav
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.AccentBrush
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.withOpacityPercent

internal val SidebarWidth = 264.dp

@Composable
fun Sidebar(nav: ShellNav, host: ShellHost, clearSearch: () -> Unit) {
    val flipTopOrder = ShellState.flipTopOptionOrder
    Row(
        modifier = Modifier.fillMaxHeight()
            .width(SidebarWidth)
            .background(LocalTheme.current.sidebarBackground.withOpacityPercent(ShellState.sidebarOpacity)),
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Navigation(nav, host, clearSearch, showTopOptions = !flipTopOrder)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (flipTopOrder) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TopOptionsSection(host)
                    }
                }
                Account()
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(LocalTheme.current.borderColor))
    }
}

@Composable
private fun Navigation(nav: ShellNav, host: ShellHost, clearSearch: () -> Unit, showTopOptions: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(top = 28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Branding()
        NavigationEntries(nav, host, clearSearch, showTopOptions)
    }
}

@Composable
private fun Branding() {
    val logoPath = LocalTheme.current.logoPath
    val tint = rememberAccentBrandTint(logoPath)
    SvgImage(logoPath, 167f, 19f, LocalDensity.current.density, recolor = tint)
}

@Composable
private fun TopOptionsSection(host: ShellHost) {
    NavigationSection {
        NavigationEntry(
            "hud",
            translated("oneconfig.sidebar.edit_hud", "Edit HUD"),
        ) {
            host.editHud()
        }
    }
}

@Composable
private fun NavigationEntries(nav: ShellNav, host: ShellHost, clearSearch: () -> Unit, showTopOptions: Boolean) {
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showTopOptions) {
            TopOptionsSection(host)
        }

        sidebarSections().forEach {
            NavigationSection(it.title) {
                it.entries.forEach { def ->
                    val selected = nav.isOn(def.route, def.arg)
                    NavigationEntry(def.icon, def.label, selected) {
                        if (selected && nav.current == def.route) clearSearch() else nav.navigate(def.route, def.arg)
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
private fun NavigationEntry(icon: String, title: String, isSelected: Boolean = false, onSelect: () -> Unit) {
    val interactionSource = rememberInteractionSource()

    val isHovered by interactionSource.collectIsHoveredAsState()

    val selectedAlpha by animateFloatAsState(if (isSelected) 1f else 0f)
    val textAndIconColor = animateColorAsState(
        if (isSelected) LocalTheme.current.accentTextColor
        else if (isHovered) LocalTheme.current.textColor
        else LocalTheme.current.textColor.copy(0.8f)
    )
    val borderColor = animateColorAsState(
        if (isHovered || isSelected) LocalTheme.current.borderColor
        else Color.Transparent
    )

    Row(
        modifier = Modifier.size(225.dp, 33.dp)
            .background(AccentBrush, LocalTheme.current.sideBarNavigationEntryShape, selectedAlpha)
            .border(1.dp, borderColor, LocalTheme.current.sideBarNavigationEntryShape)
            .onClick(interactionSource) { onSelect() }
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
    val interactionSource = rememberInteractionSource()

    Box(
        modifier = Modifier
            .onClick(interactionSource) {
                expanded = !expanded
                if (!expanded) NotificationsManager.markAllRead()
            }
            .pointerHoverIcon(PointerIcon.Hand)
    ) {
        Icon("bell", color = LocalTheme.current.textColor, modifier = Modifier.size(18.dp))
        if (NotificationsManager.unreadCount > 0) {
            val circle = LocalTheme.current.circleShape
            Box(
                Modifier.size(8.dp).align(Alignment.TopEnd).graphicsLayer(translationX = 2f, translationY = -2f)
                    .dropShadow(circle, Shadow(radius = 7.dp, color = Accent))
            ) {
                Box(Modifier.size(6.dp).align(Alignment.Center).background(Accent, circle))
            }
        }

        if (expanded) {
            Popup(
                // open up and to the right of the bell
                alignment = Alignment.BottomStart,
                offset = DpOffset(0.dp, (-12).dp),
                onDismissRequest = {
                    expanded = false
                    NotificationsManager.markAllRead()
                },
                properties = PopupProperties(focusable = true),
                anchor = Alignment.TopStart,
            ) {
                OpenTransition { NotificationsCenter() }
            }
        }
    }
}

@Composable
private fun Account() {
    Column(
        modifier = Modifier.fillMaxWidth().height(73.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(LocalTheme.current.borderColor))
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
                    Text(ShellState.playerName, color = LocalTheme.current.textColor)
                    Text(
                        ShellState.versionLabel.ifEmpty { "OneConfig" },
                        color = LocalTheme.current.textColorSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            NotificationBell()
        }
    }
}
