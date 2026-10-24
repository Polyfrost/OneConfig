package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.polyfrost.compose.animation.animateColorAsState
import org.polyfrost.compose.foundation.background
import org.polyfrost.compose.foundation.border
import org.polyfrost.compose.foundation.interaction.collectIsHoveredAsState
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.fillMaxWidth
import org.polyfrost.compose.foundation.layout.height
import org.polyfrost.compose.foundation.layout.heightIn
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.foundation.layout.weight
import org.polyfrost.compose.foundation.layout.width
import org.polyfrost.compose.foundation.rememberScrollState
import org.polyfrost.compose.foundation.verticalScroll
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.input.pointer.PointerIcon
import org.polyfrost.compose.ui.input.pointer.pointerHoverIcon
import org.polyfrost.compose.ui.layout.onPlaced
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.notifications.v1.Notification
import org.polyfrost.oneconfig.api.notifications.v1.NotificationAction
import org.polyfrost.oneconfig.api.notifications.v1.NotificationType
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.internal.ui.poly.FadingEdges
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val PanelWidth = 368.dp
private val PanelShape @Composable get() = LocalTheme.current.popupShape
private val ButtonShape @Composable get() = LocalTheme.current.buttonShape

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
            var listHeight by remember { mutableFloatStateOf(0f) }
            Box(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(scrollState)
                        .onPlaced { listHeight = it.height },
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
                FadingEdges(scrollState, panelBg, Modifier.fillMaxWidth().height(listHeight.dp))
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

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        Box(Modifier.size(32.dp).padding(4.dp), contentAlignment = Alignment.Center) {
            Icon(notification.type.iconName, color = accent, modifier = Modifier.size(24.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notification.title,
                    color = accent,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    softWrap = true,
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
                    modifier = Modifier.fillMaxWidth(),
                    softWrap = true,
                )
            }

            val progress = notification.progressOrNull()
            if (progress != null) {
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Accent.copy(0.5f), theme.circleShape),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .background(Accent, theme.circleShape),
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
    val hover = if (action.primary) base.lerp(Color.White, 0.12f) else Color.White.copy(0.19f)
    val background = animateColorAsState(if (isHovered) hover else base)

    Box(
        modifier = Modifier
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
    val color = animateColorAsState(if (isHovered) theme.textColor else theme.textColor.copy(0.85f))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
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
