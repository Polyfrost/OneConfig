package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.notifications.v1.Notification
import org.polyfrost.oneconfig.api.notifications.v1.NotificationAction
import org.polyfrost.oneconfig.api.notifications.v1.NotificationType
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val PanelWidth = 368.dp
private val PanelShape = RoundedCornerShape(12.dp)
private val ButtonShape = RoundedCornerShape(4.dp)

private val SuccessColor = Color(0xFF3CAF77)
private val ErrorColor = Color(0xFFF2545A)

@Composable
fun NotificationsCenter() {
    val theme = LocalTheme.current
    val history = NotificationsManager.history
    val panelBg = theme.popupBackground.copy(alpha = 0.8f)

    Column(
        modifier = Modifier
            .width(PanelWidth)
            .clip(PanelShape)
            .background(panelBg, PanelShape)
            .border(1.dp, theme.borderColor, PanelShape)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Notifications",
            color = theme.textColor,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
        )

        if (history.isEmpty()) {
            Text(
                "You're all caught up.",
                color = theme.textColorSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .fadingEdges(scrollState, panelBg)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                history.forEachIndexed { index, notification ->
                    NotificationRow(notification)
                    if (index != history.lastIndex) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(theme.borderColor),
                        )
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(theme.borderColor))

        Footer()
    }
}

@Composable
private fun NotificationRow(notification: Notification) {
    val theme = LocalTheme.current
    val accent = accentFor(notification.type)

    Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        Box(Modifier.size(32.dp).padding(4.dp), contentAlignment = Alignment.Center) {
            Icon(notification.type.iconName, color = accent, modifier = Modifier.size(24.dp))
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
                    relativeTime(notification.createdAt),
                    color = theme.textColorSecondary,
                    fontSize = 10.sp,
                )
                if (!notification.read) {
                    Box(
                        Modifier
                            .padding(start = 6.dp)
                            .size(6.dp)
                            .background(Accent, CircleShape),
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

            val progress = notification.progressOrNull()
            if (progress != null) {
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Accent.copy(0.5f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Accent),
                    )
                }
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
    val source = rememberInteractionSource()
    val isHovered by source.collectIsHoveredAsState()

    val base = if (action.primary) Accent else Color.White.copy(0.08f)
    val background by animateColorAsState(if (isHovered) Color.White.copy(0.12f).compositeOver(base) else base)

    Box(
        modifier = Modifier
            .clip(ButtonShape)
            .background(background, ButtonShape)
            .onClick(source) {
                action.onClick.run()
                NotificationsManager.remove(notification)
            }
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            action.label,
            color = Color.White.copy(0.9f),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun Footer() {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val isHovered by source.collectIsHoveredAsState()
    val color by animateColorAsState(if (isHovered) theme.textColor else theme.textColor.copy(0.85f))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(ButtonShape)
                .onClick(source) { NotificationsManager.clearHistory() }
                .pointerHoverIcon(PointerIcon.Hand),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon("trash", color = color, modifier = Modifier.size(18.dp))
            Text("Clear all notifications", color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun accentFor(type: NotificationType): Color = when (type) {
    NotificationType.INFO -> LocalTheme.current.textColor
    NotificationType.SUCCESS -> SuccessColor
    NotificationType.ERROR -> ErrorColor
    NotificationType.PROGRESS -> Accent
}

private fun relativeTime(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    val seconds = ((now - createdAt) / 1000L).coerceAtLeast(0L)
    return when {
        seconds < 5 -> "Just now"
        seconds < 60 -> "${seconds}s ago"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86400 -> "${seconds / 3600}h ago"
        else -> "${seconds / 86400}d ago"
    }
}
