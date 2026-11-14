package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import org.polyfrost.oneconfig.api.notifications.v1.Notification
import org.polyfrost.oneconfig.api.notifications.v1.NotificationAction
import org.polyfrost.oneconfig.api.notifications.v1.NotificationType
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.ControlStyle
import org.polyfrost.oneconfig.api.ui.v1.components.Divider
import org.polyfrost.oneconfig.api.ui.v1.components.MenuAction
import org.polyfrost.oneconfig.api.ui.v1.components.MenuEntry
import org.polyfrost.oneconfig.api.ui.v1.components.MenuSeparator
import org.polyfrost.oneconfig.api.ui.v1.components.ProgressBar
import org.polyfrost.oneconfig.api.ui.v1.components.ScrollContainer
import org.polyfrost.oneconfig.api.ui.v1.components.SurfaceKind
import org.polyfrost.oneconfig.api.ui.v1.components.TextButton
import org.polyfrost.oneconfig.api.ui.v1.components.contextMenu
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.api.ui.v1.components.themedSurface
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.utils.v1.ClipboardHelper
import org.polyfrost.oneconfig.utils.v1.Formatters

private val PanelWidth = 368.dp
private val ButtonShape @Composable get() = LocalTheme.current.buttonShape

@Composable
fun NotificationsCenter() {
    val theme = LocalTheme.current
    val history = NotificationsManager.history

    Column(
        modifier = Modifier
            .width(PanelWidth)
            .themedSurface(SurfaceKind.Popup)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            translated("oneconfig.sidebar.notifications", "Notifications"),
            color = theme.textColor,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
        )

        if (history.isEmpty()) {
            Text(
                translated("oneconfig.notifications.empty", "You're all caught up."),
                color = theme.textColorSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            ScrollContainer(rememberScrollState(), Modifier.heightIn(max = 420.dp), fadingEdges = true) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    history.forEachIndexed { index, notification ->
                        NotificationRow(notification)
                        if (index != history.lastIndex) Divider()
                    }
                }
            }
        }

        Divider()

        Footer()
    }
}

@Composable
private fun NotificationRow(notification: Notification) {
    val theme = LocalTheme.current
    val accent = accentFor(notification.type)

    Row(
        modifier = Modifier.contextMenu { notificationMenu(notification) },
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(Modifier.size(32.dp).padding(4.dp), contentAlignment = Alignment.Center) {
            Icon(notification.iconName ?: notification.type.iconName, color = accent, modifier = Modifier.size(24.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notification.title,
                    color = accent,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Text(
                    Formatters.relativeTime(Instant.ofEpochMilli(notification.createdAt)),
                    color = theme.textColorSecondary,
                    fontSize = 10.sp,
                )
                if (!notification.read) {
                    Box(
                        Modifier
                            .padding(start = 6.dp)
                            .size(6.dp)
                            .background(Accent, theme.circleShape),
                    )
                }
            }
            if (notification.message.isNotEmpty()) {
                Text(
                    notification.message,
                    color = theme.textColorSecondary,
                    fontSize = 12.sp,
                    lineHeight = 21.sp,
                )
            }

            val progress = liveProgress(notification)
            if (progress != null) {
                ProgressBar(progress, Modifier.padding(top = 8.dp), ControlSize.Regular)
            }

            if (notification.actions.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    for (action in notification.actions) ActionButton(notification, action)
                }
            }
        }
    }
}

@Composable
private fun ActionButton(notification: Notification, action: NotificationAction) {
    TextButton(
        action.label,
        {
            action.onClick.run()
            if (action.dismissOnClick) NotificationsManager.remove(notification)
        },
        style = when {
            !action.primary -> ControlStyle.Secondary
            notification.type == NotificationType.ERROR -> ControlStyle.DangerFilled
            else -> ControlStyle.Primary
        },
        size = ControlSize.Compact,
        enabled = action.enabled,
    )
}

private fun notificationMenu(notification: Notification): List<MenuEntry> = listOf(
    MenuAction(translated("oneconfig.notifications.dismiss", "Dismiss"), { NotificationsManager.remove(notification) }, icon = "close"),
    MenuAction(
        translated("oneconfig.notifications.copy", "Copy text"),
        { ClipboardHelper.setString(listOf(notification.title, notification.message).filter { it.isNotEmpty() }.joinToString("\n")) },
        icon = "copy",
    ),
    MenuSeparator,
    MenuAction(
        translated("oneconfig.notifications.clear_all", "Clear all notifications"),
        { NotificationsManager.clearHistory() },
        icon = "trash",
        danger = true,
    ),
)

@Composable
private fun Footer() {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val isHovered by source.collectIsHoveredAsState()
    val color by Motion.animateColorAsState(if (isHovered) theme.textColor else theme.textColor.copy(0.85f))
    val label = translated("oneconfig.notifications.clear_all", "Clear all notifications")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(ButtonShape)
                .oneConfigClickable(label, { NotificationsManager.clearHistory() }, indication = false, interactionSource = source)
                .pointerHoverIcon(PointerIcon.Hand),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon("trash", color = color, modifier = Modifier.size(18.dp))
            Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun accentFor(type: NotificationType): Color = when (type) {
    NotificationType.INFO -> LocalTheme.current.textColor
    NotificationType.SUCCESS -> LocalTheme.current.success
    NotificationType.ERROR -> LocalTheme.current.error
    NotificationType.WARNING -> LocalTheme.current.warning
    NotificationType.PROGRESS -> Accent
}

@Composable
private fun liveProgress(notification: Notification): Float? {
    var frame by remember(notification.id) { mutableLongStateOf(0L) }
    val progress = frame.let { notification.progressOrNull() }
    if (progress != null && progress < 1f && notification.progress != null) {
        LaunchedEffect(notification.id) {
            while (true) withFrameNanos { frame = it }
        }
    }
    return progress
}
