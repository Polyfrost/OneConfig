package org.polyfrost.oneconfig.api.ui.v1.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.polyfrost.oneconfig.internal.ui.components.NotificationsCenter as InternalNotificationsCenter

class OneConfigUIFeedbackBridgeImpl : OneConfigUIFeedbackBridge {
    @Composable
    override fun NotificationsCenter(modifier: Modifier) {
        Box(modifier) { InternalNotificationsCenter() }
    }
}
